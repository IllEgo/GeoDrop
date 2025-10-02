package com.e3hi.geodrop.util

import android.content.Context
import android.content.SharedPreferences

class NotificationPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getNotificationRadiusMeters(): Double {
        val stored = prefs.getFloat(KEY_RADIUS_METERS, DEFAULT_RADIUS_METERS.toFloat())
        return stored.toDouble().coerceIn(MIN_RADIUS_METERS, MAX_RADIUS_METERS)
    }

    fun setNotificationRadiusMeters(meters: Double) {
        val clamped = meters.coerceIn(MIN_RADIUS_METERS, MAX_RADIUS_METERS)
        prefs.edit().putFloat(KEY_RADIUS_METERS, clamped.toFloat()).apply()
    }

    companion object {
        private const val PREFS_NAME = "geodrop_notification_settings"
        private const val KEY_RADIUS_METERS = "nearby_drop_radius_meters"

        const val MIN_RADIUS_METERS = 50.0
        const val MAX_RADIUS_METERS = 1000.0
        const val DEFAULT_RADIUS_METERS = 300.0
    }
}