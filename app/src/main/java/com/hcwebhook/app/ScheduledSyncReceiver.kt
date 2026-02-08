package com.hcwebhook.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class ScheduledSyncReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScheduledSyncReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ScheduledSyncManager.ACTION_SCHEDULED_SYNC -> {
                val syncType = intent.getStringExtra(ScheduledSyncManager.EXTRA_SYNC_TYPE) ?: "unknown"
                Log.d(TAG, "Scheduled sync alarm fired: $syncType")

                // Enqueue a one-time sync via WorkManager (avoids 10s BroadcastReceiver limit)
                val syncWork = OneTimeWorkRequestBuilder<SyncWorker>().build()
                WorkManager.getInstance(context).enqueue(syncWork)

                // Re-schedule the next alarm (exact alarms are one-shot)
                ScheduledSyncManager(context).scheduleAllAlarms()
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "Re-registering scheduled sync alarms after ${intent.action}")
                ScheduledSyncManager(context).scheduleAllAlarms()
            }
        }
    }
}
