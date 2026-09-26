package com.ferhat.gymitav.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferhat.gymitav.model.WorkoutState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class GymViewModel : ViewModel() {
    var state by mutableStateOf(WorkoutState())
        private set

    private var accumulatedMillis = 0L
    private var runningSinceMillis: Long? = null

    init {
        viewModelScope.launch {
            while (isActive) {
                publishElapsedTime()
                delay(250L)
            }
        }
    }

    fun toggleTimer() {
        if (state.isRunning) {
            accumulatedMillis = currentElapsedMillis()
            runningSinceMillis = null
            state = state.copy(isRunning = false, elapsedSeconds = accumulatedMillis / 1_000L)
        } else {
            runningSinceMillis = SystemClock.elapsedRealtime()
            state = state.copy(isRunning = true)
        }
    }

    fun resetTimer() {
        accumulatedMillis = 0L
        runningSinceMillis = null
        state = state.copy(isRunning = false, elapsedSeconds = 0L)
    }

    fun completeSet() {
        val completed = state.completedSets + 1
        if (completed >= state.targetSets) {
            state = state.copy(exercise = state.exercise + 1, completedSets = 0, targetSets = state.defaultSets)
        } else {
            state = state.copy(completedSets = completed)
        }
        resetTimer()
    }

    fun increaseTargetSets() {
        state = state.copy(targetSets = (state.targetSets + 1).coerceAtMost(99))
    }

    fun adjustDefaultSets(delta: Int) {
        val updated = (state.defaultSets + delta).coerceIn(1, 99)
        state = state.copy(defaultSets = updated)
    }

    fun adjustRestLimit(deltaSeconds: Int) {
        state = state.copy(restLimitSeconds = (state.restLimitSeconds + deltaSeconds).coerceIn(10, 3_600))
    }

    private fun publishElapsedTime() {
        if (!state.isRunning) return
        val seconds = currentElapsedMillis() / 1_000L
        if (seconds != state.elapsedSeconds) state = state.copy(elapsedSeconds = seconds)
    }

    private fun currentElapsedMillis(): Long = accumulatedMillis +
        (runningSinceMillis?.let { SystemClock.elapsedRealtime() - it } ?: 0L)
}
