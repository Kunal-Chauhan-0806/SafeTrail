package com.kunalchauhan.safetrail

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.kunalchauhan.safetrail.data.AppDatabase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

data class SendResult(
    val sent: Int,
    val total: Int,
    val locationIncluded: Boolean,
    // set when nothing could be attempted at all (no contacts, no permission, no SIM...)
    val problem: String? = null,
    // contacts whose message failed after it was handed to the phone
    val failedNames: List<String> = emptyList()
)

// A plain-language message for dialogs and notifications. what = "SOS" or "Alert".
fun SendResult.summary(what: String): String {
    if (problem != null) return "$what was NOT sent. $problem"
    val locationText =
        if (locationIncluded) "Location included." else "Location was not available."
    if (failedNames.isEmpty()) {
        return "$what sent to $sent of $total trusted contacts. $locationText"
    }
    val names = failedNames.joinToString(", ")
    return if (sent == 0) {
        "$what could not be sent to anyone ($names). " +
            "Check your signal, SIM balance and the saved numbers."
    } else {
        "$what reached $sent of $total contacts. It failed for: $names. " +
            "Check those numbers and your SIM balance. $locationText"
    }
}

object AlertSender {

    @Suppress("DEPRECATION")
    private fun smsManager(context: Context): SmsManager =
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }

    // returns a reason in plain words if SMS cannot work right now, or null if it looks fine
    private fun smsProblem(context: Context): String? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return "SMS permission is off. Allow it in Settings > Apps > SafeTrail > Permissions."
        }
        val airplane = Settings.Global.getInt(
            context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0
        ) == 1
        if (airplane) return "Airplane mode is on. Turn it off so messages can be sent."
        val telephony = context.getSystemService(TelephonyManager::class.java)
        if (telephony != null && telephony.simState != TelephonyManager.SIM_STATE_READY) {
            return "No working SIM card was found, so text messages cannot be sent."
        }
        return null
    }

    suspend fun sendToContacts(
        context: Context,
        message: String,
        fallbackLink: String? = null,
        fresh: Boolean = true,
        waitMillis: Long = 20_000L
    ): SendResult = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val link = (if (fresh) LocationHelper.mapLink(context) else null) ?: fallbackLink
        val fullMessage = if (link != null) {
            "$message Last known location: $link"
        } else {
            "$message My location is not available."
        }

        val contacts = AppDatabase.get(context).contactDao().getAllOnce()
        if (contacts.isEmpty()) {
            return@withContext SendResult(
                0, 0, link != null,
                problem = "No trusted contacts are saved. Add one in the Contacts tab."
            )
        }
        smsProblem(context)?.let {
            return@withContext SendResult(0, contacts.size, link != null, problem = it)
        }

        val sms = smsManager(context)
        val partsList = contacts.map { sms.divideMessage(fullMessage) }
        val remaining = AtomicInteger(partsList.sumOf { it.size })
        val failed = Collections.synchronizedSet(mutableSetOf<Int>())
        val done = CompletableDeferred<Unit>()
        val action = "${app.packageName}.SMS_SENT_${System.nanoTime()}"

        // Android reports the real result of each message part here
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                if (resultCode != Activity.RESULT_OK) failed.add(i.getIntExtra("idx", -1))
                if (remaining.decrementAndGet() <= 0) done.complete(Unit)
            }
        }
        ContextCompat.registerReceiver(
            app, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        try {
            contacts.forEachIndexed { i, contact ->
                val parts = partsList[i]
                try {
                    val sentIntents = ArrayList<PendingIntent>()
                    parts.indices.forEach { p ->
                        val intent = Intent(action).setPackage(app.packageName).putExtra("idx", i)
                        sentIntents.add(
                            PendingIntent.getBroadcast(
                                app, i * 100 + p, intent,
                                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                            )
                        )
                    }
                    sms.sendMultipartTextMessage(contact.phone, null, parts, sentIntents, null)
                } catch (e: Exception) {
                    // this contact could not be messaged, keep going with the rest
                    failed.add(i)
                    if (remaining.addAndGet(-parts.size) <= 0) done.complete(Unit)
                }
            }
            // wait up to 20 seconds for the phone to report back
            withTimeoutOrNull(waitMillis) { done.await() }
        } finally {
            try {
                app.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // already unregistered
            }
        }

        val failedNames = failed.toList().sorted().mapNotNull { contacts.getOrNull(it)?.name }
        SendResult(
            sent = (contacts.size - failed.size).coerceAtLeast(0),
            total = contacts.size,
            locationIncluded = link != null,
            failedNames = failedNames
        )
    }
}
