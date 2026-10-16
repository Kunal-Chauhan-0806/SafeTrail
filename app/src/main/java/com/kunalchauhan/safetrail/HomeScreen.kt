package com.kunalchauhan.safetrail

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SOS_SECONDS = 5

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var countdown by remember { mutableIntStateOf(0) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var waitingForPermission by remember { mutableStateOf(false) }

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && waitingForPermission) {
            countdown = SOS_SECONDS
        } else if (!granted) {
            resultMessage = "SafeTrail needs SMS permission to send an SOS."
        }
        waitingForPermission = false
    }

    fun triggerSos() {
        if (countdown > 0) return
        resultMessage = null
        val hasSms = ContextCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasSms) {
            countdown = SOS_SECONDS
        } else {
            waitingForPermission = true
            smsLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    val latestTrigger by rememberUpdatedState(newValue = { triggerSos() })

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val detector = ShakeDetector { latestTrigger() }
        if (accelerometer != null) {
            sensorManager.registerListener(detector, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(detector) }
    }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            if (countdown > 1) {
                countdown -= 1
            } else {
                countdown = 0
                scope.launch {
                    val (sent, total) = AlertSender.sendToContacts(
                        context,
                        "SOS from SafeTrail: I need help right now. Please call me or come and find me."
                    )
                    resultMessage = "SOS sent to $sent of $total trusted contacts."
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SafeTrail", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Travel safe. We'll check in if you can't.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = { triggerSos() },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.size(180.dp)
        ) {
            Text("SOS", style = MaterialTheme.typography.headlineLarge)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Press SOS, or shake the phone hard three times.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }

    if (countdown > 0) {
        AlertDialog(
            onDismissRequest = { countdown = 0 },
            title = { Text("Sending SOS in $countdown") },
            text = { Text("Your trusted contacts will be messaged. Tap Cancel if this was a mistake.") },
            confirmButton = { TextButton(onClick = { countdown = 0 }) { Text("Cancel") } }
        )
    }

    resultMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { resultMessage = null },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { resultMessage = null }) { Text("OK") } }
        )
    }
}
