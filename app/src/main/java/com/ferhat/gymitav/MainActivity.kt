package com.ferhat.gymitav

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.ferhat.gymitav.ui.GymApp
import com.ferhat.gymitav.ui.theme.GYMITAVTheme
import com.ferhat.gymitav.viewmodel.GymViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = ViewModelProvider(this)[GymViewModel::class.java]
        setContent {
            GYMITAVTheme(dynamicColor = false, darkTheme = true) {
                GymApp(viewModel)
            }
        }
    }
}
