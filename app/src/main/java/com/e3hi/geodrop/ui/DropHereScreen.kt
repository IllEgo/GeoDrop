package com.e3hi.geodrop.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.e3hi.geodrop.data.Drop
import com.e3hi.geodrop.data.FirestoreRepo
import com.e3hi.geodrop.geo.NearbyDropRegistrar
import com.e3hi.geodrop.geo.NearbyDropRegistrar.NearbySyncStatus
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("MissingPermission")
@Composable
fun DropHereScreen() {
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val fused = remember { LocationServices.getFusedLocationProviderClient(ctx) }
    val repo = remember { FirestoreRepo() }
    val registrar = remember { NearbyDropRegistrar() }

    var note by remember { mutableStateOf(TextFieldValue("")) }
    var isSubmitting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    // Optional: also sync nearby on first open if already signed in
    LaunchedEffect(Unit) {
        if (FirebaseAuth.getInstance().currentUser != null) {
            registrar.registerNearby(ctx, maxMeters = 300.0)
        }
    }

    fun uiDone(lat: Double, lng: Double) {
        isSubmitting = false
        note = TextFieldValue("")
        status = "Dropped at (%.5f, %.5f)".format(lat, lng)
        scope.launch { snackbar.showSnackbar("Note dropped!") }
    }

    suspend fun addDropAt(lat: Double, lng: Double) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anon"
        val d = Drop(
            text = note.text.ifBlank { "New drop" },
            lat = lat,
            lng = lng,
            createdBy = uid,
            createdAt = System.currentTimeMillis()
        )
        repo.addDrop(d) // suspend (uses Firestore .await() internally)
        uiDone(lat, lng)
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Drop a note at your current location", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Your note") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            enabled = !isSubmitting,
            onClick = {
                isSubmitting = true
                scope.launch {
                    try {
                        val (lat, lng) = withContext(Dispatchers.IO) {
                            // Try fresh high-accuracy first
                            val fresh = try {
                                val cts = CancellationTokenSource()
                                Tasks.await(fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token))
                            } catch (_: Exception) { null }

                            val loc = fresh ?: try {
                                Tasks.await(fused.lastLocation)
                            } catch (_: Exception) { null }

                            if (loc == null) null else loc.latitude to loc.longitude
                        } ?: run {
                            isSubmitting = false
                            snackbar.showMessage(scope, "No location available. Turn on GPS & try again.")
                            return@launch
                        }

                        addDropAt(lat, lng)
                    } catch (e: Exception) {
                        isSubmitting = false
                        snackbar.showMessage(scope, "Error: ${e.message}")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (isSubmitting) "Dropping…" else "Drop here") }

        // Handy for testing: re-scan & register geofences without restarting
        Button(
            onClick = {
                registrar.registerNearby(ctx, maxMeters = 300.0) { statusResult ->
                    when (statusResult) {
                        is NearbySyncStatus.Success -> {
                            val msg = if (statusResult.count > 0) {
                                val base = "Found ${statusResult.count} drop" +
                                        if (statusResult.count == 1) " nearby from another user." else "s nearby from other users."
                                "$base You'll be notified when you're close."
                            } else {
                                "No nearby drops from other users right now."
                            }
                            status = msg
                            snackbar.showMessage(scope, msg)
                        }
                        NearbySyncStatus.MissingPermission -> {
                            val msg = "Location permission is required to sync nearby drops."
                            status = msg
                            snackbar.showMessage(scope, msg)
                        }
                        NearbySyncStatus.NoLocation -> {
                            val msg = "Couldn't get your current location. Turn on GPS and try again."
                            status = msg
                            snackbar.showMessage(scope, msg)
                        }
                        is NearbySyncStatus.Error -> {
                            val msg = statusResult.message
                            status = msg
                            snackbar.showMessage(scope, msg)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Sync nearby drops") }

        Button(
            onClick = {
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "anon"
                val d = com.e3hi.geodrop.data.Drop(
                    text = "Test from button",
                    lat = 0.0, lng = 0.0,
                    createdBy = uid,
                    createdAt = System.currentTimeMillis()
                )
                com.e3hi.geodrop.data.FirestoreRepo().createDrop(
                    d,
                    onId = { id ->
                        // show snackbar from a coroutine, not LaunchedEffect
                        scope.launch { snackbar.showSnackbar("Wrote doc $id") }
                    },
                    onError = { e ->
                        scope.launch { snackbar.showSnackbar("Write failed: ${e.message}") }
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Test Firestore write") }

        status?.let { Text(it) }

        Spacer(Modifier.weight(1f))
        SnackbarHost(hostState = snackbar, modifier = Modifier.fillMaxWidth())
    }
}

/** Tiny helper to show snackbars from non-suspend places. */
private fun SnackbarHostState.showMessage(scope: kotlinx.coroutines.CoroutineScope, msg: String) {
    scope.launch { showSnackbar(msg) }
}
