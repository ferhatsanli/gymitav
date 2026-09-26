package com.ferhat.gymitav.background

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.ferhat.gymitav.MainActivity
import com.ferhat.gymitav.R
import com.ferhat.gymitav.model.RestReminderPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Publishes the active workout and runs rest haptics while the UI is inactive. */
class RestReminderService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var reminderJob: Job? = null
    private var wakeLockRenewalJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var ongoingActivity: OngoingActivity? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra(EXTRA_SESSION_ACTIVE, false) != true) {
            stopReminder()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        createNotificationChannel()
        stopReminder()
        val running = intent.getBooleanExtra(EXTRA_RUNNING, false)
        val exercise = intent.getIntExtra(EXTRA_EXERCISE, 1)
        val completedSets = intent.getIntExtra(EXTRA_COMPLETED_SETS, 0)
        val targetSets = intent.getIntExtra(EXTRA_TARGET_SETS, 3)
        updateWorkoutActivity(running, exercise, completedSets, targetSets)

        if (!running) return START_NOT_STICKY
        val elapsedAtUpdateMillis = intent.getLongExtra(EXTRA_ELAPSED_MILLIS, 0L)
        val startedAtElapsedRealtime = SystemClock.elapsedRealtime() - elapsedAtUpdateMillis
        val restLimit = intent.getIntExtra(EXTRA_REST_LIMIT, 120)
        val overdueInterval = intent.getIntExtra(EXTRA_OVERDUE_INTERVAL, 10)
        var limitNotified = intent.getBooleanExtra(EXTRA_LIMIT_NOTIFIED, false)
        var nextOverdueAtSeconds = intent.getLongExtra(EXTRA_NEXT_OVERDUE, NO_OVERDUE)
            .takeUnless { it == NO_OVERDUE }
        if (limitNotified && overdueInterval == 0) return START_NOT_STICKY

        holdCpuWhileRestTimerRuns()
        scheduleWakeLockRenewal()
        reminderJob = scope.launch {
            while (true) {
                val elapsedMillis = (SystemClock.elapsedRealtime() - startedAtElapsedRealtime).coerceAtLeast(0L)
                val elapsedSeconds = elapsedMillis / 1_000L
                val nextAtSeconds = RestReminderPlan.nextReminderAtSeconds(
                    isRunning = true,
                    elapsedSeconds = elapsedSeconds,
                    restLimitSeconds = restLimit,
                    overdueIntervalSeconds = overdueInterval,
                    restLimitAlreadyNotified = limitNotified,
                    nextOverdueAtSeconds = nextOverdueAtSeconds
                )
                if (nextAtSeconds == null) {
                    wakeLockRenewalJob?.cancel()
                    wakeLockRenewalJob = null
                    releaseCpuWakeLock()
                    break
                }

                val millisUntilNext = (nextAtSeconds * 1_000L - elapsedMillis).coerceAtLeast(1L)
                delay(millisUntilNext)

                val nowElapsedSeconds = (SystemClock.elapsedRealtime() - startedAtElapsedRealtime) / 1_000L
                if (!limitNotified && nowElapsedSeconds >= restLimit) {
                    vibrate()
                    limitNotified = true
                    nextOverdueAtSeconds = if (overdueInterval > 0) {
                        val overdueBy = (nowElapsedSeconds - restLimit).coerceAtLeast(0L)
                        restLimit + (overdueBy / overdueInterval + 1L) * overdueInterval
                    } else null
                } else if (nextOverdueAtSeconds != null && nowElapsedSeconds >= nextOverdueAtSeconds!!) {
                    vibrate()
                    val overdueBy = (nowElapsedSeconds - restLimit).coerceAtLeast(0L)
                    nextOverdueAtSeconds = restLimit + (overdueBy / overdueInterval + 1L) * overdueInterval
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopReminder()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        ongoingActivity = null
        scope.cancel()
        super.onDestroy()
    }

    private fun holdCpuWhileRestTimerRuns() {
        wakeLock = getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:RestTimer")
            ?.apply { setReferenceCounted(false); acquire(WAKE_LOCK_TIMEOUT_MILLIS) }
    }

    private fun scheduleWakeLockRenewal() {
        wakeLockRenewalJob?.cancel()
        wakeLockRenewalJob = scope.launch {
            while (true) {
                delay(WAKE_LOCK_RENEWAL_MILLIS)
                wakeLock?.let { lock ->
                    if (lock.isHeld) {
                        lock.release()
                        lock.acquire(WAKE_LOCK_TIMEOUT_MILLIS)
                    }
                }
            }
        }
    }

    private fun stopReminder() {
        reminderJob?.cancel()
        reminderJob = null
        wakeLockRenewalJob?.cancel()
        wakeLockRenewalJob = null
        releaseCpuWakeLock()
    }

    private fun releaseCpuWakeLock() {
        wakeLock?.let { lock -> if (lock.isHeld) lock.release() }
        wakeLock = null
    }

    private fun vibrate() {
        getSystemService(Vibrator::class.java)?.vibrate(
            VibrationEffect.createOneShot(HAPTIC_DURATION_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Rest timer", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows the active workout and rest-timer reminders"
                setShowBadge(false)
            }
        )
    }

    private fun buildWorkoutNotification(): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Gym IT")
            .setContentText("Workout session active · tap to return")
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(createActivityPendingIntent())
            .setOngoing(true)
    }

    private fun updateWorkoutActivity(running: Boolean, exercise: Int, completedSets: Int, targetSets: Int) {
        val notificationBuilder = buildWorkoutNotification()
        val permissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED && NotificationManagerCompat.from(this).areNotificationsEnabled()

        if (!permissionGranted) {
            ongoingActivity = null
            startForegroundForWorkout(notificationBuilder)
            return
        }

        val status = Status.Builder()
            .addTemplate("#exercise# · Set #sets#/#target# · #state#")
            .addPart("exercise", Status.TextPart("Exercise $exercise"))
            .addPart("sets", Status.TextPart(completedSets.toString()))
            .addPart("target", Status.TextPart(targetSets.toString()))
            .addPart("state", Status.TextPart(if (running) "Rest timer running" else "Paused"))
            .build()

        val existing = ongoingActivity
        if (existing != null) {
            existing.update(applicationContext, status)
        } else {
            val activity = OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, notificationBuilder)
                .setStaticIcon(R.drawable.ic_launcher_monochrome)
                .setTouchIntent(createActivityPendingIntent())
                .setStatus(status)
                .setTitle("Gym IT")
                .build()
            activity.apply(applicationContext)
            ongoingActivity = activity
            startForegroundForWorkout(notificationBuilder)
        }
    }

    private fun startForegroundForWorkout(notificationBuilder: NotificationCompat.Builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notificationBuilder.build(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    private fun createActivityPendingIntent(): PendingIntent {
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            this,
            ACTIVITY_PENDING_INTENT_REQUEST_CODE,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val EXTRA_RUNNING = "running"
        private const val EXTRA_SESSION_ACTIVE = "session_active"
        private const val EXTRA_ELAPSED_MILLIS = "elapsed_millis"
        private const val EXTRA_REST_LIMIT = "rest_limit"
        private const val EXTRA_OVERDUE_INTERVAL = "overdue_interval"
        private const val EXTRA_LIMIT_NOTIFIED = "limit_notified"
        private const val EXTRA_NEXT_OVERDUE = "next_overdue"
        private const val EXTRA_EXERCISE = "exercise"
        private const val EXTRA_COMPLETED_SETS = "completed_sets"
        private const val EXTRA_TARGET_SETS = "target_sets"
        private const val NO_OVERDUE = Long.MIN_VALUE
        private const val CHANNEL_ID = "rest_timer"
        private const val NOTIFICATION_ID = 73
        private const val ACTIVITY_PENDING_INTENT_REQUEST_CODE = 74
        private const val HAPTIC_DURATION_MILLIS = 140L
        private const val WAKE_LOCK_TIMEOUT_MILLIS = 4L * 60L * 60L * 1_000L
        private const val WAKE_LOCK_RENEWAL_MILLIS = 3L * 60L * 60L * 1_000L

        fun sync(context: Context, sessionActive: Boolean, running: Boolean, elapsedMillis: Long,
                 restLimitSeconds: Int, overdueIntervalSeconds: Int, limitNotified: Boolean,
                 nextOverdueAtSeconds: Long?, exercise: Int, completedSets: Int, targetSets: Int) {
            val intent = Intent(context, RestReminderService::class.java).apply {
                putExtra(EXTRA_SESSION_ACTIVE, sessionActive)
                putExtra(EXTRA_RUNNING, running)
                putExtra(EXTRA_ELAPSED_MILLIS, elapsedMillis)
                putExtra(EXTRA_REST_LIMIT, restLimitSeconds)
                putExtra(EXTRA_OVERDUE_INTERVAL, overdueIntervalSeconds)
                putExtra(EXTRA_LIMIT_NOTIFIED, limitNotified)
                putExtra(EXTRA_NEXT_OVERDUE, nextOverdueAtSeconds ?: NO_OVERDUE)
                putExtra(EXTRA_EXERCISE, exercise)
                putExtra(EXTRA_COMPLETED_SETS, completedSets)
                putExtra(EXTRA_TARGET_SETS, targetSets)
            }
            if (sessionActive) {
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } else {
                context.stopService(Intent(context, RestReminderService::class.java))
            }
        }
    }
}
