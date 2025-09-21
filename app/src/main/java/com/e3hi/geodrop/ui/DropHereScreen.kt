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
import androidx.compose.foundation.background
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
import com.e3hi.geodrop.data.discoveryDescription
import com.e3hi.geodrop.data.discoveryTitle
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
    var ignoredDropIds by remember { mutableStateOf(noteInventory.getIgnoredDropIds()) }
    val collectedDropIds = remember(collectedNotes) { collectedNotes.map { it.id }.toSet() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                collectedNotes = noteInventory.getCollectedNotes()
                ignoredDropIds = noteInventory.getIgnoredDropIds()
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
                    ignoredDropIds = noteInventory.getIgnoredDropIds()
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
    var showOtherDropsMap by remember { mutableStateOf(false) }
    var otherDrops by remember { mutableStateOf<List<Drop>>(emptyList()) }
    var otherDropsLoading by remember { mutableStateOf(false) }
    var otherDropsError by remember { mutableStateOf<String?>(null) }
    var otherDropsCurrentLocation by remember { mutableStateOf<LatLng?>(null) }
    var otherDropsSelectedId by remember { mutableStateOf<String?>(null) }
    var otherDropsRefreshToken by remember { mutableStateOf(0) }
    var showMyDrops by remember { mutableStateOf(false) }
    var myDrops by remember { mutableStateOf<List<Drop>>(emptyList()) }
    var myDropsLoading by remember { mutableStateOf(false) }
    var myDropsError by remember { mutableStateOf<String?>(null) }
    var myDropsRefreshToken by remember { mutableStateOf(0) }
    var myDropsCurrentLocation by remember { mutableStateOf<LatLng?>(null) }
    var myDropsDeletingId by remember { mutableStateOf<String?>(null) }
    var myDropsSelectedId by remember { mutableStateOf<String?>(null) }
    var showCollectedDrops by remember { mutableStateOf(false) }
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


    LaunchedEffect(
        showOtherDropsMap,
        joinedGroups,
        otherDropsRefreshToken,
        collectedDropIds,
        ignoredDropIds
    ) {
        if (showOtherDropsMap) {
            otherDropsLoading = true
            otherDropsError = null
            otherDropsCurrentLocation = null
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                otherDrops = emptyList()
                otherDropsError = "Sign-in is still in progress. Try again in a moment."
                otherDropsLoading = false
            } else {
                try {
                    val drops = repo.getVisibleDropsForUser(uid, joinedGroups.toSet())
                        .sortedByDescending { it.createdAt }
                    val filteredDrops = drops.filterNot { drop ->
                        val id = drop.id
                        id in collectedDropIds || id in ignoredDropIds
                    }
                    otherDrops = filteredDrops
                    otherDropsCurrentLocation = getLatestLocation()?.let { (lat, lng) -> LatLng(lat, lng) }
                    otherDropsSelectedId = otherDropsSelectedId?.takeIf { id -> filteredDrops.any { it.id == id } }
                        ?: filteredDrops.firstOrNull()?.id
                } catch (e: Exception) {
                    otherDrops = emptyList()
                    otherDropsError = e.message ?: "Failed to load nearby drops."
                } finally {
                    otherDropsLoading = false
                }
            }
        } else {
            otherDrops = emptyList()
            otherDropsError = null
            otherDropsLoading = false
            otherDropsCurrentLocation = null
            otherDropsSelectedId = null
        }
    }

    LaunchedEffect(showMyDrops, myDropsRefreshToken) {
        if (showMyDrops) {
            myDropsLoading = true
            myDropsError = null
            myDropsDeletingId = null
            myDropsCurrentLocation = null
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                myDropsLoading = false
                myDropsError = "Sign-in is still in progress. Try again in a moment."
            } else {
                try {
                    val drops = repo.getDropsForUser(uid)
                        .sortedByDescending { it.createdAt }
                    myDrops = drops
                    myDropsCurrentLocation = getLatestLocation()?.let { (lat, lng) -> LatLng(lat, lng) }
                    myDropsSelectedId = myDropsSelectedId?.takeIf { id -> drops.any { it.id == id } }
                        ?: drops.firstOrNull()?.id
                } catch (e: Exception) {
                    myDropsError = e.message ?: "Failed to load your drops."
                } finally {
                    myDropsLoading = false
                }
            }
        } else {
            myDrops = emptyList()
            myDropsError = null
            myDropsLoading = false
            myDropsDeletingId = null
            myDropsCurrentLocation = null
            myDropsSelectedId = null
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
                    showMyDrops = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("View my drops") }

            Button(
                onClick = {
                    collectedNotes = noteInventory.getCollectedNotes()
                    showCollectedDrops = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                val count = collectedNotes.size
                Text(
                    text = if (count == 0) "View collected drops" else "View collected drops ($count)"
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

    if (showOtherDropsMap) {
        OtherDropsMapDialog(
            loading = otherDropsLoading,
            drops = otherDrops,
            currentLocation = otherDropsCurrentLocation,
            error = otherDropsError,
            selectedId = otherDropsSelectedId,
            onSelect = { drop -> otherDropsSelectedId = drop.id },
            onDismiss = { showOtherDropsMap = false },
            onRetry = { otherDropsRefreshToken += 1 }
        )
    }

    if (showMyDrops) {
        MyDropsDialog(
            loading = myDropsLoading,
            drops = myDrops,
            currentLocation = myDropsCurrentLocation,
            deletingId = myDropsDeletingId,
            error = myDropsError,
            selectedId = myDropsSelectedId,
            onSelect = { drop -> myDropsSelectedId = drop.id },
            onDismiss = { showMyDrops = false },
            onRetry = { myDropsRefreshToken += 1 },
            onDelete = { drop ->
                if (drop.id.isBlank()) {
                    snackbar.showMessage(scope, "Unable to delete this drop.")
                    return@MyDropsDialog
                }

                myDropsDeletingId = drop.id
                scope.launch {
                    try {
                        repo.deleteDrop(drop.id)
                        val updated = myDrops.filterNot { it.id == drop.id }
                        myDrops = updated
                        if (myDropsSelectedId == drop.id) {
                            myDropsSelectedId = updated.firstOrNull()?.id
                        }
                        snackbar.showMessage(scope, "Drop deleted.")
                    } catch (e: Exception) {
                        snackbar.showMessage(scope, "Error: ${e.message}")
                    } finally {
                        myDropsDeletingId = null
                    }
                }
            }
        )
    }

    if (showCollectedDrops) {
        CollectedDropsDialog(
            notes = collectedNotes,
            onDismiss = { showCollectedDrops = false },
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

private fun formatCoordinate(value: Double): String {
    return String.format(Locale.US, "%.5f", value)
}


@Composable
private fun CollectedDropsDialog(
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
                        title = { Text("Collected drops") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Filled.ArrowBack,
                                    contentDescription = "Back to main page",
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                if (notes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        DialogMessageContent(
                            message = "You haven't collected any drops yet.",
                            primaryLabel = null,
                            onPrimary = null,
                            onDismiss = onDismiss
                        )
                    }
                } else {
                    var highlightedId by rememberSaveable { mutableStateOf<String?>(null) }

                    LaunchedEffect(notes) {
                        if (notes.isEmpty()) {
                            highlightedId = null
                        } else if (highlightedId == null || notes.none { it.id == highlightedId }) {
                            highlightedId = notes.firstOrNull { it.lat != null && it.lng != null }?.id
                                ?: notes.firstOrNull()?.id
                        }
                    }

                    val highlightedNote = notes.firstOrNull { it.id == highlightedId }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            CollectedDropsMap(
                                notes = notes,
                                highlightedId = highlightedId
                            )

                            if (highlightedNote != null && (highlightedNote.lat == null || highlightedNote.lng == null)) {
                                Text(
                                    text = "Location unavailable for the selected drop.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Divider()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Text(
                                text = "Select a drop to focus on the map.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(notes, key = { it.id }) { note ->
                                    CollectedNoteCard(
                                        note = note,
                                        selected = note.id == highlightedId,
                                        onSelect = { highlightedId = note.id },
                                        onView = {
                                            highlightedId = note.id
                                            onView(note)
                                        },
                                        onRemove = {
                                            highlightedId = null
                                            onRemove(note)
                                        }
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
private fun OtherDropsMapDialog(
    loading: Boolean,
    drops: List<Drop>,
    currentLocation: LatLng?,
    error: String?,
    selectedId: String?,
    onSelect: (Drop) -> Unit,
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
                        title = { Text("Nearby drops") },
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
                                message = "No drops from other users are available right now.",
                                primaryLabel = null,
                                onPrimary = null,
                                onDismiss = onDismiss
                            )
                        }

                        else -> {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "Collect a drop to reveal its contents.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    OtherDropsMap(
                                        drops = drops,
                                        selectedDropId = selectedId,
                                        currentLocation = currentLocation
                                    )
                                }

                                Divider()

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    Text(
                                        text = "Select a drop to focus on the map.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    )

                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(drops, key = { it.id }) { drop ->
                                            OtherDropRow(
                                                drop = drop,
                                                isSelected = drop.id == selectedId,
                                                onSelect = { onSelect(drop) }
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
    }
}

@Composable
private fun CollectedDropsMap(
    notes: List<CollectedNote>,
    highlightedId: String?,
    modifier: Modifier = Modifier
) {
    val notesWithLocation = remember(notes) { notes.filter { it.lat != null && it.lng != null } }
    val cameraPositionState = rememberCameraPositionState()
    val uiSettings = remember { MapUiSettings(zoomControlsEnabled = true) }

    val highlightedNote = notesWithLocation.firstOrNull { it.id == highlightedId }
    val fallbackNote = notesWithLocation.firstOrNull()

    LaunchedEffect(notesWithLocation, highlightedNote?.id) {
        val target = highlightedNote ?: fallbackNote
        if (target != null) {
            val lat = target.lat ?: return@LaunchedEffect
            val lng = target.lng ?: return@LaunchedEffect
            val zoom = if (highlightedNote != null) 15f else 12f
            val update = CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), zoom)
            cameraPositionState.animate(update)
        }
    }

    if (notesWithLocation.isEmpty()) {
        Box(modifier.fillMaxSize()) {
            Text(
                text = "No location data for collected drops yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    } else {
        GoogleMap(
            modifier = modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = uiSettings
        ) {
            notesWithLocation.forEach { note ->
                val lat = note.lat ?: return@forEach
                val lng = note.lng ?: return@forEach
                val position = LatLng(lat, lng)
                val typeLabel = when (note.contentType) {
                    DropContentType.TEXT -> "Text note"
                    DropContentType.PHOTO -> "Photo drop"
                    DropContentType.AUDIO -> "Audio drop"
                }
                val snippetParts = mutableListOf<String>()
                snippetParts.add("Type: $typeLabel")
                note.dropCreatedAt?.let { created ->
                    formatTimestamp(created)?.let { snippetParts.add("Dropped $it") }
                }
                snippetParts.add("Lat: ${formatCoordinate(lat)}, Lng: ${formatCoordinate(lng)}")
                note.groupCode?.let { snippetParts.add("Group $it") }
                note.mediaUrl?.let { snippetParts.add("Link: $it") }

                val title = note.text.ifBlank {
                    when (note.contentType) {
                        DropContentType.TEXT -> "Collected text drop"
                        DropContentType.PHOTO -> "Collected photo drop"
                        DropContentType.AUDIO -> "Collected audio drop"
                    }
                }

                val markerColor = if (note.id == highlightedId) {
                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                } else {
                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                }

                Marker(
                    state = MarkerState(position),
                    title = title,
                    snippet = snippetParts.joinToString("\n"),
                    icon = markerColor,
                    zIndex = if (note.id == highlightedId) 1f else 0f
                )
            }
        }
    }
}

@Composable
private fun CollectedNoteCard(
    note: CollectedNote,
    selected: Boolean,
    onSelect: () -> Unit,
    onView: () -> Unit,
    onRemove: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val supportingColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
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
                style = MaterialTheme.typography.bodyMedium,
                color = supportingColor
            )

            note.dropCreatedAt?.let {
                Text(
                    text = "Dropped: ${formatTimestamp(it)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = supportingColor
                )
            }

            note.groupCode?.let { group ->
                Text(
                    text = "Group: $group",
                    style = MaterialTheme.typography.bodyMedium,
                    color = supportingColor
                )
            }

            if (note.lat != null && note.lng != null) {
                Text(
                    text = "Location: ${formatCoordinate(note.lat)}, ${formatCoordinate(note.lng)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = supportingColor
                )
            } else {
                Text(
                    text = "Location: Unknown",
                    style = MaterialTheme.typography.bodyMedium,
                    color = supportingColor
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
private fun MyDropsDialog(
    loading: Boolean,
    drops: List<Drop>,
    currentLocation: LatLng?,
    deletingId: String?,
    error: String?,
    selectedId: String?,
    onSelect: (Drop) -> Unit,
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
                        title = { Text("My drops") },
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
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    MyDropsMap(
                                        drops = drops,
                                        selectedDropId = selectedId,
                                        currentLocation = currentLocation
                                    )
                                }

                                Divider()

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    Text(
                                        text = "Select a drop to focus on the map.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    )

                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(drops, key = { it.id }) { drop ->
                                            ManageDropRow(
                                                drop = drop,
                                                isDeleting = deletingId == drop.id,
                                                isSelected = drop.id == selectedId,
                                                onSelect = { onSelect(drop) },
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
private fun MyDropsMap(
    drops: List<Drop>,
    selectedDropId: String?,
    currentLocation: LatLng?
) {
    val cameraPositionState = rememberCameraPositionState()
    val uiSettings = remember { MapUiSettings(zoomControlsEnabled = true) }

    LaunchedEffect(drops, selectedDropId, currentLocation) {
        val targetDrop = drops.firstOrNull { it.id == selectedDropId }
        val target = targetDrop?.let { LatLng(it.lat, it.lng) }
            ?: currentLocation
            ?: drops.firstOrNull()?.let { LatLng(it.lat, it.lng) }
        if (target != null) {
            val update = CameraUpdateFactory.newLatLngZoom(target, 15f)
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
            drop.mediaLabel()?.let { snippetParts.add("Link: $it") }

            val isSelected = drop.id == selectedDropId
            val markerIcon = if (isSelected) {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
            } else {
                null
            }

            Marker(
                state = MarkerState(position),
                title = drop.displayTitle(),
                snippet = snippetParts.joinToString("\n"),
                icon = markerIcon,
                zIndex = if (isSelected) 2f else 0f
            )
        }
    }
}

@Composable
private fun OtherDropRow(
    drop: Drop,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val supportingColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = drop.discoveryTitle(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = drop.discoveryDescription(),
                style = MaterialTheme.typography.bodyMedium,
                color = supportingColor
            )

            Spacer(Modifier.height(8.dp))

            val typeLabel = when (drop.contentType) {
                DropContentType.TEXT -> "Text note"
                DropContentType.PHOTO -> "Photo drop"
                DropContentType.AUDIO -> "Audio drop"
            }
            Text(
                text = "Type: $typeLabel",
                style = MaterialTheme.typography.bodySmall,
                color = supportingColor
            )

            formatTimestamp(drop.createdAt)?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = supportingColor
                )
            }

            drop.groupCode?.takeIf { !it.isNullOrBlank() }?.let { groupCode ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Group $groupCode",
                    style = MaterialTheme.typography.bodySmall,
                    color = supportingColor
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Lat: ${formatCoordinate(drop.lat)}, Lng: ${formatCoordinate(drop.lng)}",
                style = MaterialTheme.typography.bodySmall,
                color = supportingColor
            )
        }
    }
}

@Composable
private fun OtherDropsMap(
    drops: List<Drop>,
    selectedDropId: String?,
    currentLocation: LatLng?
) {
    val cameraPositionState = rememberCameraPositionState()
    val uiSettings = remember { MapUiSettings(zoomControlsEnabled = true) }

    LaunchedEffect(drops, selectedDropId, currentLocation) {
        val targetDrop = drops.firstOrNull { it.id == selectedDropId }
        val target = targetDrop?.let { LatLng(it.lat, it.lng) }
            ?: currentLocation
            ?: drops.firstOrNull()?.let { LatLng(it.lat, it.lng) }
        if (target != null) {
            val update = CameraUpdateFactory.newLatLngZoom(target, 15f)
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
            val snippetParts = mutableListOf<String>()
            snippetParts.add(drop.discoveryDescription())
            formatTimestamp(drop.createdAt)?.let { snippetParts.add("Dropped $it") }
            drop.groupCode?.takeIf { !it.isNullOrBlank() }?.let { snippetParts.add("Group $it") }
            snippetParts.add("Lat: %.5f, Lng: %.5f".format(drop.lat, drop.lng))

            val isSelected = drop.id == selectedDropId
            val markerIcon = if (isSelected) {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
            } else {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            }

            Marker(
                state = MarkerState(position),
                title = drop.discoveryTitle(),
                snippet = snippetParts.joinToString("\n"),
                icon = markerIcon,
                zIndex = if (isSelected) 2f else 0f
            )
        }
    }
}

@Composable
private fun ManageDropRow(
    drop: Drop,
    isDeleting: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val supportingColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
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
                color = supportingColor
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
                    color = supportingColor
                )
            }

            Spacer(Modifier.height(4.dp))

            val visibilityLabel = drop.groupCode?.takeIf { !it.isNullOrBlank() }
                ?.let { "Group-only · $it" }
                ?: "Public drop"
            Text(
                text = visibilityLabel,
                style = MaterialTheme.typography.bodySmall,
                color = supportingColor
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Lat: %.5f, Lng: %.5f".format(drop.lat, drop.lng),
                style = MaterialTheme.typography.bodySmall,
                color = supportingColor
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
