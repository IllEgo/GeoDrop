package com.e3hi.geodrop.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.e3hi.geodrop.data.Drop
import com.e3hi.geodrop.data.FirestoreRepo
import com.e3hi.geodrop.geo.NearbyDropRegistrar
import com.e3hi.geodrop.geo.NearbyDropRegistrar.NearbySyncStatus
import com.e3hi.geodrop.util.formatTimestamp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
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
    var showMap by remember { mutableStateOf(false) }
    var mapError by remember { mutableStateOf<String?>(null) }
    var mapLoading by remember { mutableStateOf(false) }
    var mapDrops by remember { mutableStateOf<List<Drop>>(emptyList()) }
    var mapRefreshToken by remember { mutableStateOf(0) }
    var mapCurrentLocation by remember { mutableStateOf<LatLng?>(null) }

    suspend fun getLatestLocation(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val fresh = try {
            val cts = CancellationTokenSource()
            Tasks.await(fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token))
        } catch (_: Exception) {
            null
        }

        val loc = fresh ?: try {
            Tasks.await(fused.lastLocation)
        } catch (_: Exception) {
            null
        }

        loc?.let { it.latitude to it.longitude }
    }

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

    LaunchedEffect(showMap, mapRefreshToken) {
        if (showMap) {
            mapLoading = true
            mapError = null
            mapCurrentLocation = null
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                mapLoading = false
                mapError = "Sign-in is still in progress. Try again in a moment."
            } else {
                try {
                    mapDrops = repo.getDropsForUser(uid)
                        .sortedByDescending { it.createdAt }
                    mapCurrentLocation = getLatestLocation()?.let { (lat, lng) -> LatLng(lat, lng) }
                } catch (e: Exception) {
                    mapError = e.message ?: "Failed to load your drops."
                } finally {
                    mapLoading = false
                }
            }
        } else {
            mapDrops = emptyList()
            mapError = null
            mapLoading = false
            mapCurrentLocation = null
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
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
//                        val (lat, lng) = withContext(Dispatchers.IO) {
//                            // Try fresh high-accuracy first
//                            val fresh = try {
//                                val cts = CancellationTokenSource()
//                                Tasks.await(fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token))
//                            } catch (_: Exception) { null }
//
//                            val loc = fresh ?: try {
//                                Tasks.await(fused.lastLocation)
//                            } catch (_: Exception) { null }
//
//                            if (loc == null) null else loc.latitude to loc.longitude
//                        } ?: run {
                        val (lat, lng) = getLatestLocation() ?: run {
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
                if (FirebaseAuth.getInstance().currentUser == null) {
                    snackbar.showMessage(scope, "Signing you in… please try again shortly.")
                } else {
                    showMap = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("View my drops on map") }


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

    if (showMap) {
        DropsMapDialog(
            loading = mapLoading,
            drops = mapDrops,
            currentLocation = mapCurrentLocation,
            error = mapError,
            onDismiss = { showMap = false },
            onRetry = { mapRefreshToken += 1 }
        )
    }
}

@Composable
private fun DropsMapDialog(
    loading: Boolean,
    drops: List<Drop>,
    currentLocation: LatLng?,
    error: String?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            @OptIn(ExperimentalMaterial3Api::class)
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("My drops on map") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Filled.ArrowBack,
                                    contentDescription = "Back to main page"
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    when {
                        loading -> {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }

                        error != null -> {
                            MapDialogMessage(
                                message = error,
                                primaryLabel = "Retry",
                                onPrimary = onRetry,
                                onDismiss = onDismiss
                            )
                        }

                        drops.isEmpty() -> {
                            MapDialogMessage(
                                message = "You haven't dropped any notes yet.",
                                primaryLabel = null,
                                onPrimary = null,
                                onDismiss = onDismiss
                            )
                        }

                        else -> {
                            DropsMapContent(drops, currentLocation)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapDialogMessage(
    message: String,
    primaryLabel: String?,
    onPrimary: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        if (primaryLabel != null && onPrimary != null) {
            Button(onClick = onPrimary) {
                Text(primaryLabel)
            }
            Spacer(Modifier.height(8.dp))
        }

        TextButton(onClick = onDismiss) {
            Text("Back to main page")
        }
    }
}

@Composable
private fun DropsMapContent(drops: List<Drop>, currentLocation: LatLng?) {
    val cameraPositionState = rememberCameraPositionState()
    val uiSettings = remember { MapUiSettings(zoomControlsEnabled = true) }


    LaunchedEffect(drops, currentLocation) {
        val target = currentLocation ?: drops.firstOrNull()?.let { LatLng(it.lat, it.lng) }
        if (target != null) {
            val update = CameraUpdateFactory.newLatLngZoom(target, 13f)
            cameraPositionState.animate(update)
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = uiSettings
    ) {
        currentLocation?.let { location ->
            Marker(
                state = MarkerState(location),
                title = "Your current location",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                zIndex = 1f
            )
        }

        drops.forEach { drop ->
            val position = LatLng(drop.lat, drop.lng)
            val snippetParts = mutableListOf(
                "Lat: %.5f, Lng: %.5f".format(drop.lat, drop.lng)
            )
            formatTimestamp(drop.createdAt)?.let { snippetParts.add(0, "Dropped $it") }

            Marker(
                state = MarkerState(position),
                title = drop.text.ifBlank { "(No message)" },
                snippet = snippetParts.joinToString("\n")
            )
        }
    }
}

/** Tiny helper to show snackbars from non-suspend places. */
private fun SnackbarHostState.showMessage(scope: kotlinx.coroutines.CoroutineScope, msg: String) {
    scope.launch { showSnackbar(msg) }
}
