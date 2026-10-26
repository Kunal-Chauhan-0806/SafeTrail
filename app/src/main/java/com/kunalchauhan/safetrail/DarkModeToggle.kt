package com.kunalchauhan.safetrail

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunalchauhan.safetrail.ui.theme.ThemeSettings

@Composable
fun DarkModeToggle(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dark = ThemeSettings.darkOverride ?: isSystemInDarkTheme()
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (dark) "Dark mode" else "Light mode",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // the box gives the scaled switch a smaller footprint in the layout
            Box(
                modifier = Modifier.size(width = 40.dp, height = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Switch(
                    checked = dark,
                    onCheckedChange = { ThemeSettings.setDark(context, it) },
                    modifier = Modifier.scale(0.75f)
                )
            }
        }
    }
}
