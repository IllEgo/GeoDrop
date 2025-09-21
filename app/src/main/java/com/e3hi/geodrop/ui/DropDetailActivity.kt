// ui/DropDetailActivity.kt
package com.e3hi.geodrop.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.e3hi.geodrop.MainActivity
import com.e3hi.geodrop.data.DropContentType
import com.e3hi.geodrop.data.NoteInventory
import com.e3hi.geodrop.geo.DropDecisionReceiver
import com.e3hi.geodrop.util.formatTimestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.webkit.MimeTypeMap

class DropDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dropId = intent.getStringExtra("dropId").orEmpty()
        val initialText = intent.getStringExtra("dropText")
        val initialLat = if (intent.hasExtra("dropLat")) intent.getDoubleExtra("dropLat", 0.0) else null
        val initialLng = if (intent.hasExtra("dropLng")) intent.getDoubleExtra("dropLng", 0.0) else null
        val initialCreatedAt = intent.getLongExtra("dropCreatedAt", -1L).takeIf { it > 0L }
        val initialGroupCode = intent.getStringExtra("dropGroupCode")?.takeIf { it.isNotBlank() }
        val initialContentType = DropContentType.fromRaw(intent.getStringExtra("dropContentType"))
        val initialMediaUrl = intent.getStringExtra("dropMediaUrl")?.takeIf { it.isNotBlank() }
        val initialMediaMimeType = intent.getStringExtra("dropMediaMimeType")?.takeIf { it.isNotBlank() }
        val initialMediaData = intent.getStringExtra("dropMediaData")?.takeIf { it.isNotBlank() }
        val showDecisionOptions = intent.getBooleanExtra(EXTRA_SHOW_DECISION_OPTIONS, false)
        setContent {
            val context = LocalContext.current

            val initialState = if (dropId.isBlank()) {
                if (
                    initialText != null ||
                    initialLat != null ||
                    initialLng != null ||
                    initialCreatedAt != null ||
                    initialGroupCode != null ||
                    initialMediaUrl != null ||
                    initialMediaData != null
                ) {
                    DropDetailUiState.Loaded(
                        text = initialText,
                        lat = initialLat,
                        lng = initialLng,
                        createdAt = initialCreatedAt,
                        groupCode = initialGroupCode,
                        contentType = initialContentType,
                        mediaUrl = initialMediaUrl,
                        mediaMimeType = initialMediaMimeType,
                        mediaData = initialMediaData
                    )
                } else {
                    DropDetailUiState.NotFound
                }
            } else if (
                initialText != null ||
                initialLat != null ||
                initialLng != null ||
                initialCreatedAt != null ||
                initialGroupCode != null ||
                initialMediaUrl != null ||
                initialMediaData != null
            ) {
                DropDetailUiState.Loaded(
                    text = initialText,
                    lat = initialLat,
                    lng = initialLng,
                    createdAt = initialCreatedAt,
                    groupCode = initialGroupCode,
                    contentType = initialContentType,
                    mediaUrl = initialMediaUrl,
                    mediaMimeType = initialMediaMimeType,
                    mediaData = initialMediaData
                )
            } else {
                DropDetailUiState.Loading
            }

            var state by remember { mutableStateOf(initialState) }

            LaunchedEffect(dropId) {
                if (dropId.isBlank()) return@LaunchedEffect
                val previousLoaded = state as? DropDetailUiState.Loaded
                if (previousLoaded == null) {
                    state = DropDetailUiState.Loading
                }
                val doc = Firebase.firestore.collection("drops").document(dropId).get().awaitOrNull()

                if (doc == null) {
                    previousLoaded?.let {
                        Toast.makeText(
                            context,
                            "Couldn't refresh drop details. Showing saved info.",
                            Toast.LENGTH_SHORT
                        ).show()
                        state = it
                    } ?: run {
                        state = DropDetailUiState.NotFound
                    }
                    return@LaunchedEffect
                }

                val isDeleted = doc.getBoolean("isDeleted") == true
                if (isDeleted) {
                    state = DropDetailUiState.Deleted
                    return@LaunchedEffect
                }

                state = DropDetailUiState.Loaded(
                    text = doc.getString("text")?.takeIf { it.isNotBlank() }
                        ?: previousLoaded?.text
                        ?: initialText,
                    lat = doc.getDouble("lat") ?: previousLoaded?.lat ?: initialLat,
                    lng = doc.getDouble("lng") ?: previousLoaded?.lng ?: initialLng,
                    createdAt = doc.getLong("createdAt")?.takeIf { it > 0L }
                        ?: previousLoaded?.createdAt
                        ?: initialCreatedAt,
                    groupCode = doc.getString("groupCode")?.takeIf { it.isNotBlank() }
                        ?: previousLoaded?.groupCode
                        ?: initialGroupCode,
                    contentType = doc.getString("contentType")?.let { DropContentType.fromRaw(it) }
                        ?: previousLoaded?.contentType
                        ?: initialContentType,
                    mediaUrl = doc.getString("mediaUrl")?.takeIf { it.isNotBlank() }
                        ?: previousLoaded?.mediaUrl
                        ?: initialMediaUrl,
                    mediaMimeType = doc.getString("mediaMimeType")?.takeIf { it.isNotBlank() }
                        ?: previousLoaded?.mediaMimeType
                        ?: initialMediaMimeType,
                    mediaData = (
                            doc.getString("mediaData")?.takeIf { it.isNotBlank() }
                                ?: doc.getString("audioFile")?.takeIf { it.isNotBlank() }
                                ?: doc.getBlob("mediaData")?.toBytes()?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                                ?: doc.getBlob("audioFile")?.toBytes()?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                            )
                        ?: previousLoaded?.mediaData
                        ?: initialMediaData
                )
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Drop detail", style = MaterialTheme.typography.titleLarge)
                Text("ID: $dropId", style = MaterialTheme.typography.bodyMedium)

                Text("Message", style = MaterialTheme.typography.titleMedium)
                val message = when (val current = state) {
                    is DropDetailUiState.Loaded -> {
                        val fallback = when (current.contentType) {
                            DropContentType.TEXT -> "Not available"
                            DropContentType.PHOTO -> "Photo drop"
                            DropContentType.AUDIO -> "Audio drop"
                        }
                        current.text?.takeIf { it.isNotBlank() } ?: fallback
                    }
                    DropDetailUiState.Loading -> "Loading…"
                    DropDetailUiState.Deleted -> "Not available"
                    DropDetailUiState.NotFound -> "Not available"
                }
                Text(message, style = MaterialTheme.typography.bodyLarge)

                if (state is DropDetailUiState.Deleted) {
                    Text(
                        "This drop has been deleted.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.height(8.dp))

                val typeText = when (val current = state) {
                    is DropDetailUiState.Loaded -> when (current.contentType) {
                        DropContentType.TEXT -> "Text note"
                        DropContentType.PHOTO -> "Photo drop"
                        DropContentType.AUDIO -> "Audio drop"
                    }
                    DropDetailUiState.Loading -> "Loading…"
                    DropDetailUiState.Deleted -> "Not available"
                    DropDetailUiState.NotFound -> "Not available"
                }
                Text(
                    "Type: $typeText",
                    style = MaterialTheme.typography.bodyMedium
                )

                val loadedState = state as? DropDetailUiState.Loaded
                val mediaAttachment = when (loadedState) {
                    null -> null
                    else -> remember(
                        loadedState.mediaUrl,
                        loadedState.mediaData,
                        loadedState.mediaMimeType,
                        loadedState.contentType
                    ) {
                        resolveMediaAttachment(context, loadedState)
                    }
                }

                if (loadedState?.contentType == DropContentType.PHOTO) {
                    val link = loadedState.mediaUrl
                    if (!link.isNullOrBlank()) {
                        val imageRequest = remember(link) {
                            ImageRequest.Builder(context)
                                .data(link)
                                .crossfade(true)
                                .build()
                        }

                        AsyncImage(
                            model = imageRequest,
                            contentDescription = loadedState.text ?: "Photo drop",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = 360.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (loadedState != null && mediaAttachment != null) {
                    val buttonLabel = when (loadedState.contentType) {
                        DropContentType.TEXT -> "Open attachment"
                        DropContentType.PHOTO -> "View photo"
                        DropContentType.AUDIO -> "Play audio"
                    }
                    Button(
                        onClick = {
                            when (mediaAttachment) {
                                is MediaAttachment.Link -> {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mediaAttachment.url))
                                    runCatching { context.startActivity(intent) }
                                        .onFailure {
                                            Toast.makeText(
                                                context,
                                                "No app found to open this media.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }

                                is MediaAttachment.Local -> {
                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(mediaAttachment.uri, mediaAttachment.mimeType)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.grantUriPermission(
                                        context.packageName,
                                        mediaAttachment.uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                    runCatching { context.startActivity(viewIntent) }
                                        .onFailure {
                                            Toast.makeText(
                                                context,
                                                "No app found to open this media.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(buttonLabel)
                    }

                    when (mediaAttachment) {
                        is MediaAttachment.Link -> {
                            Text(
                                text = mediaAttachment.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is MediaAttachment.Local -> {
                            loadedState.mediaMimeType?.let { mime ->
                                Text(
                                    text = mime,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                val appContext = context.applicationContext
                val noteInventory = remember(appContext) { NoteInventory(appContext) }
                var decisionHandled by remember(dropId) { mutableStateOf(false) }
                var decisionStatusMessage by remember(dropId) { mutableStateOf<String?>(null) }
                var decisionProcessing by remember(dropId) { mutableStateOf(false) }
                val isAlreadyCollected = remember(dropId) {
                    dropId.isNotBlank() && noteInventory.isCollected(dropId)
                }
                val isAlreadyIgnored = remember(dropId) {
                    dropId.isNotBlank() && noteInventory.isIgnored(dropId)
                }
                val shouldShowDecisionPrompt =
                    showDecisionOptions && dropId.isNotBlank() && !isAlreadyCollected && !isAlreadyIgnored && !decisionHandled
                val defaultDecisionMessage = when {
                    showDecisionOptions && isAlreadyCollected -> "You've already picked up this drop."
                    showDecisionOptions && isAlreadyIgnored -> "You've already ignored this drop."
                    else -> null
                }
                val decisionMessage = decisionStatusMessage ?: defaultDecisionMessage

                Spacer(Modifier.height(8.dp))

                val visibilityText = when (val current = state) {
                    is DropDetailUiState.Loaded -> current.groupCode?.let { "Group-only · $it" } ?: "Public drop"
                    DropDetailUiState.Loading -> "Loading…"
                    DropDetailUiState.Deleted -> "Not available"
                    DropDetailUiState.NotFound -> "Not available"
                }
                Text(
                    "Visibility: $visibilityText",
                    style = MaterialTheme.typography.bodyMedium
                )

                val createdAtText = when (val current = state) {
                    is DropDetailUiState.Loaded -> current.createdAt?.let { formatTimestamp(it) }
                    DropDetailUiState.Loading -> "Loading…"
                    DropDetailUiState.Deleted -> null
                    DropDetailUiState.NotFound -> null
                }
                Text(
                    "Dropped: ${createdAtText ?: "Not available"}",
                    style = MaterialTheme.typography.bodyMedium
                )


                val latText = when (val current = state) {
                    is DropDetailUiState.Loaded -> current.lat?.let { formatCoordinate(it) } ?: "-"
                    DropDetailUiState.Loading -> "Loading…"
                    DropDetailUiState.Deleted -> "-"
                    DropDetailUiState.NotFound -> "-"
                }
                val lngText = when (val current = state) {
                    is DropDetailUiState.Loaded -> current.lng?.let { formatCoordinate(it) } ?: "-"
                    DropDetailUiState.Loading -> "Loading…"
                    DropDetailUiState.Deleted -> "-"
                    DropDetailUiState.NotFound -> "-"
                }

                Text("Lat: $latText")
                Text("Lng: $lngText")

                if (shouldShowDecisionPrompt) {
                    Spacer(Modifier.height(16.dp))
                    val decisionPrompt = when (loadedState?.contentType) {
                        DropContentType.TEXT -> "Would you like to pick up this note?"
                        DropContentType.PHOTO -> "Would you like to pick up this photo?"
                        DropContentType.AUDIO -> "Would you like to pick up this audio drop?"
                        null -> "Would you like to pick up this drop?"
                    }
                    Text(decisionPrompt, style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val current = loadedState ?: return@Button
                                decisionProcessing = true
                                val pickupIntent = Intent(appContext, DropDecisionReceiver::class.java).apply {
                                    action = DropDecisionReceiver.ACTION_PICK_UP
                                    putExtra(DropDecisionReceiver.EXTRA_DROP_ID, dropId)
                                    current.text?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_TEXT, it) }
                                    current.lat?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_LAT, it) }
                                    current.lng?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_LNG, it) }
                                    current.createdAt?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_CREATED_AT, it) }
                                    current.groupCode?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_GROUP, it) }
                                    putExtra(DropDecisionReceiver.EXTRA_DROP_CONTENT_TYPE, current.contentType.name)
                                    current.mediaUrl?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_MEDIA_URL, it) }
                                    current.mediaMimeType?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_MEDIA_MIME_TYPE, it) }
                                    current.mediaData?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_MEDIA_DATA, it) }
                                }
                                val result = runCatching {
                                    appContext.sendBroadcast(pickupIntent)
                                    NotificationManagerCompat.from(appContext).cancel(dropId.hashCode())
                                }
                                decisionProcessing = false
                                if (result.isSuccess) {
                                    decisionHandled = true
                                    decisionStatusMessage = "Drop added to your collection."
                                    Toast.makeText(context, "Drop added to your collection.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Couldn't pick up this drop.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !decisionProcessing && loadedState != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pick up")
                        }

                        OutlinedButton(
                            onClick = {
                                decisionProcessing = true
                                val ignoreIntent = Intent(appContext, DropDecisionReceiver::class.java).apply {
                                    action = DropDecisionReceiver.ACTION_IGNORE
                                    putExtra(DropDecisionReceiver.EXTRA_DROP_ID, dropId)
                                }
                                val result = runCatching {
                                    appContext.sendBroadcast(ignoreIntent)
                                    NotificationManagerCompat.from(appContext).cancel(dropId.hashCode())
                                }
                                decisionProcessing = false
                                if (result.isSuccess) {
                                    decisionHandled = true
                                    decisionStatusMessage = "Drop ignored. You won't be notified about it again."
                                    Toast.makeText(context, "Drop ignored.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Couldn't ignore this drop.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !decisionProcessing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ignore")
                        }
                    }
                }

                decisionMessage?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        val activity = context as? Activity
                        activity?.let {
                            if (it.isTaskRoot) {
                                val backIntent = Intent(it, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                it.startActivity(backIntent)
                            }
                            it.finish()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to GeoDrop")
                }
            }
        }
    }

    companion object {
        const val EXTRA_SHOW_DECISION_OPTIONS = "showDecisionOptions"
    }
}

// tiny helper
private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitOrNull(): T? =
    try { com.google.android.gms.tasks.Tasks.await(this) } catch (_: Exception) { null }

private fun formatCoordinate(value: Double): String = "%.5f".format(value)

private fun resolveMediaAttachment(
    context: Context,
    state: DropDetailUiState.Loaded
): MediaAttachment? {
    if (state.contentType == DropContentType.AUDIO) {
        val data = state.mediaData?.takeIf { it.isNotBlank() }
        if (data != null) {
            val preferredMime = state.mediaMimeType?.takeIf { it.isNotBlank() }
            val decoded = decodeBase64ToTempFile(
                context = context,
                base64Data = data,
                mimeType = preferredMime,
                subDir = "audio",
                defaultMime = preferredMime ?: "audio/mpeg",
                defaultExtension = "m4a"
            )
            if (decoded != null) {
                return MediaAttachment.Local(decoded.uri, decoded.mimeType)
            }
        }
    }

    val url = state.mediaUrl?.takeIf { it.isNotBlank() }
    return url?.let { MediaAttachment.Link(it) }
}

private data class DecodedMedia(val uri: Uri, val mimeType: String)

private fun decodeBase64ToTempFile(
    context: Context,
    base64Data: String,
    mimeType: String?,
    subDir: String,
    defaultMime: String,
    defaultExtension: String
): DecodedMedia? {
    return try {
        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
        val resolvedMime = mimeType?.takeIf { it.isNotBlank() } ?: defaultMime
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(resolvedMime)?.takeIf { it.isNotBlank() }
            ?: defaultExtension
        val dir = File(context.cacheDir, subDir).apply { if (!exists()) mkdirs() }
        val file = File.createTempFile("geodrop_media_", ".${extension}", dir)
        FileOutputStream(file).use { output ->
            output.write(bytes)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        DecodedMedia(uri, resolvedMime)
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: IOException) {
        null
    }
}

private sealed class MediaAttachment {
    data class Link(val url: String) : MediaAttachment()
    data class Local(val uri: Uri, val mimeType: String) : MediaAttachment()
}

private sealed interface DropDetailUiState {
    data class Loaded(
        val text: String?,
        val lat: Double?,
        val lng: Double?,
        val createdAt: Long?,
        val groupCode: String?,
        val contentType: DropContentType = DropContentType.TEXT,
        val mediaUrl: String? = null,
        val mediaMimeType: String? = null,
        val mediaData: String? = null
    ) : DropDetailUiState

    object Loading : DropDetailUiState
    object NotFound : DropDetailUiState
    object Deleted : DropDetailUiState
}
