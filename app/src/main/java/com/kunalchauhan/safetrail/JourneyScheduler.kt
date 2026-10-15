package com.kunalchauhan.safetrail

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object JourneyScheduler {
    const val ACTION_CHECK = "com.kunalchauhan.safetrail.ACTION_CHECK"
    const val ACTION_SEND = "com.kunalchauhan.safetrail.ACTION_SEND"
    const val ACTION_SAFE = "com.kunalchauhan.safetrail.ACTION_SAFE"
    const val GRACE_SECONDS = 30
    const val NOTIFICATION_ID = 1001

    private fun pending(context: Context, action: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun setAt(context: Context, action: String, timeMillis: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(timeMillis, openApp(context)),
            pending(context, action)
        )
    }

    fun scheduleCheck(context: Context, endTime: Long) {
        setAt(context, ACTION_CHECK, endTime)
    }

    fun scheduleSend(context: Context) {
        setAt(context, ACTION_SEND, System.currentTimeMillis() + GRACE_SECONDS * 1000L)
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pending(context, ACTION_CHECK))
        alarmManager.cancel(pending(context, ACTION_SEND))
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }
}