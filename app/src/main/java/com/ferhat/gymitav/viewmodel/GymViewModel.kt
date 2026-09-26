package com.ferhat.gymitav.viewmodel

import android.os.SystemClock
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ferhat.gymitav.background.RestReminderService
import com.ferhat.gymitav.model.SessionUpdate
import com.ferhat.gymitav.model.WorkoutAction
import com.ferhat.gymitav.model.WorkoutSession
import com.ferhat.gymitav.model.WorkoutState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GymViewModel(application: Application) : AndroidViewModel(application) {
    private val session = WorkoutSession()
    private val _state = MutableStateFlow(session.state)
    val state: StateFlow<WorkoutState> = _state.asStateFlow()
    private val _settingsState = MutableStateFlow(session.state.toSettingsUiState())
    val settingsState: StateFlow<SettingsUiState> = _settingsState.asStateFlow()

    private var timerJob: Job? = null

    fun dispatch(action: WorkoutAction) {
        val now = SystemClock.elapsedRealtime()
        val update = when (action) {
            WorkoutAction.ToggleTimer -> session.toggleTimer(now)
            WorkoutAction.ResetTimer -> session.resetTimer()
            WorkoutAction.MainScreenBack -> session.handleMainScreenBack(now)
            WorkoutAction.CompleteSet -> session.completeSet(now)
            WorkoutAction.IncreaseTargetSets -> session.increaseTargetSets()
            WorkoutAction.ResetCurrentTargetToDefault -> session.resetCurrentTargetToDefault()
            is WorkoutAction.AdjustExercise -> session.adjustExercise(action.delta)
            is WorkoutAction.AdjustDefaultSets -> session.adjustDefaultSets(action.delta)
            is WorkoutAction.AdjustRestLimit -> session.adjustRestLimit(action.deltaSeconds, now)
            is WorkoutAction.AdjustOverdueReminder -> session.adjustOverdueReminder(action.deltaSeconds, now)
        }
        publish(update)
        syncWorkoutSessionService(now)

        if (session.state.isRunning) startTimerTicks() else stopTimerTicks()
    }

    /** Refreshes from elapsed real time when the activity returns from display sleep. */
    fun refreshElapsedTime() {
        val now = SystemClock.elapsedRealtime()
        if (session.state.isRunning) publish(session.refresh(now))
        syncWorkoutSessionService(now)
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
        val current = _settingsState.value
        val next = update.state
        if (current.exercise != next.exercise ||
            current.defaultSets != next.defaultSets ||
            current.restLimitSeconds != next.restLimitSeconds ||
            current.overdueReminderSeconds != next.overdueReminderSeconds
        ) {
            _settingsState.value = next.toSettingsUiState()
        }
    }

    private fun syncWorkoutSessionService(nowMillis: Long) {
        RestReminderService.sync(
            context = getApplication(),
            sessionActive = session.state.isWorkoutSessionActive,
            running = session.state.isRunning,
            elapsedMillis = session.elapsedMillisAt(nowMillis),
            restLimitSeconds = session.state.restLimitSeconds,
            overdueIntervalSeconds = session.state.overdueReminderSeconds,
            limitNotified = session.hasNotifiedRestLimit,
            nextOverdueAtSeconds = session.nextOverdueReminderAtSeconds,
            exercise = session.state.exercise,
            completedSets = session.state.completedSets,
            targetSets = session.state.targetSets
        )
    }

    private companion object {
        const val TIMER_DISPLAY_INTERVAL_MILLIS = 1_000L
    }
}

data class SettingsUiState(
    val exercise: Int,
    val defaultSets: Int,
    val restLimitSeconds: Int,
    val overdueReminderSeconds: Int
)

private fun WorkoutState.toSettingsUiState() = SettingsUiState(
    exercise = exercise,
    defaultSets = defaultSets,
    restLimitSeconds = restLimitSeconds,
    overdueReminderSeconds = overdueReminderSeconds
)
