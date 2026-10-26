package com.kunalchauhan.safetrail

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import com.kunalchauhan.safetrail.ui.theme.SafeTrailTheme
import com.kunalchauhan.safetrail.ui.theme.ThemeSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeSettings.load(this)
        setContent {
            val dark = ThemeSettings.darkOverride ?: isSystemInDarkTheme()
            // keeps the status bar and navigation bar icons readable in either mode
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                )
                onDispose { }
            }
            SafeTrailTheme(darkTheme = dark) {
                SafeTrailApp()
            }
        }
    }
}
