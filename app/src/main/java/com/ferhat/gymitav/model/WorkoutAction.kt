package com.ferhat.gymitav.model

sealed interface WorkoutAction {
    data object ToggleTimer : WorkoutAction
    data object ResetTimer : WorkoutAction
    data object CompleteSet : WorkoutAction
    data object IncreaseTargetSets : WorkoutAction
    data object ResetCurrentTargetToDefault : WorkoutAction
    data class AdjustExercise(val delta: Int) : WorkoutAction
    data class AdjustDefaultSets(val delta: Int) : WorkoutAction
    data class AdjustRestLimit(val deltaSeconds: Int) : WorkoutAction
    data class AdjustOverdueReminder(val deltaSeconds: Int) : WorkoutAction
}
