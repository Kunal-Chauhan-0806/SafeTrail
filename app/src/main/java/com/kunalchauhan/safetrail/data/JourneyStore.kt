package com.kunalchauhan.safetrail.data

import android.content.Context

class JourneyStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("journey", Context.MODE_PRIVATE)

    fun start(destination: String, endTimeMillis: Long) {
        prefs.edit()
            .putString("destination", destination)
            .putLong("end_time", endTimeMillis)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun destination(): String = prefs.getString("destination", "") ?: ""

    fun endTime(): Long = prefs.getLong("end_time", 0L)

    fun isActive(): Boolean = endTime() > 0L
}