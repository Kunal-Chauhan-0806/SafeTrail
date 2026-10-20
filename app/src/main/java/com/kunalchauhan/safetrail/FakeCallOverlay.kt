package com.kunalchauhan.safetrail

import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
fun FakeCallOverlay(callerName: String, onClose: () -> Unit) {
    val context = LocalContext.current
    var answered by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }

    // ringtone and vibration while the call is ringing
    DisposableEffect(answered) {
        var ringtone: Ringtone? = null
        val vibrator = context.getSystemService(Vibrator::class.java)
        if (!answered) {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(context, uri)
            if (Build.VERSION.SDK_INT >= 28) {
                ringtone?.isLooping = true
            }
            ringtone?.play()
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 800), 0))
        }
        onDispose {
            ringtone?.stop()
            vibrator?.cancel()
        }
    }

    // call timer once the call is answered
    LaunchedEffect(answered) {
        if (answered) {
            while (true) {
                delay(1000)
                seconds += 1
            }
        }
    }

    val timerText = String.format("%02d:%02d", seconds / 60, seconds % 60)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF101418)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.padding(top = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (answered) "In call" else "Incoming call",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        callerName,
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (answered) timerText else "Mobile",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                if (!answered) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CallButton("Decline", Color(0xFFD32F2F), onClose)
                        CallButton("Accept", Color(0xFF2E7D32)) { answered = true }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CallButton("End", Color(0xFFD32F2F), onClose)
                    }
                }
            }
        }
    }
}

@Composable
private fun CallButton(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier.size(88.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}
