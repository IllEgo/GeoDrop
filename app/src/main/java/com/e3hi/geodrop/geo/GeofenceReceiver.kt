// geo/GeofenceReceiver.kt
package com.e3hi.geodrop.geo

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.e3hi.geodrop.R
import com.e3hi.geodrop.data.DropContentType
import com.e3hi.geodrop.data.NoteInventory
import com.e3hi.geodrop.geo.DropDecisionReceiver
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

        val inventory = NoteInventory(context)
        if (inventory.isCollected(id) || inventory.isIgnored(id)) {
            Log.d("GeoDrop", "Skipping geofence $id because it was already processed locally.")
            return
        }

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
                val dropMediaMimeType = doc.getString("mediaMimeType")?.takeIf { it.isNotBlank() }
                val dropMediaData = (
                        doc.getString("mediaData")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("audioFile")?.takeIf { it.isNotBlank() }
                            ?: doc.getBlob("mediaData")?.toBytes()?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                            ?: doc.getBlob("audioFile")?.toBytes()?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                        )


                val open = Intent(context, DropDetailActivity::class.java).apply {
                    putExtra("dropId", id)
                    dropText?.let { putExtra("dropText", it) }
                    dropLat?.let { putExtra("dropLat", it) }
                    dropLng?.let { putExtra("dropLng", it) }
                    dropCreatedAt?.let { putExtra("dropCreatedAt", it) }
                    dropGroupCode?.let { putExtra("dropGroupCode", it) }
                    putExtra("dropContentType", dropContentType.name)
                    dropMediaUrl?.let { putExtra("dropMediaUrl", it) }
                    dropMediaMimeType?.let { putExtra("dropMediaMimeType", it) }
                    dropMediaData?.let { putExtra("dropMediaData", it) }
                    putExtra(DropDetailActivity.EXTRA_SHOW_DECISION_OPTIONS, true)
                }


                val contentIntent = TaskStackBuilder.create(context).run {
                    addNextIntentWithParentStack(open)
                    getPendingIntent(2001, PendingIntentFlagsCompat)
                } ?: PendingIntent.getActivity(context, 2001, open, PendingIntentFlagsCompat)



                val title = when (dropContentType) {
                    DropContentType.TEXT -> "Note nearby"
                    DropContentType.PHOTO -> "Photo drop nearby"
                    DropContentType.AUDIO -> "Audio drop nearby"
                }
                val pickupIntent = Intent(context, DropDecisionReceiver::class.java).apply {
                    action = DropDecisionReceiver.ACTION_PICK_UP
                    putExtra(DropDecisionReceiver.EXTRA_DROP_ID, id)
                    dropText?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_TEXT, it) }
                    dropLat?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_LAT, it) }
                    dropLng?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_LNG, it) }
                    dropCreatedAt?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_CREATED_AT, it) }
                    dropGroupCode?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_GROUP, it) }
                    putExtra(DropDecisionReceiver.EXTRA_DROP_CONTENT_TYPE, dropContentType.name)
                    dropMediaUrl?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_MEDIA_URL, it) }
                    dropMediaMimeType?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_MEDIA_MIME_TYPE, it) }
                    dropMediaData?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_MEDIA_DATA, it) }
                }

                val pickupPending = PendingIntent.getBroadcast(
                    context,
                    id.hashCode(),
                    pickupIntent,
                    PendingIntentFlagsCompat
                )

                val ignoreIntent = Intent(context, DropDecisionReceiver::class.java).apply {
                    action = DropDecisionReceiver.ACTION_IGNORE
                    putExtra(DropDecisionReceiver.EXTRA_DROP_ID, id)
                }

                val ignorePending = PendingIntent.getBroadcast(
                    context,
                    id.hashCode() xor 0x2000,
                    ignoreIntent,
                    PendingIntentFlagsCompat
                )

                val prompt = when (dropContentType) {
                    DropContentType.TEXT -> "Pick up this note?"
                    DropContentType.PHOTO -> "Pick up this photo?"
                    DropContentType.AUDIO -> "Pick up this audio note?"
                }

                val notif = NotificationCompat.Builder(context, CHANNEL_NEARBY)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(prompt)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(prompt))
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent)
                    .addAction(R.drawable.ic_notification, "Pick up", pickupPending)
                    .addAction(R.drawable.ic_notification, "Ignore", ignorePending)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
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
