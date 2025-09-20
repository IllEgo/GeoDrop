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
import com.e3hi.geodrop.data.DropContentType
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

                val isDeleted = doc?.getBoolean("isDeleted") == true
                if (doc == null || isDeleted) {
                    Log.d("GeoDrop", "Skipping notification for drop $id because it no longer exists or is deleted.")
                    return@launch
                }

                val dropText = doc.getString("text")?.takeIf { it.isNotBlank() }
                val dropLat = doc.getDouble("lat")
                val dropLng = doc.getDouble("lng")
                val dropCreatedAt = doc.getLong("createdAt")?.takeIf { it > 0L }
                val dropGroupCode = doc.getString("groupCode")?.takeIf { it.isNotBlank() }
                val dropContentType = doc.getString("contentType")?.let { DropContentType.fromRaw(it) }
                    ?: DropContentType.TEXT
                val dropMediaUrl = doc.getString("mediaUrl")?.takeIf { it.isNotBlank() }


                val open = Intent(context, DropDetailActivity::class.java).apply {
                    putExtra("dropId", id)
                    dropText?.let { putExtra("dropText", it) }
                    dropLat?.let { putExtra("dropLat", it) }
                    dropLng?.let { putExtra("dropLng", it) }
                    dropCreatedAt?.let { putExtra("dropCreatedAt", it) }
                    dropGroupCode?.let { putExtra("dropGroupCode", it) }
                    putExtra("dropContentType", dropContentType.name)
                    dropMediaUrl?.let { putExtra("dropMediaUrl", it) }
                }


                val contentIntent = TaskStackBuilder.create(context).run {
                    addNextIntentWithParentStack(open)
                    getPendingIntent(2001, PendingIntentFlagsCompat)
                } ?: PendingIntent.getActivity(context, 2001, open, PendingIntentFlagsCompat)


                val (title, body) = when (dropContentType) {
                    DropContentType.TEXT -> {
                        val message = dropText ?: "You’re near a dropped message. Tap to open."
                        "Note nearby" to message
                    }
                    DropContentType.PHOTO -> {
                        val message = dropText?.takeIf { it.isNotBlank() }
                            ?: "A photo drop is nearby. Tap to view it."
                        "Photo drop nearby" to message
                    }
                    DropContentType.AUDIO -> {
                        val message = dropText?.takeIf { it.isNotBlank() }
                            ?: "An audio drop is nearby. Tap to listen."
                        "Audio drop nearby" to message
                    }
                }
                val notif = NotificationCompat.Builder(context, CHANNEL_NEARBY)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
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
