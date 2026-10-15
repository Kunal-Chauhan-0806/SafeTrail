package com.kunalchauhan.safetrail

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import com.kunalchauhan.safetrail.data.AppDatabase
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
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val contacts = AppDatabase.get(context).contactDao().getAllOnce()
                        val message = "SafeTrail alert: I have not checked in from my journey to " +
                                "$destination. Please try to reach me."
                        var sent = 0
                        contacts.forEach { contact ->
                            try {
                                val sms = smsManager(context)
                                sms.sendMultipartTextMessage(
                                    contact.phone, null, sms.divideMessage(message), null, null
                                )
                                sent++
                            } catch (e: Exception) {
                                // this contact could not be messaged, keep going with the rest
                            }
                        }
                        store.clear()
                        showNotification(
                            context,
                            "Alert sent",
                            "Your trusted contacts were messaged ($sent of ${contacts.size}).",
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

@Suppress("DEPRECATION")
private fun smsManager(context: Context): SmsManager =
    if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(SmsManager::class.java)
    } else {
        SmsManager.getDefault()
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
        .setSmallIcon(R.drawable.ic_launcher_foreground)
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