package com.kunalchauhan.safetrail.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Remembers the person's dark mode choice. null means "follow the phone".
object ThemeSettings {
    private const val PREFS = "theme"
    private const val KEY = "dark_mode"

    var darkOverride by mutableStateOf<Boolean?>(null)
        private set

    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        darkOverride = if (prefs.contains(KEY)) prefs.getBoolean(KEY, false) else null
        loaded = true
    }

    fun setDark(context: Context, dark: Boolean) {
        darkOverride = dark
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY, dark)
            .apply()
    }
}
