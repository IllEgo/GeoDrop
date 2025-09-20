// ui/DropDetailActivity.kt
package com.e3hi.geodrop.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import com.e3hi.geodrop.MainActivity
import com.e3hi.geodrop.util.formatTimestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class DropDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dropId = intent.getStringExtra("dropId").orEmpty()
        val initialText = intent.getStringExtra("dropText")
        val initialLat = if (intent.hasExtra("dropLat")) intent.getDoubleExtra("dropLat", 0.0) else null
        val initialLng = if (intent.hasExtra("dropLng")) intent.getDoubleExtra("dropLng", 0.0) else null
        val initialCreatedAt = intent.getLongExtra("dropCreatedAt", -1L).takeIf { it > 0L }
        val initialGroupCode = intent.getStringExtra("dropGroupCode")?.takeIf { it.isNotBlank() }
        setContent {
            val context = LocalContext.current
            var state by remember {
                mutableStateOf(
                    if (dropId.isBlank()) {
                        if (initialText != null || initialLat != null || initialLng != null || initialCreatedAt != null) {
                            DropDetailUiState.Loaded(initialText, initialLat, initialLng, initialCreatedAt, initialGroupCode)
                        } else {
                            DropDetailUiState.NotFound
                        }
                    } else {
                        DropDetailUiState.Loading
                    }
                )
            }

            LaunchedEffect(dropId) {
                if (dropId.isBlank()) return@LaunchedEffect
                state = DropDetailUiState.Loading
                val doc = Firebase.firestore.collection("drops").document(dropId).get().awaitOrNull()
                val isDeleted = doc?.getBoolean("isDeleted") == true
                state = when {
                    doc == null -> DropDetailUiState.NotFound
                    isDeleted -> DropDetailUiState.Deleted
                    else -> DropDetailUiState.Loaded(
                        text = doc.getString("text")?.takeIf { it.isNotBlank() } ?: initialText,
                        lat = doc.getDouble("lat") ?: initialLat,
                        lng = doc.getDouble("lng") ?: initialLng,
                        createdAt = doc.getLong("createdAt")?.takeIf { it > 0L } ?: initialCreatedAt,
                        groupCode = doc.getString("groupCode")?.takeIf { it.isNotBlank() } ?: initialGroupCode
                    )
                }
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
                    is DropDetailUiState.Loaded -> current.text ?: "Not available"
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
        val groupCode: String?
    ) : DropDetailUiState

    object Loading : DropDetailUiState
    object NotFound : DropDetailUiState
    object Deleted : DropDetailUiState
}
