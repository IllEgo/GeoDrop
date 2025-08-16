// ui/DropDetailActivity.kt
package com.e3hi.geodrop.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.compose.runtime.*

class DropDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dropId = intent.getStringExtra("dropId").orEmpty()
        setContent {
            var text by remember { mutableStateOf<String?>(null) }
            var lat by remember { mutableStateOf<Double?>(null) }
            var lng by remember { mutableStateOf<Double?>(null) }

            LaunchedEffect(dropId) {
                if (dropId.isNotBlank()) {
                    val doc = Firebase.firestore.collection("drops").document(dropId).get().awaitOrNull()
                    text = doc?.getString("text")
                    lat = doc?.getDouble("lat")
                    lng = doc?.getDouble("lng")
                }
            }

            Column(Modifier.padding(20.dp)) {
                Text("Drop Detail", style = MaterialTheme.typography.titleLarge)
                Text("ID: $dropId")
                Text("Text: ${text ?: "(loading…)"}")
                Text("Lat: ${lat ?: "-"}")
                Text("Lng: ${lng ?: "-"}")
            }
        }
    }
}

// tiny helper
private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitOrNull(): T? =
    try { com.google.android.gms.tasks.Tasks.await(this) } catch (_: Exception) { null }
