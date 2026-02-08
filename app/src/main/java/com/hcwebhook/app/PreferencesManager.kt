package com.hcwebhook.app

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "hc_webhook_prefs"
        private const val KEY_LAST_SYNC_TS_PREFIX = "last_sync_ts_"
        private const val KEY_LAST_STEPS_SYNC_TS = "last_steps_sync_ts"
        private const val KEY_LAST_SLEEP_SYNC_TS = "last_sleep_sync_ts"
        private const val KEY_SYNC_INTERVAL_MINUTES = "sync_interval_minutes"
        private const val KEY_WEBHOOK_URLS = "webhook_urls"
        private const val KEY_ENABLED_DATA_TYPES = "enabled_data_types"
        private const val KEY_WEBHOOK_LOGS = "webhook_logs"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_LAST_SYNC_SUMMARY = "last_sync_summary"
        private const val DEFAULT_SYNC_INTERVAL_MINUTES = 60
        private const val MAX_LOGS = 100
        private const val KEY_SCHEDULED_SYNC_ENABLED = "scheduled_sync_enabled"
        private const val KEY_MORNING_SYNC_HOUR = "morning_sync_hour"
        private const val KEY_MORNING_SYNC_MINUTE = "morning_sync_minute"
        private const val KEY_EVENING_SYNC_HOUR = "evening_sync_hour"
        private const val KEY_EVENING_SYNC_MINUTE = "evening_sync_minute"
    }

    fun getLastStepsSyncTimestamp(): Long? {
        val timestamp = prefs.getLong(KEY_LAST_STEPS_SYNC_TS, -1)
        return if (timestamp == -1L) null else timestamp
    }

    fun setLastStepsSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_STEPS_SYNC_TS, timestamp).apply()
    }

    fun getLastSleepSyncTimestamp(): Long? {
        val timestamp = prefs.getLong(KEY_LAST_SLEEP_SYNC_TS, -1)
        return if (timestamp == -1L) null else timestamp
    }

    fun setLastSleepSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SLEEP_SYNC_TS, timestamp).apply()
    }

    fun getSyncIntervalMinutes(): Int {
        return prefs.getInt(KEY_SYNC_INTERVAL_MINUTES, DEFAULT_SYNC_INTERVAL_MINUTES)
    }

    fun setSyncIntervalMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_SYNC_INTERVAL_MINUTES, minutes).apply()
    }

    fun getWebhookUrls(): List<String> {
        val urlsString = prefs.getString(KEY_WEBHOOK_URLS, "") ?: ""
        return if (urlsString.isEmpty()) emptyList() else urlsString.split(",")
    }

    fun setWebhookUrls(urls: List<String>) {
        val urlsString = urls.joinToString(",")
        prefs.edit().putString(KEY_WEBHOOK_URLS, urlsString).apply()
    }

    fun getEnabledDataTypes(): Set<HealthDataType> {
        val typesString = prefs.getString(KEY_ENABLED_DATA_TYPES, "") ?: ""
        return if (typesString.isEmpty()) {
            emptySet()
        } else {
            typesString.split(",").mapNotNull {
                try { HealthDataType.valueOf(it) } catch (e: Exception) { null }
            }.toSet()
        }
    }

    fun setEnabledDataTypes(types: Set<HealthDataType>) {
        val typesString = types.joinToString(",") { it.name }
        prefs.edit().putString(KEY_ENABLED_DATA_TYPES, typesString).apply()
    }

    fun getLastSyncTimestamp(type: HealthDataType): Long? {
        val timestamp = prefs.getLong(KEY_LAST_SYNC_TS_PREFIX + type.name, -1)
        return if (timestamp == -1L) null else timestamp
    }

    fun setLastSyncTimestamp(type: HealthDataType, timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_TS_PREFIX + type.name, timestamp).apply()
    }

    fun getWebhookLogs(): List<WebhookLog> {
        val logsJson = prefs.getString(KEY_WEBHOOK_LOGS, null) ?: return emptyList()
        return try {
            Json.decodeFromString<List<WebhookLog>>(logsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addWebhookLog(log: WebhookLog) {
        val currentLogs = getWebhookLogs().toMutableList()
        currentLogs.add(0, log) // Add to beginning

        // Keep only the most recent MAX_LOGS entries
        val trimmedLogs = currentLogs.take(MAX_LOGS)

        val logsJson = Json.encodeToString(trimmedLogs)
        prefs.edit().putString(KEY_WEBHOOK_LOGS, logsJson).apply()
    }

    fun clearWebhookLogs() {
        prefs.edit().remove(KEY_WEBHOOK_LOGS).apply()
    }

    fun getLastSyncTime(): Long? {
        val timestamp = prefs.getLong(KEY_LAST_SYNC_TIME, -1)
        return if (timestamp == -1L) null else timestamp
    }

    fun setLastSyncTime(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, timestamp).apply()
    }

    fun getLastSyncSummary(): String? {
        return prefs.getString(KEY_LAST_SYNC_SUMMARY, null)
    }

    fun setLastSyncSummary(summary: String) {
        prefs.edit().putString(KEY_LAST_SYNC_SUMMARY, summary).apply()
    }

    fun isScheduledSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_SCHEDULED_SYNC_ENABLED, true)
    }

    fun setScheduledSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCHEDULED_SYNC_ENABLED, enabled).apply()
    }

    fun getMorningSyncHour(): Int = prefs.getInt(KEY_MORNING_SYNC_HOUR, 8)
    fun getMorningSyncMinute(): Int = prefs.getInt(KEY_MORNING_SYNC_MINUTE, 0)

    fun setMorningSyncTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_MORNING_SYNC_HOUR, hour)
            .putInt(KEY_MORNING_SYNC_MINUTE, minute)
            .apply()
    }

    fun getEveningSyncHour(): Int = prefs.getInt(KEY_EVENING_SYNC_HOUR, 21)
    fun getEveningSyncMinute(): Int = prefs.getInt(KEY_EVENING_SYNC_MINUTE, 0)

    fun setEveningSyncTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_EVENING_SYNC_HOUR, hour)
            .putInt(KEY_EVENING_SYNC_MINUTE, minute)
            .apply()
    }
}