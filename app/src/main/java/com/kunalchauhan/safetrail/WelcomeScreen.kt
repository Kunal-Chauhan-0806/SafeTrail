package com.kunalchauhan.safetrail

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// remembers that the welcome screen has been shown
object Onboarding {
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("onboarding", Context.MODE_PRIVATE)

    fun isDone(context: Context): Boolean = prefs(context).getBoolean("done", false)

    fun markDone(context: Context) {
        prefs(context).edit().putBoolean("done", true).apply()
    }
}

@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var denied by remember { mutableStateOf(false) }

    val permissions = buildList {
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // the SMS permission is the one the app cannot work without
        if (result[Manifest.permission.SEND_SMS] == true) onDone() else denied = true
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text("Welcome to SafeTrail", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "SafeTrail checks in for you. If you don't arrive on time, it messages the " +
                    "people you trust. To do that it needs a few permissions.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            PermissionRow(
                Icons.Filled.Email,
                "Send text messages",
                "Sends the alert and SOS to your trusted contacts. Your carrier may charge for SMS."
            )
            Spacer(Modifier.height(12.dp))
            PermissionRow(
                Icons.Filled.Place,
                "Your location",
                "Adds a map link to the alert so your contacts can find you. It is only shared in an alert."
            )
            Spacer(Modifier.height(12.dp))
            PermissionRow(
                Icons.Filled.Notifications,
                "Notifications",
                "Shows the \"Are you safe?\" prompt when your journey time ends."
            )

            if (denied) {
                Spacer(Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "SMS permission is needed",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Without it SafeTrail cannot alert your contacts. Open the app settings " +
                                "and allow SMS. If you installed from an APK file on Android 13 or newer, " +
                                "first open App info, tap the three-dot menu and choose " +
                                "\"Allow restricted settings\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            if (!denied) {
                Button(
                    onClick = { launcher.launch(permissions) },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Allow and continue", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Open app settings", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { launcher.launch(permissions) },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Try again")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue anyway")
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
