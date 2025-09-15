// geo/GeofenceReceiver.kt
package com.e3hi.geodrop.geo

import android.app.NotificationManager
import android.app.PendingIntent
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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val doc = try {
                    Firebase.firestore.collection("drops").document(id).get().await()
                } catch (e: Exception) {
                    Log.w("GeoDrop", "Failed to load drop $id for notification", e)
                    null
                }

                val dropText = doc?.getString("text")?.takeIf { it.isNotBlank() }
                val dropLat = doc?.getDouble("lat")
                val dropLng = doc?.getDouble("lng")


                val open = Intent(context, DropDetailActivity::class.java).apply {
                    putExtra("dropId", id)
                    dropText?.let { putExtra("dropText", it) }
                    dropLat?.let { putExtra("dropLat", it) }
                    dropLng?.let { putExtra("dropLng", it) }
                }


                val contentIntent = TaskStackBuilder.create(context).run {
                    addNextIntentWithParentStack(open)
                    getPendingIntent(2001, PendingIntentFlagsCompat)
                } ?: PendingIntent.getActivity(context, 2001, open, PendingIntentFlagsCompat)


                val body = dropText ?: "You’re near a dropped message. Tap to open."
                val notif = NotificationCompat.Builder(context, CHANNEL_NEARBY)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Note nearby")
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent)
                    .build()

                withContext(Dispatchers.Main) {
                    val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    mgr.notify(id.hashCode(), notif)
                }

                Log.d("GeoDrop", "Entered gf $id")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val PendingIntentFlagsCompat =
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (android.os.Build.VERSION.SDK_INT >= 23) android.app.PendingIntent.FLAG_IMMUTABLE else 0)
    }
}
