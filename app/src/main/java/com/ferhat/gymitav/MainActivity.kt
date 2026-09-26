package com.ferhat.gymitav

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ferhat.gymitav.ui.GymApp
import com.ferhat.gymitav.ui.theme.GYMITAVTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GYMITAVTheme(dynamicColor = false, darkTheme = true) {
                GymApp()
            }
        }
    }
}
