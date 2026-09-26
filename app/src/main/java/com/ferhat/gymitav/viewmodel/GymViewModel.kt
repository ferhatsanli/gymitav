package com.ferhat.gymitav.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferhat.gymitav.model.RestNotification
import com.ferhat.gymitav.model.SessionUpdate
import com.ferhat.gymitav.model.WorkoutAction
import com.ferhat.gymitav.model.WorkoutSession
import com.ferhat.gymitav.model.WorkoutState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GymViewModel : ViewModel() {
    private val session = WorkoutSession()
    private val _state = MutableStateFlow(session.state)
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    private val notificationChannel = Channel<RestNotification>(Channel.BUFFERED)
    val notifications = notificationChannel.receiveAsFlow()

    private var timerJob: Job? = null

    fun dispatch(action: WorkoutAction) {
        val now = SystemClock.elapsedRealtime()
        val update = when (action) {
            WorkoutAction.ToggleTimer -> session.toggleTimer(now)
            WorkoutAction.ResetTimer -> session.resetTimer()
            WorkoutAction.CompleteSet -> session.completeSet(now)
            WorkoutAction.IncreaseTargetSets -> session.increaseTargetSets()
            is WorkoutAction.AdjustExercise -> session.adjustExercise(action.delta)
            is WorkoutAction.AdjustDefaultSets -> session.adjustDefaultSets(action.delta)
            is WorkoutAction.AdjustRestLimit -> session.adjustRestLimit(action.deltaSeconds, now)
            is WorkoutAction.AdjustOverdueReminder -> session.adjustOverdueReminder(action.deltaSeconds, now)
        }
        publish(update)

        if (session.state.isRunning) startTimerTicks() else stopTimerTicks()
    }

    /** Refreshes from elapsed real time when the activity returns from display sleep. */
    fun refreshElapsedTime() {
        if (!session.state.isRunning) return
        publish(session.refresh(SystemClock.elapsedRealtime()))
    }

    private fun startTimerTicks() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (isActive && session.state.isRunning) {
                delay(TIMER_DISPLAY_INTERVAL_MILLIS)
                if (session.state.isRunning) {
                    publish(session.refresh(SystemClock.elapsedRealtime()))
                }
            }
        }
    }

    private fun stopTimerTicks() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun publish(update: SessionUpdate) {
        _state.value = update.state
        update.notifications.forEach(notificationChannel::trySend)
    }

    override fun onCleared() {
        notificationChannel.close()
    }

    private companion object {
        const val TIMER_DISPLAY_INTERVAL_MILLIS = 1_000L
    }
}
