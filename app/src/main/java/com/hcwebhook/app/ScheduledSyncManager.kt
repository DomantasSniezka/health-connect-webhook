package com.hcwebhook.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

class ScheduledSyncManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val preferencesManager = PreferencesManager(context)

    companion object {
        private const val TAG = "ScheduledSyncManager"
        const val ACTION_SCHEDULED_SYNC = "com.hcwebhook.app.ACTION_SCHEDULED_SYNC"
        const val EXTRA_SYNC_TYPE = "sync_type"
        const val SYNC_TYPE_MORNING = "morning"
        const val SYNC_TYPE_EVENING = "evening"
        private const val REQUEST_CODE_MORNING = 1001
        private const val REQUEST_CODE_EVENING = 1002
    }

    fun scheduleAllAlarms() {
        if (!preferencesManager.isScheduledSyncEnabled()) {
            Log.d(TAG, "Scheduled sync disabled, cancelling alarms")
            cancelAllAlarms()
            return
        }

        scheduleMorningAlarm()
        scheduleEveningAlarm()
    }

    fun cancelAllAlarms() {
        cancelAlarm(REQUEST_CODE_MORNING, SYNC_TYPE_MORNING)
        cancelAlarm(REQUEST_CODE_EVENING, SYNC_TYPE_EVENING)
        Log.d(TAG, "All scheduled sync alarms cancelled")
    }

    private fun scheduleMorningAlarm() {
        val hour = preferencesManager.getMorningSyncHour()
        val minute = preferencesManager.getMorningSyncMinute()
        scheduleAlarm(hour, minute, REQUEST_CODE_MORNING, SYNC_TYPE_MORNING)
    }

    private fun scheduleEveningAlarm() {
        val hour = preferencesManager.getEveningSyncHour()
        val minute = preferencesManager.getEveningSyncMinute()
        scheduleAlarm(hour, minute, REQUEST_CODE_EVENING, SYNC_TYPE_EVENING)
    }

    private fun scheduleAlarm(hour: Int, minute: Int, requestCode: Int, syncType: String) {
        val triggerTime = getNextTriggerTime(hour, minute)
        val pendingIntent = createPendingIntent(requestCode, syncType)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // Fallback: inexact but still fires in Doze
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled inexact $syncType alarm at $hour:${minute.toString().padStart(2, '0')}")
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact $syncType alarm at $hour:${minute.toString().padStart(2, '0')}")
            }
        } catch (e: SecurityException) {
            // Exact alarm permission denied — fall back to inexact
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            Log.w(TAG, "Exact alarm denied, using inexact for $syncType", e)
        }
    }

    private fun getNextTriggerTime(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the time has already passed today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis
    }

    private fun createPendingIntent(requestCode: Int, syncType: String): PendingIntent {
        val intent = Intent(context, ScheduledSyncReceiver::class.java).apply {
            action = ACTION_SCHEDULED_SYNC
            putExtra(EXTRA_SYNC_TYPE, syncType)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelAlarm(requestCode: Int, syncType: String) {
        val pendingIntent = createPendingIntent(requestCode, syncType)
        alarmManager.cancel(pendingIntent)
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
