package com.e3hi.geodrop.geo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.e3hi.geodrop.data.CollectedNote
import com.e3hi.geodrop.data.DropContentType
import com.e3hi.geodrop.data.DropType
import com.e3hi.geodrop.data.FirestoreRepo
import com.e3hi.geodrop.data.NoteInventory
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Handles user's response to a nearby drop notification (pick up vs ignore).
 */
class DropDecisionReceiver : BroadcastReceiver() {
    private val repo by lazy { FirestoreRepo() }

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
        val mediaMimeType = intent.getStringExtra(EXTRA_DROP_MEDIA_MIME_TYPE)
        val mediaData = intent.getStringExtra(EXTRA_DROP_MEDIA_DATA)
        val lat = if (intent.hasExtra(EXTRA_DROP_LAT)) intent.getDoubleExtra(EXTRA_DROP_LAT, 0.0) else null
        val lng = if (intent.hasExtra(EXTRA_DROP_LNG)) intent.getDoubleExtra(EXTRA_DROP_LNG, 0.0) else null
        val createdAt = intent.getLongExtra(EXTRA_DROP_CREATED_AT, -1L).takeIf { it > 0 }
        val groupCode = intent.getStringExtra(EXTRA_DROP_GROUP)
        val dropType = DropType.fromRaw(intent.getStringExtra(EXTRA_DROP_TYPE))
        val businessId = intent.getStringExtra(EXTRA_DROP_BUSINESS_ID)
        val businessName = intent.getStringExtra(EXTRA_DROP_BUSINESS_NAME)
        val redemptionLimit = if (intent.hasExtra(EXTRA_DROP_REDEMPTION_LIMIT)) {
            intent.getIntExtra(EXTRA_DROP_REDEMPTION_LIMIT, 0)
        } else {
            null
        }
        val redemptionCount = intent.getIntExtra(EXTRA_DROP_REDEMPTION_COUNT, 0)
        val isNsfw = intent.getBooleanExtra(EXTRA_DROP_IS_NSFW, false)
        val nsfwLabels = intent.getStringArrayListExtra(EXTRA_DROP_NSFW_LABELS)
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        val nsfwConfidence = if (intent.hasExtra(EXTRA_DROP_NSFW_CONFIDENCE)) {
            intent.getDoubleExtra(EXTRA_DROP_NSFW_CONFIDENCE, 0.0)
        } else {
            null
        }
        val resolvedIsNsfw = isNsfw || nsfwLabels.isNotEmpty()

        val inventory = NoteInventory(context)
        val note = CollectedNote(
            id = dropId,
            text = text,
            contentType = contentType,
            mediaUrl = mediaUrl,
            mediaMimeType = mediaMimeType,
            mediaData = mediaData,
            lat = lat,
            lng = lng,
            groupCode = groupCode,
            dropCreatedAt = createdAt,
            dropType = dropType,
            businessId = businessId,
            businessName = businessName,
            redemptionLimit = redemptionLimit,
            redemptionCount = redemptionCount,
            collectedAt = System.currentTimeMillis(),
            isNsfw = resolvedIsNsfw,
            nsfwLabels = nsfwLabels,
            nsfwConfidence = nsfwConfidence
        )
        inventory.saveCollected(note)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (!userId.isNullOrBlank()) {
            try {
                repo.markDropCollected(dropId, userId)
            } catch (error: Exception) {
                Log.w(TAG, "Failed to mark drop $dropId as collected for $userId", error)
            }
        }

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
        const val EXTRA_DROP_MEDIA_MIME_TYPE = "extra_drop_media_mime_type"
        const val EXTRA_DROP_MEDIA_DATA = "extra_drop_media_data"
        const val EXTRA_DROP_LAT = "extra_drop_lat"
        const val EXTRA_DROP_LNG = "extra_drop_lng"
        const val EXTRA_DROP_CREATED_AT = "extra_drop_created_at"
        const val EXTRA_DROP_GROUP = "extra_drop_group"
        const val EXTRA_DROP_TYPE = "extra_drop_type"
        const val EXTRA_DROP_BUSINESS_ID = "extra_drop_business_id"
        const val EXTRA_DROP_BUSINESS_NAME = "extra_drop_business_name"
        const val EXTRA_DROP_REDEMPTION_LIMIT = "extra_drop_redemption_limit"
        const val EXTRA_DROP_REDEMPTION_COUNT = "extra_drop_redemption_count"
        const val EXTRA_DROP_IS_NSFW = "extra_drop_is_nsfw"
        const val EXTRA_DROP_NSFW_LABELS = "extra_drop_nsfw_labels"
        const val EXTRA_DROP_NSFW_CONFIDENCE = "extra_drop_nsfw_confidence"

        private const val TAG = "DropDecisionReceiver"
    }
}