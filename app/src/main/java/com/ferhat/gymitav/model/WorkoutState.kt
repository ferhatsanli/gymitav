package com.ferhat.gymitav.model

data class WorkoutState(
    val exercise: Int = 1,
    val completedSets: Int = 0,
    val targetSets: Int = 3,
    val defaultSets: Int = 3,
    val restLimitSeconds: Int = 120,
    val elapsedSeconds: Long = 0,
    val isRunning: Boolean = false
) {
    val isOverRestLimit: Boolean get() = elapsedSeconds >= restLimitSeconds
}
