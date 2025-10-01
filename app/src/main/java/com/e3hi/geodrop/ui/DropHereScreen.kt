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
import android.util.Patterns
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.e3hi.geodrop.R
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
import com.e3hi.geodrop.data.DropType
import com.e3hi.geodrop.data.UserProfile
import com.e3hi.geodrop.data.UserRole
import com.e3hi.geodrop.data.canViewNsfw
import com.e3hi.geodrop.data.RedemptionResult
import com.e3hi.geodrop.data.applyUserVote
import com.e3hi.geodrop.data.isBusinessDrop
import com.e3hi.geodrop.data.isRedeemedBy
import com.e3hi.geodrop.data.remainingRedemptions
import com.e3hi.geodrop.data.requiresRedemption
import com.e3hi.geodrop.data.userVote
import com.e3hi.geodrop.data.voteScore
import com.e3hi.geodrop.data.isBusiness
import com.e3hi.geodrop.data.VisionApiStatus
import com.e3hi.geodrop.geo.DropDecisionReceiver
import com.e3hi.geodrop.geo.NearbyDropRegistrar
import com.e3hi.geodrop.util.GroupPreferences
import com.e3hi.geodrop.util.formatTimestamp
import com.e3hi.geodrop.util.DropBlockedBySafetyException
import com.e3hi.geodrop.util.DropSafetyAssessment
import com.e3hi.geodrop.util.DropSafetyEvaluator
import com.e3hi.geodrop.util.NoOpDropSafetyEvaluator
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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
import java.util.ArrayList
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
fun DropHereScreen(
    dropSafetyEvaluator: DropSafetyEvaluator = NoOpDropSafetyEvaluator
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var signingIn by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf<String?>(null) }
    var showBusinessSignIn by remember { mutableStateOf(false) }
    var businessAuthMode by remember { mutableStateOf(BusinessAuthMode.SIGN_IN) }
    var businessEmail by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var businessPassword by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var businessConfirmPassword by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var businessAuthSubmitting by remember { mutableStateOf(false) }
    var businessAuthError by remember { mutableStateOf<String?>(null) }
    var businessAuthStatus by remember { mutableStateOf<String?>(null) }
    var showBusinessOnboarding by remember { mutableStateOf(false) }
    var businessGoogleSigningIn by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }
    var signingOut by remember { mutableStateOf(false) }
    var showNsfwDialog by remember { mutableStateOf(false) }
    var nsfwUpdating by remember { mutableStateOf(false) }
    var nsfwUpdateError by remember { mutableStateOf<String?>(null) }
    val defaultWebClientId = stringResource(R.string.default_web_client_id)
    val googleSignInClient = remember(defaultWebClientId, ctx) {
        GoogleSignIn.getClient(
            ctx,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .apply {
                    if (defaultWebClientId.isNotBlank()) {
                        requestIdToken(defaultWebClientId)
                    }
                }
                .requestEmail()
                .build()
        )
    }

    fun resetBusinessAuthFields(clearEmail: Boolean) {
        if (clearEmail) {
            businessEmail = TextFieldValue("")
        }
        businessPassword = TextFieldValue("")
        businessConfirmPassword = TextFieldValue("")
        businessAuthError = null
        businessAuthStatus = null
        nsfwUpdateError = null
        nsfwUpdating = false
    }

    fun dismissBusinessAuthDialog() {
        if (businessAuthSubmitting || businessGoogleSigningIn) return
        showBusinessSignIn = false
        resetBusinessAuthFields(clearEmail = false)
        businessAuthMode = BusinessAuthMode.SIGN_IN
    }

    fun startExplorerSignIn() {
        if (signingIn || signingOut) return

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

    fun openBusinessAuthDialog(initialMode: BusinessAuthMode = BusinessAuthMode.SIGN_IN) {
        if (businessAuthSubmitting || businessGoogleSigningIn) return
        businessAuthMode = initialMode
        resetBusinessAuthFields(clearEmail = true)
        showBusinessSignIn = true
    }

    fun performBusinessAuth() {
        if (businessAuthSubmitting || businessGoogleSigningIn) return

        val email = businessEmail.text.trim()
        val password = businessPassword.text
        val confirm = businessConfirmPassword.text

        when {
            email.isEmpty() -> {
                businessAuthError = "Enter your email address."
                return
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                businessAuthError = "Enter a valid email address."
                return
            }

            password.length < 6 -> {
                businessAuthError = "Password must be at least 6 characters."
                return
            }

            businessAuthMode == BusinessAuthMode.REGISTER && confirm != password -> {
                businessAuthError = "Passwords do not match."
                return
            }
        }

        businessAuthSubmitting = true
        businessAuthError = null
        businessAuthStatus = null

        val selectedMode = businessAuthMode
        val task = try {
            when (selectedMode) {
                BusinessAuthMode.SIGN_IN -> auth.signInWithEmailAndPassword(email, password)
                BusinessAuthMode.REGISTER -> auth.createUserWithEmailAndPassword(email, password)
            }
        } catch (error: Exception) {
            businessAuthSubmitting = false
            businessAuthError = error.localizedMessage?.takeIf { it.isNotBlank() }
                ?: if (selectedMode == BusinessAuthMode.REGISTER) {
                    "Couldn't create your business account. Try again."
                } else {
                    "Couldn't sign you in. Check your email and password."
                }
            return
        }

        task.addOnCompleteListener { authTask ->
            businessAuthSubmitting = false
            if (authTask.isSuccessful) {
                resetBusinessAuthFields(clearEmail = true)
                showBusinessSignIn = false
                if (selectedMode == BusinessAuthMode.REGISTER) {
                    showBusinessOnboarding = true
                }
            } else {
                val message = authTask.exception?.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: if (selectedMode == BusinessAuthMode.REGISTER) {
                        "Couldn't create your business account. Try again."
                    } else {
                        "Couldn't sign you in. Check your email and password."
                    }
                businessAuthError = message
            }
        }
    }

    fun sendBusinessPasswordReset() {
        if (businessAuthSubmitting || businessGoogleSigningIn) return

        val email = businessEmail.text.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            businessAuthError = "Enter a valid email address to reset your password."
            return
        }

        businessAuthSubmitting = true
        businessAuthError = null
        businessAuthStatus = null

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                businessAuthSubmitting = false
                if (task.isSuccessful) {
                    businessAuthStatus = "Password reset email sent to $email."
                } else {
                    val message = task.exception?.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "Couldn't send password reset email. Try again later."
                    businessAuthError = message
                }
            }
    }

    val businessGoogleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            businessGoogleSigningIn = false
            businessAuthError = if (result.resultCode == Activity.RESULT_CANCELED) {
                "Google sign-in was cancelled."
            } else {
                "Google sign-in failed. Try again."
            }
            return@rememberLauncherForActivityResult
        }

        val signInTask = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = signInTask.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken.isNullOrBlank()) {
                businessGoogleSigningIn = false
                businessAuthError = "Google sign-in is misconfigured. Provide a valid web client ID."
                return@rememberLauncherForActivityResult
            }

            businessAuthSubmitting = true
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    businessAuthSubmitting = false
                    businessGoogleSigningIn = false
                    if (authTask.isSuccessful) {
                        resetBusinessAuthFields(clearEmail = true)
                        showBusinessSignIn = false
                        if (authTask.result?.additionalUserInfo?.isNewUser == true) {
                            showBusinessOnboarding = true
                        }
                    } else {
                        val message = authTask.exception?.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: "Couldn't sign you in with Google. Try again."
                        businessAuthError = message
                    }
                }
        } catch (error: ApiException) {
            businessGoogleSigningIn = false
            businessAuthError = error.localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Google sign-in failed. Try again."
        }
    }

    fun startBusinessGoogleSignIn() {
        if (businessAuthSubmitting || businessGoogleSigningIn) return
        if (defaultWebClientId.isBlank()) {
            businessAuthError = "Google sign-in isn't configured. Add your default_web_client_id in strings.xml."
            return
        }

        businessAuthError = null
        businessAuthStatus = null
        businessGoogleSigningIn = true

        runCatching { googleSignInClient.signOut() }

        runCatching {
            businessGoogleSignInLauncher.launch(googleSignInClient.signInIntent)
        }.onFailure { error ->
            businessGoogleSigningIn = false
            businessAuthError = error.localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Couldn't start Google sign-in."
        }
    }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(currentUser, businessAuthSubmitting, businessGoogleSigningIn, signingOut) {
        if (currentUser == null) {
            showAccountMenu = false
            if (!businessAuthSubmitting && !businessGoogleSigningIn) {
                showBusinessSignIn = false
                businessAuthMode = BusinessAuthMode.SIGN_IN
                resetBusinessAuthFields(clearEmail = true)
                if (!signingOut) {
                    startExplorerSignIn()
                }
            }
        } else {
            signingIn = false
            signInError = null
        }
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
        ExplorerAutoSignInScreen(
            isSigningIn = signingIn,
            error = signInError,
            onRetry = { startExplorerSignIn() }
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
    var dropType by remember { mutableStateOf(DropType.COMMUNITY) }
    var note by remember { mutableStateOf(TextFieldValue("")) }
    var capturedPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var capturedAudioUri by rememberSaveable { mutableStateOf<String?>(null) }
    var capturedVideoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var groupCodeInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var redemptionCodeInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var redemptionLimitInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
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
    var showBusinessDashboard by remember { mutableStateOf(false) }
    var businessDrops by remember { mutableStateOf<List<Drop>>(emptyList()) }
    var businessDashboardLoading by remember { mutableStateOf(false) }
    var businessDashboardError by remember { mutableStateOf<String?>(null) }
    var businessDashboardRefreshToken by remember { mutableStateOf(0) }
    var selectedHomeDestination by rememberSaveable { mutableStateOf(HomeDestination.Explorer.name) }

    fun handleSignOut() {
        if (signingOut) return

        showAccountMenu = false
        signingOut = true
        showBusinessDashboard = false
        showBusinessOnboarding = false
        showDropComposer = false
        showMyDrops = false
        showOtherDropsMap = false
        showCollectedDrops = false
        showManageGroups = false
        showBusinessSignIn = false
        showNsfwDialog = false
        status = null
        signInError = null
        businessAuthError = null
        businessAuthStatus = null

        scope.launch {
            val result = runCatching {
                runCatching { googleSignInClient.signOut() }
                auth.signOut()
            }

            signingOut = false

            if (result.isSuccess) {
                selectedHomeDestination = HomeDestination.Explorer.name
                snackbar.showMessage(scope, "Signed out.")
            } else {
                val message = result.exceptionOrNull()?.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Couldn't sign out. Try again."
                snackbar.showMessage(scope, message)
            }
        }
    }

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var userProfileLoading by remember { mutableStateOf(false) }
    var userProfileError by remember { mutableStateOf<String?>(null) }

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

    fun clearVideo() {
        capturedVideoUri = null
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
            putExtra(DropDecisionReceiver.EXTRA_DROP_TYPE, drop.dropType.name)
            drop.businessId?.takeIf { it.isNotBlank() }?.let {
                putExtra(DropDecisionReceiver.EXTRA_DROP_BUSINESS_ID, it)
            }
            drop.businessName?.takeIf { it.isNotBlank() }?.let {
                putExtra(DropDecisionReceiver.EXTRA_DROP_BUSINESS_NAME, it)
            }
            drop.redemptionLimit?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_REDEMPTION_LIMIT, it) }
            putExtra(DropDecisionReceiver.EXTRA_DROP_REDEMPTION_COUNT, drop.redemptionCount)
            putExtra(DropDecisionReceiver.EXTRA_DROP_IS_NSFW, drop.isNsfw)
            if (drop.nsfwLabels.isNotEmpty()) {
                putStringArrayListExtra(
                    DropDecisionReceiver.EXTRA_DROP_NSFW_LABELS,
                    ArrayList(drop.nsfwLabels)
                )
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

    LaunchedEffect(currentUser) {
        val uid = currentUser?.uid
        if (uid.isNullOrBlank()) {
            userProfile = null
            userProfileError = null
            userProfileLoading = false
            dropType = DropType.COMMUNITY
        } else {
            userProfileLoading = true
            userProfileError = null
            try {
                userProfile = repo.ensureUserProfile(uid, currentUser?.displayName)
            } catch (error: Exception) {
                userProfileError = error.localizedMessage ?: "Failed to load your profile."
            } finally {
                userProfileLoading = false
            }
        }
    }

    LaunchedEffect(userProfile?.role) {
        val currentDestination = runCatching { HomeDestination.valueOf(selectedHomeDestination) }
            .getOrDefault(HomeDestination.Explorer)
        if (userProfile?.isBusiness() == true) {
            if (currentDestination != HomeDestination.Business) {
                selectedHomeDestination = HomeDestination.Business.name
            }
        } else {
            dropType = DropType.COMMUNITY
            if (currentDestination == HomeDestination.Business) {
                selectedHomeDestination = HomeDestination.Explorer.name
            }
        }
    }

    LaunchedEffect(dropContentType) {
        when (dropContentType) {
            DropContentType.TEXT -> {
                capturedPhotoPath = null
                clearAudio()
                clearVideo()
            }

            DropContentType.PHOTO -> {
                clearAudio()
                clearVideo()
            }

            DropContentType.AUDIO -> {
                capturedPhotoPath = null
                clearVideo()
            }

            DropContentType.VIDEO -> {
                capturedPhotoPath = null
                clearAudio()
            }
        }
    }

    LaunchedEffect(dropType) {
        if (dropType != DropType.RESTAURANT_COUPON) {
            redemptionCodeInput = TextFieldValue("")
            redemptionLimitInput = TextFieldValue("")
        }
    }

    LaunchedEffect(showBusinessDashboard, businessDashboardRefreshToken, currentUserId) {
        if (showBusinessDashboard) {
            if (userProfile?.isBusiness() == true && !currentUserId.isNullOrBlank()) {
                businessDashboardLoading = true
                businessDashboardError = null
                try {
                    businessDrops = repo.getBusinessDrops(currentUserId)
                } catch (error: Exception) {
                    businessDashboardError = error.localizedMessage ?: "Couldn't load your dashboard."
                } finally {
                    businessDashboardLoading = false
                }
            } else {
                businessDrops = emptyList()
            }
        } else {
            businessDashboardLoading = false
            businessDashboardError = null
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

    val captureVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                capturedVideoUri = uri.toString()
            } else {
                snackbar.showMessage(scope, "Video unavailable. Try again.")
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

    fun ensureVideoAndLaunch() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            runCatching { captureVideoLauncher.launch(intent) }
                .onFailure {
                    snackbar.showMessage(scope, "Couldn't open the camera for video.")
                }
        } else {
            pendingPermissionAction = { ensureVideoAndLaunch() }
            cameraPermissionLauncher.launch(permission)
        }
    }

    fun uiDone(
        lat: Double,
        lng: Double,
        groupCode: String?,
        contentType: DropContentType,
        dropType: DropType
    ) {
        isSubmitting = false
        note = TextFieldValue("")
        capturedPhotoPath = null
        clearAudio()
        clearVideo()
        showDropComposer = false
        if (dropType == DropType.RESTAURANT_COUPON) {
            redemptionCodeInput = TextFieldValue("")
            redemptionLimitInput = TextFieldValue("")
        }
        val baseStatus = "Dropped at (%.5f, %.5f)".format(lat, lng)
        val typeSummary = when (dropType) {
            DropType.RESTAURANT_COUPON -> "business offer"
            DropType.TOUR_STOP -> "tour stop"
            DropType.COMMUNITY -> when (contentType) {
                DropContentType.TEXT -> "note"
                DropContentType.PHOTO -> "photo drop"
                DropContentType.AUDIO -> "audio drop"
                DropContentType.VIDEO -> "video drop"
            }
        }
        status = if (groupCode != null) {
            "$baseStatus for group $groupCode ($typeSummary)"
        } else {
            "$baseStatus ($typeSummary)"
        }
        val snackbarMessage = when {
            groupCode != null -> "Group drop saved!"
            dropType == DropType.RESTAURANT_COUPON -> "Offer published!"
            dropType == DropType.TOUR_STOP -> "Tour stop saved!"
            else -> when (contentType) {
                DropContentType.TEXT -> "Note dropped!"
                DropContentType.PHOTO -> "Photo drop saved!"
                DropContentType.AUDIO -> "Audio drop saved!"
                DropContentType.VIDEO -> "Video drop saved!"
            }
        }
        scope.launch { snackbar.showSnackbar(snackbarMessage) }
    }

    suspend fun addDropAt(
        lat: Double,
        lng: Double,
        groupCode: String?,
        contentType: DropContentType,
        dropType: DropType,
        noteText: String,
        mediaInput: String?,
        mediaMimeType: String?,
        mediaData: String?,
        mediaDataForSafety: String?,
        mediaStoragePath: String?,
        redemptionCode: String?,
        redemptionLimit: Int?,
        nsfwAllowed: Boolean
    ): DropSafetyAssessment {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anon"
        val sanitizedMedia = mediaInput?.takeIf { it.isNotBlank() }
        val sanitizedMime = mediaMimeType?.takeIf { it.isNotBlank() }
        val sanitizedRedemptionCode = redemptionCode?.trim()?.takeIf { it.isNotEmpty() }
        val sanitizedRedemptionLimit = redemptionLimit?.takeIf { it > 0 }
        val sanitizedData = mediaData?.takeIf { it.isNotBlank() }
        val sanitizedSafetyData = mediaDataForSafety?.takeIf { it.isNotBlank() }
        val sanitizedText = when (contentType) {
            DropContentType.TEXT -> noteText.trim()
            DropContentType.PHOTO, DropContentType.AUDIO, DropContentType.VIDEO -> noteText.trim()
        }
        val d = Drop(
            text = sanitizedText,
            lat = lat,
            lng = lng,
            createdBy = uid,
            createdAt = System.currentTimeMillis(),
            groupCode = groupCode,
            dropType = dropType,
            businessId = if (dropType != DropType.COMMUNITY) uid else null,
            businessName = if (dropType != DropType.COMMUNITY) userProfile?.businessName else null,
            contentType = contentType,
            mediaUrl = sanitizedMedia,
            mediaMimeType = sanitizedMime,
            mediaData = sanitizedData,
            mediaStoragePath = mediaStoragePath?.takeIf { it.isNotBlank() },
            redemptionCode = if (dropType == DropType.RESTAURANT_COUPON) sanitizedRedemptionCode else null,
            redemptionLimit = if (dropType == DropType.RESTAURANT_COUPON) sanitizedRedemptionLimit else null
        )

        val safety = dropSafetyEvaluator.assess(
            text = sanitizedText,
            contentType = contentType,
            mediaMimeType = sanitizedMime,
            mediaData = sanitizedSafetyData,
            mediaUrl = sanitizedMedia
        )

        if (safety.isNsfw && !nsfwAllowed) {
            throw DropBlockedBySafetyException(safety)
        }

        val dropToSave = d.copy(
            isNsfw = safety.isNsfw,
            nsfwLabels = safety.reasons
        )

        repo.addDrop(dropToSave) // suspend (uses Firestore .await() internally)
        if (groupCode != null) {
            groupPrefs.addGroup(groupCode)
            joinedGroups = groupPrefs.getJoinedGroups()
        }
        uiDone(lat, lng, groupCode, contentType, dropType)
        return safety
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
                var mediaStoragePathResult: String? = null
                var mediaMimeTypeResult: String? = null
                var mediaDataResult: String? = null
                var mediaDataForSafetyResult: String? = null
                var dropNoteText = note.text
                var redemptionCodeResult: String? = null
                var redemptionLimitResult: Int? = null

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
                        mediaStoragePathResult = null
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

                        val uploadResult = mediaStorage.uploadMedia(
                            DropContentType.PHOTO,
                            photoBytes,
                            "image/jpeg",
                        )

                        withContext(Dispatchers.IO) {
                            runCatching { File(path).delete() }
                        }

                        mediaUrlResult = uploadResult.downloadUrl
                        mediaMimeTypeResult = "image/jpeg"
                        mediaDataResult = null
                        mediaDataForSafetyResult = Base64.encodeToString(photoBytes, Base64.NO_WRAP)
                        mediaStoragePathResult = uploadResult.storagePath
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

                        val uploadResult = mediaStorage.uploadMedia(
                            DropContentType.AUDIO,
                            audioBytes,
                            mimeType
                        )

                        withContext(Dispatchers.IO) {
                            runCatching { ctx.contentResolver.delete(uri, null, null) }
                        }

                        mediaUrlResult = uploadResult.downloadUrl
                        mediaMimeTypeResult = mimeType
                        mediaDataResult = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
                        mediaDataForSafetyResult = mediaDataResult
                        mediaStoragePathResult = uploadResult.storagePath
                    }

                    DropContentType.VIDEO -> {
                        val uriString = capturedVideoUri ?: run {
                            isSubmitting = false
                            snackbar.showMessage(scope, "Record a video before dropping.")
                            return@launch
                        }
                        val uri = Uri.parse(uriString)

                        val videoBytes = withContext(Dispatchers.IO) {
                            try {
                                ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            } catch (e: IOException) {
                                null
                            }
                        } ?: run {
                            isSubmitting = false
                            snackbar.showMessage(
                                scope,
                                "Couldn't read the recorded video. Try again."
                            )
                            return@launch
                        }

                        val mimeType = ctx.contentResolver.getType(uri) ?: "video/mp4"

                        val uploadResult = mediaStorage.uploadMedia(
                            DropContentType.VIDEO,
                            videoBytes,
                            mimeType
                        )

                        mediaUrlResult = uploadResult.downloadUrl
                        mediaMimeTypeResult = mimeType
                        mediaDataResult = null
                        mediaDataForSafetyResult = null
                        mediaStoragePathResult = uploadResult.storagePath
                    }
                }

                if (dropType == DropType.RESTAURANT_COUPON) {
                    val trimmedCode = redemptionCodeInput.text.trim()
                    if (trimmedCode.isEmpty()) {
                        isSubmitting = false
                        snackbar.showMessage(scope, "Enter a redemption code for your offer.")
                        return@launch
                    }
                    redemptionCodeResult = trimmedCode

                    val limitText = redemptionLimitInput.text.trim()
                    if (limitText.isNotEmpty()) {
                        val parsed = limitText.toIntOrNull()
                        if (parsed == null || parsed <= 0) {
                            isSubmitting = false
                            snackbar.showMessage(scope, "Enter a valid redemption limit or leave it blank.")
                            return@launch
                        }
                        redemptionLimitResult = parsed
                    }
                }

                val (lat, lng) = getLatestLocation() ?: run {
                    isSubmitting = false
                    snackbar.showMessage(scope, "No location available. Turn on GPS & try again.")
                    return@launch
                }

                val safety = addDropAt(
                    lat = lat,
                    lng = lng,
                    groupCode = selectedGroupCode,
                    contentType = dropContentType,
                    dropType = dropType,
                    noteText = dropNoteText,
                    mediaInput = mediaUrlResult,
                    mediaMimeType = mediaMimeTypeResult,
                    mediaData = mediaDataResult,
                    mediaDataForSafety = mediaDataForSafetyResult ?: mediaDataResult,
                    mediaStoragePath = mediaStoragePathResult,
                    redemptionCode = redemptionCodeResult,
                    redemptionLimit = redemptionLimitResult,
                    nsfwAllowed = userProfile?.canViewNsfw() == true
                )
                val baseStatusMessage = status
                val visionMessage = visionStatusMessage(
                    assessment = safety,
                    contentType = dropContentType
                )
                status = when {
                    visionMessage == null -> baseStatusMessage
                    baseStatusMessage.isNullOrBlank() -> visionMessage
                    else -> listOf(baseStatusMessage, visionMessage).joinToString(separator = "\n")
                }
                if (safety.isNsfw) {
                    snackbar.showMessage(scope, "Drop saved with an 18+ warning.")
                }
            } catch (e: Exception) {
                when (e) {
                    is DropBlockedBySafetyException -> {
                        isSubmitting = false
                        val reason = e.assessment.reasons.firstOrNull()
                        val message = buildString {
                            append("This drop looks like adult content. ")
                            append("Enable 18+ drops from your account menu to share it.")
                            if (!reason.isNullOrBlank()) {
                                append('\n')
                                append(reason)
                            }
                        }
                        snackbar.showMessage(scope, message)
                        return@launch
                    }
                }
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
                    val drops = repo.getVisibleDropsForUser(
                        uid,
                        joinedGroups.toSet(),
                        allowNsfw = userProfile?.canViewNsfw() == true
                    )
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


    val canViewNsfw = userProfile?.canViewNsfw() == true
    val visibleCollectedNotes = if (canViewNsfw) {
        collectedNotes
    } else {
        collectedNotes.filterNot { note -> note.isNsfw || note.nsfwLabels.isNotEmpty() }
    }
    val hiddenNsfwCollectedCount = collectedNotes.size - visibleCollectedNotes.size
    val collectedCount = visibleCollectedNotes.size
    val isBusinessUser = userProfile?.isBusiness() == true
    val currentHomeDestination = runCatching { HomeDestination.valueOf(selectedHomeDestination) }
        .getOrDefault(HomeDestination.Explorer)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("GeoDrop") },
                    actions = {
                        Box {
                            IconButton(onClick = { showAccountMenu = !showAccountMenu }) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = "Account options"
                                )
                            }

                            DropdownMenu(
                                expanded = showAccountMenu,
                                onDismissRequest = { showAccountMenu = false }
                            ) {
                                val nsfwEnabled = userProfile?.canViewNsfw() == true
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (nsfwEnabled) "Disable NSFW drops" else "Enable NSFW drops"
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.Flag, contentDescription = null) },
                                    onClick = {
                                        showAccountMenu = false
                                        nsfwUpdateError = null
                                        showNsfwDialog = true
                                    }
                                )
                                if (isBusinessUser) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(if (signingOut) "Signing out…" else "Sign out")
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.Logout, contentDescription = null)
                                        },
                                        enabled = !signingOut,
                                        onClick = { handleSignOut() }
                                    )
                                }
                            }
                        }
                    }
                )
                if (isBusinessUser) {
                    Divider()
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp, bottom = 12.dp)
                    ) {
                        val options = listOf(HomeDestination.Explorer, HomeDestination.Business)
                        options.forEachIndexed { index, option ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                                selected = currentHomeDestination == option,
                                onClick = { selectedHomeDestination = option.name },
                                label = {
                                    Text(
                                        when (option) {
                                            HomeDestination.Explorer -> "Explorer"
                                            HomeDestination.Business -> "Business"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
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
        if (isBusinessUser && currentHomeDestination == HomeDestination.Business) {
            BusinessHomeScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                businessName = userProfile?.businessName,
                joinedGroups = joinedGroups,
                statusMessage = status,
                onCreateDrop = {
                    if (!isSubmitting) {
                        showDropComposer = true
                    }
                },
                onViewDashboard = {
                    if (!userProfileLoading) {
                        showBusinessDashboard = true
                    }
                },
                onUpdateBusinessProfile = { showBusinessOnboarding = true },
                onViewMyDrops = { showMyDrops = true },
                onManageGroups = { showManageGroups = true },
                onSignOut = { handleSignOut() }
            )
        } else {
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
                            description = when {
                                collectedCount == 0 && hiddenNsfwCollectedCount > 0 -> {
                                    val plural = if (hiddenNsfwCollectedCount == 1) "drop" else "drops"
                                    "Your NSFW settings are hiding $hiddenNsfwCollectedCount collected $plural."
                                }
                                collectedCount == 0 -> {
                                    "Open the drops you've saved for later."
                                }
                                hiddenNsfwCollectedCount > 0 -> {
                                    val plural = if (collectedCount == 1) "drop" else "drops"
                                    "Open the $collectedCount $plural you've saved. ($hiddenNsfwCollectedCount hidden by NSFW settings.)"
                                }
                                else -> {
                                    "Open the $collectedCount drop" + if (collectedCount == 1) " you've saved." else "s you've saved."
                                }
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

                if (!isBusinessUser) {
                    item {
                        ActionCard(
                            icon = Icons.Rounded.Storefront,
                            title = "Business tools",
                            description = "Sign in or create a business account to share offers and guided tours.",
                            onClick = {
                                if (FirebaseAuth.getInstance().currentUser == null) {
                                    snackbar.showMessage(scope, "Signing you in… please try again shortly.")
                                } else {
                                    openBusinessAuthDialog(BusinessAuthMode.SIGN_IN)
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
    }

    if (showDropComposer) {
        DropComposerDialog(
            isSubmitting = isSubmitting,
            isBusinessUser = userProfile?.isBusiness() == true,
            businessName = userProfile?.businessName,
            userProfileLoading = userProfileLoading,
            userProfileError = userProfileError,
            dropType = dropType,
            onDropTypeChange = { dropType = it },
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
            capturedVideoUri = capturedVideoUri,
            onRecordVideo = { ensureVideoAndLaunch() },
            onClearVideo = { clearVideo() },
            dropVisibility = dropVisibility,
            onDropVisibilityChange = { dropVisibility = it },
            groupCodeInput = groupCodeInput,
            onGroupCodeInputChange = { groupCodeInput = it },
            joinedGroups = joinedGroups,
            onSelectGroupCode = { code -> groupCodeInput = TextFieldValue(code) },
            redemptionCodeInput = redemptionCodeInput,
            onRedemptionCodeChange = { redemptionCodeInput = it },
            redemptionLimitInput = redemptionLimitInput,
            onRedemptionLimitChange = { redemptionLimitInput = it },
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
            onSelect = { drop ->
                otherDropsSelectedId = if (otherDropsSelectedId == drop.id) {
                    null
                } else {
                    drop.id
                }
            },
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
            onSelect = { drop ->
                myDropsSelectedId = if (myDropsSelectedId == drop.id) {
                    null
                } else {
                    drop.id
                }
            },
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
                    putExtra("dropType", drop.dropType.name)
                    drop.businessName?.let { putExtra("dropBusinessName", it) }
                    drop.businessId?.let { putExtra("dropBusinessId", it) }
                    drop.redemptionLimit?.let { putExtra("dropRedemptionLimit", it) }
                    putExtra("dropRedemptionCount", drop.redemptionCount)
                    putExtra("dropIsNsfw", drop.isNsfw)
                    if (drop.nsfwLabels.isNotEmpty()) {
                        putStringArrayListExtra("dropNsfwLabels", ArrayList(drop.nsfwLabels))
                    }
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
            notes = visibleCollectedNotes,
            hiddenNsfwCount = hiddenNsfwCollectedCount,
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
                    putExtra("dropType", note.dropType.name)
                    note.businessName?.let { putExtra("dropBusinessName", it) }
                    note.businessId?.let { putExtra("dropBusinessId", it) }
                    note.redemptionLimit?.let { putExtra("dropRedemptionLimit", it) }
                    putExtra("dropRedemptionCount", note.redemptionCount)
                    putExtra("dropCollectedAt", note.collectedAt)
                    putExtra("dropIsRedeemed", note.isRedeemed)
                    note.redeemedAt?.let { putExtra("dropRedeemedAt", it) }
                    putExtra("dropIsNsfw", note.isNsfw)
                    if (note.nsfwLabels.isNotEmpty()) {
                        putStringArrayListExtra("dropNsfwLabels", ArrayList(note.nsfwLabels))
                    }
                }
                ctx.startActivity(intent)
            },
            onRemove = { note ->
                noteInventory.removeCollected(note.id)
                collectedNotes = noteInventory.getCollectedNotes()
            }
        )
    }

    if (showNsfwDialog) {
        NsfwPreferenceDialog(
            enabled = userProfile?.canViewNsfw() == true,
            isProcessing = nsfwUpdating,
            error = nsfwUpdateError,
            onConfirm = { enable ->
                val userId = currentUserId
                if (userId.isNullOrBlank()) {
                    nsfwUpdateError = "Sign in again to update content settings."
                    return@NsfwPreferenceDialog
                }
                nsfwUpdating = true
                nsfwUpdateError = null
                scope.launch {
                    try {
                        val updated = repo.updateNsfwPreference(userId, enable)
                        userProfile = updated
                        showNsfwDialog = false
                        val message = if (enable) {
                            "NSFW drops enabled."
                        } else {
                            "NSFW drops disabled."
                        }
                        snackbar.showMessage(scope, message)
                        registrar.registerNearby(
                            ctx,
                            maxMeters = 300.0,
                            groupCodes = groupPrefs.getJoinedGroups().toSet()
                        )
                    } catch (error: Exception) {
                        nsfwUpdateError = error.localizedMessage ?: "Couldn't update settings."
                    } finally {
                        nsfwUpdating = false
                    }
                }
            },
            onDismiss = { showNsfwDialog = false }
        )
    }

    if (showBusinessSignIn) {
        BusinessSignInDialog(
            mode = businessAuthMode,
            onModeChange = { mode ->
                if (businessAuthSubmitting || businessGoogleSigningIn) return@BusinessSignInDialog
                businessAuthMode = mode
                businessAuthError = null
                businessAuthStatus = null
            },
            email = businessEmail,
            onEmailChange = { businessEmail = it },
            password = businessPassword,
            onPasswordChange = { businessPassword = it },
            confirmPassword = businessConfirmPassword,
            onConfirmPasswordChange = { businessConfirmPassword = it },
            isSubmitting = businessAuthSubmitting,
            isGoogleSigningIn = businessGoogleSigningIn,
            error = businessAuthError,
            status = businessAuthStatus,
            onSubmit = { performBusinessAuth() },
            onDismiss = { dismissBusinessAuthDialog() },
            onForgotPassword = { sendBusinessPasswordReset() },
            onGoogleSignIn = { startBusinessGoogleSignIn() }
        )
    }

    if (showBusinessOnboarding) {
        var businessNameField by rememberSaveable(
            userProfile?.businessName,
            stateSaver = TextFieldValue.Saver
        ) {
            mutableStateOf(TextFieldValue(userProfile?.businessName.orEmpty()))
        }
        var onboardingError by remember { mutableStateOf<String?>(null) }
        var onboardingSubmitting by remember { mutableStateOf(false) }

        BusinessOnboardingDialog(
            name = businessNameField,
            onNameChange = { businessNameField = it },
            isSubmitting = onboardingSubmitting,
            error = onboardingError,
            onSubmit = {
                val trimmed = businessNameField.text.trim()
                if (trimmed.isEmpty()) {
                    onboardingError = "Enter your business name."
                    return@BusinessOnboardingDialog
                }
                val uid = currentUserId
                if (uid.isNullOrBlank()) {
                    onboardingError = "Sign-in is required."
                    return@BusinessOnboardingDialog
                }
                onboardingSubmitting = true
                onboardingError = null
                scope.launch {
                    try {
                        val updated = repo.updateBusinessProfile(uid, trimmed)
                        userProfile = updated
                        showBusinessOnboarding = false
                        snackbar.showMessage(scope, "Business profile saved.")
                    } catch (error: Exception) {
                        onboardingError = error.localizedMessage ?: "Couldn't save business info."
                    } finally {
                        onboardingSubmitting = false
                    }
                }
            },
            onDismiss = {
                if (!onboardingSubmitting) {
                    showBusinessOnboarding = false
                }
            }
        )
    }

    if (showBusinessDashboard) {
        BusinessDashboardDialog(
            drops = businessDrops,
            loading = businessDashboardLoading,
            error = businessDashboardError,
            onDismiss = { showBusinessDashboard = false },
            onRefresh = { businessDashboardRefreshToken += 1 }
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
private fun ExplorerAutoSignInScreen(
    isSigningIn: Boolean,
    error: String?,
    onRetry: () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Getting things ready",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = "We're signing you in as an explorer so you can start dropping right away.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = onRetry,
                enabled = !isSigningIn,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Signing you in…")
                } else {
                    Text("Try again")
                }
            }
        }
    }
}

@Composable
private fun BusinessHomeScreen(
    modifier: Modifier = Modifier,
    businessName: String?,
    joinedGroups: List<String>,
    statusMessage: String?,
    onCreateDrop: () -> Unit,
    onViewDashboard: () -> Unit,
    onUpdateBusinessProfile: () -> Unit,
    onViewMyDrops: () -> Unit,
    onManageGroups: () -> Unit,
    onSignOut: () -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            BusinessHeroCard(
                businessName = businessName,
                joinedGroups = joinedGroups,
                onCreateDrop = onCreateDrop,
                onManageGroups = onManageGroups
            )
        }

        item { SectionHeader(text = "Manage drops") }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(
                    icon = Icons.Rounded.Place,
                    title = "Create a drop",
                    description = "Publish a new coupon, tour stop, or update for nearby explorers.",
                    onClick = onCreateDrop
                )

                ActionCard(
                    icon = Icons.Rounded.Inbox,
                    title = "Manage existing drops",
                    description = "Review performance and make changes to the drops you've shared.",
                    onClick = onViewMyDrops
                )

                ActionCard(
                    icon = Icons.Rounded.Groups,
                    title = "Manage group codes",
                    description = "Control who can access private campaigns or team-only drops.",
                    onClick = onManageGroups
                )
            }
        }

        item { SectionHeader(text = "Insights & branding") }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(
                    icon = Icons.Rounded.Storefront,
                    title = "Business dashboard",
                    description = "Track discoveries, redemptions, and engagement in one place.",
                    onClick = onViewDashboard
                )

                ActionCard(
                    icon = Icons.Rounded.Edit,
                    title = "Update business name",
                    description = "Adjust how your brand appears across GeoDrop experiences.",
                    onClick = onUpdateBusinessProfile
                )
            }
        }

        item { SectionHeader(text = "Account") }

        item {
            ActionCard(
                icon = Icons.Rounded.Logout,
                title = "Sign out",
                description = "Return to the explorer experience on this device.",
                onClick = onSignOut
            )
        }

        statusMessage?.let { message ->
            item { StatusCard(message = message) }
        }
    }
}

@Composable
private fun BusinessHeroCard(
    businessName: String?,
    joinedGroups: List<String>,
    onCreateDrop: () -> Unit,
    onManageGroups: () -> Unit,
) {
    val title = businessName?.takeIf { it.isNotBlank() }?.let { "$it on GeoDrop" }
        ?: "Welcome to GeoDrop Business"
    val subtitle = if (businessName.isNullOrBlank()) {
        "Share exclusive offers, stories, and tours to reach explorers right when they're nearby."
    } else {
        "Keep explorers engaged with timely offers and experiences from your team."
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )
            }

            Button(
                onClick = onCreateDrop,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(Icons.Rounded.Place, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create a new drop")
            }

            TextButton(
                onClick = onManageGroups,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Rounded.Groups, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Manage group codes")
            }
        }
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
    isBusinessUser: Boolean,
    businessName: String?,
    userProfileLoading: Boolean,
    userProfileError: String?,
    dropType: DropType,
    onDropTypeChange: (DropType) -> Unit,
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
    capturedVideoUri: String?,
    onRecordVideo: () -> Unit,
    onClearVideo: () -> Unit,
    dropVisibility: DropVisibility,
    onDropVisibilityChange: (DropVisibility) -> Unit,
    groupCodeInput: TextFieldValue,
    onGroupCodeInputChange: (TextFieldValue) -> Unit,
    joinedGroups: List<String>,
    onSelectGroupCode: (String) -> Unit,
    redemptionCodeInput: TextFieldValue,
    onRedemptionCodeChange: (TextFieldValue) -> Unit,
    redemptionLimitInput: TextFieldValue,
    onRedemptionLimitChange: (TextFieldValue) -> Unit,
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

                if (userProfileLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                userProfileError?.let { errorMessage ->
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (isBusinessUser) {
                    BusinessDropTypeSection(
                        dropType = dropType,
                        onDropTypeChange = onDropTypeChange,
                        businessName = businessName
                    )
                }

                DropContentTypeSection(
                    selected = dropContentType,
                    onSelect = onDropContentTypeChange
                )

                if (isBusinessUser && dropType == DropType.RESTAURANT_COUPON) {
                    BusinessRedemptionSection(
                        redemptionCode = redemptionCodeInput,
                        onRedemptionCodeChange = onRedemptionCodeChange,
                        redemptionLimit = redemptionLimitInput,
                        onRedemptionLimitChange = onRedemptionLimitChange
                    )
                }

                val noteLabel = when (dropContentType) {
                    DropContentType.TEXT -> "Your note"
                    DropContentType.PHOTO, DropContentType.AUDIO, DropContentType.VIDEO -> "Caption (optional)"
                }
                val noteSupporting = when (dropContentType) {
                    DropContentType.TEXT -> null
                    DropContentType.PHOTO -> "Add a short caption to go with your photo."
                    DropContentType.AUDIO -> "Add a short caption to go with your audio clip."
                    DropContentType.VIDEO -> "Add a short caption to go with your video clip."
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

                    DropContentType.VIDEO -> {
                        val hasVideo = capturedVideoUri != null
                        val videoPreview: (@Composable () -> Unit)? = if (hasVideo) {
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
                                                text = "Video attached",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Ready to drop your clip.",
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
                            title = "Record video",
                            description = "Capture a short clip to share at this location.",
                            status = if (hasVideo) "Video ready to upload." else "No video recorded yet.",
                            isReady = hasVideo,
                            primaryLabel = if (hasVideo) "Record again" else "Record video",
                            primaryIcon = Icons.Rounded.Videocam,
                            onPrimary = {
                                if (hasVideo) {
                                    onClearVideo()
                                }
                                onRecordVideo()
                            },
                            secondaryLabel = if (hasVideo) "Remove video" else null,
                            onSecondary = if (hasVideo) {
                                { onClearVideo() }
                            } else {
                                null
                            },
                            previewContent = videoPreview
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

@Composable
private fun NsfwPreferenceDialog(
    enabled: Boolean,
    isProcessing: Boolean,
    error: String?,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var confirmChecked by remember(enabled) { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val description = if (enabled) {
        "Turn off access to adult content in GeoDrop. You'll stop seeing NSFW drops and won't be able to create them."
    } else {
        "Enable access to adult (18+) drops. This allows you to view and share NSFW content when you are legally permitted to do so."
    }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        icon = { Icon(Icons.Rounded.Flag, contentDescription = null) },
        title = { Text("18+ content") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(description)
                if (!enabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = confirmChecked,
                            onCheckedChange = { checked ->
                                confirmChecked = checked
                                if (checked) localError = null
                            },
                            enabled = !isProcessing
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("I confirm that I am at least 18 years old.")
                    }
                }
                val message = error ?: localError
                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!enabled && !confirmChecked) {
                        localError = "Confirm that you are 18 or older."
                    } else if (!isProcessing) {
                        localError = null
                        onConfirm(!enabled)
                    }
                },
                enabled = !isProcessing && (enabled || confirmChecked)
            ) {
                Text(if (enabled) "Disable" else "Enable")
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isProcessing) onDismiss() }, enabled = !isProcessing) {
                Text("Cancel")
            }
        }
    )
}

private fun visionStatusMessage(
    assessment: DropSafetyAssessment,
    contentType: DropContentType
): String? {
    return when (assessment.visionStatus) {
        VisionApiStatus.NOT_CONFIGURED ->
            "Google Vision SafeSearch isn't configured, so this drop wasn't scanned."

        VisionApiStatus.NOT_ELIGIBLE -> when (contentType) {
            DropContentType.PHOTO ->
                "Google Vision SafeSearch skipped this photo because it couldn't be processed."

            else ->
                "Google Vision SafeSearch only scans photo drops, so this one was skipped."
        }

        VisionApiStatus.ERROR ->
            "Google Vision SafeSearch couldn't be reached, so the drop was saved without a scan."

        VisionApiStatus.CLEARED ->
            "Google Vision SafeSearch scanned the drop and cleared it."

        VisionApiStatus.FLAGGED -> {
            val reason = assessment.reasons.firstOrNull()?.takeIf { it.isNotBlank() }
            if (reason != null) {
                "Google Vision SafeSearch flagged this drop: $reason"
            } else {
                "Google Vision SafeSearch flagged this drop as adult content."
            }
        }
    }
}

private fun formatCoordinate(value: Double): String {
    return String.format(Locale.US, "%.5f", value)
}


@Composable
private fun CollectedDropsDialog(
    notes: List<CollectedNote>,
    hiddenNsfwCount: Int,
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
                    val message = if (hiddenNsfwCount > 0) {
                        val plural = if (hiddenNsfwCount == 1) "drop" else "drops"
                        "Your NSFW settings are hiding $hiddenNsfwCount collected $plural."
                    } else {
                        "You haven't collected any drops yet."
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        DialogMessageContent(
                            message = message,
                            primaryLabel = null,
                            onPrimary = null,
                            onDismiss = onDismiss
                        )
                    }
                } else {
                    var highlightedId by rememberSaveable { mutableStateOf<String?>(null) }

                    LaunchedEffect(notes) {
                        highlightedId = highlightedId?.takeIf { id -> notes.any { it.id == id } }
                    }

                    val highlightedNote = notes.firstOrNull { it.id == highlightedId }

                    val listState = rememberLazyListState()

                    LaunchedEffect(highlightedId, notes) {
                        val targetId = highlightedId ?: return@LaunchedEffect
                        val index = notes.indexOfFirst { it.id == targetId }
                        if (index >= 0) {
                            listState.animateScrollToItem(index)
                        }
                    }
                    val minMapWeight = MAP_LIST_MIN_WEIGHT
                    val maxMapWeight = MAP_LIST_MAX_WEIGHT
                    var containerHeight by remember { mutableStateOf(0) }
                    var mapWeight by rememberSaveable {
                        mutableStateOf(DEFAULT_MAP_WEIGHT.coerceIn(minMapWeight, maxMapWeight))
                    }

                    val listWeight = 1f - mapWeight
                    val dividerDragState = rememberDraggableState { delta ->
                        val height = containerHeight.takeIf { it > 0 }?.toFloat()
                            ?: return@rememberDraggableState
                        val deltaWeight = delta / height
                        val updated = (mapWeight + deltaWeight).coerceIn(minMapWeight, maxMapWeight)
                        if (updated != mapWeight) {
                            mapWeight = updated
                        }
                    }
                    val dividerInteraction = remember { MutableInteractionSource() }
                    val dividerModifier = Modifier
                        .fillMaxWidth()
                        .height(DIVIDER_DRAG_HANDLE_HEIGHT)
                        .draggable(
                            state = dividerDragState,
                            orientation = Orientation.Vertical,
                            interactionSource = dividerInteraction
                        )
                        .semantics(mergeDescendants = true) {
                            progressBarRangeInfo = ProgressBarRangeInfo(
                                current = mapWeight,
                                range = minMapWeight..maxMapWeight
                            )
                            stateDescription =
                                "Map occupies ${'$'}{(mapWeight * 100).roundToInt()} percent of the available height"
                            setProgress { target ->
                                val coerced = target.coerceIn(minMapWeight, maxMapWeight)
                                if (coerced != mapWeight) {
                                    mapWeight = coerced
                                }
                                true
                            }
                        }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .onSizeChanged { containerHeight = it.height }
                    ) {
                        if (hiddenNsfwCount > 0) {
                            val plural = if (hiddenNsfwCount == 1) "drop" else "drops"
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 2.dp,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Your NSFW settings are hiding $hiddenNsfwCount collected $plural.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(mapWeight)
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

                        Box(modifier = dividerModifier) {
                            Divider(modifier = Modifier.align(Alignment.Center))
                            DividerDragHandleHint(
                                modifier = Modifier.align(Alignment.Center),
                                text = stringResource(R.string.drag_to_resize)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(listWeight)
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
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(notes, key = { it.id }) { note ->
                                    val isHighlighted = note.id == highlightedId
                                    CollectedNoteCard(
                                        note = note,
                                        selected = isHighlighted,
                                        expanded = isHighlighted,
                                        onSelect = {
                                            highlightedId = if (isHighlighted) null else note.id
                                        },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BusinessSignInDialog(
    mode: BusinessAuthMode,
    onModeChange: (BusinessAuthMode) -> Unit,
    email: TextFieldValue,
    onEmailChange: (TextFieldValue) -> Unit,
    password: TextFieldValue,
    onPasswordChange: (TextFieldValue) -> Unit,
    confirmPassword: TextFieldValue,
    onConfirmPasswordChange: (TextFieldValue) -> Unit,
    isSubmitting: Boolean,
    isGoogleSigningIn: Boolean,
    error: String?,
    status: String?,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    val isBusy = isSubmitting || isGoogleSigningIn
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = !isBusy,
            dismissOnClickOutside = !isBusy
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Business account",
                    style = MaterialTheme.typography.titleLarge
                )

                SingleChoiceSegmentedButtonRow {
                    BusinessAuthMode.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = BusinessAuthMode.entries.size),
                            selected = mode == option,
                            onClick = { onModeChange(option) }
                        ) {
                            Text(
                                text = when (option) {
                                    BusinessAuthMode.SIGN_IN -> "Sign in"
                                    BusinessAuthMode.REGISTER -> "Create account"
                                }
                            )
                        }
                    }
                }

                val isRegister = mode == BusinessAuthMode.REGISTER

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email address") },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isRegister) ImeAction.Next else ImeAction.Done
                    )
                )

                if (isRegister) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = { Text("Confirm password") },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )
                }

                error?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                status?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (!isRegister) {
                    TextButton(
                        onClick = onForgotPassword,
                        enabled = !isBusy,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot password?")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onSubmit,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Working…")
                        } else {
                            Text(
                                text = when (mode) {
                                    BusinessAuthMode.SIGN_IN -> "Sign in"
                                    BusinessAuthMode.REGISTER -> "Create account"
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Divider()
                Text(
                    text = "Or continue with",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                OutlinedButton(
                    onClick = onGoogleSignIn,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isGoogleSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Connecting to Google…")
                    } else {
                        Text("Sign in with Google")
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessOnboardingDialog(
    name: TextFieldValue,
    onNameChange: (TextFieldValue) -> Unit,
    isSubmitting: Boolean,
    error: String?,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (!isSubmitting) onDismiss()
        }
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Business profile",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Add a display name so explorers know which business dropped this content.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Business or brand name") },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onSubmit,
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Saving…")
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessDashboardDialog(
    drops: List<Drop>,
    loading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Business dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close dashboard")
                    }
                }

                Button(
                    onClick = onRefresh,
                    enabled = !loading,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Refreshing…")
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Refresh")
                    }
                }

                when {
                    loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    error != null -> {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    drops.isEmpty() -> {
                        Text(
                            text = "You haven't shared any business drops yet. Create one to see analytics here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    else -> {
                        val sorted = drops.sortedByDescending { it.createdAt }
                        val totalRedemptions = sorted.sumOf { it.redemptionCount }
                        val uniqueRedeemers = sorted.flatMap { it.redeemedBy.keys }.toSet().size
                        val activeOffers = sorted.count { it.dropType == DropType.RESTAURANT_COUPON }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DashboardMetricCard(
                                value = sorted.size.toString(),
                                label = "Live drops",
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCard(
                                value = totalRedemptions.toString(),
                                label = "Total redemptions",
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCard(
                                value = uniqueRedeemers.toString(),
                                label = "Unique redeemers",
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCard(
                                value = activeOffers.toString(),
                                label = "Active offers",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(sorted, key = { it.id }) { drop ->
                                BusinessDropAnalyticsCard(drop = drop)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardMetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        tonalElevation = 3.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BusinessDropAnalyticsCard(drop: Drop) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = drop.displayTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                if (drop.isNsfw) {
                    DropNsfwBadge()
                }
            }

            Text(
                text = when (drop.dropType) {
                    DropType.RESTAURANT_COUPON -> "Offer · ${drop.businessName ?: "Your business"}"
                    DropType.TOUR_STOP -> "Tour stop"
                    DropType.COMMUNITY -> "Community drop"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val redemptionStatus = if (drop.dropType == DropType.RESTAURANT_COUPON) {
                val remaining = drop.remainingRedemptions()
                buildString {
                    append("Redemptions: ${drop.redemptionCount}")
                    drop.redemptionLimit?.let { limit ->
                        append(" / $limit")
                        remaining?.let { append(" · $it left") }
                    }
                }
            } else {
                "Redemptions: n/a"
            }

            Text(
                text = redemptionStatus,
                style = MaterialTheme.typography.bodyMedium
            )

            val voteSummary = "Votes: ${drop.voteScore()} (↑${drop.upvoteCount} / ↓${drop.downvoteCount})"
            Text(
                text = voteSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                            val listState = rememberLazyListState()


                            LaunchedEffect(selectedId, drops) {
                                val targetId = selectedId ?: return@LaunchedEffect
                                val index = drops.indexOfFirst { it.id == targetId }
                                if (index >= 0) {
                                    listState.animateScrollToItem(index)
                                }
                            }
                            val minMapWeight = MAP_LIST_MIN_WEIGHT
                            val maxMapWeight = MAP_LIST_MAX_WEIGHT
                            var containerHeight by remember { mutableStateOf(0) }
                            var mapWeight by rememberSaveable {
                                mutableStateOf(DEFAULT_MAP_WEIGHT.coerceIn(minMapWeight, maxMapWeight))
                            }

                            val listWeight = 1f - mapWeight
                            val dividerDragState = rememberDraggableState { delta ->
                                val height = containerHeight.takeIf { it > 0 }?.toFloat()
                                    ?: return@rememberDraggableState
                                val deltaWeight = delta / height
                                val updated = (mapWeight + deltaWeight).coerceIn(minMapWeight, maxMapWeight)
                                if (updated != mapWeight) {
                                    mapWeight = updated
                                }
                            }
                            val dividerInteraction = remember { MutableInteractionSource() }
                            val dividerModifier = Modifier
                                .fillMaxWidth()
                                .height(DIVIDER_DRAG_HANDLE_HEIGHT)
                                .draggable(
                                    state = dividerDragState,
                                    orientation = Orientation.Vertical,
                                    interactionSource = dividerInteraction
                                )
                                .semantics(mergeDescendants = true) {
                                    progressBarRangeInfo = ProgressBarRangeInfo(
                                        current = mapWeight,
                                        range = minMapWeight..maxMapWeight
                                    )
                                    stateDescription =
                                        "Map occupies ${'$'}{(mapWeight * 100).roundToInt()} percent of the available height"
                                    setProgress { target ->
                                        val coerced = target.coerceIn(minMapWeight, maxMapWeight)
                                        if (coerced != mapWeight) {
                                            mapWeight = coerced
                                        }
                                        true
                                    }
                                }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onSizeChanged { containerHeight = it.height }
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
                                        .weight(mapWeight)
                                ) {
                                    OtherDropsMap(
                                        drops = drops,
                                        selectedDropId = selectedId,
                                        currentLocation = currentLocation
                                    )
                                }

                                Box(modifier = dividerModifier) {
                                    Divider(modifier = Modifier.align(Alignment.Center))
                                    DividerDragHandleHint(
                                        modifier = Modifier.align(Alignment.Center),
                                        text = stringResource(R.string.drag_to_resize)
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(listWeight)
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
                                        state = listState,
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
                    DropContentType.VIDEO -> "Video drop"
                }
                val snippetParts = mutableListOf<String>()
                snippetParts.add("Type: $typeLabel")
                note.dropCreatedAt?.let { created ->
                    formatTimestamp(created)?.let { snippetParts.add("Dropped $it") }
                }
                snippetParts.add("Lat: ${formatCoordinate(lat)}, Lng: ${formatCoordinate(lng)}")
                note.groupCode?.let { snippetParts.add("Group $it") }
                if (note.isNsfw) {
                    snippetParts.add("Marked as adult content")
                }

                val title = note.text.ifBlank {
                    when (note.contentType) {
                        DropContentType.TEXT -> "Collected text drop"
                        DropContentType.PHOTO -> "Collected photo drop"
                        DropContentType.AUDIO -> "Collected audio drop"
                        DropContentType.VIDEO -> "Collected video drop"
                    }
                }

                val markerIcon = when {
                    note.isNsfw -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
                    note.id == highlightedId -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                    else -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                }

                Marker(
                    state = MarkerState(position),
                    title = title,
                    snippet = snippetParts.joinToString("\n"),
                    icon = markerIcon,
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
    expanded: Boolean,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val typeLabel = when (note.contentType) {
                DropContentType.TEXT -> "Text note"
                DropContentType.PHOTO -> "Photo drop"
                DropContentType.AUDIO -> "Audio drop"
                DropContentType.VIDEO -> "Video drop"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                if (note.isNsfw) {
                    DropNsfwBadge()
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) {
                        "Collapse drop details"
                    } else {
                        "Expand drop details"
                    }
                )
            }

            val preview = note.text.ifBlank {
                when (note.contentType) {
                    DropContentType.TEXT -> "(No message)"
                    DropContentType.PHOTO -> "Photo drop"
                    DropContentType.AUDIO -> "Audio drop"
                    DropContentType.VIDEO -> "Video drop"
                }
            }
            Text(
                text = preview,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            val listState = rememberLazyListState()

                            LaunchedEffect(selectedId, drops) {
                                val targetId = selectedId ?: return@LaunchedEffect
                                val index = drops.indexOfFirst { it.id == targetId }
                                if (index >= 0) {
                                    listState.animateScrollToItem(index)
                                }
                            }
                            val minMapWeight = MAP_LIST_MIN_WEIGHT
                            val maxMapWeight = MAP_LIST_MAX_WEIGHT
                            var containerHeight by remember { mutableStateOf(0) }
                            var mapWeight by rememberSaveable {
                                mutableStateOf(DEFAULT_MAP_WEIGHT.coerceIn(minMapWeight, maxMapWeight))
                            }

                            val listWeight = 1f - mapWeight
                            val dividerDragState = rememberDraggableState { delta ->
                                val height = containerHeight.takeIf { it > 0 }?.toFloat()
                                    ?: return@rememberDraggableState
                                val deltaWeight = delta / height
                                val updated = (mapWeight + deltaWeight).coerceIn(minMapWeight, maxMapWeight)
                                if (updated != mapWeight) {
                                    mapWeight = updated
                                }
                            }
                            val dividerInteraction = remember { MutableInteractionSource() }
                            val dividerModifier = Modifier
                                .fillMaxWidth()
                                .height(DIVIDER_DRAG_HANDLE_HEIGHT)
                                .draggable(
                                    state = dividerDragState,
                                    orientation = Orientation.Vertical,
                                    interactionSource = dividerInteraction
                                )
                                .semantics(mergeDescendants = true) {
                                    progressBarRangeInfo = ProgressBarRangeInfo(
                                        current = mapWeight,
                                        range = minMapWeight..maxMapWeight
                                    )
                                    stateDescription =
                                        "Map occupies ${'$'}{(mapWeight * 100).roundToInt()} percent of the available height"
                                    setProgress { target ->
                                        val coerced = target.coerceIn(minMapWeight, maxMapWeight)
                                        if (coerced != mapWeight) {
                                            mapWeight = coerced
                                        }
                                        true
                                    }
                                }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onSizeChanged { containerHeight = it.height }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(mapWeight)
                                ) {
                                    MyDropsMap(
                                        drops = drops,
                                        selectedDropId = selectedId,
                                        currentLocation = currentLocation
                                    )
                                }

                                Box(modifier = dividerModifier) {
                                    Divider(modifier = Modifier.align(Alignment.Center))
                                    DividerDragHandleHint(
                                        modifier = Modifier.align(Alignment.Center),
                                        text = stringResource(R.string.drag_to_resize)
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(listWeight)
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
                                        state = listState,
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
private fun DividerDragHandleHint(
    modifier: Modifier = Modifier,
    text: String
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        tonalElevation = 2.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
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
                DropContentType.VIDEO -> "Video drop"
            }
            snippetParts.add("Type: $typeLabel")
            formatTimestamp(drop.createdAt)?.let { snippetParts.add("Dropped $it") }
            drop.groupCode?.takeIf { !it.isNullOrBlank() }?.let { snippetParts.add("Group $it") }
            snippetParts.add("Lat: %.5f, Lng: %.5f".format(drop.lat, drop.lng))
            snippetParts.add("Score: ${formatVoteScore(drop.voteScore())} (↑${drop.upvoteCount} / ↓${drop.downvoteCount})")
            if (drop.isNsfw) {
                snippetParts.add("Marked as adult content")
            }

            val isSelected = drop.id == selectedDropId
            val markerIcon = if (drop.isNsfw) {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
            } else {
                BitmapDescriptorFactory.defaultMarker(voteHueFor(drop.upvoteCount))
            }

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
private fun DropNsfwBadge(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.errorContainer,
    contentColor: Color = MaterialTheme.colorScheme.onErrorContainer
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Block,
                contentDescription = "Adult content",
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "18+",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = drop.discoveryTitle(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                if (drop.isNsfw) {
                    DropNsfwBadge()
                }

                Icon(
                    imageVector = if (isSelected) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isSelected) {
                        "Collapse drop details"
                    } else {
                        "Expand drop details"
                    }
                )
            }

            val description = drop.discoveryDescription()
            if (description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = supportingColor,
                    maxLines = if (isSelected) Int.MAX_VALUE else 2
                )
            }

            AnimatedVisibility(visible = isSelected) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    val typeLabel = when (drop.contentType) {
                        DropContentType.TEXT -> "Text note"
                        DropContentType.PHOTO -> "Photo drop"
                        DropContentType.AUDIO -> "Audio drop"
                        DropContentType.VIDEO -> "Video drop"
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
            if (drop.isNsfw) {
                snippetParts.add("Marked as adult content")
            }

            val isSelected = drop.id == selectedDropId

            val markerIcon = if (drop.isNsfw) {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
            } else {
                BitmapDescriptorFactory.defaultMarker(voteHueFor(drop.upvoteCount))
            }

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = drop.displayTitle(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                if (drop.isNsfw) {
                    DropNsfwBadge()
                }

                Icon(
                    imageVector = if (isSelected) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isSelected) {
                        "Collapse drop details"
                    } else {
                        "Expand drop details"
                    }
                )
            }

        AnimatedVisibility(visible = isSelected) {
            Column {
                Spacer(Modifier.height(4.dp))

                val typeLabel = when (drop.contentType) {
                    DropContentType.TEXT -> "Text note"
                    DropContentType.PHOTO -> "Photo drop"
                    DropContentType.AUDIO -> "Audio drop"
                    DropContentType.VIDEO -> "Video drop"
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

                val visibilityLabel = drop.groupCode
                    ?.takeIf { it.isNotBlank() }
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
                type = DropContentType.VIDEO,
                title = "Video drop",
                description = "Record a short clip for nearby explorers to watch.",
                icon = Icons.Rounded.Videocam
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BusinessDropTypeSection(
    dropType: DropType,
    onDropTypeChange: (DropType) -> Unit,
    businessName: String?
) {
    val options = remember {
        listOf(
            BusinessDropTypeOption(
                type = DropType.COMMUNITY,
                title = "Community drop",
                description = "Share something fun or helpful for anyone nearby.",
                icon = Icons.Rounded.Public
            ),
            BusinessDropTypeOption(
                type = DropType.RESTAURANT_COUPON,
                title = "Business offer",
                description = "Reward visitors with a code they must show to redeem.",
                icon = Icons.Rounded.Storefront
            ),
            BusinessDropTypeOption(
                type = DropType.TOUR_STOP,
                title = "Tour stop",
                description = "Create guided stops that highlight key locations.",
                icon = Icons.Rounded.Flag
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val header = businessName?.takeIf { it.isNotBlank() }?.let { "Business tools for $it" }
            ?: "Business tools"
        Text(header, style = MaterialTheme.typography.titleSmall)

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    onClick = { onDropTypeChange(option.type) },
                    selected = option.type == dropType,
                    label = { Text(option.title) },
                    icon = { Icon(option.icon, contentDescription = null) }
                )
            }
        }

        Crossfade(targetState = dropType, label = "businessDropTypeDescription") { current ->
            val message = options.firstOrNull { it.type == current }?.description ?: ""
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BusinessRedemptionSection(
    redemptionCode: TextFieldValue,
    onRedemptionCodeChange: (TextFieldValue) -> Unit,
    redemptionLimit: TextFieldValue,
    onRedemptionLimitChange: (TextFieldValue) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Offer security", style = MaterialTheme.typography.titleSmall)

        OutlinedTextField(
            value = redemptionCode,
            onValueChange = onRedemptionCodeChange,
            label = { Text("Redemption code") },
            supportingText = {
                Text("Share this code in person so each guest redeems only once.")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = redemptionLimit,
            onValueChange = onRedemptionLimitChange,
            label = { Text("Optional redemption limit") },
            supportingText = {
                Text("Set a maximum number of redemptions (leave blank for unlimited).")
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private val DIVIDER_DRAG_HANDLE_HEIGHT = 24.dp
private const val MAP_LIST_MIN_WEIGHT = 0.2f
private const val MAP_LIST_MAX_WEIGHT = 0.8f
private const val DEFAULT_MAP_WEIGHT = 0.5f

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

private data class BusinessDropTypeOption(
    val type: DropType,
    val title: String,
    val description: String,
    val icon: ImageVector
)

private enum class HomeDestination { Explorer, Business }

private enum class BusinessAuthMode {
    SIGN_IN,
    REGISTER
}

private enum class DropVisibility { Public, GroupOnly }

/** Tiny helper to show snackbars from non-suspend places. */
private fun SnackbarHostState.showMessage(scope: kotlinx.coroutines.CoroutineScope, msg: String) {
    scope.launch { showSnackbar(msg) }
}
