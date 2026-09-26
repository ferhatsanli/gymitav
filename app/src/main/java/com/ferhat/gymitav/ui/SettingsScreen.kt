package com.ferhat.gymitav.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import com.ferhat.gymitav.model.WorkoutAction
import com.ferhat.gymitav.viewmodel.SettingsUiState

private val SettingsBackground = Color(0xFF05080B)
private val PrimaryText = Color(0xFFE7EFF3)
private val SecondaryText = Color(0xFF91A1AA)

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onAction: (WorkoutAction) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val reminderLabel = remember(state.overdueReminderSeconds) {
        if (state.overdueReminderSeconds == 0) "OFF" else "${state.overdueReminderSeconds} SEC"
    }

    Box(modifier = Modifier.fillMaxSize().background(SettingsBackground)) {
      Canvas(Modifier.fillMaxSize()) {
          val radius = size.minDimension * 0.72f
          drawCircle(
              brush = Brush.radialGradient(
                  colors = listOf(Color(0xFF986BFF).copy(alpha = 0.075f), Color(0xFF7650D8).copy(alpha = 0.025f), Color.Transparent),
                  center = Offset(size.width * 0.28f, size.height * 0.17f),
                  radius = radius
              ),
              radius = radius,
              center = Offset(size.width * 0.28f, size.height * 0.17f)
          )
      }
      ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 30.dp, bottom = 34.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        item(key = "settings-title") {
            Box(
                Modifier.size(width = 156.dp, height = 36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0C1822).copy(alpha = 0.92f), Color(0xFF142638).copy(alpha = 0.96f), Color(0xFF0C1822).copy(alpha = 0.92f))))
                    .border(1.dp, Color(0xFF70CFFF).copy(alpha = 0.28f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("SETTINGS", color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, textAlign = TextAlign.Center)
            }
        }
        item(key = "exercise") {
            SettingStepper(
                label = "EXERCISE",
                value = state.exercise.toString(),
                onDecrease = { onAction(WorkoutAction.AdjustExercise(-1)) },
                onIncrease = { onAction(WorkoutAction.AdjustExercise(1)) }
            )
        }
        item(key = "default-sets") {
            SettingStepper(
                label = "SETS PER EXERCISE",
                value = state.defaultSets.toString(),
                onDecrease = { onAction(WorkoutAction.AdjustDefaultSets(-1)) },
                onIncrease = { onAction(WorkoutAction.AdjustDefaultSets(1)) }
            )
        }
        item(key = "reset-current-target") {
            val buttonShape = RoundedCornerShape(18.dp)
            Box(
                modifier = Modifier.size(width = 136.dp, height = 44.dp)
                    .clip(buttonShape)
                    .background(Brush.horizontalGradient(listOf(Color(0xFF102536), Color(0xFF172D42), Color(0xFF102536))))
                    .border(1.dp, Color(0xFF68C9F2).copy(alpha = 0.24f), buttonShape)
                    .clickable(role = Role.Button) { onAction(WorkoutAction.ResetCurrentTargetToDefault) }
                    .semantics { contentDescription = "Reset current exercise target to default sets" },
                contentAlignment = Alignment.Center
            ) {
                Text("RESET TO DEFAULT", color = Color(0xFFB7D6E6), fontSize = 9.sp,
                    fontWeight = FontWeight.Medium, letterSpacing = 0.65.sp, textAlign = TextAlign.Center)
            }
        }
        item(key = "rest-limit") {
            SettingStepper(
                label = "REST LIMIT",
                value = formatLimit(state.restLimitSeconds),
                onDecrease = { onAction(WorkoutAction.AdjustRestLimit(-15)) },
                onIncrease = { onAction(WorkoutAction.AdjustRestLimit(15)) }
            )
        }
        item(key = "overdue-reminder") {
            SettingStepper(
                label = "OVERDUE REMINDER",
                value = reminderLabel,
                onDecrease = { onAction(WorkoutAction.AdjustOverdueReminder(-5)) },
                onIncrease = { onAction(WorkoutAction.AdjustOverdueReminder(5)) }
            )
        }
        item(key = "back") {
            Box(
                modifier = Modifier.size(width = 136.dp, height = 44.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF102536), Color(0xFF172D42), Color(0xFF102536))))
                    .border(1.dp, Color(0xFF68C9F2).copy(alpha = 0.24f), RoundedCornerShape(18.dp))
                    .clickable(role = Role.Button, onClick = onBack)
                    .semantics { contentDescription = "Back to timer" },
                contentAlignment = Alignment.Center
            ) {
                Text("BACK TO TIMER", color = Color(0xFFB7D6E6), fontSize = 9.sp,
                    fontWeight = FontWeight.Medium, letterSpacing = 0.65.sp, textAlign = TextAlign.Center)
            }
        }
      }
      PositionIndicator(scalingLazyListState = listState, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun SettingStepper(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = SecondaryText, fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp, maxLines = 1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StepControl("−", "Decrease $label", onDecrease)
            Text(value, color = PrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1)
            StepControl("+", "Increase $label", onIncrease)
        }
    }
}

@Composable
private fun StepControl(symbol: String, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = Color(0xFFD9E4E9), fontSize = 24.sp, fontWeight = FontWeight.Light)
    }
}

private fun formatLimit(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)
