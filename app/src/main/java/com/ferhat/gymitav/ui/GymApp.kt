package com.ferhat.gymitav.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferhat.gymitav.model.WorkoutAction
import com.ferhat.gymitav.viewmodel.GymViewModel

@Composable
fun GymApp(viewModel: GymViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showingSettings by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshElapsedTime()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler {
        if (showingSettings) {
            showingSettings = false
            viewModel.refreshElapsedTime()
        } else {
            viewModel.dispatch(WorkoutAction.ResetTimer)
        }
    }

    if (showingSettings) {
        SettingsScreen(
            state = state,
            onAction = viewModel::dispatch,
            onBack = {
                showingSettings = false
                viewModel.refreshElapsedTime()
            }
        )
    } else {
        TimerScreen(state = state) { action ->
            when (action) {
                MainScreenAction.TOGGLE_TIMER -> viewModel.dispatch(WorkoutAction.ToggleTimer)
                MainScreenAction.INCREASE_TARGET_SETS -> viewModel.dispatch(WorkoutAction.IncreaseTargetSets)
                MainScreenAction.OPEN_SETTINGS -> showingSettings = true
                MainScreenAction.COMPLETE_SET -> viewModel.dispatch(WorkoutAction.CompleteSet)
                MainScreenAction.RESET_TIMER -> viewModel.dispatch(WorkoutAction.ResetTimer)
            }
        }
    }
}
