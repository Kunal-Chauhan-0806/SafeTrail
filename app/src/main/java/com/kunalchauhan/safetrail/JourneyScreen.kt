package com.kunalchauhan.safetrail

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
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

private class PendingJourney(val destination: String, val minutes: Int, val address: String?)

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
    var checking by remember { mutableStateOf(false) }
    var placeError by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<PendingJourney?>(null) }

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

    fun beginJourney(dest: String, minutes: Int) {
        if (hasAllPermissions(context) && LocationHelper.hasPermission(context)) {
            startJourney(dest, minutes)
        } else {
            pendingStart = dest to minutes
            launcher.launch(requestedPermissions())
        }
    }

    if (active) {
        ActiveJourney(
            destination = destination,
            endTime = endTime,
            store = store,
            onArrived = { clearJourney() }
        )
    } else {
        StartJourney(
            contactCount = contacts.size,
            permissionError = permissionError,
            checking = checking,
            placeError = placeError,
            onStart = { dest, minutes ->
                placeError = null
                checking = true
                scope.launch {
                    when (val check = GeoHelper.findPlace(context, dest)) {
                        is PlaceCheck.Found -> confirm = PendingJourney(dest, minutes, check.address)
                        PlaceCheck.NotFound -> placeError =
                            "We couldn't find \"$dest\". Try a more specific place name or address."
                        PlaceCheck.Unavailable -> confirm = PendingJourney(dest, minutes, null)
                    }
                    checking = false
                }
            }
        )
    }

    confirm?.let { pending ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (pending.address != null) "Heading here?" else "Place not verified") },
            text = {
                Text(
                    pending.address
                        ?: "SafeTrail couldn't check \"${pending.destination}\" right now " +
                        "(no internet or place service). You can still start the journey."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    beginJourney(pending.destination, pending.minutes)
                }) {
                    Text(if (pending.address != null) "Start journey" else "Start anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text("Change") }
            }
        )
    }
}

@Composable
private fun NoticeCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
fun StartJourney(
    contactCount: Int,
    permissionError: Boolean,
    checking: Boolean,
    placeError: String?,
    onStart: (String, Int) -> Unit
) {
    var destination by remember { mutableStateOf("") }
    var minutes by remember { mutableIntStateOf(30) }
    // 1 minute is only for the demo, it lets you see the whole alert flow quickly
    val options = listOf(1, 15, 30, 45, 60, 90)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("Start a journey", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Set when you expect to arrive. If you haven't checked in by then, " +
                "SafeTrail will alert your trusted contacts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Destination", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Where are you going?") },
                    singleLine = true,
                    isError = placeError != null,
                    supportingText = { placeError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("I expect to arrive in", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { m ->
                        FilterChip(
                            selected = minutes == m,
                            onClick = { minutes = m },
                            label = { Text(if (m == 1) "1 min (demo)" else "$m min") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }
        if (contactCount == 0) {
            NoticeCard("Add at least one trusted contact in the Contacts tab before starting a journey.")
        }
        if (permissionError) {
            NoticeCard("SafeTrail needs SMS and notification permission to alert your contacts.")
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onStart(destination.trim(), minutes) },
            enabled = destination.trim().length >= 3 && contactCount > 0 && !checking,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(
                if (checking) "Checking place..." else "Start journey",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun ActiveJourney(
    destination: String,
    endTime: Long,
    store: JourneyStore,
    onArrived: () -> Unit
) {
    val context = LocalContext.current
    val locationOn = LocationHelper.hasPermission(context)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            if (!store.isActive()) {
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
    val timerBg =
        if (expired) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val timerFg =
        if (expired) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Text("Journey in progress", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "To: $destination",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = timerBg)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (expired) "Time's up" else "Time remaining",
                    style = MaterialTheme.typography.titleSmall,
                    color = timerFg
                )
                Spacer(Modifier.height(8.dp))
                Text(timeText, style = MaterialTheme.typography.displayMedium, color = timerFg)
                if (expired) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Check in now.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = timerFg,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (locationOn) "Live location is on"
            else "Location is off. Allow it in app settings to share a live link.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        LiveLocationCard(store.locationLink(), store.locationTime(), now)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onArrived,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("I've arrived safely", style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun parseCoordinates(link: String?): Pair<Double, Double>? {
    val part = link?.substringAfter("q=", "") ?: return null
    val numbers = part.split(",")
    val lat = numbers.getOrNull(0)?.toDoubleOrNull() ?: return null
    val lng = numbers.getOrNull(1)?.toDoubleOrNull() ?: return null
    return lat to lng
}

@Composable
private fun LiveLocationCard(link: String?, updatedAt: Long, now: Long) {
    val context = LocalContext.current
    val coords = parseCoordinates(link)
    var address by remember { mutableStateOf<String?>(null) }
    val lookupKey = coords?.let { String.format("%.3f,%.3f", it.first, it.second) }

    // looks up the street address again whenever the phone has moved about 100 m
    LaunchedEffect(lookupKey) {
        address = null
        if (coords != null) {
            address = GeoHelper.addressFor(context, coords.first, coords.second)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Your live location", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(10.dp))
            if (coords == null) {
                Text("Waiting for a GPS fix...", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    address ?: "Looking up the address...",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    String.format("%.5f, %.5f", coords.first, coords.second),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val secondsAgo = ((now - updatedAt) / 1000).coerceAtLeast(0L)
                val agoText = if (secondsAgo < 60) "$secondsAgo s ago" else "${secondsAgo / 60} min ago"
                Text(
                    "Updated $agoText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = {
                    try {
                        val url = "https://maps.google.com/?q=${coords.first},${coords.second}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        // no app can open the link
                    }
                }) {
                    Text("Open in Maps")
                }
            }
        }
    }
}
