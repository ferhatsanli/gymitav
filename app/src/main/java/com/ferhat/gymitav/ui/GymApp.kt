package com.ferhat.gymitav.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.ferhat.gymitav.viewmodel.GymViewModel

@Composable
fun GymApp() {
    val context = LocalContext.current
    val owner = context as ViewModelStoreOwner
    val viewModel = remember(owner) { ViewModelProvider(owner)[GymViewModel::class.java] }
    var showingSettings by remember { mutableStateOf(false) }

    BackHandler {
        if (showingSettings) showingSettings = false else viewModel.resetTimer()
    }

    if (showingSettings) {
        SettingsScreen(
            state = viewModel.state,
            onAdjustRest = viewModel::adjustRestLimit,
            onAdjustSets = viewModel::adjustDefaultSets
        )
    } else {
        TimerScreen(
            state = viewModel.state,
            onToggleTimer = viewModel::toggleTimer,
            onIncreaseTarget = viewModel::increaseTargetSets,
            onCompleteSet = viewModel::completeSet,
            onOpenSettings = { showingSettings = true },
            onReset = viewModel::resetTimer
        )
    }
}
