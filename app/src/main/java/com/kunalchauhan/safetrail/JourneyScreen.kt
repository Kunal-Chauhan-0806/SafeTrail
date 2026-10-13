package com.kunalchauhan.safetrail

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
import com.kunalchauhan.safetrail.data.JourneyStore
import kotlinx.coroutines.delay

@Composable
fun JourneyScreen() {
    val context = LocalContext.current
    val store = remember { JourneyStore(context) }
    var active by remember { mutableStateOf(store.isActive()) }
    var destination by remember { mutableStateOf(store.destination()) }
    var endTime by remember { mutableLongStateOf(store.endTime()) }

    if (active) {
        ActiveJourney(
            destination = destination,
            endTime = endTime,
            onArrived = {
                store.clear()
                active = false
                destination = ""
                endTime = 0L
            }
        )
    } else {
        StartJourney(
            onStart = { dest, minutes ->
                val end = System.currentTimeMillis() + minutes * 60_000L
                store.start(dest, end)
                destination = dest
                endTime = end
                active = true
            }
        )
    }
}

@Composable
fun StartJourney(onStart: (String, Int) -> Unit) {
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
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onStart(destination.trim(), minutes) },
            enabled = destination.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start journey")
        }
    }
}

@Composable
fun ActiveJourney(destination: String, endTime: Long, onArrived: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
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