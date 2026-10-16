package com.kunalchauhan.safetrail

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import com.kunalchauhan.safetrail.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SendResult(val sent: Int, val total: Int, val locationIncluded: Boolean)

object AlertSender {

    @Suppress("DEPRECATION")
    private fun smsManager(context: Context): SmsManager =
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }

    suspend fun sendToContacts(
        context: Context,
        message: String,
        fallbackLink: String? = null
    ): SendResult = withContext(Dispatchers.IO) {
        val link = LocationHelper.mapLink(context) ?: fallbackLink
        val fullMessage = if (link != null) {
            "$message Last known location: $link"
        } else {
            "$message My location is not available."
        }
        // debug aid so the final text can be checked in logcat, remove before release
        Log.d("AlertSender", fullMessage)

        val contacts = AppDatabase.get(context).contactDao().getAllOnce()
        var sent = 0
        contacts.forEach { contact ->
            try {
                val sms = smsManager(context)
                sms.sendMultipartTextMessage(
                    contact.phone, null, sms.divideMessage(fullMessage), null, null
                )
                sent++
            } catch (e: Exception) {
                // this contact could not be messaged, keep going with the rest
            }
        }
        SendResult(sent, contacts.size, link != null)
    }
}
