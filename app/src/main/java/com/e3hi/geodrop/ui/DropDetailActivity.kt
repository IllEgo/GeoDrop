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
import androidx.compose.ui.unit.dp
import com.e3hi.geodrop.MainActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class DropDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dropId = intent.getStringExtra("dropId").orEmpty()
        val initialText = intent.getStringExtra("dropText")
        val initialLat = if (intent.hasExtra("dropLat")) intent.getDoubleExtra("dropLat", 0.0) else null
        val initialLng = if (intent.hasExtra("dropLng")) intent.getDoubleExtra("dropLng", 0.0) else null
        setContent {
            val context = LocalContext.current
            var text by remember { mutableStateOf(initialText) }
            var lat by remember { mutableStateOf(initialLat) }
            var lng by remember { mutableStateOf(initialLng) }
            var isLoading by remember { mutableStateOf(text == null) }

            LaunchedEffect(dropId) {
                if (dropId.isBlank()) {
                    isLoading = false
                    return@LaunchedEffect
                }
                if (text == null || lat == null || lng == null) {
                    isLoading = true
                    val doc = Firebase.firestore.collection("drops").document(dropId).get().awaitOrNull()
                    text = doc?.getString("text")?.takeIf { it.isNotBlank() } ?: text
                    lat = doc?.getDouble("lat") ?: lat
                    lng = doc?.getDouble("lng") ?: lng
                    isLoading = false
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
                val message = when {
                    !text.isNullOrBlank() -> text!!
                    isLoading -> "Loading…"
                    else -> "Not available"
                }
                Text(message, style = MaterialTheme.typography.bodyLarge)

                Text("Lat: ${lat?.let { formatCoordinate(it) } ?: "-"}")
                Text("Lng: ${lng?.let { formatCoordinate(it) } ?: "-"}")

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
