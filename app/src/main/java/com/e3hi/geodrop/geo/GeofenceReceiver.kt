// geo/GeofenceReceiver.kt
package com.e3hi.geodrop.geo

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.e3hi.geodrop.R
import com.e3hi.geodrop.ui.DropDetailActivity
import com.e3hi.geodrop.util.CHANNEL_NEARBY
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.e("GeoDrop", "Geofence err: ${event.errorCode}")
            return
        }
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        // There can be multiple. We'll notify for the first.
        val id = event.triggeringGeofences?.firstOrNull()?.requestId ?: return

        // Build an intent to open the detail screen
        val open = Intent(context, DropDetailActivity::class.java).apply {
            putExtra("dropId", id)
        }
        val contentIntent = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(open)
            getPendingIntent(2001, PendingIntentFlagsCompat)
        }

        val notif = NotificationCompat.Builder(context, CHANNEL_NEARBY)
            .setSmallIcon(R.drawable.ic_notification) // add a small icon to res/drawable
            .setContentTitle("Note nearby")
            .setContentText("You’re near a dropped message. Tap to open.")
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(id.hashCode(), notif)

        Log.d("GeoDrop", "Entered gf $id")
    }

    companion object {
        private val PendingIntentFlagsCompat =
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (android.os.Build.VERSION.SDK_INT >= 23) android.app.PendingIntent.FLAG_IMMUTABLE else 0)
    }
}
