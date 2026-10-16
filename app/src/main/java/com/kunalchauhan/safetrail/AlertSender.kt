package com.kunalchauhan.safetrail

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import com.kunalchauhan.safetrail.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AlertSender {

    @Suppress("DEPRECATION")
    private fun smsManager(context: Context): SmsManager =
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }

    // returns (messages sent, total contacts)
    suspend fun sendToContacts(context: Context, message: String): Pair<Int, Int> =
        withContext(Dispatchers.IO) {
            val contacts = AppDatabase.get(context).contactDao().getAllOnce()
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
            sent to contacts.size
        }
}
