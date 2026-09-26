package com.ferhat.gymitav.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferhat.gymitav.model.WorkoutState

@Composable
fun SettingsScreen(
    state: WorkoutState,
    onAdjustRest: (Int) -> Unit,
    onAdjustSets: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF080B0D)).padding(horizontal = 34.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text("SETTINGS", color = Color(0xFFEAF2F4), fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        SettingStepper("REST LIMIT", formatLimit(state.restLimitSeconds), onDecrease = { onAdjustRest(-15) }, onIncrease = { onAdjustRest(15) })
        SettingStepper("DEFAULT SETS", state.defaultSets.toString(), onDecrease = { onAdjustSets(-1) }, onIncrease = { onAdjustSets(1) })
        Text("BACK TO TIMER", color = Color(0xFF849399), fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun SettingStepper(label: String, value: String, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF829197), fontSize = 10.sp, letterSpacing = 1.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StepButton("−", onDecrease)
            Text(value, color = Color(0xFFF3F8FA), fontSize = 20.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            StepButton("+", onIncrease)
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color(0xFFCAD5D9), fontSize = 20.sp)
    }
}

private fun formatLimit(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
