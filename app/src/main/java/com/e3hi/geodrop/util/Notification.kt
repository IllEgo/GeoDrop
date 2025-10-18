// util/Notifications.kt
package com.e3hi.geodrop.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

const val CHANNEL_NEARBY = "nearby_drops"
const val CHANNEL_USER_UPDATES = "user_updates"

fun createNotificationChannelIfNeeded(ctx: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val nearbyChannel = NotificationChannel(
            CHANNEL_NEARBY,
            "Nearby drops",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when you enter a drop’s area"
        }

        val updatesChannel = NotificationChannel(
            CHANNEL_USER_UPDATES,
            "GeoDrop updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders about your drops and activity"
        }

        mgr.createNotificationChannel(nearbyChannel)
        mgr.createNotificationChannel(updatesChannel)
    }
}
