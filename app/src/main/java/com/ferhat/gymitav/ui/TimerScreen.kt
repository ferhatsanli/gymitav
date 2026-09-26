package com.ferhat.gymitav.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferhat.gymitav.model.WorkoutState

private val WatchBackground = Color(0xFF080B0D)
private val RestGreen = Color(0xFF2DFF83)
private val OvertimeRed = Color(0xFFFF4D56)

@Composable
fun TimerScreen(
    state: WorkoutState,
    onToggleTimer: () -> Unit,
    onIncreaseTarget: () -> Unit,
    onCompleteSet: () -> Unit,
    onOpenSettings: () -> Unit,
    onReset: () -> Unit
) {
    val accent = if (state.isOverRestLimit) OvertimeRed else Color(0xFFB8C6CC)
    val glow = if (state.isOverRestLimit) OvertimeRed else RestGreen

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(WatchBackground),
        contentAlignment = Alignment.Center
    ) {
        val side = minOf(maxWidth, maxHeight)
        val centerDiameter = side * 0.72f
        Box(
            modifier = Modifier
                .size(centerDiameter)
                .clip(CircleShape)
                .drawBehind {
                    if (state.isRunning) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(glow.copy(alpha = 0.14f), glow.copy(alpha = 0.055f), Color.Transparent),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.minDimension * 0.62f
                            )
                        )
                    }
                }
                .clickable(role = Role.Button, onClickLabel = if (state.isRunning) "Pause rest timer" else "Start rest timer") {
                    onToggleTimer()
                }
                .semantics { contentDescription = if (state.isRunning) "Pause rest timer" else "Start rest timer"; role = Role.Button },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("EXERCISE ${state.exercise}", color = Color(0xFFA5B4BA), fontSize = (side.value * 0.043f).sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(side * 0.025f))
                Text(
                    text = formatElapsed(state.elapsedSeconds),
                    color = if (state.isOverRestLimit) OvertimeRed else Color(0xFF82CFFF),
                    fontSize = (side.value * 0.125f).sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1.5).sp,
                    maxLines = 1
                )
                Spacer(Modifier.height(side * 0.02f))
                Text("SET ${state.completedSets}/${state.targetSets}", color = accent, fontSize = (side.value * 0.05f).sp, fontWeight = FontWeight.Medium, letterSpacing = 1.6.sp)
                Spacer(Modifier.height(side * 0.018f))
                Text(if (state.isRunning) "TAP TO PAUSE" else "TAP TO START", color = Color(0xFF617177), fontSize = (side.value * 0.027f).sp, letterSpacing = 1.sp)
            }
        }

        val edgeFont = maxOf(8f, side.value * 0.034f).sp
        EdgeAction("+ SET", modifier = Modifier.align(Alignment.TopCenter).offset(y = side * 0.065f), width = 76.dp, fontSize = edgeFont, onClick = onIncreaseTarget)
        EdgeAction("SET OK", modifier = Modifier.align(Alignment.BottomCenter).offset(y = -(side * 0.065f)), width = 76.dp, fontSize = edgeFont, onClick = onCompleteSet, emphasized = true)
        EdgeAction("SETTINGS", modifier = Modifier.align(Alignment.CenterEnd).offset(x = -(side * 0.05f)), width = 52.dp, fontSize = edgeFont, onClick = onOpenSettings)
        EdgeAction("BACK", modifier = Modifier.align(Alignment.CenterStart).offset(x = side * 0.05f), width = 52.dp, fontSize = edgeFont, onClick = onReset)
    }
}

@Composable
private fun EdgeAction(label: String, modifier: Modifier, width: Dp, fontSize: TextUnit, onClick: () -> Unit, emphasized: Boolean = false) {
    Box(
        modifier = modifier.size(width = width, height = 48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label; role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (emphasized) Color(0xFFEAF2F4) else Color(0xFF7F8E94), fontSize = fontSize, fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1, letterSpacing = 0.5.sp)
    }
}

private fun formatElapsed(seconds: Long): String {
    val minutes = seconds / 60
    val remainder = seconds % 60
    return "%02d:%02d".format(minutes, remainder)
}
