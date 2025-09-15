// util/Notifications.kt
package com.e3hi.geodrop.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

const val CHANNEL_NEARBY = "nearby_drops"

fun createNotificationChannelIfNeeded(ctx: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(
            CHANNEL_NEARBY,
            "Nearby drops",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        ch.description = "Alerts when you enter a drop’s area"
        mgr.createNotificationChannel(ch)
    }
}
