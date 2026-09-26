package com.ferhat.gymitav.model

/** Returns the next elapsed-time boundary at which the Android reminder runner should wake. */
object RestReminderPlan {
    fun nextReminderAtSeconds(
        isRunning: Boolean,
        elapsedSeconds: Long,
        restLimitSeconds: Int,
        overdueIntervalSeconds: Int,
        restLimitAlreadyNotified: Boolean,
        nextOverdueAtSeconds: Long?
    ): Long? {
        if (!isRunning) return null
        if (elapsedSeconds < restLimitSeconds) return restLimitSeconds.toLong()
        if (!restLimitAlreadyNotified) return restLimitSeconds.toLong()
        if (overdueIntervalSeconds <= 0) return null

        nextOverdueAtSeconds?.takeIf { it > elapsedSeconds }?.let { return it }
        val overdueBy = (elapsedSeconds - restLimitSeconds).coerceAtLeast(0L)
        return restLimitSeconds + (overdueBy / overdueIntervalSeconds + 1L) * overdueIntervalSeconds
    }
}
