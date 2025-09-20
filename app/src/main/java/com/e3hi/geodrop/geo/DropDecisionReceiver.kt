package com.e3hi.geodrop.geo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.e3hi.geodrop.data.CollectedNote
import com.e3hi.geodrop.data.DropContentType
import com.e3hi.geodrop.data.NoteInventory
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Handles user's response to a nearby drop notification (pick up vs ignore).
 */
class DropDecisionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val dropId = intent.getStringExtra(EXTRA_DROP_ID) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_PICK_UP -> handlePickUp(context, intent, dropId)
                    ACTION_IGNORE -> handleIgnore(context, dropId)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    NotificationManagerCompat.from(context).cancel(dropId.hashCode())
                }
                pendingResult.finish()
            }
        }
    }

    private suspend fun handlePickUp(context: Context, intent: Intent, dropId: String) {
        val text = intent.getStringExtra(EXTRA_DROP_TEXT) ?: ""
        val contentType = DropContentType.fromRaw(intent.getStringExtra(EXTRA_DROP_CONTENT_TYPE))
        val mediaUrl = intent.getStringExtra(EXTRA_DROP_MEDIA_URL)
        val lat = if (intent.hasExtra(EXTRA_DROP_LAT)) intent.getDoubleExtra(EXTRA_DROP_LAT, 0.0) else null
        val lng = if (intent.hasExtra(EXTRA_DROP_LNG)) intent.getDoubleExtra(EXTRA_DROP_LNG, 0.0) else null
        val createdAt = intent.getLongExtra(EXTRA_DROP_CREATED_AT, -1L).takeIf { it > 0 }
        val groupCode = intent.getStringExtra(EXTRA_DROP_GROUP)

        val inventory = NoteInventory(context)
        val note = CollectedNote(
            id = dropId,
            text = text,
            contentType = contentType,
            mediaUrl = mediaUrl,
            lat = lat,
            lng = lng,
            groupCode = groupCode,
            dropCreatedAt = createdAt,
            collectedAt = System.currentTimeMillis()
        )
        inventory.saveCollected(note)

        removeGeofence(context, dropId)
        Log.d(TAG, "Collected drop $dropId and added to inventory")
    }

    private suspend fun handleIgnore(context: Context, dropId: String) {
        val inventory = NoteInventory(context)
        inventory.markIgnored(dropId)
        removeGeofence(context, dropId)
        Log.d(TAG, "Ignored drop $dropId")
    }

    private suspend fun removeGeofence(context: Context, dropId: String) {
        runCatching {
            LocationServices.getGeofencingClient(context)
                .removeGeofences(listOf(dropId))
                .await()
        }.onFailure { error ->
            Log.w(TAG, "Failed to remove geofence $dropId", error)
        }
    }

    companion object {
        const val ACTION_PICK_UP = "com.e3hi.geodrop.action.PICK_UP_DROP"
        const val ACTION_IGNORE = "com.e3hi.geodrop.action.IGNORE_DROP"

        const val EXTRA_DROP_ID = "extra_drop_id"
        const val EXTRA_DROP_TEXT = "extra_drop_text"
        const val EXTRA_DROP_CONTENT_TYPE = "extra_drop_content_type"
        const val EXTRA_DROP_MEDIA_URL = "extra_drop_media_url"
        const val EXTRA_DROP_LAT = "extra_drop_lat"
        const val EXTRA_DROP_LNG = "extra_drop_lng"
        const val EXTRA_DROP_CREATED_AT = "extra_drop_created_at"
        const val EXTRA_DROP_GROUP = "extra_drop_group"

        private const val TAG = "DropDecisionReceiver"
    }
}