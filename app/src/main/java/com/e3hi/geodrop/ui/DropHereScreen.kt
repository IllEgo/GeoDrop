package com.e3hi.geodrop.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.e3hi.geodrop.data.Drop
import com.e3hi.geodrop.data.DropContentType
import com.e3hi.geodrop.data.displayTitle
import com.e3hi.geodrop.data.mediaLabel
import com.e3hi.geodrop.data.FirestoreRepo
import com.e3hi.geodrop.geo.NearbyDropRegistrar
import com.e3hi.geodrop.geo.NearbyDropRegistrar.NearbySyncStatus
import com.e3hi.geodrop.util.GroupPreferences
import com.e3hi.geodrop.util.formatTimestamp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.Circle
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
    val groupPrefs = remember { GroupPreferences(ctx) }

    var joinedGroups by remember { mutableStateOf(groupPrefs.getJoinedGroups()) }
    var dropVisibility by remember { mutableStateOf(DropVisibility.Public) }
    var dropContentType by remember { mutableStateOf(DropContentType.TEXT) }
    var note by remember { mutableStateOf(TextFieldValue("")) }
    var mediaUrl by remember { mutableStateOf(TextFieldValue("")) }
    var groupCodeInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var isSubmitting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var showMap by remember { mutableStateOf(false) }
    var mapError by remember { mutableStateOf<String?>(null) }
    var mapLoading by remember { mutableStateOf(false) }
    var mapDrops by remember { mutableStateOf<List<Drop>>(emptyList()) }
    var mapRefreshToken by remember { mutableStateOf(0) }
    var mapCurrentLocation by remember { mutableStateOf<LatLng?>(null) }
    var showManageDrops by remember { mutableStateOf(false) }
    var manageDrops by remember { mutableStateOf<List<Drop>>(emptyList()) }
    var manageLoading by remember { mutableStateOf(false) }
    var manageError by remember { mutableStateOf<String?>(null) }
    var manageRefreshToken by remember { mutableStateOf(0) }
    var manageDeletingId by remember { mutableStateOf<String?>(null) }
    var showManageGroups by remember { mutableStateOf(false) }

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
    LaunchedEffect(joinedGroups) {
        if (FirebaseAuth.getInstance().currentUser != null) {
            registrar.registerNearby(
                ctx,
                maxMeters = 300.0,
                groupCodes = joinedGroups.toSet()
            )
        }
    }

    fun uiDone(lat: Double, lng: Double, groupCode: String?, contentType: DropContentType) {
        isSubmitting = false
        note = TextFieldValue("")
        mediaUrl = TextFieldValue("")
        val baseStatus = "Dropped at (%.5f, %.5f)".format(lat, lng)
        val typeSummary = when (contentType) {
            DropContentType.TEXT -> "note"
            DropContentType.PHOTO -> "photo drop"
            DropContentType.AUDIO -> "audio drop"
        }
        status = if (groupCode != null) {
            "$baseStatus for group $groupCode ($typeSummary)"
        } else {
            "$baseStatus ($typeSummary)"
        }
        val snackbarMessage = if (groupCode != null) {
            "Group drop saved!"
        } else {
            when (contentType) {
                DropContentType.TEXT -> "Note dropped!"
                DropContentType.PHOTO -> "Photo drop saved!"
                DropContentType.AUDIO -> "Audio drop saved!"
            }
        }
        scope.launch { snackbar.showSnackbar(snackbarMessage) }
    }

    suspend fun addDropAt(
        lat: Double,
        lng: Double,
        groupCode: String?,
        contentType: DropContentType,
        noteText: String,
        mediaInput: String?
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anon"
        val sanitizedMedia = mediaInput?.takeIf { it.isNotBlank() }
        val sanitizedText = when (contentType) {
            DropContentType.TEXT -> noteText.ifBlank { "New drop" }
            DropContentType.PHOTO, DropContentType.AUDIO -> noteText.trim()
        }
        val d = Drop(
            text = sanitizedText,
            lat = lat,
            lng = lng,
            createdBy = uid,
            createdAt = System.currentTimeMillis(),
            groupCode = groupCode,
            contentType = contentType,
            mediaUrl = sanitizedMedia
        )
        repo.addDrop(d) // suspend (uses Firestore .await() internally)
        if (groupCode != null) {
            groupPrefs.addGroup(groupCode)
            joinedGroups = groupPrefs.getJoinedGroups()
        }
        uiDone(lat, lng, groupCode, contentType)
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

    LaunchedEffect(showManageDrops, manageRefreshToken) {
        if (showManageDrops) {
            manageLoading = true
            manageError = null
            manageDeletingId = null
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                manageLoading = false
                manageError = "Sign-in is still in progress. Try again in a moment."
            } else {
                try {
                    manageDrops = repo.getDropsForUser(uid)
                        .sortedByDescending { it.createdAt }
                } catch (e: Exception) {
                    manageError = e.message ?: "Failed to load your drops."
                } finally {
                    manageLoading = false
                }
            }
        } else {
            manageDrops = emptyList()
            manageError = null
            manageLoading = false
            manageDeletingId = null
        }
    }


    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Drop something at your current location", style = MaterialTheme.typography.titleMedium)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Drop content", style = MaterialTheme.typography.titleSmall)

            ContentTypeOption(
                title = "Text note",
                description = "Share a written message for people nearby.",
                selected = dropContentType == DropContentType.TEXT,
                onClick = { dropContentType = DropContentType.TEXT }
            )

            ContentTypeOption(
                title = "Photo drop",
                description = "Leave a link to a photo that others can open.",
                selected = dropContentType == DropContentType.PHOTO,
                onClick = { dropContentType = DropContentType.PHOTO }
            )

            ContentTypeOption(
                title = "Audio drop",
                description = "Share a link to a sound or song clip.",
                selected = dropContentType == DropContentType.AUDIO,
                onClick = { dropContentType = DropContentType.AUDIO }
            )
        }

        val noteLabel = when (dropContentType) {
            DropContentType.TEXT -> "Your note"
            DropContentType.PHOTO, DropContentType.AUDIO -> "Caption (optional)"
        }
        val noteSupporting = when (dropContentType) {
            DropContentType.TEXT -> null
            DropContentType.PHOTO -> "Add a short caption to go with your photo."
            DropContentType.AUDIO -> "Add a short caption to go with your audio clip."
        }
        val noteMinLines = if (dropContentType == DropContentType.TEXT) 3 else 1
        val supportingTextContent: (@Composable () -> Unit)? = noteSupporting?.let { helper ->
            { Text(helper) }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text(noteLabel) },
            minLines = noteMinLines,
            modifier = Modifier.fillMaxWidth(),
            supportingText = supportingTextContent
        )

        if (dropContentType != DropContentType.TEXT) {
            val mediaLabel = if (dropContentType == DropContentType.PHOTO) "Photo URL" else "Audio URL"
            val mediaSupporting = if (dropContentType == DropContentType.PHOTO) {
                "Paste a public link so others can open the photo."
            } else {
                "Paste a link to the audio file or clip."
            }
            OutlinedTextField(
                value = mediaUrl,
                onValueChange = { mediaUrl = it },
                label = { Text(mediaLabel) },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text(mediaSupporting) }
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Drop visibility", style = MaterialTheme.typography.titleSmall)

            VisibilityOption(
                title = "Public drop",
                description = "Anyone nearby can discover this note.",
                selected = dropVisibility == DropVisibility.Public,
                onClick = { dropVisibility = DropVisibility.Public }
            )

            VisibilityOption(
                title = "Group-only drop",
                description = "Limit discovery to people who share your group code.",
                selected = dropVisibility == DropVisibility.GroupOnly,
                onClick = { dropVisibility = DropVisibility.GroupOnly }
            )

            if (dropVisibility == DropVisibility.GroupOnly) {
                OutlinedTextField(
                    value = groupCodeInput,
                    onValueChange = { groupCodeInput = it },
                    label = { Text("Group code") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("Codes stay on this device. Share them with your crew or guests.")
                    }
                )

                if (joinedGroups.isNotEmpty()) {
                    Text(
                        text = "Tap a saved code to reuse it:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        joinedGroups.forEach { code ->
                            AssistChip(
                                onClick = { groupCodeInput = TextFieldValue(code) },
                                label = { Text(code) }
                            )
                        }
                    }
                }
            } else {
                val visibilityMessage = if (joinedGroups.isEmpty()) {
                    "Add a group code to keep drops private for weddings, crew ops, or hunts."
                } else {
                    "Active group codes: ${joinedGroups.joinToString()}."
                }

                Text(
                    text = visibilityMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(
            enabled = !isSubmitting,
            onClick = {
                isSubmitting = true
                scope.launch {
                    try {
                        val selectedGroupCode = if (dropVisibility == DropVisibility.GroupOnly) {
                            GroupPreferences.normalizeGroupCode(groupCodeInput.text)
                                ?: run {
                                    isSubmitting = false
                                    snackbar.showMessage(
                                        scope,
                                        "Enter a group code to make this drop private."
                                    )
                                    return@launch
                                }
                        } else {
                            null
                        }
                        val mediaInput = if (dropContentType == DropContentType.TEXT) {
                            null
                        } else {
                            mediaUrl.text.trim().takeIf { it.isNotEmpty() }
                                ?: run {
                                    isSubmitting = false
                                    val missingMessage = when (dropContentType) {
                                        DropContentType.PHOTO -> "Add a link to your photo before dropping."
                                        DropContentType.AUDIO -> "Add a link to your audio before dropping."
                                        DropContentType.TEXT -> ""
                                    }
                                    if (missingMessage.isNotBlank()) {
                                        snackbar.showMessage(scope, missingMessage)
                                    }
                                    return@launch
                                }
                        }
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

                        addDropAt(
                            lat = lat,
                            lng = lng,
                            groupCode = selectedGroupCode,
                            contentType = dropContentType,
                            noteText = note.text,
                            mediaInput = mediaInput
                        )
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
                registrar.registerNearby(
                    ctx,
                    maxMeters = 300.0,
                    groupCodes = joinedGroups.toSet()
                ) { statusResult ->
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
            onClick = { showManageGroups = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Manage group codes") }

        Button(
            onClick = {
                if (FirebaseAuth.getInstance().currentUser == null) {
                    snackbar.showMessage(scope, "Signing you in… please try again shortly.")
                } else {
                    showManageDrops = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Manage my drops") }

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

    if (showManageDrops) {
        ManageDropsDialog(
            loading = manageLoading,
            drops = manageDrops,
            deletingId = manageDeletingId,
            error = manageError,
            onDismiss = { showManageDrops = false },
            onRetry = { manageRefreshToken += 1 },
            onDelete = { drop ->
                if (drop.id.isBlank()) {
                    snackbar.showMessage(scope, "Unable to delete this drop.")
                    return@ManageDropsDialog
                }

                manageDeletingId = drop.id
                scope.launch {
                    try {
                        repo.deleteDrop(drop.id)
                        manageDrops = manageDrops.filterNot { it.id == drop.id }
                        snackbar.showMessage(scope, "Drop deleted.")
                    } catch (e: Exception) {
                        snackbar.showMessage(scope, "Error: ${e.message}")
                    } finally {
                        manageDeletingId = null
                    }
                }
            }
        )
    }

    if (showManageGroups) {
        ManageGroupsDialog(
            groups = joinedGroups,
            onDismiss = { showManageGroups = false },
            onAdd = { code ->
                groupPrefs.addGroup(code)
                joinedGroups = groupPrefs.getJoinedGroups()
                if (dropVisibility == DropVisibility.GroupOnly) {
                    groupCodeInput = TextFieldValue(code)
                }
                snackbar.showMessage(scope, "Saved group $code")
            },
            onRemove = { code ->
                groupPrefs.removeGroup(code)
                joinedGroups = groupPrefs.getJoinedGroups()
                val currentInput = GroupPreferences.normalizeGroupCode(groupCodeInput.text)
                if (currentInput == code) {
                    groupCodeInput = TextFieldValue("")
                }
                snackbar.showMessage(scope, "Removed group $code")
            }
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
                            DialogMessageContent(
                                message = error,
                                primaryLabel = "Retry",
                                onPrimary = onRetry,
                                onDismiss = onDismiss
                            )
                        }

                        drops.isEmpty() -> {
                            DialogMessageContent(
                                message = "You haven't dropped anything yet.",
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
private fun DialogMessageContent(
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
private fun ManageGroupsDialog(
    groups: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
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
                        title = { Text("Manage group codes") },
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    var newCode by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                        mutableStateOf(TextFieldValue(""))
                    }

                    OutlinedTextField(
                        value = newCode,
                        onValueChange = { newCode = it },
                        label = { Text("Add a group code") },
                        supportingText = {
                            Text("Codes stay on this device. Share with people you trust.")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    val normalized = GroupPreferences.normalizeGroupCode(newCode.text)
                    Button(
                        onClick = {
                            normalized?.let {
                                onAdd(it)
                                newCode = TextFieldValue("")
                            }
                        },
                        enabled = normalized != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save code")
                    }

                    if (groups.isEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No saved group codes yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Use group codes to keep drops private for weddings, team ops, and scavenger hunts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "Your saved codes",
                            style = MaterialTheme.typography.titleMedium
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(groups, key = { it }) { code ->
                                GroupCodeRow(
                                    code = code,
                                    onRemove = { onRemove(code) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManageDropsDialog(
    loading: Boolean,
    drops: List<Drop>,
    deletingId: String?,
    error: String?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onDelete: (Drop) -> Unit
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
                        title = { Text("Manage my drops") },
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
                            DialogMessageContent(
                                message = error,
                                primaryLabel = "Retry",
                                onPrimary = onRetry,
                                onDismiss = onDismiss
                            )
                        }

                        drops.isEmpty() -> {
                            DialogMessageContent(
                                message = "You haven't dropped any notes yet.",
                                primaryLabel = null,
                                onPrimary = null,
                                onDismiss = onDismiss
                            )
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(drops, key = { it.id }) { drop ->
                                    ManageDropRow(
                                        drop = drop,
                                        isDeleting = deletingId == drop.id,
                                        onDelete = { onDelete(drop) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupCodeRow(
    code: String,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Share this code so only your group sees the drop.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove group code"
                )
            }
        }
    }
}

@Composable
private fun ManageDropRow(
    drop: Drop,
    isDeleting: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = drop.displayTitle(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))

            val typeLabel = when (drop.contentType) {
                DropContentType.TEXT -> "Text note"
                DropContentType.PHOTO -> "Photo drop"
                DropContentType.AUDIO -> "Audio drop"
            }
            Text(
                text = "Type: $typeLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            drop.mediaLabel()?.let { link ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Link: $link",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(4.dp))

            formatTimestamp(drop.createdAt)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))

            val visibilityLabel = drop.groupCode?.takeIf { !it.isNullOrBlank() }
                ?.let { "Group-only · $it" }
                ?: "Public drop"
            Text(
                text = visibilityLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Lat: %.5f, Lng: %.5f".format(drop.lat, drop.lng),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDelete,
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Deleting…")
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete drop"
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun DropsMapContent(drops: List<Drop>, currentLocation: LatLng?) {
    val cameraPositionState = rememberCameraPositionState()
    val uiSettings = remember { MapUiSettings(zoomControlsEnabled = true) }

    var showRadius by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(drops, currentLocation) {
        val target = currentLocation ?: drops.firstOrNull()?.let { LatLng(it.lat, it.lng) }
        if (target != null) {
            val update = CameraUpdateFactory.newLatLngZoom(target, 13f)
            cameraPositionState.animate(update)
        }
    }


    LaunchedEffect(currentLocation) {
        if (currentLocation == null) {
            showRadius = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = uiSettings
        ) {
            currentLocation?.let { location ->
                if (showRadius) {
                    Circle(
                        center = location,
                        radius = 300.0,
                        strokeColor = Color(0xFF1E88E5),
                        strokeWidth = 2f,
                        fillColor = Color(0x331E88E5)
                    )
                }

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
                val typeLabel = when (drop.contentType) {
                    DropContentType.TEXT -> "Text note"
                    DropContentType.PHOTO -> "Photo drop"
                    DropContentType.AUDIO -> "Audio drop"
                }
                snippetParts.add(0, "Type: $typeLabel")
                formatTimestamp(drop.createdAt)?.let { snippetParts.add(0, "Dropped $it") }
                drop.groupCode?.takeIf { !it.isNullOrBlank() }?.let { snippetParts.add("Group $it") }
                drop.mediaLabel()?.let { snippetParts.add("Link: $it") }

                Marker(
                    state = MarkerState(position),
                    title = drop.displayTitle(),
                    snippet = snippetParts.joinToString("\n")
                )
            }
        }


        if (currentLocation != null) {
            ExtendedFloatingActionButton(
                onClick = { showRadius = !showRadius },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = if (showRadius) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (showRadius) "Hide 300 m radius" else "Show 300 m radius")
            }
        }
    }
}

@Composable
private fun ContentTypeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    SelectionOption(
        title = title,
        description = description,
        selected = selected,
        onClick = onClick
    )
}

@Composable
private fun VisibilityOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    SelectionOption(
        title = title,
        description = description,
        selected = selected,
        onClick = onClick
    )
}

@Composable
private fun SelectionOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private enum class DropVisibility { Public, GroupOnly }

/** Tiny helper to show snackbars from non-suspend places. */
private fun SnackbarHostState.showMessage(scope: kotlinx.coroutines.CoroutineScope, msg: String) {
    scope.launch { showSnackbar(msg) }
}
