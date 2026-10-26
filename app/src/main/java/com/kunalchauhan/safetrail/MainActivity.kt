package com.kunalchauhan.safetrail

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
            var onboarded by remember { mutableStateOf(Onboarding.isDone(this@MainActivity)) }
            SafeTrailTheme(darkTheme = dark) {
                if (onboarded) {
                    SafeTrailApp()
                } else {
                    WelcomeScreen(onDone = {
                        Onboarding.markDone(this@MainActivity)
                        onboarded = true
                    })
                }
            }
        }
    }
}
