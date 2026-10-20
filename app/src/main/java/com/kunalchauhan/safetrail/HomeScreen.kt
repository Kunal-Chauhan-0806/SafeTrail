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

    var showFakeSetup by remember { mutableStateOf(false) }
    var showFakeCall by remember { mutableStateOf(false) }
    var fakeCallDelay by remember { mutableIntStateOf(0) }
    var callerName by remember { mutableStateOf("Mom") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val smsGranted = result[Manifest.permission.SEND_SMS] == true
        if (smsGranted && waitingForPermission) {
            countdown = SOS_SECONDS
        } else if (!smsGranted) {
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
        if (hasSms && LocationHelper.hasPermission(context)) {
            countdown = SOS_SECONDS
        } else {
            waitingForPermission = true
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
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
                    val result = AlertSender.sendToContacts(
                        context,
                        "SOS from SafeTrail: I need help right now. Please call me or come and find me."
                    )
                    val locationText =
                        if (result.locationIncluded) "Location included." else "Location was not available."
                    resultMessage =
                        "SOS sent to ${result.sent} of ${result.total} trusted contacts. $locationText"
                }
            }
        }
    }

    // counts down to the moment the fake call starts ringing
    LaunchedEffect(fakeCallDelay) {
        if (fakeCallDelay > 0) {
            delay(1000)
            fakeCallDelay -= 1
            if (fakeCallDelay == 0) showFakeCall = true
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
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = { showFakeSetup = true }) {
            Text("Fake call")
        }
        if (fakeCallDelay > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "$callerName will call in $fakeCallDelay s",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = { fakeCallDelay = 0 }) {
                Text("Cancel fake call")
            }
        }
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

    if (showFakeSetup) {
        var name by remember { mutableStateOf(callerName) }
        var ringIn by remember { mutableIntStateOf(10) }
        AlertDialog(
            onDismissRequest = { showFakeSetup = false },
            title = { Text("Fake call") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Caller name") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Ring in", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 10, 30).forEach { s ->
                            FilterChip(
                                selected = ringIn == s,
                                onClick = { ringIn = s },
                                label = { Text(if (s == 0) "Now" else "$s sec") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    callerName = name.ifBlank { "Mom" }
                    showFakeSetup = false
                    if (ringIn == 0) showFakeCall = true else fakeCallDelay = ringIn
                }) { Text("Start") }
            },
            dismissButton = {
                TextButton(onClick = { showFakeSetup = false }) { Text("Cancel") }
            }
        )
    }

    if (showFakeCall) {
        FakeCallOverlay(callerName = callerName, onClose = { showFakeCall = false })
    }
}
