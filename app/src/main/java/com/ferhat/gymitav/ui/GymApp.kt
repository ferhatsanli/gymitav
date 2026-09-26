package com.ferhat.gymitav.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferhat.gymitav.model.WorkoutAction
import com.ferhat.gymitav.viewmodel.GymViewModel

@Composable
fun GymApp(viewModel: GymViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var showingSettings by rememberSaveable { mutableStateOf(false) }
    val edgePulseProgress = remember { Animatable(0f) }
    val pulseAction = remember { mutableStateOf<MainScreenAction?>(null) }
    var pulseSequence by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshElapsedTime()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(pulseSequence) {
        if (pulseSequence == 0) return@LaunchedEffect
        edgePulseProgress.snapTo(0f)
        edgePulseProgress.animateTo(1f, tween(durationMillis = LED_ATTACK_MILLIS))
        edgePulseProgress.animateTo(0f, tween(durationMillis = LED_FADE_MILLIS))
    }

    val onMainAction: (MainScreenAction) -> Unit = { action ->
        if (action != MainScreenAction.TOGGLE_TIMER) {
            pulseAction.value = action
            pulseSequence += 1
        }
        when (action) {
            MainScreenAction.TOGGLE_TIMER -> viewModel.dispatch(WorkoutAction.ToggleTimer)
            MainScreenAction.INCREASE_TARGET_SETS -> viewModel.dispatch(WorkoutAction.IncreaseTargetSets)
            MainScreenAction.OPEN_SETTINGS -> showingSettings = true
            MainScreenAction.COMPLETE_SET -> viewModel.dispatch(WorkoutAction.CompleteSet)
            MainScreenAction.BACK -> viewModel.dispatch(WorkoutAction.MainScreenBack)
        }
    }

    BackHandler {
        if (showingSettings) {
            showingSettings = false
            viewModel.refreshElapsedTime()
        } else {
            onMainAction(MainScreenAction.BACK)
        }
    }

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        if (showingSettings) {
            SettingsContent(
                viewModel = viewModel,
                onBack = {
                    showingSettings = false
                    viewModel.refreshElapsedTime()
                }
            )
        } else {
            TimerContent(viewModel, onMainAction)
        }

        Canvas(Modifier.fillMaxSize()) {
            pulseAction.value?.let { action -> drawActionEdgePulse(action, edgePulseProgress.value) }
        }
    }
}

@Composable
private fun TimerContent(viewModel: GymViewModel, onAction: (MainScreenAction) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TimerScreen(state = state, onAction = onAction)
}

@Composable
private fun SettingsContent(viewModel: GymViewModel, onBack: () -> Unit) {
    val state by viewModel.settingsState.collectAsStateWithLifecycle()
    SettingsScreen(state = state, onAction = viewModel::dispatch, onBack = onBack)
}

private fun DrawScope.drawActionEdgePulse(action: MainScreenAction, progress: Float) {
    if (progress <= 0f || action == MainScreenAction.TOGGLE_TIMER) return
    val startAngle = when (action) {
        MainScreenAction.INCREASE_TARGET_SETS -> -130f
        MainScreenAction.OPEN_SETTINGS -> -40f
        MainScreenAction.COMPLETE_SET -> 50f
        MainScreenAction.BACK -> 140f
        MainScreenAction.TOGGLE_TIMER -> return
    }
    val radius = size.minDimension * EDGE_LED_RADIUS_FRACTION
    val arcSize = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)
    val topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius)
    drawArc(
        color = EdgeLedGreen.copy(alpha = progress * EDGE_LED_BLOOM_ALPHA),
        startAngle = startAngle,
        sweepAngle = EDGE_LED_SWEEP,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = EDGE_LED_BLOOM_WIDTH.dp.toPx(), cap = StrokeCap.Round)
    )
    drawArc(
        color = EdgeLedGreen.copy(alpha = progress * EDGE_LED_CORE_ALPHA),
        startAngle = startAngle,
        sweepAngle = EDGE_LED_SWEEP,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = EDGE_LED_CORE_WIDTH.dp.toPx(), cap = StrokeCap.Round)
    )
}

private const val LED_ATTACK_MILLIS = 50
private const val LED_FADE_MILLIS = 200
private const val EDGE_LED_RADIUS_FRACTION = 0.486f
private const val EDGE_LED_SWEEP = 80f
private const val EDGE_LED_BLOOM_ALPHA = 0.13f
private const val EDGE_LED_CORE_ALPHA = 0.88f
private const val EDGE_LED_BLOOM_WIDTH = 7f
private const val EDGE_LED_CORE_WIDTH = 1.8f
private val EdgeLedGreen = Color(0xFF50FFA8)
