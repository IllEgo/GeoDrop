package com.kitheapp.geo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.kitheapp.data.DropContentType
import com.kitheapp.data.DropType
import com.kitheapp.data.FirestoreRepo
import com.kitheapp.data.NoteInventory
import com.kitheapp.util.ExplorerAccountStore
import com.kitheapp.util.PilotFeatureFlags
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Handles the user's response to a nearby drop *notification* (pick up vs ignore).
 *
 * The in-app pickup buttons call [DropCollector] directly. They used to broadcast
 * here, which silently coupled collecting a drop to the notification feature flag
 * below: with notifications disabled the receiver returned before saving anything,
 * while the UI still reported success. Keep this entry point notification-only.
 */
class DropDecisionReceiver : BroadcastReceiver() {
    private val repo by lazy { FirestoreRepo() }

    override fun onReceive(context: Context, intent: Intent) {
        // Scoped to this receiver because it exists to serve notification actions.
        // Collecting a drop must not depend on it — see DropCollector.
        if (!PilotFeatureFlags.notificationsEnabled) return
        val action = intent.action ?: return
        val dropId = intent.getStringExtra(EXTRA_DROP_ID) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_PICK_UP -> handlePickUp(context, intent, dropId)
                    ACTION_IGNORE -> handleIgnore(context, intent, dropId)
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
        val accountStore = ExplorerAccountStore(context)
        val storedExplorerId = accountStore.getLastExplorerUid()
        val userId = intent.getStringExtra(EXTRA_USER_ID)?.takeIf { it.isNotBlank() }
            ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: storedExplorerId
        val text = intent.getStringExtra(EXTRA_DROP_TEXT) ?: ""
        val description = intent.getStringExtra(EXTRA_DROP_DESCRIPTION)
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
        val resolvedIsNsfw = isNsfw || nsfwLabels.isNotEmpty()
        val decayDays = if (intent.hasExtra(EXTRA_DROP_DECAY_DAYS)) {
            intent.getIntExtra(EXTRA_DROP_DECAY_DAYS, 0).takeIf { it > 0 }
        } else {
            null
        }
        val huntId = intent.getStringExtra(EXTRA_DROP_HUNT_ID)?.takeIf { it.isNotBlank() }
        val huntStepIndex = if (intent.hasExtra(EXTRA_DROP_HUNT_STEP_INDEX)) {
            intent.getIntExtra(EXTRA_DROP_HUNT_STEP_INDEX, -1).takeIf { it >= 0 }
        } else null
        val huntTotalSteps = if (intent.hasExtra(EXTRA_DROP_HUNT_TOTAL_STEPS)) {
            intent.getIntExtra(EXTRA_DROP_HUNT_TOTAL_STEPS, 0).takeIf { it > 0 }
        } else null
        val request = DropCollectionRequest(
            dropId = dropId,
            text = text,
            description = description,
            contentType = contentType,
            mediaUrl = mediaUrl,
            mediaMimeType = mediaMimeType,
            mediaData = mediaData,
            lat = lat,
            lng = lng,
            createdAt = createdAt,
            groupCode = groupCode,
            dropType = dropType,
            businessId = businessId,
            businessName = businessName,
            redemptionLimit = redemptionLimit,
            redemptionCount = redemptionCount,
            isNsfw = resolvedIsNsfw,
            nsfwLabels = nsfwLabels,
            decayDays = decayDays,
            huntId = huntId,
            huntStepIndex = huntStepIndex,
            huntTotalSteps = huntTotalSteps
        )

        val result = DropCollector.collect(
            context = context,
            request = request,
            userId = userId,
            repo = repo
        )
        if (result != DropCollectionResult.Collected) {
            Log.d(TAG, "Pick up for drop $dropId was not collected: $result")
            return
        }

        removeGeofence(context, dropId)
    }

    private suspend fun handleIgnore(context: Context, intent: Intent, dropId: String) {
        val userId = intent.getStringExtra(EXTRA_USER_ID)?.takeIf { it.isNotBlank() }
            ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: ExplorerAccountStore(context).getLastExplorerUid()
        val inventory = NoteInventory(context)
        inventory.setActiveUser(userId)
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
        const val ACTION_PICK_UP = "com.kitheapp.action.PICK_UP_DROP"
        const val ACTION_IGNORE = "com.kitheapp.action.IGNORE_DROP"

        const val EXTRA_DROP_ID = "extra_drop_id"
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_DROP_TEXT = "extra_drop_text"
        const val EXTRA_DROP_DESCRIPTION = "extra_drop_description"
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
        const val EXTRA_DROP_DECAY_DAYS = "extra_drop_decay_days"
        const val EXTRA_DROP_HUNT_ID = "extra_drop_hunt_id"
        const val EXTRA_DROP_HUNT_STEP_INDEX = "extra_drop_hunt_step_index"
        const val EXTRA_DROP_HUNT_TOTAL_STEPS = "extra_drop_hunt_total_steps"
        private const val TAG = "DropDecisionReceiver"
    }
}
