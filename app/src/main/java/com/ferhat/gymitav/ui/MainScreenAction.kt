package com.ferhat.gymitav.ui

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

enum class MainScreenAction {
    TOGGLE_TIMER,
    INCREASE_TARGET_SETS,
    OPEN_SETTINGS,
    COMPLETE_SET,
    RESET_TIMER
}

fun resolveSwipeAction(dx: Float, dy: Float, thresholdPx: Float): MainScreenAction? {
    if (hypot(dx.toDouble(), dy.toDouble()) < thresholdPx) return null
    return when {
        abs(dy) >= abs(dx) * AXIS_DOMINANCE && dy > 0f -> MainScreenAction.INCREASE_TARGET_SETS
        abs(dy) >= abs(dx) * AXIS_DOMINANCE && dy < 0f -> MainScreenAction.COMPLETE_SET
        abs(dx) >= abs(dy) * AXIS_DOMINANCE && dx < 0f -> MainScreenAction.OPEN_SETTINGS
        abs(dx) >= abs(dy) * AXIS_DOMINANCE && dx > 0f -> MainScreenAction.RESET_TIMER
        else -> null
    }
}

fun resolveTapAction(x: Float, y: Float, centerX: Float, centerY: Float, sidePx: Float): MainScreenAction? {
    val dx = x - centerX
    val dy = y - centerY
    val radius = hypot(dx.toDouble(), dy.toDouble()).toFloat()
    return when {
        radius <= sidePx * INNER_RING_FRACTION -> MainScreenAction.TOGGLE_TIMER
        radius <= sidePx * OUTER_RING_FRACTION -> actionForAngle(atan2(dy, dx))
        else -> null
    }
}

private fun actionForAngle(radians: Float): MainScreenAction {
    val degrees = Math.toDegrees(radians.toDouble())
    return when {
        degrees >= -135 && degrees < -45 -> MainScreenAction.INCREASE_TARGET_SETS
        degrees >= -45 && degrees < 45 -> MainScreenAction.OPEN_SETTINGS
        degrees >= 45 && degrees < 135 -> MainScreenAction.COMPLETE_SET
        else -> MainScreenAction.RESET_TIMER
    }
}

private const val AXIS_DOMINANCE = 1.25f
private const val INNER_RING_FRACTION = 0.315f
private const val OUTER_RING_FRACTION = 0.49f
