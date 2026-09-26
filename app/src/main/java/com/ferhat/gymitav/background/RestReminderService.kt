package com.ferhat.gymitav.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.ServiceCompat
import com.ferhat.gymitav.R
import com.ferhat.gymitav.model.RestReminderPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Owns rest haptics while the UI is inactive. It waits for event boundaries instead of polling. */
class RestReminderService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var reminderJob: Job? = null
    private var wakeLockRenewalJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra(EXTRA_RUNNING, false) != true) {
            stopReminder()
            stopSelf(startId)
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        stopReminder()
        val elapsedAtUpdateMillis = intent.getLongExtra(EXTRA_ELAPSED_MILLIS, 0L)
        val startedAtElapsedRealtime = SystemClock.elapsedRealtime() - elapsedAtUpdateMillis
        val restLimit = intent.getIntExtra(EXTRA_REST_LIMIT, 120)
        val overdueInterval = intent.getIntExtra(EXTRA_OVERDUE_INTERVAL, 10)
        var limitNotified = intent.getBooleanExtra(EXTRA_LIMIT_NOTIFIED, false)
        var nextOverdueAtSeconds = intent.getLongExtra(EXTRA_NEXT_OVERDUE, NO_OVERDUE)
            .takeUnless { it == NO_OVERDUE }
        val currentStartId = startId

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
                    stopSelf(currentStartId)
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
                description = "Shown while the rest timer is running"
                setShowBadge(false)
            }
        )
    }

    private fun buildNotification(): Notification = Notification.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("Rest timer running")
        .setContentText("Workout reminders are active")
        .setOngoing(true)
        .setCategory(Notification.CATEGORY_STOPWATCH)
        .build()

    companion object {
        private const val EXTRA_RUNNING = "running"
        private const val EXTRA_ELAPSED_MILLIS = "elapsed_millis"
        private const val EXTRA_REST_LIMIT = "rest_limit"
        private const val EXTRA_OVERDUE_INTERVAL = "overdue_interval"
        private const val EXTRA_LIMIT_NOTIFIED = "limit_notified"
        private const val EXTRA_NEXT_OVERDUE = "next_overdue"
        private const val NO_OVERDUE = Long.MIN_VALUE
        private const val CHANNEL_ID = "rest_timer"
        private const val NOTIFICATION_ID = 73
        private const val HAPTIC_DURATION_MILLIS = 140L
        private const val WAKE_LOCK_TIMEOUT_MILLIS = 4L * 60L * 60L * 1_000L
        private const val WAKE_LOCK_RENEWAL_MILLIS = 3L * 60L * 60L * 1_000L

        fun sync(context: Context, running: Boolean, elapsedMillis: Long, restLimitSeconds: Int,
                 overdueIntervalSeconds: Int, limitNotified: Boolean, nextOverdueAtSeconds: Long?) {
            val intent = Intent(context, RestReminderService::class.java).apply {
                putExtra(EXTRA_RUNNING, running)
                putExtra(EXTRA_ELAPSED_MILLIS, elapsedMillis)
                putExtra(EXTRA_REST_LIMIT, restLimitSeconds)
                putExtra(EXTRA_OVERDUE_INTERVAL, overdueIntervalSeconds)
                putExtra(EXTRA_LIMIT_NOTIFIED, limitNotified)
                putExtra(EXTRA_NEXT_OVERDUE, nextOverdueAtSeconds ?: NO_OVERDUE)
            }
            if (running && (!limitNotified || overdueIntervalSeconds > 0)) {
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } else {
                context.stopService(Intent(context, RestReminderService::class.java))
            }
        }
    }
}
