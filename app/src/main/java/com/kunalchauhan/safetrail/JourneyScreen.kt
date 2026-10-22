package com.kunalchauhan.safetrail

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kunalchauhan.safetrail.data.AppDatabase
import com.kunalchauhan.safetrail.data.JourneyStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// the journey cannot start without these
private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.SEND_SMS)
    }

// location is requested too, but the journey still starts if it is denied
private fun requestedPermissions(): Array<String> =
    requiredPermissions() + arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

private fun hasAllPermissions(context: Context): Boolean =
    requiredPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

@Composable
fun JourneyScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { JourneyStore(context) }
    val contactsFlow = remember { AppDatabase.get(context).contactDao().getAll() }
    val contacts by contactsFlow.collectAsState(initial = emptyList())

    var active by remember { mutableStateOf(store.isActive()) }
    var destination by remember { mutableStateOf(store.destination()) }
    var endTime by remember { mutableLongStateOf(store.endTime()) }
    var permissionError by remember { mutableStateOf(false) }
    var pendingStart by remember { mutableStateOf<Pair<String, Int>?>(null) }

    fun clearJourney() {
        JourneyScheduler.cancelAll(context)
        store.clear()
        active = false
        destination = ""
        endTime = 0L
    }

    fun startJourney(dest: String, minutes: Int) {
        val end = System.currentTimeMillis() + minutes * 60_000L
        store.start(dest, end)
        JourneyScheduler.scheduleCheck(context, end)
        if (LocationHelper.hasPermission(context)) {
            // keeps the location fresh for the whole journey, even with the app closed
            ContextCompat.startForegroundService(
                context, Intent(context, LocationService::class.java)
            )
        }
        scope.launch {
            LocationHelper.mapLink(context)?.let { store.saveLocation(it) }
        }
        destination = dest
        endTime = end
        active = true
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = requiredPermissions().all { result[it] == true }
        val request = pendingStart
        if (allGranted && request != null) {
            permissionError = false
            startJourney(request.first, request.second)
        } else {
            permissionError = true
        }
        pendingStart = null
    }

    if (active) {
        ActiveJourney(
            destination = destination,
            endTime = endTime,
            isStillActive = { store.isActive() },
            onArrived = { clearJourney() }
        )
    } else {
        StartJourney(
            contactCount = contacts.size,
            permissionError = permissionError,
            onStart = { dest, minutes ->
                if (hasAllPermissions(context) && LocationHelper.hasPermission(context)) {
                    startJourney(dest, minutes)
                } else {
                    pendingStart = dest to minutes
                    launcher.launch(requestedPermissions())
                }
            }
        )
    }
}

@Composable
fun StartJourney(
    contactCount: Int,
    permissionError: Boolean,
    onStart: (String, Int) -> Unit
) {
    var destination by remember { mutableStateOf("") }
    var minutes by remember { mutableIntStateOf(30) }
    // 1 minute is only for testing, we will remove it later
    val options = listOf(1, 15, 30, 45, 60, 90)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Start a journey", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Set when you expect to arrive. If you haven't checked in by then, " +
                "SafeTrail will alert your trusted contacts.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            label = { Text("Where are you going?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Text("I expect to arrive in", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { m ->
                FilterChip(
                    selected = minutes == m,
                    onClick = { minutes = m },
                    label = { Text("$m min") }
                )
            }
        }
        if (contactCount == 0) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Add at least one trusted contact in the Contacts tab before starting a journey.",
                color = MaterialTheme.colorScheme.error
            )
        }
        if (permissionError) {
            Spacer(Modifier.height(16.dp))
            Text(
                "SafeTrail needs SMS and notification permission to alert your contacts.",
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onStart(destination.trim(), minutes) },
            enabled = destination.isNotBlank() && contactCount > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start journey")
        }
    }
}

@Composable
fun ActiveJourney(
    destination: String,
    endTime: Long,
    isStillActive: () -> Boolean,
    onArrived: () -> Unit
) {
    val context = LocalContext.current
    val locationOn = LocationHelper.hasPermission(context)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            if (!isStillActive()) {
                onArrived()
                break
            }
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val expired = now >= endTime
    val totalSeconds = (endTime - now).coerceAtLeast(0L) / 1000
    val hours = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    val timeText = if (hours > 0) {
        String.format("%d:%02d:%02d", hours, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Journey in progress", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("To: $destination", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            if (locationOn) "Live location is on"
            else "Location is off. Allow it in app settings to share a live link.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Text(
            timeText,
            style = MaterialTheme.typography.displayLarge,
            color = if (expired) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface
        )
        if (expired) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Time's up. Check in now.",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(48.dp))
        Button(onClick = onArrived, modifier = Modifier.fillMaxWidth()) {
            Text("I've arrived safely")
        }
    }
}
