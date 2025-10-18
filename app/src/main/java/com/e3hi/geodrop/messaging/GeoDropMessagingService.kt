package com.e3hi.geodrop.messaging

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.e3hi.geodrop.R
import com.e3hi.geodrop.data.FirestoreRepo
import com.e3hi.geodrop.ui.DropDetailActivity
import com.e3hi.geodrop.util.CHANNEL_USER_UPDATES
import com.e3hi.geodrop.util.MessagingTokenStore
import com.e3hi.geodrop.util.createNotificationChannelIfNeeded
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeoDropMessagingService : FirebaseMessagingService() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return

        val store = MessagingTokenStore(applicationContext)
        store.saveToken(trimmed)

        val userId = auth.currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirestoreRepo().registerMessagingToken(userId, trimmed)
                store.markSynced(trimmed)
            } catch (error: Exception) {
                Log.e(TAG, "Failed to register refreshed messaging token", error)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        createNotificationChannelIfNeeded(this)

        val data = message.data
        val event = data[KEY_EVENT]

        val title = data[KEY_TITLE]?.takeIf { it.isNotBlank() }
            ?: resolveTitle(event)
        val body = data[KEY_BODY]?.takeIf { it.isNotBlank() }
            ?: resolveBody(event, data)

        if (title.isNullOrBlank() || body.isNullOrBlank()) {
            Log.w(TAG, "Skipping push with missing title/body for event=$event")
            return
        }

        val dropId = data[KEY_DROP_ID]?.takeIf { it.isNotBlank() }
        val notificationId = dropId?.hashCode() ?: message.messageId?.hashCode()
        ?: abs(System.currentTimeMillis().toInt())

        val builder = NotificationCompat.Builder(this, CHANNEL_USER_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)

        val contentIntent = dropId?.let { id ->
            val open = Intent(this, DropDetailActivity::class.java).apply {
                putExtra("dropId", id)
            }
            TaskStackBuilder.create(this).run {
                addNextIntentWithParentStack(open)
                getPendingIntent(id.hashCode(), PendingIntentFlagsCompat)
            } ?: PendingIntent.getActivity(this, id.hashCode(), open, PendingIntentFlagsCompat)
        }

        if (contentIntent != null) {
            builder.setContentIntent(contentIntent)
        }

        NotificationManagerCompat.from(this).notify(notificationId, builder.build())
    }

    private fun resolveTitle(event: String?): String {
        return when (event) {
            EVENT_DROP_COLLECTED -> getString(R.string.push_drop_collected_title)
            else -> getString(R.string.app_name)
        }
    }

    private fun resolveBody(event: String?, data: Map<String, String>): String? {
        return when (event) {
            EVENT_DROP_COLLECTED -> buildDropCollectedBody(data)
            else -> null
        }
    }

    private fun buildDropCollectedBody(data: Map<String, String>): String? {
        val collectorName = data[KEY_COLLECTOR_NAME]?.takeIf { it.isNotBlank() }
            ?: getString(R.string.push_drop_collected_default_collector)
        val collectorCount = data[KEY_COLLECTOR_COUNT]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val dropTitle = data[KEY_DROP_TITLE]
        val dropDescription = data[KEY_DROP_DESCRIPTION]
        val dropContentType = data[KEY_DROP_CONTENT_TYPE]

        val dropLabel = data[KEY_DROP_LABEL]?.takeIf { it.isNotBlank() }
            ?: resolveDropLabel(dropTitle, dropDescription, dropContentType)

        return if (collectorCount <= 1) {
            getString(R.string.push_drop_collected_body_singular, collectorName, dropLabel)
        } else {
            val others = (collectorCount - 1).coerceAtLeast(1)
            resources.getQuantityString(
                R.plurals.push_drop_collected_body_plural,
                others,
                collectorName,
                others,
                dropLabel
            )
        }
    }

    private fun resolveDropLabel(
        rawTitle: String?,
        rawDescription: String?,
        rawContentType: String?
    ): String {
        val normalizedTitle = rawTitle?.trim().orEmpty()
        if (normalizedTitle.isNotEmpty()) {
            return "\"${truncate(normalizedTitle)}\""
        }

        val normalizedDescription = rawDescription?.trim().orEmpty()
        if (normalizedDescription.isNotEmpty()) {
            return "\"${truncate(normalizedDescription)}\""
        }

        return when (rawContentType?.uppercase(Locale.US)) {
            "PHOTO" -> getString(R.string.push_drop_label_photo)
            "AUDIO" -> getString(R.string.push_drop_label_audio)
            "VIDEO" -> getString(R.string.push_drop_label_video)
            else -> getString(R.string.push_drop_default_label)
        }
    }

    private fun truncate(value: String, maxLength: Int = 60): String {
        if (value.length <= maxLength) return value
        return value.take(maxLength - 1) + '\u2026'
    }

    companion object {
        private const val TAG = "GeoDropMsgSvc"

        private const val KEY_EVENT = "event"
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        private const val KEY_DROP_ID = "dropId"
        private const val KEY_DROP_TITLE = "dropTitle"
        private const val KEY_DROP_DESCRIPTION = "dropDescription"
        private const val KEY_DROP_CONTENT_TYPE = "dropContentType"
        private const val KEY_DROP_LABEL = "dropLabel"
        private const val KEY_COLLECTOR_NAME = "collectorName"
        private const val KEY_COLLECTOR_COUNT = "collectorCount"

        private const val EVENT_DROP_COLLECTED = "DROP_COLLECTED"

        private val PendingIntentFlagsCompat =
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (android.os.Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
    }
}