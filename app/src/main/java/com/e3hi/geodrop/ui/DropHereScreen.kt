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
import android.util.Base64
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.e3hi.geodrop.data.DropVoteType
import com.e3hi.geodrop.data.MediaStorageRepo
import com.e3hi.geodrop.data.NoteInventory
import com.e3hi.geodrop.data.applyUserVote
import com.e3hi.geodrop.data.userVote
import com.e3hi.geodrop.data.voteScore
import com.e3hi.geodrop.geo.DropDecisionReceiver
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
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
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
    var votingDropIds by remember { mutableStateOf(setOf<String>()) }
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
    var showDropComposer by remember { mutableStateOf(false) }

    val currentUserId = currentUser?.uid

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

    fun clearAudio() {
        val uriString = capturedAudioUri
        capturedAudioUri = null
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            scope.launch(Dispatchers.IO) {
                runCatching { ctx.contentResolver.delete(uri, null, null) }
            }
        }
    }

    fun pickUpDrop(drop: Drop) {
        val currentLocation = otherDropsCurrentLocation
        val withinRange = currentLocation?.let {
            distanceBetweenMeters(it.latitude, it.longitude, drop.lat, drop.lng) <= 10.0
        } ?: false
        if (!withinRange) {
            snackbar.showMessage(scope, "Move closer to pick up this drop.")
            return
        }

        val appContext = ctx.applicationContext
        val intent = Intent(appContext, DropDecisionReceiver::class.java).apply {
            action = DropDecisionReceiver.ACTION_PICK_UP
            putExtra(DropDecisionReceiver.EXTRA_DROP_ID, drop.id)
            if (drop.text.isNotBlank()) {
                putExtra(DropDecisionReceiver.EXTRA_DROP_TEXT, drop.text)
            }
            putExtra(DropDecisionReceiver.EXTRA_DROP_CONTENT_TYPE, drop.contentType.name)
            drop.mediaUrl?.takeIf { it.isNotBlank() }?.let {
                putExtra(DropDecisionReceiver.EXTRA_DROP_MEDIA_URL, it)
            }
            drop.mediaMimeType?.takeIf { it.isNotBlank() }?.let {
                putExtra(DropDecisionReceiver.EXTRA_DROP_MEDIA_MIME_TYPE, it)
            }
            drop.mediaData?.takeIf { it.isNotBlank() }?.let {
                putExtra(DropDecisionReceiver.EXTRA_DROP_MEDIA_DATA, it)
            }
            putExtra(DropDecisionReceiver.EXTRA_DROP_LAT, drop.lat)
            putExtra(DropDecisionReceiver.EXTRA_DROP_LNG, drop.lng)
            if (drop.createdAt > 0) {
                putExtra(DropDecisionReceiver.EXTRA_DROP_CREATED_AT, drop.createdAt)
            }
            drop.groupCode?.takeIf { it.isNotBlank() }?.let {
                putExtra(DropDecisionReceiver.EXTRA_DROP_GROUP, it)
            }
        }

        val result = runCatching { appContext.sendBroadcast(intent) }
        if (result.isSuccess) {
            val remaining = otherDrops.filterNot { it.id == drop.id }
            otherDrops = remaining
            if (otherDropsSelectedId == drop.id) {
                otherDropsSelectedId = remaining.firstOrNull()?.id
            }
            snackbar.showMessage(scope, "Drop added to your collection.")

            val userId = currentUserId
            if (!userId.isNullOrBlank()) {
                scope.launch {
                    try {
                        repo.markDropCollected(drop.id, userId)
                    } catch (error: Exception) {
                        Log.w("DropHere", "Failed to sync collected drop ${drop.id}", error)
                    }
                }
            }
        } else {
            snackbar.showMessage(scope, "Couldn't pick up this drop.")
        }
    }

    fun updateDropInLists(dropId: String, transform: (Drop) -> Drop) {
        otherDrops = otherDrops.map { current ->
            if (current.id == dropId) transform(current) else current
        }
        myDrops = myDrops.map { current ->
            if (current.id == dropId) transform(current) else current
        }
    }

    fun submitVote(drop: Drop, desiredVote: DropVoteType) {
        val userId = currentUserId
        if (userId.isNullOrBlank()) {
            snackbar.showMessage(scope, "Sign in to vote on drops.")
            return
        }

        val dropId = drop.id
        if (dropId.isBlank()) return

        if (!collectedDropIds.contains(dropId)) {
            snackbar.showMessage(scope, "Collect this drop before voting on it.")
            return
        }

        val updatedDrop = drop.applyUserVote(userId, desiredVote)
        if (updatedDrop == drop) return

        val previousOtherDrops = otherDrops
        val previousMyDrops = myDrops

        votingDropIds = votingDropIds + dropId
        updateDropInLists(dropId) { current -> current.applyUserVote(userId, desiredVote) }

        scope.launch {
            try {
                repo.voteOnDrop(dropId, userId, desiredVote)
            } catch (e: Exception) {
                otherDrops = previousOtherDrops
                myDrops = previousMyDrops
                val message = e.message?.takeIf { it.isNotBlank() }
                    ?: "Couldn't update your vote. Try again."
                snackbar.showMessage(scope, message)
            } finally {
                votingDropIds = votingDropIds - dropId
            }
        }
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
                clearAudio()
            }

            DropContentType.PHOTO -> {
                clearAudio()
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
            val intent = Intent(ctx, AudioRecorderActivity::class.java)
            recordAudioLauncher.launch(intent)
        } else {
            pendingAudioPermissionAction = { ensureAudioAndLaunch() }
            audioPermissionLauncher.launch(permission)
        }
    }

    fun uiDone(lat: Double, lng: Double, groupCode: String?, contentType: DropContentType) {
        isSubmitting = false
        note = TextFieldValue("")
        capturedPhotoPath = null
        clearAudio()
        showDropComposer = false
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
        mediaInput: String?,
        mediaMimeType: String?,
        mediaData: String?
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anon"
        val sanitizedMedia = mediaInput?.takeIf { it.isNotBlank() }
        val sanitizedMime = mediaMimeType?.takeIf { it.isNotBlank() }
        val sanitizedData = mediaData?.takeIf { it.isNotBlank() }
        val sanitizedText = when (contentType) {
            DropContentType.TEXT -> noteText.trim()
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
            mediaUrl = sanitizedMedia,
            mediaMimeType = sanitizedMime,
            mediaData = sanitizedData
        )
        repo.addDrop(d) // suspend (uses Firestore .await() internally)
        if (groupCode != null) {
            groupPrefs.addGroup(groupCode)
            joinedGroups = groupPrefs.getJoinedGroups()
        }
        uiDone(lat, lng, groupCode, contentType)
    }

    fun submitDrop() {
        if (isSubmitting) return
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
                var mediaUrlResult: String? = null
                var mediaMimeTypeResult: String? = null
                var mediaDataResult: String? = null
                var dropNoteText = note.text

                when (dropContentType) {
                    DropContentType.TEXT -> {
                        val trimmed = note.text.trim()
                        if (trimmed.isEmpty()) {
                            isSubmitting = false
                            snackbar.showMessage(scope, "Enter a note before dropping.")
                            return@launch
                        }
                        dropNoteText = trimmed
                        mediaUrlResult = null
                        mediaMimeTypeResult = null
                        mediaDataResult = null
                    }

                    DropContentType.PHOTO -> {
                        val path = capturedPhotoPath ?: run {
                            dropNoteText = note.text.trim()
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
                            snackbar.showMessage(
                                scope,
                                "Couldn't read the captured photo. Retake it and try again."
                            )
                            return@launch
                        }

                        val uploaded = mediaStorage.uploadMedia(
                            DropContentType.PHOTO,
                            photoBytes,
                            "image/jpeg",
                        )

                        withContext(Dispatchers.IO) {
                            runCatching { File(path).delete() }
                        }

                        mediaUrlResult = uploaded
                        mediaMimeTypeResult = "image/jpeg"
                        mediaDataResult = null
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
                            snackbar.showMessage(
                                scope,
                                "Couldn't read the audio recording. Record again and try once more."
                            )
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

                        mediaUrlResult = uploaded
                        mediaMimeTypeResult = mimeType
                        mediaDataResult = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
                    }
                }

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
                    mediaInput = mediaUrlResult,
                    mediaMimeType = mediaMimeTypeResult,
                    mediaData = mediaDataResult
                )
            } catch (e: Exception) {
                isSubmitting = false
                snackbar.showMessage(scope, "Error: ${e.message}")
            }
        }
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


    val collectedCount = collectedNotes.size

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("GeoDrop") },
                actions = {
                    IconButton(onClick = { showManageGroups = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Groups,
                            contentDescription = "Manage group codes"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (!isSubmitting) {
                        showDropComposer = true
                    }
                },
                icon = { Icon(Icons.Rounded.Place, contentDescription = null) },
                text = { Text(if (isSubmitting) "Dropping…" else "Drop something") }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                HeroCard(
                    joinedGroups = joinedGroups,
                    onManageGroups = { showManageGroups = true }
                )
            }

            item { SectionHeader(text = "Quick actions") }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionCard(
                        icon = Icons.Rounded.Sync,
                        title = "Sync nearby drops",
                        description = "Refresh geofences and check for new surprises nearby.",
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
                        }
                    )

                    ActionCard(
                        icon = Icons.Rounded.Map,
                        title = "Browse map",
                        description = "See every drop you can currently unlock.",
                        onClick = {
                            if (FirebaseAuth.getInstance().currentUser == null) {
                                snackbar.showMessage(scope, "Signing you in… please try again shortly.")
                            } else {
                                showOtherDropsMap = true
                            }
                        }
                    )

                    ActionCard(
                        icon = Icons.Rounded.Inbox,
                        title = "My drops",
                        description = "Review, open, and manage the drops you've shared.",
                        onClick = {
                            if (FirebaseAuth.getInstance().currentUser == null) {
                                snackbar.showMessage(scope, "Signing you in… please try again shortly.")
                            } else {
                                showMyDrops = true
                            }
                        }
                    )

                    ActionCard(
                        icon = Icons.Rounded.Bookmark,
                        title = "Collected drops",
                        description = if (collectedCount == 0) {
                            "Open the drops you've saved for later."
                        } else {
                            "Open the $collectedCount drop" + if (collectedCount == 1) " you've saved." else "s you've saved."
                        },
                        onClick = {
                            collectedNotes = noteInventory.getCollectedNotes()
                            showCollectedDrops = true
                        },
                        trailingContent = {
                            if (collectedCount > 0) {
                                CountBadge(count = collectedCount)
                            }
                        }
                    )
                }
            }

            status?.let { message ->
                item { StatusCard(message = message) }
            }
        }
    }

    if (showDropComposer) {
        DropComposerDialog(
            isSubmitting = isSubmitting,
            dropContentType = dropContentType,
            onDropContentTypeChange = { dropContentType = it },
            note = note,
            onNoteChange = { note = it },
            capturedPhotoPath = capturedPhotoPath,
            onCapturePhoto = { ensureCameraAndLaunch() },
            onClearPhoto = { clearPhoto() },
            capturedAudioUri = capturedAudioUri,
            onRecordAudio = { ensureAudioAndLaunch() },
            onClearAudio = { clearAudio() },
            dropVisibility = dropVisibility,
            onDropVisibilityChange = { dropVisibility = it },
            groupCodeInput = groupCodeInput,
            onGroupCodeInputChange = { groupCodeInput = it },
            joinedGroups = joinedGroups,
            onSelectGroupCode = { code -> groupCodeInput = TextFieldValue(code) },
            onManageGroupCodes = { showManageGroups = true },
            onSubmit = { submitDrop() },
            onDismiss = {
                if (!isSubmitting) {
                    showDropComposer = false
                }
            }
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
            onPickUp = { drop -> pickUpDrop(drop) },
            currentUserId = currentUserId,
            votingDropIds = votingDropIds,
            collectedDropIds = collectedDropIds,
            onVote = { drop, vote -> submitVote(drop, vote) },
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
            onView = { drop ->
                val intent = Intent(ctx, DropDetailActivity::class.java).apply {
                    putExtra("dropId", drop.id)
                    if (drop.text.isNotBlank()) putExtra("dropText", drop.text)
                    putExtra("dropContentType", drop.contentType.name)
                    putExtra("dropLat", drop.lat)
                    putExtra("dropLng", drop.lng)
                    putExtra("dropCreatedAt", drop.createdAt)
                    drop.groupCode?.let { putExtra("dropGroupCode", it) }
                    drop.mediaUrl?.let { putExtra("dropMediaUrl", it) }
                    drop.mediaMimeType?.let { putExtra("dropMediaMimeType", it) }
                    drop.mediaData?.let { putExtra("dropMediaData", it) }
                }
                ctx.startActivity(intent)
            },
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
                    note.mediaMimeType?.let { putExtra("dropMediaMimeType", it) }
                    note.mediaData?.let { putExtra("dropMediaData", it) }
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
private fun HeroCard(
    joinedGroups: List<String>,
    onManageGroups: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Drop something at your spot",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "Leave voice notes, photos, or text that unlock when explorers arrive nearby.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )

            if (joinedGroups.isEmpty()) {
                Text(
                    text = "Add a group code to share privately with teammates, friends, or events.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            } else {
                Text(
                    text = "Active group codes",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    joinedGroups.forEach { code ->
                        AssistChip(
                            onClick = {},
                            label = { Text(code) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onManageGroups,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
            ) {
                Text("Manage group codes")
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            trailingContent?.let {
                Spacer(Modifier.width(16.dp))
                it()
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Text(
            text = count.toString(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatusCard(message: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Latest status",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun DropComposerDialog(
    isSubmitting: Boolean,
    dropContentType: DropContentType,
    onDropContentTypeChange: (DropContentType) -> Unit,
    note: TextFieldValue,
    onNoteChange: (TextFieldValue) -> Unit,
    capturedPhotoPath: String?,
    onCapturePhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    capturedAudioUri: String?,
    onRecordAudio: () -> Unit,
    onClearAudio: () -> Unit,
    dropVisibility: DropVisibility,
    onDropVisibilityChange: (DropVisibility) -> Unit,
    groupCodeInput: TextFieldValue,
    onGroupCodeInputChange: (TextFieldValue) -> Unit,
    joinedGroups: List<String>,
    onSelectGroupCode: (String) -> Unit,
    onManageGroupCodes: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = {
            if (!isSubmitting) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isSubmitting,
            dismissOnClickOutside = !isSubmitting,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create a drop",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting
                    ) {
                        Text("Cancel")
                    }
                }

                DropContentTypeSection(
                    selected = dropContentType,
                    onSelect = onDropContentTypeChange
                )

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
                    onValueChange = onNoteChange,
                    label = { Text(noteLabel) },
                    minLines = noteMinLines,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = supportingTextContent
                )

                when (dropContentType) {
                    DropContentType.PHOTO -> {
                        val hasPhoto = capturedPhotoPath != null
                        val photoPreview: (@Composable () -> Unit)? = capturedPhotoPath?.let { path ->
                            {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(File(path))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Captured photo preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 160.dp, max = 240.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        MediaCaptureCard(
                            title = "Attach a photo",
                            description = "Snap a picture with your camera to pin at this location.",
                            status = if (hasPhoto) "Photo ready to upload." else "No photo captured yet.",
                            isReady = hasPhoto,
                            primaryLabel = if (hasPhoto) "Retake photo" else "Open camera",
                            primaryIcon = Icons.Rounded.PhotoCamera,
                            onPrimary = {
                                if (hasPhoto) {
                                    onClearPhoto()
                                }
                                onCapturePhoto()
                            },
                            secondaryLabel = if (hasPhoto) "Remove photo" else null,
                            onSecondary = if (hasPhoto) {
                                { onClearPhoto() }
                            } else {
                                null
                            },
                            previewContent = photoPreview
                        )
                    }

                    DropContentType.AUDIO -> {
                        val hasAudio = capturedAudioUri != null
                        val audioPreview: (@Composable () -> Unit)? = if (hasAudio) {
                            {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Audio attached",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Ready to drop your voice note.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            null
                        }
                        MediaCaptureCard(
                            title = "Record audio",
                            description = "Capture a short voice note for anyone who discovers this drop.",
                            status = if (hasAudio) "Audio message ready to upload." else "No recording yet.",
                            isReady = hasAudio,
                            primaryLabel = if (hasAudio) "Record again" else "Record audio",
                            primaryIcon = Icons.Rounded.Mic,
                            onPrimary = {
                                if (hasAudio) {
                                    onClearAudio()
                                }
                                onRecordAudio()
                            },
                            secondaryLabel = if (hasAudio) "Remove audio" else null,
                            onSecondary = if (hasAudio) {
                                { onClearAudio() }
                            } else {
                                null
                            },
                            previewContent = audioPreview
                        )
                    }

                    DropContentType.TEXT -> Unit
                }

                DropVisibilitySection(
                    visibility = dropVisibility,
                    onVisibilityChange = onDropVisibilityChange,
                    groupCodeInput = groupCodeInput,
                    onGroupCodeInputChange = onGroupCodeInputChange,
                    joinedGroups = joinedGroups,
                    onSelectGroupCode = onSelectGroupCode
                )

                OutlinedButton(
                    onClick = onManageGroupCodes,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Groups,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Manage group codes")
                }

                Button(
                    enabled = !isSubmitting,
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Dropping…")
                    } else {
                        Icon(Icons.Rounded.Place, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Drop content")
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
    onPickUp: (Drop) -> Unit,
    currentUserId: String?,
    votingDropIds: Set<String>,
    collectedDropIds: Set<String>,
    onVote: (Drop, DropVoteType) -> Unit,
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
                                    val isSignedIn = !currentUserId.isNullOrBlank()
                                    Text(
                                        text = "Select a drop to focus on the map. If you're close enough, pick it up here.",
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
                                            val canVote = isSignedIn && collectedDropIds.contains(drop.id)
                                            OtherDropRow(
                                                drop = drop,
                                                isSelected = drop.id == selectedId,
                                                currentLocation = currentLocation,
                                                userVote = drop.userVote(currentUserId),
                                                canVote = canVote,
                                                isVoting = votingDropIds.contains(drop.id),
                                                onSelect = { onSelect(drop) },
                                                onVote = { vote -> onVote(drop, vote) },
                                                onPickUp = { onPickUp(drop) }
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

            val mediaUrl = note.mediaUrl?.takeIf { it.isNotBlank() }
            if (note.contentType == DropContentType.PHOTO && mediaUrl != null) {
                val context = LocalContext.current
                val imageRequest = remember(mediaUrl) {
                    ImageRequest.Builder(context)
                        .data(mediaUrl)
                        .crossfade(true)
                        .build()
                }

                AsyncImage(
                    model = imageRequest,
                    contentDescription = note.text.ifBlank { "Photo drop" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 280.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

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
    onView: (Drop) -> Unit,
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
                                                onView = { onView(drop) },
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
    isReady: Boolean,
    primaryLabel: String,
    primaryIcon: ImageVector? = null,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    previewContent: (@Composable () -> Unit)? = null
) {
    val containerColor by animateColorAsState(
        targetValue = if (isReady) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "mediaContainer"
    )
    val contentColor = if (isReady) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val supportingColor = if (isReady) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusIcon = if (isReady) Icons.Rounded.CheckCircle else Icons.Rounded.Info
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = supportingColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = if (isReady) MaterialTheme.colorScheme.primary else supportingColor
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = supportingColor,
                    modifier = Modifier.weight(1f)
                )
            }

            previewContent?.let {
                it()
            }

            Button(
                onClick = onPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (primaryIcon != null) {
                    Icon(primaryIcon, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text(primaryLabel)
            }
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
            snippetParts.add("Score: ${formatVoteScore(drop.voteScore())} (↑${drop.upvoteCount} / ↓${drop.downvoteCount})")

            val isSelected = drop.id == selectedDropId
            val markerIcon = BitmapDescriptorFactory.defaultMarker(voteHueFor(drop.upvoteCount))

            Marker(
                state = MarkerState(position),
                title = drop.displayTitle(),
                snippet = snippetParts.joinToString("\n"),
                icon = markerIcon,
                alpha = if (isSelected) 1f else 0.9f,
                zIndex = if (isSelected) 2f else 0f
            )
        }
    }
}

@Composable
private fun OtherDropRow(
    drop: Drop,
    isSelected: Boolean,
    currentLocation: LatLng?,
    userVote: DropVoteType,
    canVote: Boolean,
    isVoting: Boolean,
    onSelect: () -> Unit,
    onVote: (DropVoteType) -> Unit,
    onPickUp: () -> Unit
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
    val distanceMeters = currentLocation?.let { location ->
        distanceBetweenMeters(location.latitude, location.longitude, drop.lat, drop.lng)
    }
    val withinPickupRange = distanceMeters != null && distanceMeters <= 10.0

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

            distanceMeters?.let { distance ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "You're ${formatDistanceMeters(distance)} away.",
                    style = MaterialTheme.typography.bodySmall,
                    color = supportingColor
                )
            }

            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VoteToggleButton(
                        icon = Icons.Rounded.ThumbUp,
                        label = drop.upvoteCount.toString(),
                        selected = userVote == DropVoteType.UPVOTE,
                        enabled = canVote && !isVoting,
                        onClick = {
                            val nextVote = if (userVote == DropVoteType.UPVOTE) {
                                DropVoteType.NONE
                            } else {
                                DropVoteType.UPVOTE
                            }
                            onVote(nextVote)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    VoteToggleButton(
                        icon = Icons.Rounded.ThumbDown,
                        label = drop.downvoteCount.toString(),
                        selected = userVote == DropVoteType.DOWNVOTE,
                        enabled = canVote && !isVoting,
                        onClick = {
                            val nextVote = if (userVote == DropVoteType.DOWNVOTE) {
                                DropVoteType.NONE
                            } else {
                                DropVoteType.DOWNVOTE
                            }
                            onVote(nextVote)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    if (isVoting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                Text(
                    text = "Score: ${formatVoteScore(drop.voteScore())} (↑${drop.upvoteCount} / ↓${drop.downvoteCount})",
                    style = MaterialTheme.typography.bodySmall,
                    color = supportingColor
                )
            }

            if (!canVote) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Collect this drop to vote on it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = supportingColor
                )
            }

            if (withinPickupRange) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onPickUp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pick up drop")
                }
            }
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
            snippetParts.add("Score: ${formatVoteScore(drop.voteScore())} (↑${drop.upvoteCount} / ↓${drop.downvoteCount})")

            val isSelected = drop.id == selectedDropId

            val markerIcon = BitmapDescriptorFactory.defaultMarker(voteHueFor(drop.upvoteCount))

            Marker(
                state = MarkerState(position),
                title = drop.discoveryTitle(),
                snippet = snippetParts.joinToString("\n"),
                icon = markerIcon,
                alpha = if (isSelected) 1f else 0.9f,
                zIndex = if (isSelected) 2f else 0f
            )
        }
    }
}

private fun distanceBetweenMeters(
    startLat: Double,
    startLng: Double,
    endLat: Double,
    endLng: Double
): Double {
    val radius = 6371000.0
    val dLat = Math.toRadians(endLat - startLat)
    val dLng = Math.toRadians(endLng - startLng)
    val originLat = Math.toRadians(startLat)
    val targetLat = Math.toRadians(endLat)
    val sinLat = sin(dLat / 2)
    val sinLng = sin(dLng / 2)
    val h = sinLat * sinLat + cos(originLat) * cos(targetLat) * sinLng * sinLng
    return 2 * radius * asin(min(1.0, sqrt(h)))
}

private fun formatDistanceMeters(distance: Double): String {
    return if (distance >= 1000) {
        String.format(Locale.getDefault(), "%.2f km", distance / 1000.0)
    } else {
        "${distance.roundToInt()} m"
    }
}

private fun voteHueFor(upvotes: Long): Float {
    return when {
        upvotes >= 25 -> BitmapDescriptorFactory.HUE_AZURE
        upvotes >= 10 -> BitmapDescriptorFactory.HUE_GREEN
        upvotes >= 5 -> BitmapDescriptorFactory.HUE_YELLOW
        upvotes >= 1 -> BitmapDescriptorFactory.HUE_ORANGE
        else -> BitmapDescriptorFactory.HUE_RED
    }
}

private fun formatVoteScore(score: Long): String {
    return when {
        score > 0 -> "+$score"
        score < 0 -> score.toString()
        else -> "0"
    }
}

@Composable
private fun VoteToggleButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        FilledTonalButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = 40.dp)
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = 40.dp)
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(label)
        }
    }
}

@Composable
private fun ManageDropRow(
    drop: Drop,
    isDeleting: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onView: () -> Unit,
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
                Spacer(Modifier.height(12.dp))
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

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Score: ${formatVoteScore(drop.voteScore())} (↑${drop.upvoteCount} / ↓${drop.downvoteCount})",
                style = MaterialTheme.typography.bodySmall,
                color = supportingColor
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onView,
                    enabled = !isDeleting
                ) {
                    Text("View details")
                }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropContentTypeSection(
    selected: DropContentType,
    onSelect: (DropContentType) -> Unit
) {
    val options = remember {
        listOf(
            DropContentTypeOption(
                type = DropContentType.TEXT,
                title = "Text note",
                description = "Share a written message for people nearby.",
                icon = Icons.Rounded.Edit
            ),
            DropContentTypeOption(
                type = DropContentType.PHOTO,
                title = "Photo drop",
                description = "Capture a photo with your camera that others can open.",
                icon = Icons.Rounded.PhotoCamera
            ),
            DropContentTypeOption(
                type = DropContentType.AUDIO,
                title = "Audio drop",
                description = "Record a quick voice message for nearby explorers.",
                icon = Icons.Rounded.Mic
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Drop content", style = MaterialTheme.typography.titleSmall)

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    onClick = { onSelect(option.type) },
                    selected = option.type == selected,
                    label = { Text(option.title) },
                    icon = {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null
                        )
                    }
                )
            }
        }

        Crossfade(targetState = selected, label = "dropContentDescription") { type ->
            val message = options.firstOrNull { it.type == type }?.description ?: ""
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DropVisibilitySection(
    visibility: DropVisibility,
    onVisibilityChange: (DropVisibility) -> Unit,
    groupCodeInput: TextFieldValue,
    onGroupCodeInputChange: (TextFieldValue) -> Unit,
    joinedGroups: List<String>,
    onSelectGroupCode: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Drop visibility", style = MaterialTheme.typography.titleSmall)

        DropVisibilityOptionCard(
            title = "Public drop",
            description = "Anyone nearby can discover this note.",
            icon = Icons.Rounded.Public,
            selected = visibility == DropVisibility.Public,
            onClick = { onVisibilityChange(DropVisibility.Public) }
        )

        DropVisibilityOptionCard(
            title = "Group-only drop",
            description = "Limit discovery to people who share your group code.",
            icon = Icons.Rounded.Lock,
            selected = visibility == DropVisibility.GroupOnly,
            onClick = { onVisibilityChange(DropVisibility.GroupOnly) }
        )

        if (visibility == DropVisibility.GroupOnly) {
            OutlinedTextField(
                value = groupCodeInput,
                onValueChange = onGroupCodeInputChange,
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
                            onClick = { onSelectGroupCode(code) },
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropVisibilityOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "visibilityContainer"
    )
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
    val cardShape = CardDefaults.elevatedShape
    val cardModifier = Modifier
        .fillMaxWidth()
        .then(
            if (selected) {
                Modifier
            } else {
                Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = cardShape
                )
            }
        )
        .clickable(onClick = onClick)

    ElevatedCard(
        modifier = cardModifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = cardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = supportingColor
                )
            }

            Icon(
                imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

private data class DropContentTypeOption(
    val type: DropContentType,
    val title: String,
    val description: String,
    val icon: ImageVector
)

private enum class DropVisibility { Public, GroupOnly }

/** Tiny helper to show snackbars from non-suspend places. */
private fun SnackbarHostState.showMessage(scope: kotlinx.coroutines.CoroutineScope, msg: String) {
    scope.launch { showSnackbar(msg) }
}
