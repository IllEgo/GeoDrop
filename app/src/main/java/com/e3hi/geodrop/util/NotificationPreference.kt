package com.e3hi.geodrop.util

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

    fun getNotificationRadiusMeters(): Double {
        val key = userRadiusKey()
        val stored = if (prefs.contains(key)) {
            prefs.getFloat(key, DEFAULT_RADIUS_METERS.toFloat())
        } else {
            val legacy = if (prefs.contains(KEY_RADIUS_METERS_LEGACY)) {
                prefs.getFloat(KEY_RADIUS_METERS_LEGACY, DEFAULT_RADIUS_METERS.toFloat())
            } else {
                DEFAULT_RADIUS_METERS.toFloat()
            }
            prefs.edit().putFloat(key, legacy).apply()
            legacy
        }
        return stored.toDouble().coerceIn(MIN_RADIUS_METERS, MAX_RADIUS_METERS)
    }

    fun setNotificationRadiusMeters(meters: Double) {
        val clamped = meters.coerceIn(MIN_RADIUS_METERS, MAX_RADIUS_METERS)
        prefs.edit().putFloat(userRadiusKey(), clamped.toFloat()).apply()
    }

    private fun userRadiusKey(): String = "$KEY_RADIUS_METERS_PREFIX$activeUserKey"

    companion object {
        private const val PREFS_NAME = "geodrop_notification_settings"
        private const val KEY_RADIUS_METERS_LEGACY = "nearby_drop_radius_meters"
        private const val KEY_RADIUS_METERS_PREFIX = "nearby_drop_radius_meters_user_"
        private const val USER_KEY_ANONYMOUS = "anon"

        const val MIN_RADIUS_METERS = 50.0
        const val MAX_RADIUS_METERS = 1000.0
        const val DEFAULT_RADIUS_METERS = 300.0

        @VisibleForTesting
        fun resolveUserKey(userId: String?): String {
            return userId?.takeIf { it.isNotBlank() } ?: USER_KEY_ANONYMOUS
        }
    }
}