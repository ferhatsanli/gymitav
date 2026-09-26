package com.ferhat.gymitav.model

/** Pure workout/timer rules. [nowMillis] values must come from a monotonic clock. */
class WorkoutSession(initialState: WorkoutState = WorkoutState()) {
    var state: WorkoutState = initialState
        private set

    private var accumulatedMillis = initialState.elapsedSeconds * 1_000L
    private var startedAtMillis: Long? = null
    private var restLimitNotified = false
    private var nextOverdueAtSeconds: Long? = null
    val hasNotifiedRestLimit: Boolean get() = restLimitNotified
    val nextOverdueReminderAtSeconds: Long? get() = nextOverdueAtSeconds
    fun elapsedMillisAt(nowMillis: Long): Long = elapsedMillis(nowMillis)

    fun toggleTimer(nowMillis: Long): SessionUpdate {
        if (state.isRunning) {
            val update = refresh(nowMillis)
            accumulatedMillis = elapsedMillis(nowMillis)
            startedAtMillis = null
            state = state.copy(isRunning = false, elapsedSeconds = accumulatedMillis / 1_000L)
            return update.copy(state = state)
        }

        startedAtMillis = nowMillis
        state = state.copy(isRunning = true, isWorkoutSessionActive = true)
        return SessionUpdate(state)
    }

    fun refresh(nowMillis: Long): SessionUpdate {
        if (!state.isRunning) return SessionUpdate(state)

        val elapsedMillis = elapsedMillis(nowMillis)
        val elapsedSeconds = elapsedMillis / 1_000L
        state = state.copy(elapsedSeconds = elapsedSeconds)
        val notifications = mutableListOf<RestNotification>()

        if (elapsedSeconds >= state.restLimitSeconds) {
            var limitReachedNow = false
            if (!restLimitNotified) {
                restLimitNotified = true
                limitReachedNow = true
                notifications += RestNotification.REST_LIMIT_REACHED
                nextOverdueAtSeconds = state.overdueReminderSeconds.takeIf { it > 0 }?.let { interval ->
                    val secondsPastLimit = (elapsedSeconds - state.restLimitSeconds).coerceAtLeast(0L)
                    state.restLimitSeconds + (secondsPastLimit / interval + 1L) * interval
                }
            }

            val nextOverdue = nextOverdueAtSeconds
            if (!limitReachedNow && nextOverdue != null && elapsedSeconds >= nextOverdue) {
                notifications += RestNotification.OVERDUE_REMINDER
                val interval = state.overdueReminderSeconds.toLong()
                nextOverdueAtSeconds = state.restLimitSeconds +
                    ((elapsedSeconds - state.restLimitSeconds) / interval + 1L) * interval
            }
        }

        return SessionUpdate(state, notifications)
    }

    fun resetTimer(): SessionUpdate {
        accumulatedMillis = 0L
        startedAtMillis = null
        restLimitNotified = false
        nextOverdueAtSeconds = null
        state = state.copy(elapsedSeconds = 0L, isRunning = false)
        return SessionUpdate(state)
    }

    /** Back resets a nonzero timer first; a second Back at zero ends the workout session. */
    fun handleMainScreenBack(nowMillis: Long): SessionUpdate {
        if (state.isRunning || elapsedMillis(nowMillis) > 0L) return resetTimer()
        return endWorkoutSession()
    }

    fun completeSet(nowMillis: Long): SessionUpdate {
        val notifications = refresh(nowMillis).notifications
        state = state.copy(isWorkoutSessionActive = true)
        val completed = state.completedSets + 1
        state = if (completed >= state.targetSets) {
            state.copy(
                exercise = state.exercise + 1,
                completedSets = 0,
                targetSets = state.defaultSets
            )
        } else {
            state.copy(completedSets = completed)
        }
        resetTimer()
        return SessionUpdate(state, notifications)
    }

    fun endWorkoutSession(): SessionUpdate {
        resetTimer()
        resetCurrentTargetToDefault()
        state = state.copy(
            exercise = 1,
            completedSets = 0,
            isWorkoutSessionActive = false
        )
        return SessionUpdate(state)
    }

    fun increaseTargetSets(): SessionUpdate {
        state = state.copy(
            targetSets = (state.targetSets + 1).coerceAtMost(MAX_SETS),
            isWorkoutSessionActive = true
        )
        return SessionUpdate(state)
    }

    fun resetCurrentTargetToDefault(): SessionUpdate {
        state = state.copy(targetSets = state.defaultSets)
        return SessionUpdate(state)
    }

    fun adjustExercise(delta: Int): SessionUpdate {
        state = state.copy(exercise = (state.exercise + delta).coerceIn(1, MAX_EXERCISE))
        return SessionUpdate(state)
    }

    fun adjustDefaultSets(delta: Int): SessionUpdate {
        state = state.copy(defaultSets = (state.defaultSets + delta).coerceIn(1, MAX_SETS))
        return SessionUpdate(state)
    }

    fun adjustRestLimit(deltaSeconds: Int, nowMillis: Long): SessionUpdate {
        val notifications = refresh(nowMillis).notifications
        val currentStep = state.restLimitSeconds / WorkoutState.REST_LIMIT_STEP_SECONDS
        val direction = deltaSeconds.compareTo(0)
        val nextStep = (currentStep + direction).coerceIn(
            WorkoutState.MIN_REST_LIMIT_SECONDS / WorkoutState.REST_LIMIT_STEP_SECONDS,
            WorkoutState.MAX_REST_LIMIT_SECONDS / WorkoutState.REST_LIMIT_STEP_SECONDS
        )
        state = state.copy(restLimitSeconds = nextStep * WorkoutState.REST_LIMIT_STEP_SECONDS)
        return SessionUpdate(state, notifications)
    }

    fun adjustOverdueReminder(deltaSeconds: Int, nowMillis: Long): SessionUpdate {
        val notifications = refresh(nowMillis).notifications
        val selectedIndex = REMINDER_INTERVALS.indexOf(state.overdueReminderSeconds).coerceAtLeast(0)
        val requestedIndex = selectedIndex + if (deltaSeconds < 0) -1 else 1
        val updatedInterval = REMINDER_INTERVALS[requestedIndex.coerceIn(0, REMINDER_INTERVALS.lastIndex)]
        state = state.copy(overdueReminderSeconds = updatedInterval)

        nextOverdueAtSeconds = when {
            !restLimitNotified || updatedInterval == 0 -> null
            else -> {
                val elapsedSinceLimit = (state.elapsedSeconds - state.restLimitSeconds).coerceAtLeast(0)
                state.restLimitSeconds + (elapsedSinceLimit / updatedInterval + 1) * updatedInterval
            }
        }
        return SessionUpdate(state, notifications)
    }

    private fun elapsedMillis(nowMillis: Long): Long = accumulatedMillis +
        (startedAtMillis?.let { (nowMillis - it).coerceAtLeast(0L) } ?: 0L)

    companion object {
        const val MAX_SETS = 99
        const val MAX_EXERCISE = 999
        val REMINDER_INTERVALS = listOf(0, 5, 10, 15, 20, 25, 30)
    }
}
