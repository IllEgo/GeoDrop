// geo/GeofencePendingIntent.kt
package com.e3hi.geodrop.geo

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object GeofencePendingIntent {
    private const val REQ_CODE = 1001
    private var cached: PendingIntent? = null

    fun get(context: Context): PendingIntent {
        cached?.let { return it }
        val intent = Intent(context, GeofenceReceiver::class.java)
        val flags = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            }
            else -> PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, REQ_CODE, intent, flags).also { cached = it }
    }
}
