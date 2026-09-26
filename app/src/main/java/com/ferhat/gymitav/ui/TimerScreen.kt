package com.ferhat.gymitav.ui

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import com.ferhat.gymitav.model.WorkoutState

private val ScreenBlack = Color(0xFF05080B)
private val TimerBlue = Color(0xFF82CFFF)
private val RunningGreen = Color(0xFF51E7A0)
private val OvertimeRed = Color(0xFFFF5964)

@Composable
fun TimerScreen(
    state: WorkoutState,
    onAction: (MainScreenAction) -> Unit,
    onSwipeAction: (MainScreenAction) -> Unit = onAction
) {
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val currentAction = rememberUpdatedState(onAction)
    val currentSwipeAction = rememberUpdatedState(onSwipeAction)
    val currentDescription = "Exercise ${state.exercise}. ${formatElapsed(state.elapsedSeconds)}. " +
        "${state.completedSets} of ${state.targetSets} sets completed. ${if (state.isRunning) "Timer running" else "Timer paused"}."

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBlack)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val threshold = maxOf(viewConfiguration.touchSlop * 2f, with(density) { 36.dp.toPx() })
                    var triggered = false
                    var finalPosition = down.position

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        finalPosition = change.position
                        val dx = finalPosition.x - down.position.x
                        val dy = finalPosition.y - down.position.y
                        val distance = distance(dx, dy)

                        if (distance >= threshold) {
                            val action = resolveSwipeAction(dx, dy, threshold)
                            if (action != null) {
                                change.consume()
                                currentSwipeAction.value(action)
                                triggered = true
                                break
                            }
                        }

                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                    }

                    if (!triggered) {
                        val dx = finalPosition.x - down.position.x
                        val dy = finalPosition.y - down.position.y
                        val threshold = maxOf(viewConfiguration.touchSlop * 2f, with(density) { 36.dp.toPx() })
                        if (distance(dx, dy) < threshold) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            resolveTapAction(
                                finalPosition.x,
                                finalPosition.y,
                                center.x,
                                center.y,
                                minOf(size.width, size.height).toFloat()
                            )?.let { action ->
                                currentAction.value(action)
                            }
                        }
                    }
                }
            }
            .semantics {
                contentDescription = currentDescription
                onClick(label = if (state.isRunning) "Pause rest timer" else "Start rest timer") {
                    currentAction.value(MainScreenAction.TOGGLE_TIMER)
                    true
                }
                customActions = listOf(
                    CustomAccessibilityAction("Increase target sets") { currentAction.value(MainScreenAction.INCREASE_TARGET_SETS); true },
                    CustomAccessibilityAction("Open settings") { currentAction.value(MainScreenAction.OPEN_SETTINGS); true },
                    CustomAccessibilityAction("Complete set") { currentAction.value(MainScreenAction.COMPLETE_SET); true },
                    CustomAccessibilityAction("Back or reset current exercise") { currentAction.value(MainScreenAction.BACK); true }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val side = minOf(maxWidth, maxHeight)
        Canvas(Modifier.fillMaxSize()) {
            drawControlRing(state)
        }

        Column(
            modifier = Modifier.size(side * INNER_DIAMETER_FRACTION),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            val exerciseLabelSize = when {
                state.exercise >= 100 -> 8.sp
                state.exercise >= 10 -> 9.sp
                else -> 10.sp
            }
            TextLabel("EXERCISE ${state.exercise}", size = exerciseLabelSize, color = Color(0xFFB6C4CC), weight = FontWeight.Medium)
            Spacer(Modifier.height(5.dp))
            androidx.compose.material3.Text(
                text = formatElapsed(state.elapsedSeconds),
                color = if (state.isOverRestLimit) OvertimeRed else TimerBlue,
                fontSize = (side.value * 0.155f).sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1.2).sp,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            TextLabel("SET ${state.completedSets}/${state.targetSets}", size = 14.sp, color = Color(0xFFE3EBEF), weight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            TextLabel(if (state.isRunning) "RUNNING" else "PAUSED", size = 9.sp, color = if (state.isRunning) RunningGreen else Color(0xFF73818A), weight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TextLabel(text: String, size: androidx.compose.ui.unit.TextUnit, color: Color, weight: FontWeight) {
    androidx.compose.material3.Text(text = text, color = color, fontSize = size, fontWeight = weight, letterSpacing = 1.sp, maxLines = 1)
}

private fun DrawScope.drawControlRing(state: WorkoutState) {
    val side = size.minDimension
    val outerRadius = side * OUTER_RADIUS_FRACTION
    val innerRadius = side * INNER_RADIUS_FRACTION
    val bandWidth = outerRadius - innerRadius
    val ringRadius = (outerRadius + innerRadius) / 2f
    val topLeft = Offset(center.x - ringRadius, center.y - ringRadius)
    val arcSize = Size(ringRadius * 2, ringRadius * 2)
    val sectorColor = if (state.isOverRestLimit) OvertimeRed else TimerBlue

    val glassBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF365364).copy(alpha = 0.72f), Color(0xFF101C27).copy(alpha = 0.96f), Color(0xFF27424F).copy(alpha = 0.76f)),
        start = Offset(topLeft.x, topLeft.y),
        end = Offset(topLeft.x + arcSize.width, topLeft.y + arcSize.height)
    )

    listOf(-130f, -40f, 50f, 140f).forEachIndexed { index, start ->
        val tint = when (index) {
            0 -> Color(0xFF193039)
            1 -> Color(0xFF182A33)
            2 -> Color(0xFF202B34)
            else -> Color(0xFF1D2931)
        }
        drawArc(
            brush = glassBrush,
            startAngle = start,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = bandWidth, cap = StrokeCap.Butt)
        )
        drawArc(
            color = tint.copy(alpha = 0.30f),
            startAngle = start + 7f,
            sweepAngle = 66f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = bandWidth * 0.56f, cap = StrokeCap.Butt)
        )
        drawArc(
            color = Color(0xFF8DDBF7).copy(alpha = 0.16f),
            startAngle = start + 4f,
            sweepAngle = 72f,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = Size(outerRadius * 2f, outerRadius * 2f),
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Butt)
        )
    }

    drawCircle(Color(0xFF7DCFEF).copy(alpha = 0.16f), outerRadius, center, style = Stroke(width = 1.dp.toPx()))

    if (state.isRunning) {
        val runningColor = if (state.isOverRestLimit) OvertimeRed else RunningGreen
        val edgeRadius = innerRadius + 0.5.dp.toPx()
        val edgeTopLeft = Offset(center.x - edgeRadius, center.y - edgeRadius)
        val edgeSize = Size(edgeRadius * 2f, edgeRadius * 2f)
        listOf(-130f, -40f, 50f, 140f).forEach { start ->
            drawArc(
                color = runningColor.copy(alpha = INNER_LED_BLOOM_ALPHA),
                startAngle = start,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = edgeTopLeft,
                size = edgeSize,
                style = Stroke(width = INNER_LED_BLOOM_WIDTH.dp.toPx(), cap = StrokeCap.Butt)
            )
            drawArc(
                color = runningColor.copy(alpha = INNER_LED_CORE_ALPHA),
                startAngle = start,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = edgeTopLeft,
                size = edgeSize,
                style = Stroke(width = INNER_LED_CORE_WIDTH.dp.toPx(), cap = StrokeCap.Butt)
            )
        }
    }

    drawCurvedLabels(sectorColor)
}

private fun DrawScope.drawCurvedLabels(color: Color) {
    val outerRadius = size.minDimension * OUTER_RADIUS_FRACTION
    val innerRadius = size.minDimension * INNER_RADIUS_FRACTION
    val labelRadius = (outerRadius + innerRadius) / 2f
    val labels = listOf(
        "+ SET" to 230f,
        "SETTINGS" to 320f,
        "BACK" to 140f
    )
    val arcSweep = 80f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.copy(alpha = 0.92f).toArgb()
        textSize = 11.sp.toPx()
        textAlign = Paint.Align.LEFT
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
    }
    val bounds = RectF(center.x - labelRadius, center.y - labelRadius, center.x + labelRadius, center.y + labelRadius)

    drawIntoCanvas { canvas ->
        labels.forEach { (label, startAngle) ->
            val path = Path().apply { addArc(bounds, startAngle, arcSweep) }
            val availableLength = labelRadius * Math.toRadians(arcSweep.toDouble()).toFloat()
            val offset = ((availableLength - paint.measureText(label)) / 2f).coerceAtLeast(0f)
            canvas.nativeCanvas.drawTextOnPath(label, path, offset, 0f, paint)
        }
        val bottomPath = Path().apply { addArc(bounds, 130f, -80f) }
        val bottomLength = labelRadius * Math.toRadians(80.0).toFloat()
        val bottomOffset = ((bottomLength - paint.measureText("SET OK")) / 2f).coerceAtLeast(0f)
        canvas.nativeCanvas.drawTextOnPath("SET OK", bottomPath, bottomOffset, 0f, paint)
    }
}

private fun distance(dx: Float, dy: Float): Float =
    kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

private fun formatElapsed(seconds: Long): String = "%02d:%02d".format(seconds / 60, seconds % 60)

private const val INNER_RADIUS_FRACTION = 0.315f
private const val OUTER_RADIUS_FRACTION = 0.49f
private const val INNER_DIAMETER_FRACTION = INNER_RADIUS_FRACTION * 2f
private const val INNER_LED_BLOOM_ALPHA = 0.16f
private const val INNER_LED_CORE_ALPHA = 0.78f
private const val INNER_LED_BLOOM_WIDTH = 5f
private const val INNER_LED_CORE_WIDTH = 1.5f
