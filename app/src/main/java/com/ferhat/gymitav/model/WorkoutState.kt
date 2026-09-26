package com.ferhat.gymitav.model

data class WorkoutState(
    val exercise: Int = 1,
    val completedSets: Int = 0,
    val targetSets: Int = 3,
    val defaultSets: Int = 3,
    val restLimitSeconds: Int = 120,
    val overdueReminderSeconds: Int = 10,
    val elapsedSeconds: Long = 0,
    val isRunning: Boolean = false
) {
    init {
        require(restLimitSeconds in MIN_REST_LIMIT_SECONDS..MAX_REST_LIMIT_SECONDS)
        require(restLimitSeconds % REST_LIMIT_STEP_SECONDS == 0)
    }

    val isOverRestLimit: Boolean get() = elapsedSeconds >= restLimitSeconds

    companion object {
        const val REST_LIMIT_STEP_SECONDS = 15
        const val MIN_REST_LIMIT_SECONDS = 15
        const val MAX_REST_LIMIT_SECONDS = 3_600
    }
}

data class SessionUpdate(
    val state: WorkoutState,
    val notifications: List<RestNotification> = emptyList()
)

enum class RestNotification {
    REST_LIMIT_REACHED,
    OVERDUE_REMINDER
}
