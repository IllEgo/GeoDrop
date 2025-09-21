package com.e3hi.geodrop.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.e3hi.geodrop.data.CollectedNote
import com.e3hi.geodrop.data.Drop
import com.e3hi.geodrop.data.DropContentType
import com.e3hi.geodrop.data.displayTitle
import com.e3hi.geodrop.data.mediaLabel
import com.e3hi.geodrop.data.FirestoreRepo
import com.e3hi.geodrop.data.MediaStorageRepo
import com.e3hi.geodrop.data.NoteInventory
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
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Locale

@SuppressLint("MissingPermission")
@Composable
fun DropHereScreen() {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var signingIn by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val noteInventory = remember { NoteInventory(ctx) }
    var collectedNotes by remember { mutableStateOf(noteInventory.getCollectedNotes()) }
    val collectedIds = remember(collectedNotes) { collectedNotes.map { it.id }.toSet() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                collectedNotes = noteInventory.getCollectedNotes()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(noteInventory) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == NoteInventory.ACTION_INVENTORY_CHANGED) {
                    collectedNotes = noteInventory.getCollectedNotes()
                }
            }
        }
        val filter = IntentFilter(NoteInventory.ACTION_INVENTORY_CHANGED)
        val registered = ContextCompat.registerReceiver(
            ctx,
            receiver,
            filter,
            RECEIVER_NOT_EXPORTED
        )
        onDispose {
            if (registered != null) {
                ctx.unregisterReceiver(receiver)
            }
        }
    }

    if (currentUser == null) {
        SignInRequiredScreen(
            isSigningIn = signingIn,
            error = signInError,
            onSignInClick = {
                if (signingIn) return@SignInRequiredScreen
                signInError = null
                signingIn = true
                runCatching {
                    auth.signInAnonymously()
                        .addOnCompleteListener { task ->
                            signingIn = false
                            if (!task.isSuccessful) {
                                val message = task.exception?.localizedMessage?.takeIf { it.isNotBlank() }
                                    ?: "Couldn't sign you in. Check your connection and try again."
                                signInError = message
                            }
                        }
                }.onFailure { throwable ->
                    signingIn = false
                    val message = throwable.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "Couldn't sign you in. Check your connection and try again."
                    signInError = message
                }
            }
        )
        return
    }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val fused = remember { LocationServices.getFusedLocationProviderClient(ctx) }
    val repo = remember { FirestoreRepo() }
    val mediaStorage = remember { MediaStorageRepo() }
    val registrar = remember { NearbyDropRegistrar() }
    val groupPrefs = remember { GroupPreferences(ctx) }

    var joinedGroups by remember { mutableStateOf(groupPrefs.getJoinedGroups()) }
    var dropVisibility by remember { mutableStateOf(DropVisibility.Public) }
    var dropContentType by remember { mutableStateOf(DropContentType.TEXT) }
    var note by remember { mutableStateOf(TextFieldValue("")) }
    var capturedPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var capturedAudioUri by rememberSaveable { mutableStateOf<String?>(null) }
    var groupCodeInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var isSubmitting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var showMap by remember { mutableStateOf(false) }
    var mapError by remember { mutableStateOf<String?>(null) }
    var mapLoading by remember { mutableStateOf(false) }
    var mapDrops by remember { mutableStateOf<List<Drop>>(emptyList()) }
    var mapRefreshToken by remember { mutableStateOf(0) }
    var mapCurrentLocation by remember { mutableStateOf<LatLng?>(null) }
    var showOtherDropsMap by remember { mutableStateOf(false) }
    var otherMapDrops by remember { mutableStateOf<List<Drop>>(emptyList()) }
    var otherMapLoading by remember { mutableStateOf(false) }
    var otherMapError by remember { mutableStateOf<String?>(null) }
    var otherMapRefreshToken by remember { mutableStateOf(0) }
    var otherMapCurrentLocation by remember { mutableStateOf<LatLng?>(null) }
    var showManageDrops by remember { mutableStateOf(false) }
    var manageDrops by remember { mutableStateOf<List<Drop>>(emptyList()) }
    var manageLoading by remember { mutableStateOf(false) }
    var manageError by remember { mutableStateOf<String?>(null) }
    var manageRefreshToken by remember { mutableStateOf(0) }
    var manageDeletingId by remember { mutableStateOf<String?>(null) }
    var showManageGroups by remember { mutableStateOf(false) }
    var showCollectedNotes by remember { mutableStateOf(false) }

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

    LaunchedEffect(dropContentType) {
        when (dropContentType) {
            DropContentType.TEXT -> {
                capturedPhotoPath = null
                capturedAudioUri = null
            }

            DropContentType.PHOTO -> {
                capturedAudioUri = null
            }

            DropContentType.AUDIO -> {
                capturedPhotoPath = null
            }
        }
    }

    var pendingPhotoPath by remember { mutableStateOf<String?>(null) }
    var pendingPermissionAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingAudioPermissionAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val path = pendingPhotoPath
        if (success && path != null) {
            capturedPhotoPath = path
        } else if (path != null) {
            runCatching { File(path).delete() }
        }
        pendingPhotoPath = null
    }

    val recordAudioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                capturedAudioUri = uri.toString()
            } else {
                snackbar.showMessage(scope, "Recording unavailable. Try again.")
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = pendingPermissionAction
        pendingPermissionAction = null
        if (granted) {
            action?.invoke()
        } else {
            snackbar.showMessage(scope, "Camera permission is required to capture a photo.")
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = pendingAudioPermissionAction
        pendingAudioPermissionAction = null
        if (granted) {
            action?.invoke()
        } else {
            snackbar.showMessage(scope, "Microphone permission is required to record audio.")
        }
    }

    fun clearPhoto() {
        val path = capturedPhotoPath
        if (path != null) {
            runCatching { File(path).delete() }
        }
        capturedPhotoPath = null
    }

    fun ensureCameraAndLaunch() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED) {
            val photoDir = File(ctx.cacheDir, "camera").apply { if (!exists()) mkdirs() }
            val photoFile = kotlin.runCatching { File.createTempFile("geodrop_photo_", ".jpg", photoDir) }
                .getOrNull()
            if (photoFile == null) {
                snackbar.showMessage(scope, "Couldn't prepare a file for the camera.")
                return
            }
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", photoFile)
            pendingPhotoPath = photoFile.absolutePath
            takePictureLauncher.launch(uri)
        } else {
            pendingPermissionAction = { ensureCameraAndLaunch() }
            cameraPermissionLauncher.launch(permission)
        }
    }

    fun ensureAudioAndLaunch() {
        val permission = Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
            if (intent.resolveActivity(ctx.packageManager) != null) {
                recordAudioLauncher.launch(intent)
            } else {
                snackbar.showMessage(scope, "No recorder app available on this device.")
            }
        } else {
            pendingAudioPermissionAction = { ensureAudioAndLaunch() }
            audioPermissionLauncher.launch(permission)
        }
    }

    fun uiDone(lat: Double, lng: Double, groupCode: String?, contentType: DropContentType) {
        isSubmitting = false
        note = TextFieldValue("")
        capturedPhotoPath = null
        capturedAudioUri = null
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

    LaunchedEffect(showOtherDropsMap, otherMapRefreshToken, joinedGroups) {
        if (showOtherDropsMap) {
            otherMapLoading = true
            otherMapError = null
            otherMapCurrentLocation = null
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                otherMapLoading = false
                otherMapError = "Sign-in is still in progress. Try again in a moment."
            } else {
                try {
                    val allowedGroups = joinedGroups.toSet()
                    val drops = repo.getVisibleDropsForUser(uid, allowedGroups)
                        .filterNot { noteInventory.isIgnored(it.id) }
                        .sortedByDescending { it.createdAt }
                    otherMapDrops = drops
                    otherMapCurrentLocation = getLatestLocation()?.let { (lat, lng) -> LatLng(lat, lng) }
                } catch (e: Exception) {
                    otherMapError = e.message ?: "Failed to load other drops."
                } finally {
                    otherMapLoading = false
                }
            }
        } else {
            otherMapDrops = emptyList()
            otherMapError = null
            otherMapLoading = false
            otherMapCurrentLocation = null
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


    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
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
                description = "Capture a photo with your camera that others can open.",
                selected = dropContentType == DropContentType.PHOTO,
                onClick = { dropContentType = DropContentType.PHOTO }
            )

            ContentTypeOption(
                title = "Audio drop",
                description = "Record a quick voice message for nearby explorers.",
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

        when (dropContentType) {
            DropContentType.PHOTO -> {
                val hasPhoto = capturedPhotoPath != null
                MediaCaptureCard(
                    title = "Attach a photo",
                    description = "Snap a picture with your camera to pin at this location.",
                    status = if (hasPhoto) "Photo ready to upload." else "No photo captured yet.",
                    primaryLabel = if (hasPhoto) "Retake photo" else "Open camera",
                    onPrimary = {
                        if (hasPhoto) {
                            clearPhoto()
                        }
                        ensureCameraAndLaunch()
                    },
                    secondaryLabel = if (hasPhoto) "Remove photo" else null,
                    onSecondary = if (hasPhoto) {
                        { clearPhoto() }
                    } else {
                        null
                    }
                )
            }

            DropContentType.AUDIO -> {
                val hasAudio = capturedAudioUri != null
                MediaCaptureCard(
                    title = "Record audio",
                    description = "Capture a short voice note for anyone who discovers this drop.",
                    status = if (hasAudio) "Audio message ready to upload." else "No recording yet.",
                    primaryLabel = if (hasAudio) "Record again" else "Record audio",
                    onPrimary = {
                        if (hasAudio) {
                            capturedAudioUri = null
                        }
                        ensureAudioAndLaunch()
                    },
                    secondaryLabel = if (hasAudio) "Remove audio" else null,
                    onSecondary = if (hasAudio) {
                        { capturedAudioUri = null }
                    } else {
                        null
                    }
                )
            }

            DropContentType.TEXT -> Unit
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
                        val mediaUrlResult = when (dropContentType) {
                            DropContentType.TEXT -> null

                            DropContentType.PHOTO -> {
                                val path = capturedPhotoPath ?: run {
                                    isSubmitting = false
                                    snackbar.showMessage(scope, "Capture a photo before dropping.")
                                    return@launch
                                }

                                val photoBytes = withContext(Dispatchers.IO) {
                                    try {
                                        File(path).takeIf { it.exists() }?.readBytes()
                                    } catch (e: IOException) {
                                        null
                                    }
                                } ?: run {
                                    isSubmitting = false
                                    snackbar.showMessage(scope, "Couldn't read the captured photo. Retake it and try again.")
                                    return@launch
                                }

                                val uploaded = mediaStorage.uploadMedia(
                                    DropContentType.PHOTO,
                                    photoBytes,
                                    "image/jpeg"
                                )

                                withContext(Dispatchers.IO) {
                                    runCatching { File(path).delete() }
                                }

                                uploaded
                            }

                            DropContentType.AUDIO -> {
                                val uriString = capturedAudioUri ?: run {
                                    isSubmitting = false
                                    snackbar.showMessage(scope, "Record an audio message before dropping.")
                                    return@launch
                                }
                                val uri = Uri.parse(uriString)

                                val audioBytes = withContext(Dispatchers.IO) {
                                    try {
                                        ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                    } catch (e: IOException) {
                                        null
                                    }
                                } ?: run {
                                    isSubmitting = false
                                    snackbar.showMessage(scope, "Couldn't read the audio recording. Record again and try once more.")
                                    return@launch
                                }

                                val mimeType = ctx.contentResolver.getType(uri) ?: "audio/mpeg"

                                val uploaded = mediaStorage.uploadMedia(
                                    DropContentType.AUDIO,
                                    audioBytes,
                                    mimeType
                                )

                                withContext(Dispatchers.IO) {
                                    runCatching { ctx.contentResolver.delete(uri, null, null) }
                                }

                                uploaded
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
                            mediaInput = mediaUrlResult
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
            onClick = {
                if (FirebaseAuth.getInstance().currentUser == null) {
                    snackbar.showMessage(scope, "Signing you in… please try again shortly.")
                } else {
                    showOtherDropsMap = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("View other drops on map") }

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

            Button(
                onClick = {
                    collectedNotes = noteInventory.getCollectedNotes()
                    showCollectedNotes = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                val count = collectedNotes.size
                Text(
                    text = if (count == 0) "View collected notes" else "View collected notes ($count)"
                )
            }

            status?.let { Text(it) }

            Spacer(Modifier.height(80.dp))
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        )
    }

    if (showMap) {
        DropsMapDialog(
            title = "My drops on map",
            loading = mapLoading,
            drops = mapDrops,
            currentLocation = mapCurrentLocation,
            error = mapError,
            emptyMessage = "You haven't dropped anything yet.",
            redactedDropIds = emptySet(),
            collectedDropIds = emptySet(),
            onDismiss = { showMap = false },
            onRetry = { mapRefreshToken += 1 }
        )
    }

    if (showOtherDropsMap) {
        val redactedIds = otherMapDrops
            .mapNotNull { drop -> drop.id.takeIf { it !in collectedIds } }
            .toSet()
        DropsMapDialog(
            title = "Other drops on map",
            loading = otherMapLoading,
            drops = otherMapDrops,
            currentLocation = otherMapCurrentLocation,
            error = otherMapError,
            emptyMessage = "No drops from other users are available right now.",
            redactedDropIds = redactedIds,
            collectedDropIds = collectedIds,
            onDismiss = { showOtherDropsMap = false },
            onRetry = { otherMapRefreshToken += 1 }
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

    if (showCollectedNotes) {
        CollectedNotesDialog(
            notes = collectedNotes,
            onDismiss = { showCollectedNotes = false },
            onView = { note ->
                val intent = Intent(ctx, DropDetailActivity::class.java).apply {
                    putExtra("dropId", note.id)
                    if (note.text.isNotBlank()) putExtra("dropText", note.text)
                    note.lat?.let { putExtra("dropLat", it) }
                    note.lng?.let { putExtra("dropLng", it) }
                    note.dropCreatedAt?.let { putExtra("dropCreatedAt", it) }
                    note.groupCode?.let { putExtra("dropGroupCode", it) }
                    putExtra("dropContentType", note.contentType.name)
                    note.mediaUrl?.let { putExtra("dropMediaUrl", it) }
                }
                ctx.startActivity(intent)
            },
            onRemove = { note ->
                noteInventory.removeCollected(note.id)
                collectedNotes = noteInventory.getCollectedNotes()
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
    title: String,
    loading: Boolean,
    drops: List<Drop>,
    currentLocation: LatLng?,
    error: String?,
    emptyMessage: String,
    redactedDropIds: Set<String>,
    collectedDropIds: Set<String>,
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
                        title = { Text(title) },
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
                                message = emptyMessage,
                                primaryLabel = null,
                                onPrimary = null,
                                onDismiss = onDismiss
                            )
                        }

                        else -> {
                            DropsMapContent(
                                drops = drops,
                                currentLocation = currentLocation,
                                redactedDropIds = redactedDropIds,
                                collectedDropIds = collectedDropIds
                            )
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
private fun CollectedNotesDialog(
    notes: List<CollectedNote>,
    onDismiss: () -> Unit,
    onView: (CollectedNote) -> Unit,
    onRemove: (CollectedNote) -> Unit
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
                        title = { Text("Collected notes") },
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
                    if (notes.isEmpty()) {
                        DialogMessageContent(
                            message = "You haven't collected any notes yet.",
                            primaryLabel = null,
                            onPrimary = null,
                            onDismiss = onDismiss
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(notes, key = { it.id }) { note ->
                                CollectedNoteCard(
                                    note = note,
                                    onView = { onView(note) },
                                    onRemove = { onRemove(note) }
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
private fun CollectedNoteCard(
    note: CollectedNote,
    onView: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val typeLabel = when (note.contentType) {
                DropContentType.TEXT -> "Text note"
                DropContentType.PHOTO -> "Photo drop"
                DropContentType.AUDIO -> "Audio drop"
            }
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            val preview = note.text.ifBlank {
                when (note.contentType) {
                    DropContentType.TEXT -> "(No message)"
                    DropContentType.PHOTO -> "Photo drop"
                    DropContentType.AUDIO -> "Audio drop"
                }
            }
            Text(
                text = preview,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Collected: ${formatTimestamp(note.collectedAt)}",
                style = MaterialTheme.typography.bodyMedium
            )

            note.dropCreatedAt?.let {
                Text(
                    text = "Dropped: ${formatTimestamp(it)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            note.groupCode?.let { group ->
                Text(
                    text = "Group: $group",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (note.lat != null && note.lng != null) {
                Text(
                    text = "Location: ${formatCoordinate(note.lat)}, ${formatCoordinate(note.lng)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            note.mediaUrl?.let { link ->
                Text(
                    text = "Media: $link",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onView) {
                    Text("View details")
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove from inventory")
                }
            }
        }
    }
}

private fun formatCoordinate(value: Double): String {
    return String.format(Locale.US, "%.5f", value)
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
private fun MediaCaptureCard(
    title: String,
    description: String,
    status: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth()) { Text(primaryLabel) }
            if (secondaryLabel != null && onSecondary != null) {
                TextButton(onClick = onSecondary) {
                    Text(secondaryLabel)
                }
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

            val mediaUrl = drop.mediaLabel()
            if (drop.contentType == DropContentType.PHOTO && mediaUrl != null) {
                Spacer(Modifier.height(12.dp))

                val context = LocalContext.current
                val imageRequest = remember(mediaUrl) {
                    ImageRequest.Builder(context)
                        .data(mediaUrl)
                        .crossfade(true)
                        .build()
                }

                AsyncImage(
                    model = imageRequest,
                    contentDescription = drop.displayTitle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 280.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            mediaUrl?.let { link ->
                Spacer(Modifier.height(8.dp))
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
private fun DropsMapContent(
    drops: List<Drop>,
    currentLocation: LatLng?,
    redactedDropIds: Set<String>,
    collectedDropIds: Set<String>
) {
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
                val isRedacted = redactedDropIds.contains(drop.id)
                val isCollected = drop.id.isNotBlank() && collectedDropIds.contains(drop.id)
                val snippetParts = mutableListOf<String>()
                val typeLabel = when (drop.contentType) {
                    DropContentType.TEXT -> "Text note"
                    DropContentType.PHOTO -> "Photo drop"
                    DropContentType.AUDIO -> "Audio drop"
                }
                snippetParts.add("Type: $typeLabel")
                formatTimestamp(drop.createdAt)?.let { snippetParts.add("Dropped $it") }
                drop.groupCode?.takeIf { !it.isNullOrBlank() }?.let { snippetParts.add("Group $it") }
                snippetParts.add("Lat: %.5f, Lng: %.5f".format(drop.lat, drop.lng))
                if (isRedacted) {
                    snippetParts.add("Move closer to unlock this drop.")
                } else {
                    drop.mediaLabel()?.let { snippetParts.add("Link: $it") }
                }

                val markerTitle = if (isRedacted) {
                    when (drop.contentType) {
                        DropContentType.TEXT -> "Locked text drop"
                        DropContentType.PHOTO -> "Locked photo drop"
                        DropContentType.AUDIO -> "Locked audio drop"
                    }
                } else {
                    drop.displayTitle()
                }

                val markerIcon = if (isCollected) {
                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                } else {
                    null
                }

                Marker(
                    state = MarkerState(position),
                    title = markerTitle,
                    snippet = snippetParts.joinToString("\n"),
                    icon = markerIcon
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
private fun SignInRequiredScreen(
    isSigningIn: Boolean,
    error: String?,
    onSignInClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Sign in to continue",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Sign in to drop notes, photos, and audio. This keeps your uploads secure.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            error?.let {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onSignInClick,
                enabled = !isSigningIn,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Signing in…")
                } else {
                    Text("Sign in")
                }
            }
        }
    }
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
