package com.kunalchauhan.safetrail

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.kunalchauhan.safetrail.data.JourneyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "safetrail_alerts"

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val store = JourneyStore(context)
        when (intent.action) {
            JourneyScheduler.ACTION_CHECK -> {
                if (!store.isActive()) return
                showNotification(
                    context,
                    "Are you safe?",
                    "Tap I'm safe within ${JourneyScheduler.GRACE_SECONDS} seconds or your trusted contacts will be alerted.",
                    withSafeButton = true
                )
                JourneyScheduler.scheduleSend(context)
            }

            JourneyScheduler.ACTION_SAFE -> {
                JourneyScheduler.cancelAll(context)
                store.clear()
            }

            JourneyScheduler.ACTION_SEND -> {
                if (!store.isActive()) return
                val destination = store.destination()
                val fallbackLink = store.locationLink()
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val result = AlertSender.sendToContacts(
                            context,
                            "SafeTrail alert: I have not checked in from my journey to " +
                                "$destination. Please try to reach me.",
                            fallbackLink
                        )
                        store.clear()
                        val locationText =
                            if (result.locationIncluded) "Location included." else "Location was not available."
                        showNotification(
                            context,
                            "Alert sent",
                            "Your trusted contacts were messaged (${result.sent} of ${result.total}). $locationText",
                            withSafeButton = false
                        )
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}

private fun showNotification(
    context: Context,
    title: String,
    text: String,
    withSafeButton: Boolean
) {
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
        NotificationChannel(CHANNEL_ID, "SafeTrail alerts", NotificationManager.IMPORTANCE_HIGH)
    )
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle(title)
        .setContentText(text)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
    if (withSafeButton) {
        val safeIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, AlarmReceiver::class.java).setAction(JourneyScheduler.ACTION_SAFE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.addAction(0, "I'm safe", safeIntent)
    }
    manager.notify(JourneyScheduler.NOTIFICATION_ID, builder.build())
}
