package com.kitheapp.util

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting

class NotificationPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var activeUserKey: String = resolveUserKey(null)

    @Synchronized
    fun setActiveUser(userId: String?) {
        val normalized = resolveUserKey(userId)
        if (activeUserKey == normalized) return

        activeUserKey = normalized
    }

    fun areNearbyAlertsEnabled(): Boolean =
        prefs.getBoolean(userAlertsEnabledKey(), false)

    fun setNearbyAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(userAlertsEnabledKey(), enabled).apply()
    }

    private fun userAlertsEnabledKey(): String = "$KEY_ALERTS_ENABLED_PREFIX$activeUserKey"

    companion object {
        private const val PREFS_NAME = "geodrop_notification_settings"
        private const val KEY_ALERTS_ENABLED_PREFIX = "nearby_drop_alerts_enabled_user_"
        private const val USER_KEY_ANONYMOUS = "anon"

        @VisibleForTesting
        fun resolveUserKey(userId: String?): String {
            return userId?.takeIf { it.isNotBlank() } ?: USER_KEY_ANONYMOUS
        }
    }
}
