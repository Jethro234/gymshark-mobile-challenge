package com.gymshark.catalogue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.gymshark.catalogue.core.designsystem.theme.GymsharkTheme
import com.gymshark.catalogue.navigation.GymsharkNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GymsharkApp()
        }
    }
}

@Composable
private fun GymsharkApp() {
    GymsharkTheme(isDarkTheme = isSystemInDarkTheme()) {
        GymsharkNavHost()
    }
}
