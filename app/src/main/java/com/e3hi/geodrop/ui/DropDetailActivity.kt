// ui/DropDetailActivity.kt
package com.e3hi.geodrop.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import com.e3hi.geodrop.MainActivity
import com.e3hi.geodrop.data.DropContentType
import com.e3hi.geodrop.util.formatTimestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import coil.compose.AsyncImage
import coil.request.ImageRequest

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
        setContent {
            val context = LocalContext.current

            val initialState = if (dropId.isBlank()) {
                if (
                    initialText != null ||
                    initialLat != null ||
                    initialLng != null ||
                    initialCreatedAt != null ||
                    initialGroupCode != null ||
                    initialMediaUrl != null
                ) {
                    DropDetailUiState.Loaded(
                        text = initialText,
                        lat = initialLat,
                        lng = initialLng,
                        createdAt = initialCreatedAt,
                        groupCode = initialGroupCode,
                        contentType = initialContentType,
                        mediaUrl = initialMediaUrl
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
                initialMediaUrl != null
            ) {
                DropDetailUiState.Loaded(
                    text = initialText,
                    lat = initialLat,
                    lng = initialLng,
                    createdAt = initialCreatedAt,
                    groupCode = initialGroupCode,
                    contentType = initialContentType,
                    mediaUrl = initialMediaUrl
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
                        ?: initialMediaUrl
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


                (state as? DropDetailUiState.Loaded)?.let { current ->
                    current.mediaUrl?.let { link ->
                        if (current.contentType == DropContentType.PHOTO) {
                            val imageRequest = remember(link) {
                                ImageRequest.Builder(context)
                                    .data(link)
                                    .crossfade(true)
                                    .build()
                            }

                            AsyncImage(
                                model = imageRequest,
                                contentDescription = current.text ?: "Photo drop",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp, max = 360.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(Modifier.height(8.dp))
                        }

                        val buttonLabel = when (current.contentType) {
                            DropContentType.TEXT -> "Open attachment"
                            DropContentType.PHOTO -> "View photo"
                            DropContentType.AUDIO -> "Play audio"
                        }
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                runCatching { context.startActivity(intent) }
                                    .onFailure {
                                        Toast.makeText(
                                            context,
                                            "No app found to open this media.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(buttonLabel)
                        }

                        Text(
                            text = link,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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
}

// tiny helper
private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitOrNull(): T? =
    try { com.google.android.gms.tasks.Tasks.await(this) } catch (_: Exception) { null }

private fun formatCoordinate(value: Double): String = "%.5f".format(value)

private sealed interface DropDetailUiState {
    data class Loaded(
        val text: String?,
        val lat: Double?,
        val lng: Double?,
        val createdAt: Long?,
        val groupCode: String?,
        val contentType: DropContentType = DropContentType.TEXT,
        val mediaUrl: String? = null
    ) : DropDetailUiState

    object Loading : DropDetailUiState
    object NotFound : DropDetailUiState
    object Deleted : DropDetailUiState
}
