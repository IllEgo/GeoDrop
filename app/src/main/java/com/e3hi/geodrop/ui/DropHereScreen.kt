package com.e3hi.geodrop.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.util.Patterns
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GroupAdd
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
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
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
import com.e3hi.geodrop.BuildConfig
import com.e3hi.geodrop.R
import com.e3hi.geodrop.data.BusinessCategory
import com.e3hi.geodrop.data.CollectedNote
import com.e3hi.geodrop.data.BusinessDropTemplate
import com.e3hi.geodrop.data.Drop
import com.e3hi.geodrop.data.DropContentType
import com.e3hi.geodrop.data.GroupMembership
import com.e3hi.geodrop.data.GroupAlreadyExistsException
import com.e3hi.geodrop.data.GroupNotFoundException
import com.e3hi.geodrop.data.GroupRole
import com.e3hi.geodrop.data.displayTitle
import com.e3hi.geodrop.data.displayTitleParts
import com.e3hi.geodrop.data.mediaLabel
import com.e3hi.geodrop.data.FirestoreRepo
import com.e3hi.geodrop.data.DropLikeStatus
import com.e3hi.geodrop.data.MediaStorageRepo
import com.e3hi.geodrop.data.NoteInventory
import com.e3hi.geodrop.data.UserDataSyncRepository
import com.e3hi.geodrop.data.DropType
import com.e3hi.geodrop.data.UserProfile
import com.e3hi.geodrop.data.ExplorerUsername
import com.e3hi.geodrop.data.UserMode
import com.e3hi.geodrop.data.dropTemplatesFor
import com.e3hi.geodrop.data.businessDropTypeOptionsFor
import com.e3hi.geodrop.data.UserRole
import com.e3hi.geodrop.data.canViewNsfw
import com.e3hi.geodrop.data.RedemptionResult
import com.e3hi.geodrop.data.applyUserLike
import com.e3hi.geodrop.data.isBusinessDrop
import com.e3hi.geodrop.data.isRedeemedBy
import com.e3hi.geodrop.data.remainingRedemptions
import com.e3hi.geodrop.data.requiresRedemption
import com.e3hi.geodrop.data.userLikeStatus
import com.e3hi.geodrop.data.isBusiness
import com.e3hi.geodrop.data.VisionApiStatus
import com.e3hi.geodrop.data.isExpired
import com.e3hi.geodrop.data.remainingDecayMillis
import com.e3hi.geodrop.data.decayAtMillis
import com.e3hi.geodrop.data.likeStatus
import com.e3hi.geodrop.geo.DropDecisionReceiver
import com.e3hi.geodrop.geo.NearbyDropRegistrar
import com.e3hi.geodrop.util.ExplorerAccountStore
import com.e3hi.geodrop.util.GroupPreferences
import com.e3hi.geodrop.util.NotificationPreferences
import com.e3hi.geodrop.util.formatTimestamp
import com.e3hi.geodrop.util.TermsPreferences
import com.e3hi.geodrop.util.DropBlockedBySafetyException
import com.e3hi.geodrop.util.DropSafetyAssessment
import com.e3hi.geodrop.util.DropSafetyEvaluator
import com.e3hi.geodrop.util.NoOpDropSafetyEvaluator
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestoreException
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerControlView
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
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
    val termsPrefs = remember(ctx) { TermsPreferences(ctx) }
    var hasAcceptedTerms by remember { mutableStateOf(termsPrefs.hasAcceptedTerms()) }
    var hasViewedOnboarding by remember { mutableStateOf(termsPrefs.hasViewedFirstRunOnboarding()) }
    var guestModeEnabled by rememberSaveable { mutableStateOf(false) }
    var showAccountSignIn by remember { mutableStateOf(false) }
    var accountAuthMode by remember { mutableStateOf(AccountAuthMode.SIGN_IN) }
    var accountType by remember { mutableStateOf(AccountType.EXPLORER) }
    var accountTypeSelectionLocked by remember { mutableStateOf(false) }
    var accountEmail by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var accountPassword by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var accountConfirmPassword by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var accountUsername by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var accountAuthSubmitting by remember { mutableStateOf(false) }
    var accountAuthError by remember { mutableStateOf<String?>(null) }
    var accountAuthStatus by remember { mutableStateOf<String?>(null) }
    var pendingExplorerUsername by remember { mutableStateOf<String?>(null) }
    var showBusinessOnboarding by remember { mutableStateOf(false) }
    var accountGoogleSigningIn by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }
    var showExplorerProfile by remember { mutableStateOf(false) }
    var explorerDestination by rememberSaveable { mutableStateOf(ExplorerDestination.Discover.name) }
    var explorerUsernameField by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var explorerProfileSubmitting by remember { mutableStateOf(false) }
    var explorerProfileError by remember { mutableStateOf<String?>(null) }
    var signingOut by remember { mutableStateOf(false) }
    var showNsfwDialog by remember { mutableStateOf(false) }
    var nsfwUpdating by remember { mutableStateOf(false) }
    var nsfwUpdateError by remember { mutableStateOf<String?>(null) }
    val defaultWebClientId = remember { BuildConfig.GOOGLE_WEB_CLIENT_ID }
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

    fun resetAccountAuthFields(clearEmail: Boolean) {
        if (clearEmail) {
            accountEmail = TextFieldValue("")
        }
        accountPassword = TextFieldValue("")
        accountConfirmPassword = TextFieldValue("")
        accountUsername = TextFieldValue("")
        accountAuthError = null
        accountAuthStatus = null
        nsfwUpdateError = null
        nsfwUpdating = false
    }

    fun dismissAccountAuthDialog() {
        if (accountAuthSubmitting || accountGoogleSigningIn) return
        showAccountSignIn = false
        resetAccountAuthFields(clearEmail = false)
        accountAuthMode = AccountAuthMode.SIGN_IN
        accountType = AccountType.EXPLORER
        accountTypeSelectionLocked = false
    }

    fun openAccountAuthDialog(
        initialType: AccountType = AccountType.EXPLORER,
        initialMode: AccountAuthMode = AccountAuthMode.SIGN_IN,
        lockAccountType: Boolean = false
    ) {
        if (accountAuthSubmitting || accountGoogleSigningIn) return
        accountTypeSelectionLocked = lockAccountType
        accountType = initialType
        accountAuthMode = initialMode
        resetAccountAuthFields(clearEmail = true)
        showAccountSignIn = true
    }

    fun performAccountAuth() {
        if (accountAuthSubmitting || accountGoogleSigningIn) return

        val email = accountEmail.text.trim()
        val password = accountPassword.text
        val confirm = accountConfirmPassword.text
        val username = accountUsername.text
        val needsExplorerUsername = accountAuthMode == AccountAuthMode.REGISTER && accountType == AccountType.EXPLORER
        var sanitizedExplorerUsername: String? = null

        when {
            email.isEmpty() -> {
                accountAuthError = "Enter your email address."
                return
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                accountAuthError = "Enter a valid email address."
                return
            }

            password.length < 6 -> {
                accountAuthError = "Password must be at least 6 characters."
                return
            }

            accountAuthMode == AccountAuthMode.REGISTER && confirm != password -> {
                accountAuthError = "Passwords do not match."
                return
            }

            needsExplorerUsername -> {
                sanitizedExplorerUsername = try {
                    ExplorerUsername.sanitize(username)
                } catch (error: ExplorerUsername.InvalidUsernameException) {
                    accountAuthError = when (error.reason) {
                        ExplorerUsername.ValidationError.TOO_SHORT ->
                            ctx.getString(R.string.explorer_profile_error_too_short)

                        ExplorerUsername.ValidationError.TOO_LONG ->
                            ctx.getString(R.string.explorer_profile_error_too_long)

                        ExplorerUsername.ValidationError.INVALID_CHARACTERS ->
                            ctx.getString(R.string.explorer_profile_error_invalid_characters)
                    }
                    return
                }
            }
        }

        accountAuthSubmitting = true
        accountAuthError = null
        accountAuthStatus = null

        val selectedMode = accountAuthMode
        val selectedType = accountType
        val task = try {
            when (selectedMode) {
                AccountAuthMode.SIGN_IN -> auth.signInWithEmailAndPassword(email, password)
                AccountAuthMode.REGISTER -> auth.createUserWithEmailAndPassword(email, password)
            }
        } catch (error: Exception) {
            accountAuthSubmitting = false
            accountAuthError = error.localizedMessage?.takeIf { it.isNotBlank() }
                ?: if (selectedMode == AccountAuthMode.REGISTER) {
                    "Couldn't create your account. Try again."
                } else {
                    "Couldn't sign you in. Check your email and password."
                }
            return
        }

        task.addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                if (sanitizedExplorerUsername != null) {
                    accountAuthStatus = ctx.getString(R.string.explorer_profile_status_claiming)
                    pendingExplorerUsername = sanitizedExplorerUsername
                    return@addOnCompleteListener
                }

                accountAuthSubmitting = false
                resetAccountAuthFields(clearEmail = true)
                showAccountSignIn = false
                if (selectedType == AccountType.BUSINESS && selectedMode == AccountAuthMode.REGISTER) {
                    showBusinessOnboarding = true
                }
            } else {
                accountAuthSubmitting = false
                val message = authTask.exception?.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: if (selectedMode == AccountAuthMode.REGISTER) {
                        "Couldn't create your account. Try again."
                    } else {
                        "Couldn't sign you in. Check your email and password."
                    }
                accountAuthError = message
            }
        }
    }

    fun sendAccountPasswordReset() {
        if (accountAuthSubmitting || accountGoogleSigningIn) return

        val email = accountEmail.text.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            accountAuthError = "Enter a valid email address to reset your password."
            return
        }

        accountAuthSubmitting = true
        accountAuthError = null
        accountAuthStatus = null

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                accountAuthSubmitting = false
                if (task.isSuccessful) {
                    accountAuthStatus = "Password reset email sent to $email."
                } else {
                    val message = task.exception?.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "Couldn't send password reset email. Try again later."
                    accountAuthError = message
                }
            }
    }

    val accountGoogleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            accountGoogleSigningIn = false
            accountAuthError = if (result.resultCode == Activity.RESULT_CANCELED) {
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
                accountGoogleSigningIn = false
                accountAuthError = "Google sign-in is misconfigured. Provide a valid web client ID."
                return@rememberLauncherForActivityResult
            }

            accountAuthSubmitting = true
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    accountAuthSubmitting = false
                    accountGoogleSigningIn = false
                    if (authTask.isSuccessful) {
                        resetAccountAuthFields(clearEmail = true)
                        val selectedType = accountType
                        showAccountSignIn = false
                        val isNewUser = authTask.result?.additionalUserInfo?.isNewUser == true
                        if (selectedType == AccountType.BUSINESS && isNewUser) {
                            showBusinessOnboarding = true
                        }
                    } else {
                        val message = authTask.exception?.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: "Couldn't sign you in with Google. Try again."
                        accountAuthError = message
                    }
                }
        } catch (error: ApiException) {
            accountGoogleSigningIn = false
            accountAuthError = error.localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Google sign-in failed. Try again."
        }
    }

    fun startAccountGoogleSignIn() {
        if (accountAuthSubmitting || accountGoogleSigningIn) return
        if (defaultWebClientId.isBlank()) {
            accountAuthError = "Google sign-in isn't configured. Set GOOGLE_WEB_CLIENT_ID in your Gradle properties or environment."
            return
        }

        accountAuthError = null
        accountAuthStatus = null
        accountGoogleSigningIn = true

        runCatching { googleSignInClient.signOut() }

        runCatching {
            accountGoogleSignInLauncher.launch(googleSignInClient.signInIntent)
        }.onFailure { error ->
            accountGoogleSigningIn = false
            accountAuthError = error.localizedMessage?.takeIf { it.isNotBlank() }
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

    val userMode = when {
        currentUser != null -> UserMode.SIGNED_IN
        guestModeEnabled -> UserMode.GUEST
        else -> null
    }

    LaunchedEffect(userMode) {
        when (userMode) {
            UserMode.SIGNED_IN -> {
                guestModeEnabled = false
            }

            UserMode.GUEST -> {
                explorerDestination = ExplorerDestination.Discover.name
            }

            null -> {}
        }
    }

    LaunchedEffect(
        currentUser,
        accountAuthSubmitting,
        accountGoogleSigningIn,
        signingOut,
        hasAcceptedTerms,
        hasViewedOnboarding
    ) {
        if (currentUser == null) {
            showAccountMenu = false
            if (!accountAuthSubmitting && !accountGoogleSigningIn) {
                showAccountSignIn = false
                accountAuthMode = AccountAuthMode.SIGN_IN
                accountType = AccountType.EXPLORER
                resetAccountAuthFields(clearEmail = true)
            }
        } else {
            guestModeEnabled = false
        }
    }

    val noteInventory = remember { NoteInventory(ctx) }
    var collectedNotes by remember { mutableStateOf(noteInventory.getCollectedNotes()) }
    var ignoredDropIds by remember { mutableStateOf(noteInventory.getIgnoredDropIds()) }
    val collectedDropIds = remember(collectedNotes) { collectedNotes.map { it.id }.toSet() }

    LaunchedEffect(currentUser?.uid) {
        noteInventory.setActiveUser(currentUser?.uid)
        collectedNotes = noteInventory.getCollectedNotes()
        ignoredDropIds = noteInventory.getIgnoredDropIds()
    }

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

    if (!hasAcceptedTerms) {
        TermsAcceptanceScreen(
            onAccept = {
                termsPrefs.recordAcceptance()
                hasAcceptedTerms = true
            },
            onExit = {
                (ctx as? Activity)?.finish()
            }
        )
        return
    }

    if (!hasViewedOnboarding) {
        FirstRunOnboardingScreen(
            onContinue = {
                termsPrefs.recordOnboardingViewed()
                hasViewedOnboarding = true
            },
            onExit = {
                (ctx as? Activity)?.finish()
            }
        )
        return
    }

    if (showAccountSignIn) {
        AccountSignInDialog(
            accountType = accountType,
            canChangeAccountType = !accountTypeSelectionLocked,
            onAccountTypeChange = { type ->
                if (
                    accountAuthSubmitting ||
                    accountGoogleSigningIn ||
                    accountTypeSelectionLocked
                ) return@AccountSignInDialog
                accountType = type
                accountAuthError = null
                accountAuthStatus = null
            },
            mode = accountAuthMode,
            onModeChange = { mode ->
                if (accountAuthSubmitting || accountGoogleSigningIn) return@AccountSignInDialog
                accountAuthMode = mode
                accountAuthError = null
                accountAuthStatus = null
            },
            email = accountEmail,
            onEmailChange = { accountEmail = it },
            password = accountPassword,
            onPasswordChange = { accountPassword = it },
            confirmPassword = accountConfirmPassword,
            onConfirmPasswordChange = { accountConfirmPassword = it },
            username = accountUsername,
            onUsernameChange = { accountUsername = it },
            isSubmitting = accountAuthSubmitting,
            isGoogleSigningIn = accountGoogleSigningIn,
            error = accountAuthError,
            status = accountAuthStatus,
            onSubmit = { performAccountAuth() },
            onDismiss = { dismissAccountAuthDialog() },
            onForgotPassword = { sendAccountPasswordReset() },
            onGoogleSignIn = { startAccountGoogleSignIn() }
        )
    }

    if (userMode == null) {
        UserModeSelectionScreen(
            onSelectGuest = {
                guestModeEnabled = true
            },
            onSelectExplorerSignIn = {
                guestModeEnabled = false
                openAccountAuthDialog(
                    initialType = AccountType.EXPLORER,
                    initialMode = AccountAuthMode.SIGN_IN,
                    lockAccountType = true
                )
            },
            onSelectBusinessSignIn = {
                guestModeEnabled = false
                openAccountAuthDialog(
                    initialType = AccountType.BUSINESS,
                    initialMode = AccountAuthMode.SIGN_IN,
                    lockAccountType = true
                )
            }
        )
        return
    }

    val snackbar = remember { SnackbarHostState() }
    val manageGroupsSnackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val fused = remember { LocationServices.getFusedLocationProviderClient(ctx) }
    val repo = remember { FirestoreRepo() }
    val mediaStorage = remember { MediaStorageRepo() }
    val registrar = remember { NearbyDropRegistrar() }
    val groupPrefs = remember { GroupPreferences(ctx) }
    val explorerAccountStore = remember { ExplorerAccountStore(ctx) }
    val notificationPrefs = remember { NotificationPreferences(ctx) }
    val userDataSync = remember { UserDataSyncRepository(repo, groupPrefs, noteInventory, scope) }

    val canParticipate = userMode.canParticipate
    val hasExplorerAccount = userMode != UserMode.GUEST
    val readOnlyParticipationMessage: String? = when (userMode) {
        UserMode.GUEST -> null
        UserMode.SIGNED_IN -> null
    }

    fun participationRestriction(action: String): String = when (userMode) {
        UserMode.GUEST -> "Create an account to $action."
        UserMode.SIGNED_IN -> ""
    }

    var joinedGroups by remember { mutableStateOf(groupPrefs.getMemberships()) }
    var selectedExplorerGroupCode by rememberSaveable { mutableStateOf<String?>(null) }
    val createdGroups = remember(joinedGroups) {
        joinedGroups
            .filter { membership -> membership.role == GroupRole.OWNER }
            .sortedBy { it.code }
    }
    val subscribedGroups = remember(joinedGroups) {
        joinedGroups
            .filter { membership -> membership.role == GroupRole.SUBSCRIBER }
            .sortedBy { it.code }
    }
    val explorerGroups = remember(createdGroups, subscribedGroups) {
        (createdGroups + subscribedGroups).distinctBy { it.code }
    }
    var dropVisibility by remember { mutableStateOf(DropVisibility.Public) }
    var dropAnonymously by remember { mutableStateOf(false) }
    var dropContentType by remember { mutableStateOf(DropContentType.TEXT) }
    var dropType by remember { mutableStateOf(DropType.COMMUNITY) }
    var note by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var capturedPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var capturedAudioUri by rememberSaveable { mutableStateOf<String?>(null) }
    var capturedVideoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var groupCodeInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var redemptionCodeInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var redemptionLimitInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var decayDaysInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var status by remember { mutableStateOf<String?>(null) }
    var showOtherDropsMap by remember { mutableStateOf(false) }
    var otherDrops by remember { mutableStateOf<List<Drop>>(emptyList()) }
    var otherDropsLoading by remember { mutableStateOf(false) }
    var otherDropsError by remember { mutableStateOf<String?>(null) }
    var otherDropsCurrentLocation by remember { mutableStateOf<LatLng?>(null) }
    var otherDropsSelectedId by remember { mutableStateOf<String?>(null) }
    val dismissedBrowseDropIds = rememberSaveable(
        saver = listSaver(
            save = { stateList -> stateList.toList() },
            restore = { restored ->
                mutableStateListOf<String>().apply { addAll(restored) }
            }
        )
    ) {
        mutableStateListOf<String>()
    }
    val reportedCollectedDropIds = rememberSaveable(
        saver = listSaver(
            save = { stateList -> stateList.toList() },
            restore = { restored ->
                mutableStateListOf<String>().apply { addAll(restored) }
            }
        )
    ) {
        mutableStateListOf<String>()
    }
    var otherDropsRefreshToken by remember { mutableStateOf(0) }
    var otherDropsMapWeight by rememberSaveable {
        mutableStateOf(DEFAULT_MAP_WEIGHT.coerceIn(MAP_LIST_MIN_WEIGHT, MAP_LIST_MAX_WEIGHT))
    }
    val otherDropsListState = rememberLazyListState()
    var votingDropIds by remember { mutableStateOf(setOf<String>()) }
    val dropReportReasons = remember { DefaultReportReasons }
    var browseReportDialogOpen by remember { mutableStateOf(false) }
    var browseReportSelectedReasons by remember { mutableStateOf(setOf<String>()) }
    var browseReportError by remember { mutableStateOf<String?>(null) }
    var browseReportProcessing by remember { mutableStateOf(false) }
    var browseReportTarget by remember { mutableStateOf<ReportableDrop?>(null) }
    var browseReportingDropId by remember { mutableStateOf<String?>(null) }
    var myDrops by remember { mutableStateOf<List<Drop>>(emptyList()) }
    var myDropsLoading by remember { mutableStateOf(false) }
    var myDropsError by remember { mutableStateOf<String?>(null) }
    var myDropsRefreshToken by remember { mutableStateOf(0) }
    var myDropsCurrentLocation by remember { mutableStateOf<LatLng?>(null) }
    var myDropsDeletingId by remember { mutableStateOf<String?>(null) }
    var myDropsSelectedId by remember { mutableStateOf<String?>(null) }
    var myDropsSortKey by rememberSaveable { mutableStateOf(DropSortOption.NEWEST.name) }
    var myDropCountHint by remember { mutableStateOf<Int?>(null) }
    var myDropPendingReviewHint by remember { mutableStateOf<Int?>(null) }
    var showManageGroups by remember { mutableStateOf(false) }
    var showGroupMenu by remember { mutableStateOf(false) }
    var showDropComposer by remember { mutableStateOf(false) }
    var showBusinessDashboard by remember { mutableStateOf(false) }
    var businessDrops by remember { mutableStateOf<List<Drop>>(emptyList()) }
    var businessDashboardLoading by remember { mutableStateOf(false) }
    var businessDashboardError by remember { mutableStateOf<String?>(null) }
    var businessDashboardRefreshToken by remember { mutableStateOf(0) }
    var selectedHomeDestination by rememberSaveable { mutableStateOf(HomeDestination.Explorer.name) }
    var notificationRadius by remember { mutableStateOf(notificationPrefs.getNotificationRadiusMeters()) }
    var showNotificationRadiusDialog by remember { mutableStateOf(false) }

    DisposableEffect(groupPrefs) {
        val listener = GroupPreferences.ChangeListener { groups, _ ->
            joinedGroups = groups
        }
        groupPrefs.addChangeListener(listener)
        joinedGroups = groupPrefs.getMemberships()
        onDispose { groupPrefs.removeChangeListener(listener) }
    }

    LaunchedEffect(explorerGroups) {
        val availableCodes = explorerGroups.map { it.code }
        val current = selectedExplorerGroupCode
        if (availableCodes.isEmpty()) {
            if (current != null) {
                selectedExplorerGroupCode = null
            }
        } else if (current != null && current !in availableCodes) {
            selectedExplorerGroupCode = availableCodes.firstOrNull()
        }
    }

    DisposableEffect(currentUser?.uid) {
        val uid = currentUser?.uid
        if (uid.isNullOrBlank()) {
            userDataSync.stop()
        } else {
            userDataSync.start(uid)
        }
        onDispose { userDataSync.stop() }
    }

    LaunchedEffect(currentUser?.uid) {
        val user = currentUser ?: return@LaunchedEffect
        if (!user.isAnonymous) return@LaunchedEffect

        val storedExplorerId = explorerAccountStore.getLastExplorerUid()
        if (storedExplorerId.isNullOrBlank()) {
            explorerAccountStore.setLastExplorerUid(user.uid)
            return@LaunchedEffect
        }

        if (storedExplorerId == user.uid) return@LaunchedEffect

        val migrationResult = runCatching {
            repo.migrateExplorerAccount(storedExplorerId, user.uid)
        }

        if (migrationResult.isSuccess) {
            explorerAccountStore.setLastExplorerUid(user.uid)
            myDropsRefreshToken += 1
        } else {
            Log.e("GeoDrop", "Explorer account migration failed", migrationResult.exceptionOrNull())
        }
    }

    fun handleSignOut(switchToGuest: Boolean = false) {
        if (signingOut) return

        showAccountMenu = false
        signingOut = true
        showBusinessDashboard = false
        showBusinessOnboarding = false
        showDropComposer = false
        showManageGroups = false
        showAccountSignIn = false
        showNsfwDialog = false
        status = null
        showExplorerProfile = false
        accountAuthError = null
        accountAuthStatus = null
        explorerProfileError = null
        explorerProfileSubmitting = false

        scope.launch {
            val result = runCatching {
                runCatching { googleSignInClient.signOut() }
                auth.signOut()
            }

            signingOut = false

            if (result.isSuccess) {
                selectedHomeDestination = HomeDestination.Explorer.name
                explorerDestination = ExplorerDestination.Discover.name
                guestModeEnabled = switchToGuest
                val message = if (switchToGuest) {
                    "Browsing as a guest."
                } else {
                    "Signed out."
                }
                snackbar.showMessage(scope, message)
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

    val businessCategories = userProfile?.businessCategories.orEmpty()

    LaunchedEffect(businessCategories, dropType) {
        val permittedTypes = businessDropTypeOptionsFor(businessCategories).map { it.type }
        if (permittedTypes.isNotEmpty() && dropType !in permittedTypes) {
            dropType = permittedTypes.first()
        }
    }

    val currentUserId = currentUser?.uid

    LaunchedEffect(currentUserId) {
        notificationPrefs.setActiveUser(currentUserId)
        notificationRadius = notificationPrefs.getNotificationRadiusMeters()
    }

    LaunchedEffect(pendingExplorerUsername, currentUserId) {
        val desired = pendingExplorerUsername
        val userId = currentUserId
        if (desired.isNullOrBlank() || userId.isNullOrBlank()) return@LaunchedEffect

        val updateResult = runCatching { repo.updateExplorerUsername(userId, desired) }
        pendingExplorerUsername = null

        updateResult.onSuccess { updated ->
            userProfile = updated
            accountAuthSubmitting = false
            accountAuthStatus = null
            resetAccountAuthFields(clearEmail = true)
            showAccountSignIn = false
            val usernameForMessage = updated.username ?: desired
            snackbar.showMessage(
                scope,
                ctx.getString(R.string.explorer_profile_status_saved, "@$usernameForMessage")
            )
        }.onFailure { error ->
            accountAuthSubmitting = false
            accountAuthStatus = null
            val message = when (error) {
                is ExplorerUsername.InvalidUsernameException -> when (error.reason) {
                    ExplorerUsername.ValidationError.TOO_SHORT ->
                        ctx.getString(R.string.explorer_profile_error_too_short)

                    ExplorerUsername.ValidationError.TOO_LONG ->
                        ctx.getString(R.string.explorer_profile_error_too_long)

                    ExplorerUsername.ValidationError.INVALID_CHARACTERS ->
                        ctx.getString(R.string.explorer_profile_error_invalid_characters)
                }

                is IllegalStateException -> ctx.getString(R.string.explorer_profile_error_taken)
                else -> ctx.getString(R.string.explorer_profile_error_generic)
            }
            resetAccountAuthFields(clearEmail = true)
            showAccountSignIn = false
            explorerUsernameField = TextFieldValue(desired)
            explorerProfileError = message
            explorerProfileSubmitting = false
            showExplorerProfile = true
            snackbar.showMessage(scope, message)
        }
    }

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

    fun saveExplorerUsername() {
        if (explorerProfileSubmitting) return

        val userId = currentUserId
        if (userId.isNullOrBlank()) {
            explorerProfileError = ctx.getString(R.string.explorer_profile_error_sign_in)
            return
        }

        val desired = explorerUsernameField.text
        val sanitized = try {
            ExplorerUsername.sanitize(desired)
        } catch (error: ExplorerUsername.InvalidUsernameException) {
            explorerProfileError = when (error.reason) {
                ExplorerUsername.ValidationError.TOO_SHORT -> ctx.getString(R.string.explorer_profile_error_too_short)
                ExplorerUsername.ValidationError.TOO_LONG -> ctx.getString(R.string.explorer_profile_error_too_long)
                ExplorerUsername.ValidationError.INVALID_CHARACTERS ->
                    ctx.getString(R.string.explorer_profile_error_invalid_characters)
            }
            return
        }

        explorerProfileSubmitting = true
        explorerProfileError = null

        scope.launch {
            try {
                val updated = repo.updateExplorerUsername(userId, sanitized)
                userProfile = updated
                showExplorerProfile = false
                val usernameForMessage = updated.username ?: sanitized
                snackbar.showMessage(
                    scope,
                    ctx.getString(R.string.explorer_profile_status_saved, "@$usernameForMessage")
                )
            } catch (error: ExplorerUsername.InvalidUsernameException) {
                explorerProfileError = when (error.reason) {
                    ExplorerUsername.ValidationError.TOO_SHORT -> ctx.getString(R.string.explorer_profile_error_too_short)
                    ExplorerUsername.ValidationError.TOO_LONG -> ctx.getString(R.string.explorer_profile_error_too_long)
                    ExplorerUsername.ValidationError.INVALID_CHARACTERS ->
                        ctx.getString(R.string.explorer_profile_error_invalid_characters)
                }
            } catch (error: IllegalStateException) {
                explorerProfileError = error.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: ctx.getString(R.string.explorer_profile_error_taken)
            } catch (error: Exception) {
                explorerProfileError = error.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: ctx.getString(R.string.explorer_profile_error_generic)
            } finally {
                explorerProfileSubmitting = false
            }
        }
    }

    fun pickUpDrop(drop: Drop) {
        if (!canParticipate) {
            snackbar.showMessage(scope, participationRestriction("pick up drops"))
            return
        }
        val expiresAt = drop.decayAtMillis()
        if (expiresAt != null && expiresAt <= System.currentTimeMillis()) {
            snackbar.showMessage(scope, "This drop has already expired.")
            return
        }
        val currentLocation = otherDropsCurrentLocation
        val withinRange = currentLocation?.let {
            distanceBetweenMeters(it.latitude, it.longitude, drop.lat, drop.lng) <=
                    DROP_PICKUP_RADIUS_METERS
        } ?: false
        if (!withinRange) {
            snackbar.showMessage(
                scope,
                "Move within ${DROP_PICKUP_RADIUS_METERS.roundToInt()} meters to pick up this drop."
            )
            return
        }

        val appContext = ctx.applicationContext
        val intent = Intent(appContext, DropDecisionReceiver::class.java).apply {
            action = DropDecisionReceiver.ACTION_PICK_UP
            putExtra(DropDecisionReceiver.EXTRA_DROP_ID, drop.id)
            if (drop.text.isNotBlank()) {
                putExtra(DropDecisionReceiver.EXTRA_DROP_TEXT, drop.text)
            }
            drop.description?.takeIf { it.isNotBlank() }?.let { description ->
                putExtra(DropDecisionReceiver.EXTRA_DROP_DESCRIPTION, description)
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
            drop.decayDays?.let { putExtra(DropDecisionReceiver.EXTRA_DROP_DECAY_DAYS, it) }
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

    fun submitLike(drop: Drop, desiredStatus: DropLikeStatus) {
        if (!canParticipate) {
            snackbar.showMessage(scope, participationRestriction("react to drops"))
            return
        }
        val userId = currentUserId
        if (userId.isNullOrBlank()) {
            snackbar.showMessage(scope, "Sign in to react to drops.")
            return
        }

        val dropId = drop.id
        if (dropId.isBlank()) return

        if (!collectedDropIds.contains(dropId)) {
            snackbar.showMessage(scope, "Collect this drop before reacting to it.")
            return
        }

        val updatedDrop = drop.applyUserLike(userId, desiredStatus)
        if (updatedDrop == drop) return

        val previousOtherDrops = otherDrops
        val previousMyDrops = myDrops
        val previousCollectedNote = noteInventory.getCollectedNotes().firstOrNull { it.id == dropId }

        votingDropIds = votingDropIds + dropId
        updateDropInLists(dropId) { current -> current.applyUserLike(userId, desiredStatus) }
        previousCollectedNote?.let {
            noteInventory.updateLikeStatus(
                dropId,
                updatedDrop.likeCount,
                updatedDrop.dislikeCount,
                updatedDrop.userLikeStatus(userId)
            )
            collectedNotes = noteInventory.getCollectedNotes()
        }

        scope.launch {
            try {
                repo.setDropLike(dropId, userId, desiredStatus)
            } catch (e: Exception) {
                otherDrops = previousOtherDrops
                myDrops = previousMyDrops
                previousCollectedNote?.let {
                    noteInventory.updateLikeStatus(
                        dropId,
                        it.likeCount,
                        it.dislikeCount,
                        it.likeStatus()
                    )
                    collectedNotes = noteInventory.getCollectedNotes()
                }
                if (e is FirebaseFirestoreException &&
                    e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                ) {
                    Log.w(
                        "DropHere",
                        "Permission denied while updating reaction for drop $dropId for $userId",
                        e
                    )
                } else {
                    Log.e(
                        "DropHere",
                        "Failed to update reaction for drop $dropId for $userId",
                        e
                    )
                }
                snackbar.showMessage(scope, "Couldn't update your reaction. Try again.")
            } finally {
                votingDropIds = votingDropIds - dropId
            }
        }
    }

    // Optional: also sync nearby on first open if already signed in
    LaunchedEffect(joinedGroups, notificationRadius) {
        if (FirebaseAuth.getInstance().currentUser != null) {
            registrar.registerNearby(
                ctx,
                maxMeters = notificationRadius,
                groupCodes = joinedGroups.map { it.code }.toSet()
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
            explorerUsernameField = TextFieldValue("")
            explorerProfileSubmitting = false
            explorerProfileError = null
            showExplorerProfile = false
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

    LaunchedEffect(showExplorerProfile) {
        if (showExplorerProfile) {
            explorerUsernameField = TextFieldValue(userProfile?.username.orEmpty())
            explorerProfileSubmitting = false
            explorerProfileError = null
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

    LaunchedEffect(userProfile?.isBusiness()) {
        if (userProfile?.isBusiness() == true) {
            dropAnonymously = false
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
        description = TextFieldValue("")
        capturedPhotoPath = null
        clearAudio()
        clearVideo()
        showDropComposer = false
        dropAnonymously = false
        if (dropType == DropType.RESTAURANT_COUPON) {
            redemptionCodeInput = TextFieldValue("")
            redemptionLimitInput = TextFieldValue("")
        }
        decayDaysInput = TextFieldValue("")
        val baseStatus = "Dropped at (%.5f, %.5f)".format(lat, lng)
        val dropTypeCopy = businessDropTypeOptionsFor(businessCategories)
            .firstOrNull { it.type == dropType }
        val dropTypeTitle = dropTypeCopy?.title
        val defaultTypeSummary = when (dropType) {
            DropType.RESTAURANT_COUPON -> "business offer"
            DropType.TOUR_STOP -> "tour stop"
            DropType.COMMUNITY -> when (contentType) {
                DropContentType.TEXT -> "note"
                DropContentType.PHOTO -> "photo drop"
                DropContentType.AUDIO -> "audio drop"
                DropContentType.VIDEO -> "video drop"
            }
        }
        val typeSummary = dropTypeTitle?.takeIf { it.isNotBlank() }
            ?.replaceFirstChar { if (it.isLowerCase()) it else it.lowercaseChar() }
            ?: defaultTypeSummary
        status = if (groupCode != null) {
            "$baseStatus for group $groupCode ($typeSummary)"
        } else {
            "$baseStatus ($typeSummary)"
        }
        val snackbarMessage = when {
            groupCode != null -> "Group drop saved!"
            !dropTypeTitle.isNullOrBlank() -> "${dropTypeTitle} drop saved!"
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
        descriptionText: String?,
        mediaInput: String?,
        mediaMimeType: String?,
        mediaData: String?,
        mediaDataForSafety: String?,
        mediaStoragePath: String?,
        redemptionCode: String?,
        redemptionLimit: Int?,
        decayDays: Int?,
        dropAnonymously: Boolean,
        nsfwAllowed: Boolean
    ): DropSafetyAssessment {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anon"
        val sanitizedMedia = mediaInput?.takeIf { it.isNotBlank() }
        val sanitizedMime = mediaMimeType?.takeIf { it.isNotBlank() }
        val sanitizedRedemptionCode = redemptionCode?.trim()?.takeIf { it.isNotEmpty() }
        val sanitizedRedemptionLimit = redemptionLimit?.takeIf { it > 0 }
        val sanitizedData = mediaData?.takeIf { it.isNotBlank() }
        val sanitizedSafetyData = mediaDataForSafety?.takeIf { it.isNotBlank() }
        val sanitizedDecayDays = decayDays?.takeIf { it > 0 }
        val sanitizedText = noteText.trim()
        val sanitizedDescription = descriptionText?.trim()?.takeIf { it.isNotEmpty() }
        val dropperUsername = if (dropAnonymously) {
            null
        } else {
            userProfile?.username?.trim()?.takeIf { it.isNotEmpty() }
        }
        val d = Drop(
            text = sanitizedText,
            description = sanitizedDescription,
            lat = lat,
            lng = lng,
            createdBy = uid,
            createdAt = System.currentTimeMillis(),
            dropperUsername = dropperUsername,
            isAnonymous = dropAnonymously,
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
            redemptionLimit = if (dropType == DropType.RESTAURANT_COUPON) sanitizedRedemptionLimit else null,
            decayDays = sanitizedDecayDays
        )

        val safetyPieces = mutableListOf<String>()
        if (sanitizedText.isNotBlank()) {
            safetyPieces += sanitizedText
        }
        sanitizedDescription?.let { safetyPieces += it }
        val safetyText = safetyPieces.takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "\n")
        val safety = dropSafetyEvaluator.assess(
            text = safetyText,
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
        uiDone(lat, lng, groupCode, contentType, dropType)
        return safety
    }

    fun submitDrop() {
        if (isSubmitting) return
        if (!canParticipate) {
            snackbar.showMessage(scope, participationRestriction("share drops"))
            return
        }
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
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (selectedGroupCode != null) {
                    if (!groupPrefs.isGroupOwned(selectedGroupCode)) {
                        isSubmitting = false
                        snackbar.showMessage(
                            scope,
                            "Only groups you created can receive drops."
                        )
                        return@launch
                    }
                    val ownerId = uid ?: run {
                        isSubmitting = false
                        snackbar.showMessage(scope, "Sign in to share drops with a group.")
                        return@launch
                    }
                    var ownsGroup = runCatching { repo.isGroupOwner(ownerId, selectedGroupCode) }
                        .getOrDefault(false)
                    if (!ownsGroup) {
                        val claimed = runCatching {
                            repo.joinGroup(
                                ownerId,
                                selectedGroupCode,
                                allowCreateIfMissing = true
                            )
                        }
                            .getOrNull()
                        if (claimed != null) {
                            groupPrefs.addGroup(claimed)
                            joinedGroups = groupPrefs.getMemberships()
                            ownsGroup = claimed.role == GroupRole.OWNER
                        }
                    }
                    if (!ownsGroup) {
                        isSubmitting = false
                        snackbar.showMessage(
                            scope,
                            "Only the creator of $selectedGroupCode can share drops with that group."
                        )
                        return@launch
                    }
                }
                var mediaUrlResult: String? = null
                var mediaStoragePathResult: String? = null
                var mediaMimeTypeResult: String? = null
                var mediaDataResult: String? = null
                var mediaDataForSafetyResult: String? = null
                var dropNoteText = note.text
                var dropDescriptionText = description.text
                var redemptionCodeResult: String? = null
                var redemptionLimitResult: Int? = null
                var decayDaysResult: Int? = null

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

                val decayText = decayDaysInput.text.trim()
                if (decayText.isNotEmpty()) {
                    val parsedDecay = decayText.toIntOrNull()
                    if (parsedDecay == null || parsedDecay <= 0) {
                        isSubmitting = false
                        snackbar.showMessage(scope, "Enter a valid number of days or leave it blank.")
                        return@launch
                    }
                    if (parsedDecay > MAX_DECAY_DAYS) {
                        isSubmitting = false
                        snackbar.showMessage(scope, "Choose a decay up to $MAX_DECAY_DAYS days.")
                        return@launch
                    }
                    decayDaysResult = parsedDecay
                }

                val (lat, lng) = getLatestLocation() ?: run {
                    isSubmitting = false
                    snackbar.showMessage(scope, "No location available. Turn on GPS & try again.")
                    return@launch
                }

                val dropDescription = dropDescriptionText.trim().takeIf { it.isNotEmpty() }
                val anonymizeDrop = dropAnonymously && userProfile?.isBusiness() != true
                val safety = addDropAt(
                    lat = lat,
                    lng = lng,
                    groupCode = selectedGroupCode,
                    contentType = dropContentType,
                    dropType = dropType,
                    noteText = dropNoteText,
                    descriptionText = dropDescription,
                    mediaInput = mediaUrlResult,
                    mediaMimeType = mediaMimeTypeResult,
                    mediaData = mediaDataResult,
                    mediaDataForSafety = mediaDataForSafetyResult ?: mediaDataResult,
                    mediaStoragePath = mediaStoragePathResult,
                    redemptionCode = redemptionCodeResult,
                    redemptionLimit = redemptionLimitResult,
                    decayDays = decayDaysResult,
                    dropAnonymously = anonymizeDrop,
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


    val isBusinessUser = userProfile?.isBusiness() == true
    val currentHomeDestination = if (isBusinessUser) {
        runCatching { HomeDestination.valueOf(selectedHomeDestination) }
            .getOrDefault(HomeDestination.Business)
    } else {
        runCatching { HomeDestination.valueOf(selectedHomeDestination) }
            .getOrDefault(HomeDestination.Explorer)
    }
    val explorerHomeVisible = !isBusinessUser || currentHomeDestination == HomeDestination.Explorer

    LaunchedEffect(
        explorerHomeVisible,
        joinedGroups,
        otherDropsRefreshToken,
        collectedDropIds,
        ignoredDropIds,
        dismissedBrowseDropIds.toList()
    ) {
        if (explorerHomeVisible) {
            otherDropsLoading = true
            otherDropsError = null
            otherDropsCurrentLocation = null
            val rawUid = FirebaseAuth.getInstance().currentUser?.uid
            val effectiveUid = when (userMode) {
                UserMode.GUEST -> null
                else -> rawUid
            }
            if (userMode != UserMode.GUEST && effectiveUid == null) {
                otherDrops = emptyList()
                otherDropsError = "Sign-in is still in progress. Try again in a moment."
                otherDropsLoading = false
            } else {
                try {
                    val drops = repo.getVisibleDropsForUser(
                        effectiveUid,
                        joinedGroups.map { it.code }.toSet(),
                        allowNsfw = userProfile?.canViewNsfw() == true && canParticipate
                    )
                        .sortedByDescending { it.createdAt }
                    val latestLocation = getLatestLocation()?.let { (lat, lng) -> LatLng(lat, lng) }
                    dismissedBrowseDropIds.removeAll { id -> drops.none { it.id == id } }
                    val filteredDrops = drops.filterNot { drop ->
                        val id = drop.id
                        when {
                            id in collectedDropIds || id in ignoredDropIds -> true
                            dismissedBrowseDropIds.contains(id) -> {
                                val withinPickupRange = latestLocation?.let { location ->
                                    distanceBetweenMeters(
                                        location.latitude,
                                        location.longitude,
                                        drop.lat,
                                        drop.lng
                                    ) <= DROP_PICKUP_RADIUS_METERS
                                } ?: false
                                !withinPickupRange
                            }
                            else -> false
                        }
                    }
                    otherDrops = filteredDrops
                    otherDropsCurrentLocation = latestLocation
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
            if (!browseReportProcessing) {
                browseReportDialogOpen = false
                browseReportTarget = null
                browseReportSelectedReasons = emptySet()
                browseReportError = null
                browseReportingDropId = null
            }
        }
    }

    val currentExplorerDestination = remember(explorerDestination) {
        runCatching { ExplorerDestination.valueOf(explorerDestination) }
            .getOrDefault(ExplorerDestination.Discover)
    }

    val effectiveExplorerDestination = remember(currentExplorerDestination, hasExplorerAccount) {
        if (!hasExplorerAccount && currentExplorerDestination != ExplorerDestination.Discover) {
            ExplorerDestination.Discover
        } else {
            currentExplorerDestination
        }
    }

    LaunchedEffect(effectiveExplorerDestination) {
        val desired = effectiveExplorerDestination.name
        if (desired != explorerDestination) {
            explorerDestination = desired
        }
    }

    LaunchedEffect(currentHomeDestination, currentExplorerDestination, myDropsRefreshToken) {
        val shouldLoad = currentHomeDestination == HomeDestination.Explorer &&
                currentExplorerDestination == ExplorerDestination.MyDrops
        if (shouldLoad) {
            myDropsLoading = true
            myDropsError = null
            myDropsDeletingId = null
            myDropsCurrentLocation = null
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (!hasExplorerAccount) {
                myDrops = emptyList()
                myDropsLoading = false
                myDropsError = participationRestriction("view your drops")
            } else if (uid == null) {
                myDropsLoading = false
                myDropsError = "Sign-in is still in progress. Try again in a moment."
            } else {
                try {
                    val drops = repo.getDropsForUser(uid)
                        .sortedByDescending { it.createdAt }
                    myDrops = drops
                    myDropCountHint = drops.size
                    myDropPendingReviewHint = drops.count { it.reportCount > 0 }
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

    LaunchedEffect(showDropComposer, canParticipate) {
        if (showDropComposer && !canParticipate) {
            showDropComposer = false
            snackbar.showMessage(scope, participationRestriction("share drops"))
        }
    }

    fun openExplorerDestination(destination: ExplorerDestination) {
        selectedHomeDestination = HomeDestination.Explorer.name
        when (destination) {
            ExplorerDestination.Discover -> {
                explorerDestination = destination.name
            }

            ExplorerDestination.MyDrops -> {
                if (!hasExplorerAccount) {
                    snackbar.showMessage(scope, participationRestriction("view and manage your drops"))
                    return
                }
                explorerDestination = destination.name
            }

            ExplorerDestination.Collected -> {
                if (!hasExplorerAccount) {
                    snackbar.showMessage(scope, participationRestriction("view collected drops"))
                    return
                }
                val storedNotes = noteInventory.getCollectedNotes()
                if (!canParticipate && storedNotes.isEmpty()) {
                    snackbar.showMessage(scope, participationRestriction("view collected drops"))
                    return
                }
                collectedNotes = storedNotes
                explorerDestination = destination.name
            }
        }
    }


    val filteredOtherDrops = remember(selectedExplorerGroupCode, otherDrops, explorerGroups) {
        selectedExplorerGroupCode?.takeIf { code -> explorerGroups.any { it.code == code } }?.let { code ->
            otherDrops.filter { drop -> drop.groupCode == code }
        } ?: otherDrops
    }
    var otherDropsSortKey by rememberSaveable { mutableStateOf(DropSortOption.NEAREST.name) }
    val otherDropsSortOption = remember(otherDropsSortKey) {
        runCatching { DropSortOption.valueOf(otherDropsSortKey) }
            .getOrDefault(DropSortOption.NEAREST)
    }
    val dropSortOptions = remember { DropSortOption.entries }
    val sortedOtherDrops = remember(filteredOtherDrops, otherDropsSortOption, otherDropsCurrentLocation) {
        sortDrops(filteredOtherDrops, otherDropsSortOption, otherDropsCurrentLocation)
    }
    val myDropsSortOption = remember(myDropsSortKey) {
        runCatching { DropSortOption.valueOf(myDropsSortKey) }
            .getOrDefault(DropSortOption.NEWEST)
    }
    val filteredMyDrops = remember(selectedExplorerGroupCode, myDrops, explorerGroups) {
        selectedExplorerGroupCode?.takeIf { code -> explorerGroups.any { it.code == code } }?.let { code ->
            myDrops.filter { drop -> drop.groupCode == code }
        } ?: myDrops
    }
    val sortedMyDrops = remember(filteredMyDrops, myDropsSortOption, myDropsCurrentLocation) {
        sortDrops(filteredMyDrops, myDropsSortOption, myDropsCurrentLocation)
    }
    var collectedSortKey by rememberSaveable { mutableStateOf(DropSortOption.NEWEST.name) }
    val collectedSortOption = remember(collectedSortKey) {
        runCatching { DropSortOption.valueOf(collectedSortKey) }
            .getOrDefault(DropSortOption.NEWEST)
    }
    var collectedCurrentLocation by remember { mutableStateOf<LatLng?>(null) }
    val filteredCollected = remember(selectedExplorerGroupCode, collectedNotes, explorerGroups) {
        selectedExplorerGroupCode?.takeIf { code -> explorerGroups.any { it.code == code } }?.let { code ->
            collectedNotes.filter { note -> note.groupCode == code }
        } ?: collectedNotes
    }
    val canViewNsfw = userProfile?.canViewNsfw() == true
    val visibleCollectedNotes = if (canViewNsfw) {
        filteredCollected
    } else {
        filteredCollected.filterNot { note -> note.isNsfw || note.nsfwLabels.isNotEmpty() }
    }
    val hiddenNsfwCollectedCount = filteredCollected.size - visibleCollectedNotes.size
    val sortedCollectedNotes = remember(visibleCollectedNotes, collectedSortOption, collectedCurrentLocation) {
        sortCollectedNotes(visibleCollectedNotes, collectedSortOption, collectedCurrentLocation)
    }

    LaunchedEffect(selectedExplorerGroupCode, sortedOtherDrops) {
        val current = otherDropsSelectedId
        if (current != null && sortedOtherDrops.none { drop -> drop.id == current }) {
            otherDropsSelectedId = sortedOtherDrops.firstOrNull()?.id
        }
    }

    LaunchedEffect(selectedExplorerGroupCode, sortedMyDrops) {
        val current = myDropsSelectedId
        if (current != null && sortedMyDrops.none { drop -> drop.id == current }) {
            myDropsSelectedId = sortedMyDrops.firstOrNull()?.id
        }
    }

    LaunchedEffect(currentHomeDestination, currentExplorerDestination) {
        val shouldUpdateLocation = currentHomeDestination == HomeDestination.Explorer &&
                currentExplorerDestination == ExplorerDestination.Collected
        collectedCurrentLocation = if (shouldUpdateLocation) {
            getLatestLocation()?.let { (lat, lng) -> LatLng(lat, lng) }
        } else {
            null
        }
    }

    LaunchedEffect(
        isBusinessUser,
        currentHomeDestination,
        showBusinessDashboard,
        businessDashboardRefreshToken,
        currentUserId
    ) {
        val userId = currentUserId
        val shouldFetch = isBusinessUser && !userId.isNullOrBlank() &&
                (showBusinessDashboard || currentHomeDestination == HomeDestination.Business)

        if (shouldFetch) {
            if (showBusinessDashboard) {
                businessDashboardLoading = true
                businessDashboardError = null
            }

            try {
                businessDrops = repo.getBusinessDrops(userId!!)
            } catch (error: Exception) {
                if (showBusinessDashboard) {
                    businessDashboardError = error.localizedMessage ?: "Couldn't load your dashboard."
                }
            } finally {
                if (showBusinessDashboard) {
                    businessDashboardLoading = false
                }
            }
        } else {
            if (!showBusinessDashboard) {
                businessDashboardLoading = false
                businessDashboardError = null
            }

            if (!isBusinessUser || userId.isNullOrBlank()) {
                businessDrops = emptyList()
            }
        }
    }

    val businessHomeMetrics = remember(
        isBusinessUser,
        businessDrops,
        myDrops,
        myDropCountHint,
        myDropPendingReviewHint
    ) {
        if (!isBusinessUser) {
            BusinessHomeMetrics.Empty
        } else {
            deriveBusinessHomeMetrics(
                businessDrops = businessDrops,
                fallbackDrops = myDrops,
                myDropCountHint = myDropCountHint,
                myDropPendingReviewHint = myDropPendingReviewHint
            )
        }
    }

    var topBarHeightPx by remember { mutableStateOf(0) }
    var explorerNavigationHeightPx by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .onSizeChanged { size -> topBarHeightPx = size.height }
                .zIndex(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        modifier = Modifier.fillMaxWidth(),
                        title = { GeoDropHeader() },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            actionIconContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    if (currentHomeDestination == HomeDestination.Explorer && userMode != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { size -> explorerNavigationHeightPx = size.height }
                        ) {
                            ExplorerDestinationTabs(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 4.dp),
                                current = effectiveExplorerDestination,
                                onSelect = { destination -> openExplorerDestination(destination) },
                                showMyDrops = hasExplorerAccount,
                                showCollected = hasExplorerAccount
                            )
                        }
                    } else {
                        SideEffect { explorerNavigationHeightPx = 0 }
                    }
                }
            }
            if (isBusinessUser) {
                Divider()
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.height(56.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    val labelSpacingModifier = Modifier.offset(y = (-4).dp)
                    val navigationBarScope = this
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        navigationBarScope.NavigationBarItem(
                            modifier = Modifier.fillMaxSize(),
                            selected = showAccountMenu,
                            onClick = {
                                showGroupMenu = false
                                showAccountMenu = !showAccountMenu
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = stringResource(R.string.content_description_account_options)
                                )
                            },
                            label = { Text(stringResource(R.string.bottom_nav_profile), modifier = labelSpacingModifier) },
                            alwaysShowLabel = true
                        )

                        DropdownMenu(
                            expanded = showAccountMenu,
                            onDismissRequest = { showAccountMenu = false },
                            modifier = Modifier.zIndex(1f)
                        ) {
                            when (userMode) {
                                UserMode.GUEST -> {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_sign_in_full_participation)) },
                                        leadingIcon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null) },
                                        onClick = {
                                            showAccountMenu = false
                                            guestModeEnabled = false
                                            openAccountAuthDialog(
                                                initialType = AccountType.EXPLORER,
                                                initialMode = AccountAuthMode.SIGN_IN,
                                                lockAccountType = true
                                            )
                                        }
                                    )
                                }

                                UserMode.SIGNED_IN -> {
                                    if (!isBusinessUser) {
                                        val explorerUsername = userProfile?.username?.takeIf { it.isNotBlank() }
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    explorerUsername?.let {
                                                        stringResource(
                                                            R.string.menu_edit_username_with_value,
                                                            it
                                                        )
                                                    } ?: stringResource(R.string.menu_set_username)
                                                )
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                            onClick = {
                                                showAccountMenu = false
                                                explorerProfileError = null
                                                explorerProfileSubmitting = false
                                                explorerUsernameField = TextFieldValue(explorerUsername.orEmpty())
                                                showExplorerProfile = true
                                            }
                                        )
                                    }
                                }
                            }

                            if (canParticipate) {
                                val nsfwEnabled = userProfile?.canViewNsfw() == true
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(
                                                R.string.menu_notification_radius,
                                                notificationRadius.roundToInt()
                                            )
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.Map, contentDescription = null) },
                                    onClick = {
                                        showAccountMenu = false
                                        showNotificationRadiusDialog = true
                                    }
                                )
                                if (!isBusinessUser) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(
                                                    if (nsfwEnabled) {
                                                        R.string.menu_disable_nsfw_drops
                                                    } else {
                                                        R.string.menu_enable_nsfw_drops
                                                    }
                                                )
                                            )
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.Flag, contentDescription = null) },
                                        onClick = {
                                            showAccountMenu = false
                                            nsfwUpdateError = null
                                            showNsfwDialog = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(
                                                if (signingOut) R.string.status_signing_out else R.string.menu_sign_out
                                            )
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.Logout, contentDescription = null) },
                                    enabled = !signingOut,
                                    onClick = { handleSignOut() }
                                )
                            }
                        }
                    }

                    NavigationBarItem(
                        modifier = Modifier.weight(1f),
                        selected = showDropComposer,
                        onClick = {
                            if (isSubmitting) return@NavigationBarItem
                            showAccountMenu = false
                            showGroupMenu = false
                            if (!canParticipate) {
                                snackbar.showMessage(scope, participationRestriction("share drops"))
                                return@NavigationBarItem
                            }
                            showDropComposer = true
                        },
                        icon = { Icon(Icons.Rounded.Place, contentDescription = null) },
                        label = {
                            Text(
                                modifier = labelSpacingModifier,
                                text = stringResource(
                                    if (isSubmitting) R.string.status_dropping else R.string.action_drop_something
                                )
                            )
                        },
                        enabled = !isSubmitting,
                        alwaysShowLabel = true
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        navigationBarScope.NavigationBarItem(
                            modifier = Modifier.fillMaxSize(),
                            selected = showGroupMenu,
                            onClick = {
                                if (userMode == UserMode.GUEST) {
                                    snackbar.showMessage(scope, participationRestriction("manage groups"))
                                    return@NavigationBarItem
                                }
                                showAccountMenu = false
                                showGroupMenu = !showGroupMenu
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Groups,
                                    contentDescription = stringResource(R.string.manage_groups)
                                )
                            },
                            label = { Text(stringResource(R.string.manage_groups), modifier = labelSpacingModifier) },
                            alwaysShowLabel = true
                        )

                        DropdownMenu(
                            expanded = showGroupMenu,
                            onDismissRequest = { showGroupMenu = false },
                            modifier = Modifier.zIndex(1f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Create/Subscribe") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.AddCircle,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showGroupMenu = false
                                    showAccountMenu = false
                                    showManageGroups = true
                                }
                            )

                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            val activeGroupSelection = selectedExplorerGroupCode?.takeIf { code ->
                                joinedGroups.any { it.code == code }
                            }

                            DropdownMenuItem(
                                text = { Text("All groups") },
                                leadingIcon = if (activeGroupSelection == null) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null
                                        )
                                    }
                                } else {
                                    null
                                },
                                onClick = {
                                    showGroupMenu = false
                                    selectedHomeDestination = HomeDestination.Explorer.name
                                    selectedExplorerGroupCode = null
                                }
                            )

                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = "Owned groups",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (createdGroups.isEmpty()) {
                                Text(
                                    text = "You haven't created any groups yet",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                createdGroups.forEach { membership ->
                                    val isSelected = activeGroupSelection == membership.code
                                    DropdownMenuItem(
                                        text = { Text(membership.code) },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                        onClick = {
                                            showGroupMenu = false
                                            selectedHomeDestination = HomeDestination.Explorer.name
                                            selectedExplorerGroupCode = membership.code
                                        }
                                    )
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = "Subscribed groups",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (subscribedGroups.isEmpty()) {
                                Text(
                                    text = "You're not subscribed to any groups yet",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                subscribedGroups.forEach { membership ->
                                    val isSelected = activeGroupSelection == membership.code
                                    DropdownMenuItem(
                                        text = { Text(membership.code) },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                        onClick = {
                                            showGroupMenu = false
                                            selectedHomeDestination = HomeDestination.Explorer.name
                                            selectedExplorerGroupCode = membership.code
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            snackbarHost = {}
        ) { innerPadding ->
            val layoutDirection = LocalLayoutDirection.current
            val topPadding = innerPadding.calculateTopPadding()
            val bottomPadding = innerPadding.calculateBottomPadding()
            val startPadding = innerPadding.calculateStartPadding(layoutDirection)
            val endPadding = innerPadding.calculateEndPadding(layoutDirection)
            val density = LocalDensity.current
            val topPaddingPx = with(density) { topPadding.toPx() }
            val headerHeightPx = (topBarHeightPx - explorerNavigationHeightPx).coerceAtLeast(0)
            val navAwareTopPaddingPx = max(topPaddingPx, headerHeightPx.toFloat())
            val navAwareTopPadding = with(density) { navAwareTopPaddingPx.toDp() }
            val mapAwareTopPaddingPx = max(navAwareTopPaddingPx, topBarHeightPx.toFloat())
            val mapAwareTopPadding = with(density) { mapAwareTopPaddingPx.toDp() }

            if (isBusinessUser && currentHomeDestination == HomeDestination.Business) {
                BusinessHomeScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = startPadding,
                            top = navAwareTopPadding,
                            end = endPadding,
                            bottom = bottomPadding
                        ),
                    businessName = userProfile?.businessName,
                    businessCategories = businessCategories,
                    statusMessage = status,
                    metrics = businessHomeMetrics,
                    onViewDashboard = {
                        if (!userProfileLoading) {
                            showBusinessDashboard = true
                        }
                    },
                    onUpdateBusinessProfile = { showBusinessOnboarding = true },
                    onViewMyDrops = { openExplorerDestination(ExplorerDestination.MyDrops) }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = startPadding,
                            end = endPadding,
                            bottom = bottomPadding
                        )
                ) {
                    when (effectiveExplorerDestination) {
                    ExplorerDestination.Discover -> {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            if (readOnlyParticipationMessage != null) {
                                Spacer(Modifier.height(navAwareTopPadding))
                                Box(Modifier.padding(horizontal = 20.dp)) {
                                    ReadOnlyModeCard(message = readOnlyParticipationMessage)
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    OtherDropsExplorerSection(
                                        modifier = Modifier.fillMaxSize(),
                                        topContentPadding = mapAwareTopPadding,
                                        loading = otherDropsLoading,
                                        drops = sortedOtherDrops,
                                        currentLocation = otherDropsCurrentLocation,
                                        notificationRadiusMeters = notificationRadius,
                                        error = otherDropsError,
                                        emptyMessage = selectedExplorerGroupCode?.let { code ->
                                            "No drops for $code yet."
                                        },
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
                                        canCollectDrops = canParticipate,
                                        collectRestrictionMessage = when (userMode) {
                                            UserMode.GUEST -> "Preview drops nearby, then create an account to pick them up when you're ready."
                                            UserMode.SIGNED_IN -> null
                                        },
                                        sortOption = otherDropsSortOption,
                                        sortOptions = dropSortOptions,
                                        onSortOptionChange = { option ->
                                            otherDropsSortKey = option.name
                                        },
                                        canLikeDrops = canParticipate,
                                        likeRestrictionMessage = if (canParticipate) null else participationRestriction("react to drops"),
                                        onLike = { drop, status -> submitLike(drop, status) },
                                        onReport = report@{ drop ->
                                            if (browseReportProcessing) return@report
                                            val userId = currentUserId
                                            if (userId.isNullOrBlank()) {
                                                Toast.makeText(ctx, "Sign in to report drops.", Toast.LENGTH_SHORT).show()
                                                return@report
                                            }
                                            if (drop.createdBy == userId) {
                                                Toast.makeText(ctx, "You can't report your own drop.", Toast.LENGTH_SHORT).show()
                                                return@report
                                            }
                                            val hasCollected = collectedDropIds.contains(drop.id)
                                            val withinPickupRange = otherDropsCurrentLocation?.let { location ->
                                                distanceBetweenMeters(
                                                    location.latitude,
                                                    location.longitude,
                                                    drop.lat,
                                                    drop.lng
                                                ) <= DROP_PICKUP_RADIUS_METERS
                                            } ?: false
//                                            if (!hasCollected && !withinPickupRange) {
//                                                val radiusMeters = DROP_PICKUP_RADIUS_METERS.roundToInt()
//                                                Toast.makeText(
//                                                    ctx,
//                                                    "Move within ${'$'}radiusMeters meters to report this drop, or collect it first.",
//                                                    Toast.LENGTH_SHORT
//                                                ).show()
//                                                return@report
//                                            }
                                            if (drop.reportedBy.containsKey(userId)) {
                                                Toast.makeText(ctx, "You've already reported this drop.", Toast.LENGTH_SHORT).show()
                                                return@report
                                            }
                                            browseReportTarget = drop.toReportableDrop(source = REPORT_SOURCE_BROWSE_MAP)
                                            browseReportSelectedReasons = emptySet()
                                            browseReportError = null
                                            browseReportDialogOpen = true
                                        },
                                        reportingDropId = browseReportingDropId,
                                        dismissedBrowseDropIds = dismissedBrowseDropIds,
                                        snackbar = snackbar,
                                        scope = scope,
                                        onRefresh = { otherDropsRefreshToken += 1 },
                                        listState = otherDropsListState,
                                        mapWeight = otherDropsMapWeight,
                                        onMapWeightChange = { weight ->
                                            val coerced = weight.coerceIn(MAP_LIST_MIN_WEIGHT, MAP_LIST_MAX_WEIGHT)
                                            if (coerced != otherDropsMapWeight) {
                                                otherDropsMapWeight = coerced
                                            }
                                        }
                                    )
                                }
                            }

                            status?.let { message ->
                                Box(Modifier.padding(horizontal = 20.dp)) {
                                    StatusCard(message = message)
                                }
                            }
                        }
                    }

                    ExplorerDestination.MyDrops -> {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                MyDropsContent(
                                    modifier = Modifier.fillMaxSize(),
                                    topContentPadding = mapAwareTopPadding,
                                    contentPadding = PaddingValues(bottom = 0.dp),
                                    loading = myDropsLoading,
                                    drops = sortedMyDrops,
                                    currentLocation = myDropsCurrentLocation,
                                    deletingId = myDropsDeletingId,
                                    error = myDropsError,
                                    emptyMessage = selectedExplorerGroupCode?.let { code ->
                                        "You haven't dropped anything for $code yet."
                                    },
                                    selectedId = myDropsSelectedId,
                                    sortOption = myDropsSortOption,
                                    sortOptions = dropSortOptions,
                                    onSortOptionChange = { option ->
                                        myDropsSortKey = option.name
                                    },
                                    onSelect = { drop ->
                                        myDropsSelectedId = if (myDropsSelectedId == drop.id) {
                                            null
                                        } else {
                                            drop.id
                                        }
                                    },
                                    onRetry = { myDropsRefreshToken += 1 },
                                    onView = { drop ->
                                    val intent = Intent(ctx, DropDetailActivity::class.java).apply {
                                        putExtra("dropId", drop.id)
                                        if (drop.text.isNotBlank()) putExtra("dropText", drop.text)
                                        drop.description?.takeIf { it.isNotBlank() }?.let { putExtra("dropDescription", it) }
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
                                        putExtra("dropLikeCount", drop.likeCount)
                                        putExtra("dropDislikeCount", drop.dislikeCount)
                                        val userId = currentUserId
                                        when (drop.userLikeStatus(userId)) {
                                            DropLikeStatus.LIKED -> putExtra("dropIsLiked", true)
                                            DropLikeStatus.DISLIKED -> putExtra("dropIsDisliked", true)
                                            DropLikeStatus.NONE -> Unit
                                        }
                                        putExtra("dropIsNsfw", drop.isNsfw)
                                        if (drop.nsfwLabels.isNotEmpty()) {
                                            putStringArrayListExtra("dropNsfwLabels", ArrayList(drop.nsfwLabels))
                                        }
                                        drop.decayDays?.let { putExtra("dropDecayDays", it) }
                                    }
                                    ctx.startActivity(intent)
                                },
                                    onDelete = { drop ->
                                        if (drop.id.isBlank()) {
                                            snackbar.showMessage(scope, "Unable to delete this drop.")
                                            return@MyDropsContent
                                        }

                                        myDropsDeletingId = drop.id
                                        scope.launch {
                                            try {
                                                repo.deleteDrop(drop.id)
                                                val updated = myDrops.filterNot { it.id == drop.id }
                                                myDrops = updated
                                                myDropCountHint = updated.size
                                                myDropPendingReviewHint = updated.count { it.reportCount > 0 }
                                                if (myDropsSelectedId == drop.id) {
                                                    myDropsSelectedId = updated.firstOrNull()?.id
                                                }
                                                snackbar.showMessage(scope, "Drop deleted.")
                                            } catch (e: Exception) {
                                                snackbar.showMessage(scope, "Error: ${'$'}{e.message}")
                                            } finally {
                                                myDropsDeletingId = null
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    ExplorerDestination.Collected -> {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                CollectedDropsContent(
                                    modifier = Modifier.fillMaxSize(),
                                    topContentPadding = mapAwareTopPadding,
                                    contentPadding = PaddingValues(bottom = 0.dp),
                                    notes = sortedCollectedNotes,
                                    hiddenNsfwCount = hiddenNsfwCollectedCount,
                                    canReportDrops = !currentUserId.isNullOrBlank(),
                                    reportedDropIds = reportedCollectedDropIds.toSet(),
                                    reportingDropId = browseReportingDropId,
                                    isReportProcessing = browseReportProcessing,
                                    emptyMessage = selectedExplorerGroupCode?.let { code ->
                                        "You haven't collected any drops for $code yet."
                                    },
                                    sortOption = collectedSortOption,
                                    sortOptions = dropSortOptions,
                                    onSortOptionChange = { option ->
                                        collectedSortKey = option.name
                                    },
                                    onReport = report@{ note ->
                                        if (browseReportProcessing) return@report
                                        val userId = currentUserId
                                        if (userId.isNullOrBlank()) {
                                            Toast.makeText(ctx, "Sign in to report drops.", Toast.LENGTH_SHORT).show()
                                            return@report
                                        }
                                        if (reportedCollectedDropIds.contains(note.id)) {
                                            Toast.makeText(ctx, "You've already reported this drop.", Toast.LENGTH_SHORT).show()
                                            return@report
                                        }
                                        browseReportTarget = note.toReportableDrop(source = REPORT_SOURCE_COLLECTED)
                                        browseReportSelectedReasons = emptySet()
                                        browseReportError = null
                                        browseReportDialogOpen = true
                                    },
                                    onView = { note ->
                                        val intent = Intent(ctx, DropDetailActivity::class.java).apply {
                                            putExtra("dropId", note.id)
                                            if (note.text.isNotBlank()) putExtra("dropText", note.text)
                                            note.description?.takeIf { it.isNotBlank() }?.let {
                                                putExtra("dropDescription", it)
                                            }
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
                                            putExtra("dropLikeCount", note.likeCount)
                                            putExtra("dropDislikeCount", note.dislikeCount)
                                            if (note.isLiked) {
                                                putExtra("dropIsLiked", true)
                                            }
                                            if (note.isDisliked) {
                                                putExtra("dropIsDisliked", true)
                                            }
                                            putExtra("dropIsNsfw", note.isNsfw)
                                            if (note.nsfwLabels.isNotEmpty()) {
                                                putStringArrayListExtra("dropNsfwLabels", ArrayList(note.nsfwLabels))
                                            }
                                            note.decayDays?.let { putExtra("dropDecayDays", it) }
                                        }
                                        ctx.startActivity(intent)
                                    },
                                    onRemove = { note ->
                                        noteInventory.removeCollected(note.id)
                                        collectedNotes = noteInventory.getCollectedNotes()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .zIndex(1f)
            )
    }

    if (showDropComposer) {
        DropComposerDialog(
            isSubmitting = isSubmitting,
            isBusinessUser = userProfile?.isBusiness() == true,
            businessName = userProfile?.businessName,
            businessCategories = businessCategories,
            userProfileLoading = userProfileLoading,
            userProfileError = userProfileError,
            dropType = dropType,
            onDropTypeChange = { dropType = it },
            dropContentType = dropContentType,
            onDropContentTypeChange = { dropContentType = it },
            note = note,
            onNoteChange = { note = it },
            description = description,
            onDescriptionChange = { description = it },
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
            dropAnonymously = dropAnonymously,
            onDropAnonymouslyChange = { dropAnonymously = it },
            groupCodeInput = groupCodeInput,
            onGroupCodeInputChange = { groupCodeInput = it },
            joinedGroups = createdGroups.map { it.code },
            onSelectGroupCode = { code -> groupCodeInput = TextFieldValue(code) },
            redemptionCodeInput = redemptionCodeInput,
            onRedemptionCodeChange = { redemptionCodeInput = it },
            redemptionLimitInput = redemptionLimitInput,
            onRedemptionLimitChange = { redemptionLimitInput = it },
            decayDaysInput = decayDaysInput,
            onDecayDaysChange = { decayDaysInput = it },
            onManageGroupCodes = { showManageGroups = true },
            onSubmit = { submitDrop() },
            onDismiss = {
                if (!isSubmitting) {
                    showDropComposer = false
                }
            }
        )
    }

    if (browseReportDialogOpen) {
        val targetDrop = browseReportTarget
        ReportDropDialog(
            reasons = dropReportReasons,
            selectedReasons = browseReportSelectedReasons,
            onReasonToggle = { code ->
                browseReportSelectedReasons = if (browseReportSelectedReasons.contains(code)) {
                    browseReportSelectedReasons - code
                } else {
                    browseReportSelectedReasons + code
                }
            },
            onDismiss = {
                if (!browseReportProcessing) {
                    browseReportDialogOpen = false
                    browseReportTarget = null
                    browseReportSelectedReasons = emptySet()
                    browseReportError = null
                    browseReportingDropId = null
                }
            },
            onSubmit = submit@{
                val userId = currentUserId
                val target = targetDrop
                if (userId.isNullOrBlank()) {
                    Toast.makeText(ctx, "Sign in to report drops.", Toast.LENGTH_SHORT).show()
                    return@submit
                }
                if (target == null || target.id.isBlank()) {
                    browseReportError = "Drop information is missing."
                    return@submit
                }
                if (browseReportSelectedReasons.isEmpty()) {
                    browseReportError = "Select at least one reason."
                    return@submit
                }
                browseReportProcessing = true
                browseReportError = null
                browseReportingDropId = target.id
                scope.launch {
                    try {
                        repo.submitDropReport(
                            dropId = target.id,
                            reporterId = userId,
                            reasonCodes = browseReportSelectedReasons,
                            additionalContext = mapOf(
                                "source" to target.source,
                                "contentType" to target.contentType.name,
                                "hasMedia" to target.hasMedia,
                                "dropType" to target.dropType.name
                            )
                        )
                        browseReportDialogOpen = false
                        browseReportTarget = null
                        browseReportSelectedReasons = emptySet()
                        browseReportError = null
                        val now = System.currentTimeMillis()
                        when (target.source) {
                            REPORT_SOURCE_BROWSE_MAP -> {
                                otherDrops = otherDrops.map { existing ->
                                    if (existing.id == target.id) {
                                        val updatedReportedBy = existing.reportedBy.toMutableMap()
                                        val already = updatedReportedBy.containsKey(userId)
                                        updatedReportedBy[userId] = now
                                        val updatedCount = if (already) {
                                            existing.reportCount
                                        } else {
                                            existing.reportCount + 1
                                        }
                                        existing.copy(
                                            reportedBy = updatedReportedBy,
                                            reportCount = updatedCount
                                        )
                                    } else {
                                        existing
                                    }
                                }
                            }

                            REPORT_SOURCE_COLLECTED -> {
                                if (!reportedCollectedDropIds.contains(target.id)) {
                                    reportedCollectedDropIds.add(target.id)
                                }
                            }

                            else -> Unit
                        }
                        Toast.makeText(ctx, "Report submitted.", Toast.LENGTH_SHORT).show()
                    } catch (error: Exception) {
                        val message = error.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: "Couldn't submit report. Try again."
                        browseReportError = message
                    } finally {
                        browseReportProcessing = false
                        browseReportingDropId = null
                    }
                }
            },
            isSubmitting = browseReportProcessing,
            errorMessage = browseReportError
        )
    }

    if (showExplorerProfile) {
        ExplorerProfileDialog(
            currentUsername = userProfile?.username,
            username = explorerUsernameField,
            onUsernameChange = { explorerUsernameField = it },
            isSubmitting = explorerProfileSubmitting,
            error = explorerProfileError,
            onSubmit = { saveExplorerUsername() },
            onDismiss = {
                if (!explorerProfileSubmitting) {
                    showExplorerProfile = false
                }
            }
        )
    }

    if (showNotificationRadiusDialog) {
        NotificationRadiusDialog(
            initialRadius = notificationRadius,
            onConfirm = { newRadius ->
                notificationRadius = newRadius
                notificationPrefs.setNotificationRadiusMeters(newRadius)
                showNotificationRadiusDialog = false
                snackbar.showMessage(
                    scope,
                    "We'll alert you to drops within ${newRadius.roundToInt()} meters."
                )
                registrar.registerNearby(
                    ctx,
                    maxMeters = newRadius,
                    groupCodes = groupPrefs.getMemberships().map { it.code }.toSet()
                )
            },
            onDismiss = { showNotificationRadiusDialog = false }
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
                        otherDropsRefreshToken += 1
                        showNsfwDialog = false
                        val message = if (enable) {
                            "NSFW drops enabled."
                        } else {
                            "NSFW drops disabled."
                        }
                        snackbar.showMessage(scope, message)
                        registrar.registerNearby(
                            ctx,
                            maxMeters = notificationRadius,
                            groupCodes = groupPrefs.getMemberships().map { it.code }.toSet()
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

    if (showBusinessOnboarding) {
        var businessNameField by rememberSaveable(
            userProfile?.businessName,
            stateSaver = TextFieldValue.Saver
        ) {
            mutableStateOf(TextFieldValue(userProfile?.businessName.orEmpty()))
        }
        var onboardingError by remember { mutableStateOf<String?>(null) }
        var onboardingSubmitting by remember { mutableStateOf(false) }
        val categoriesKey = userProfile?.businessCategories
            ?.joinToString(separator = ",") { it.id }
        var selectedCategoryIds by remember(categoriesKey) {
            mutableStateOf(userProfile?.businessCategories?.map { it.id } ?: emptyList())
        }

        BusinessOnboardingDialog(
            name = businessNameField,
            selectedCategories = selectedCategoryIds
                .mapNotNull { id -> BusinessCategory.fromId(id) }
                .toSet(),
            onNameChange = { businessNameField = it },
            onToggleCategory = { category ->
                selectedCategoryIds = if (selectedCategoryIds.contains(category.id)) {
                    selectedCategoryIds.filterNot { it == category.id }
                } else {
                    selectedCategoryIds + category.id
                }
            },
            isSubmitting = onboardingSubmitting,
            error = onboardingError,
            onSubmit = {
                val trimmed = businessNameField.text.trim()
                if (trimmed.isEmpty()) {
                    onboardingError = "Enter your business name."
                    return@BusinessOnboardingDialog
                }
                val selectedCategories = selectedCategoryIds
                    .mapNotNull { id -> BusinessCategory.fromId(id) }
                if (selectedCategories.isEmpty()) {
                    onboardingError = "Select at least one business category."
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
                        val updated = repo.updateBusinessProfile(uid, trimmed, selectedCategories)
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
            snackbarHostState = manageGroupsSnackbar,
            onDismiss = {
                showManageGroups = false
                showGroupMenu = false
            },
            onCreate = { code ->
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid.isNullOrBlank()) {
                    manageGroupsSnackbar.showMessage(scope, "Sign in to manage groups.")
                    return@ManageGroupsDialog
                }
                scope.launch {
                    val normalized = GroupPreferences.normalizeGroupCode(code) ?: return@launch
                    try {
                        val membership = repo.joinGroup(
                            uid,
                            normalized,
                            allowCreateIfMissing = true
                        )
                        groupPrefs.addGroup(membership)
                        joinedGroups = groupPrefs.getMemberships()
                        if (dropVisibility == DropVisibility.GroupOnly && membership.role == GroupRole.OWNER) {
                            groupCodeInput = TextFieldValue(normalized)
                        }
                        val message = if (membership.role == GroupRole.OWNER) {
                            "Created group $normalized"
                        } else {
                            "Subscribed to $normalized"
                        }
                        manageGroupsSnackbar.showMessage(scope, message)
                    } catch (error: Exception) {
                        val message = when (error) {
                            is GroupAlreadyExistsException ->
                                "Group ${error.code} already exist."
                            is GroupNotFoundException ->
                                "Group $normalized doesn't exist. Ask the creator to share it once it's ready."
                            else -> error.localizedMessage ?: "Couldn't save group $normalized"
                        }
                        manageGroupsSnackbar.showMessage(scope, message)
                    }
                }
            },
            onSubscribe = { code ->
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid.isNullOrBlank()) {
                    manageGroupsSnackbar.showMessage(scope, "Sign in to manage groups.")
                    return@ManageGroupsDialog
                }
                scope.launch {
                    val normalized = GroupPreferences.normalizeGroupCode(code) ?: return@launch
                    try {
                        val membership = repo.joinGroup(
                            uid,
                            normalized,
                            allowCreateIfMissing = false
                        )
                        groupPrefs.addGroup(membership)
                        joinedGroups = groupPrefs.getMemberships()
                        if (dropVisibility == DropVisibility.GroupOnly && membership.role == GroupRole.OWNER) {
                            groupCodeInput = TextFieldValue(normalized)
                        }
                        val message = if (membership.role == GroupRole.OWNER) {
                            "Created group $normalized"
                        } else {
                            "Subscribed to $normalized"
                        }
                        manageGroupsSnackbar.showMessage(scope, message)
                    } catch (error: Exception) {
                        val message = when (error) {
                            is GroupNotFoundException ->
                                "Group $normalized doesn't exist. Ask the creator to share it once it's ready."
                            else -> error.localizedMessage ?: "Couldn't save group $normalized"
                        }
                        manageGroupsSnackbar.showMessage(scope, message)
                    }
                }
            },
            onRemove = { code ->
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid.isNullOrBlank()) {
                    manageGroupsSnackbar.showMessage(scope, "Sign in to manage groups.")
                    return@ManageGroupsDialog
                }
                scope.launch {
                    val normalized = GroupPreferences.normalizeGroupCode(code) ?: return@launch
                    try {
                        repo.leaveGroup(uid, normalized)
                        groupPrefs.removeGroup(normalized)
                        joinedGroups = groupPrefs.getMemberships()
                        val currentInput = GroupPreferences.normalizeGroupCode(groupCodeInput.text)
                        if (currentInput == normalized) {
                            groupCodeInput = TextFieldValue("")
                        }
                        manageGroupsSnackbar.showMessage(scope, "Removed group $normalized")
                    } catch (error: Exception) {
                        val message = error.localizedMessage ?: "Couldn't remove group $normalized"
                        manageGroupsSnackbar.showMessage(scope, message)
                    }
                }
            }
        )
    }
}

}

@Composable
private fun TermsAcceptanceScreen(
    onAccept: () -> Unit,
    onExit: () -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableStateOf(0) }
    val agreementText = remember(selectedTab) {
        if (selectedTab == 0) TERMS_OF_SERVICE_TEXT else PRIVACY_POLICY_TEXT
    }

    LaunchedEffect(selectedTab) {
        scrollState.scrollTo(0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Welcome to GeoDrop",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = TERMS_PRIVACY_SUMMARY,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider()
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TERMS_PRIVACY_TABS.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 360.dp)
                        .verticalScroll(scrollState)
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = agreementText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                }
                Text(
                    text = "By tapping Accept & Continue you agree to these terms and policies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onExit) {
                        Text("Exit app")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onAccept) {
                        Text("Accept & Continue")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FirstRunOnboardingScreen(
    onContinue: () -> Unit,
    onExit: () -> Unit
) {
    val slides = remember {
        listOf(
            OnboardingSlide(
                icon = Icons.Rounded.Map,
                title = "Discover nearby drops",
                description = "See stories, rewards, and community posts pinned to real-world locations around you."
            ),
            OnboardingSlide(
                icon = Icons.Rounded.Place,
                title = "Collect and redeem",
                description = "Walk up to a drop to unlock it, save it to your inventory, and redeem special offers in person."
            ),
            OnboardingSlide(
                icon = Icons.Rounded.Storefront,
                title = "Share your own moments",
                description = "Create drops with photos, audio, or coupons so nearby explorers can discover your business or story."
            )
        )
    }
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Welcome to GeoDrop",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onContinue) {
                        Text("Skip")
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val slide = slides[page]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(140.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = slide.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                        }

                        Text(
                            text = slide.title,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = slide.description,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    slides.forEachIndexed { index, _ ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(if (isSelected) 24.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage == slides.lastIndex) {
                            onContinue()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (pagerState.currentPage == slides.lastIndex) "Continue" else "Next")
                }
            }
        }

        TextButton(
            onClick = onExit,
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Text("Exit app")
        }
    }
}

private data class OnboardingSlide(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private const val TERMS_PRIVACY_SUMMARY =
    "GeoDrop uses your location and saved preferences to help you discover nearby drops. " +
            "Use the tabs below to review our Terms of Service and Privacy Policy, then accept to continue."

private val TERMS_PRIVACY_TABS = listOf(
    "Terms of Service",
    "Privacy Policy"
)

private val TERMS_OF_SERVICE_TEXT = """
📜 GeoDrop – Terms of Service
Last updated: 10/02/2025
Welcome to GeoDrop! By using our app, you agree to the following:

1. Use of the App
You may use GeoDrop to create and discover location-based messages, media, or coupons (“drops”).
You agree not to post harmful, illegal, hateful, or malicious content.
You agree not to spam, harass, or misuse the service.

2. Location Services
GeoDrop uses your device’s location to notify you of nearby drops.
You must grant location permissions for the app to function properly.

3. User Content
You are responsible for any content you drop.
GeoDrop may remove content that violates these terms.
Coupons, promotions, or offers from businesses are managed by those businesses — GeoDrop is not responsible for their accuracy or fulfillment.

4. NSFW Content
GeoDrop includes an optional NSFW (Not Safe For Work) feature.
By enabling NSFW mode, you confirm you are at least 18 years old (or the age of majority in your country).
NSFW content may include mature, adult, or offensive material.
GeoDrop is not responsible for the nature of user-generated NSFW content.
Businesses are prohibited from posting NSFW coupons or promotions.

5. Accounts & Data
GeoDrop collects only the information needed to operate the service, such as your device ID, location (while you use the app), and any drops you create.
We never sell your personal data.
You may delete your account at any time from the in-app settings.

6. Business Accounts
Business users must keep their account information up to date.
Business users are responsible for honoring coupons or offers they distribute through GeoDrop.
GeoDrop may suspend business accounts that violate these terms or applicable laws.

7. Changes to the Terms
We may update these terms from time to time. If the changes are material, we'll notify you in the app.
Continuing to use GeoDrop after an update means you accept the revised terms.

8. Contact
Questions? Reach us at support@geodrop.app.

📍 Location Notice
GeoDrop relies on accurate GPS data. Turn on high accuracy mode for the best experience.

📢 Notifications
GeoDrop may send push notifications about nearby drops, reminders, or account updates. You can opt out in your device settings.

🔒 Data Security
We use industry-standard safeguards to protect your data. However, we cannot guarantee the security of information transmitted over the internet.

By accepting, you agree to follow these terms whenever you use GeoDrop.
""".trimIndent()

private val PRIVACY_POLICY_TEXT = """
🔐 GeoDrop – Privacy Policy
Last updated: 10/02/2025

1. Information We Collect
• Account basics: email address for explorer and business accounts.
• Location data: precise GPS coordinates while you use key features like the map and background alerts for nearby drops.
• Content you provide: text, media, and coupons that you create or redeem.
• Device data: app version, device model, and crash diagnostics.

2. How We Use Information
• Deliver core features such as finding and unlocking drops near you.
• Maintain the safety of the community by moderating content and preventing abuse.
• Provide analytics to improve app performance and plan future features.
• Notify you about nearby drops, redemption reminders, or account changes.

3. Sharing & Disclosure
• We never sell your personal information.
• Drop content is shared with other explorers in the area based on your privacy settings.
• Business partners only see analytics for drops they create.
• Service providers (like cloud hosting and crash reporting) process data on our behalf under strict confidentiality agreements.

4. Your Choices
• You can disable push notifications or location permissions at any time in system settings.
• You may delete drops you posted or remove collected notes from your inventory.
• Request account deletion or data export by emailing support@geodrop.app.

5. Data Retention
• Account and drop data are retained as long as your account is active.
• We keep minimal logs for fraud prevention, typically no longer than 30 days.

6. Children’s Privacy
• GeoDrop is not intended for children under 13.
• NSFW mode is restricted to users 18+ only.

7. Updates
• We will notify you in-app or via email (for business accounts) when this policy changes.

By accepting, you acknowledge that you have read and understood how GeoDrop handles your data.
""".trimIndent()

@Composable
private fun UserModeSelectionScreen(
    onSelectGuest: () -> Unit,
    onSelectExplorerSignIn: () -> Unit,
    onSelectBusinessSignIn: () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Choose how to explore",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "You can browse GeoDrop without an account or sign in to participate.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.Public, contentDescription = null)
                        Text(
                            text = "Continue as guest",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Read drop details without creating an account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onSelectGuest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start reading")
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = null)
                        Text(
                            text = "Sign in or create account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Choose the right sign-in experience for how you plan to use GeoDrop.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onSelectExplorerSignIn,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Explorer sign in")
                        }
                        OutlinedButton(
                            onClick = onSelectBusinessSignIn,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Business sign in")
                        }
                    }
                }
            }
        }
    }
}

private data class BusinessHomeMetrics(
    val liveDropCount: Int,
    val pendingReviewCount: Int,
    val unresolvedRedemptionCount: Int,
    val expiringOfferCount: Int
) {
    companion object {
        val Empty = BusinessHomeMetrics(0, 0, 0, 0)
    }
}

private val BUSINESS_EXPIRING_SOON_THRESHOLD_MILLIS = TimeUnit.DAYS.toMillis(3)

private fun deriveBusinessHomeMetrics(
    businessDrops: List<Drop>,
    fallbackDrops: List<Drop>,
    myDropCountHint: Int?,
    myDropPendingReviewHint: Int?
): BusinessHomeMetrics {
    val sourceDrops = when {
        businessDrops.isNotEmpty() -> businessDrops
        fallbackDrops.isNotEmpty() -> fallbackDrops
        else -> emptyList()
    }

    if (sourceDrops.isEmpty()) {
        return BusinessHomeMetrics(
            liveDropCount = myDropCountHint ?: 0,
            pendingReviewCount = myDropPendingReviewHint ?: 0,
            unresolvedRedemptionCount = 0,
            expiringOfferCount = 0
        )
    }

    val now = System.currentTimeMillis()
    val businessEntries = sourceDrops
        .filter { it.isBusinessDrop() && !it.isDeleted }

    if (businessEntries.isEmpty()) {
        return BusinessHomeMetrics(
            liveDropCount = myDropCountHint ?: 0,
            pendingReviewCount = myDropPendingReviewHint ?: 0,
            unresolvedRedemptionCount = 0,
            expiringOfferCount = 0
        )
    }

    val liveDropCount = businessEntries.count { !it.isExpired(now) }
    val pendingReviewCount = businessEntries.count { it.reportCount > 0 }
    val unresolvedRedemptionCount = businessEntries.count { drop ->
        if (drop.isExpired(now) || !drop.requiresRedemption()) return@count false
        val remaining = drop.remainingRedemptions()
        remaining == null || remaining > 0
    }
    val expiringOfferCount = businessEntries.count { drop ->
        val remaining = drop.remainingDecayMillis(now) ?: return@count false
        remaining in 1..BUSINESS_EXPIRING_SOON_THRESHOLD_MILLIS
    }

    return BusinessHomeMetrics(
        liveDropCount = liveDropCount,
        pendingReviewCount = pendingReviewCount,
        unresolvedRedemptionCount = unresolvedRedemptionCount,
        expiringOfferCount = expiringOfferCount
    )
}

@Composable
private fun BusinessHomeScreen(
    modifier: Modifier = Modifier,
    businessName: String?,
    businessCategories: List<BusinessCategory>,
    statusMessage: String?,
    metrics: BusinessHomeMetrics,
    onViewDashboard: () -> Unit,
    onUpdateBusinessProfile: () -> Unit,
    onViewMyDrops: () -> Unit,
) {
    val activeDropCount = metrics.liveDropCount
    val pendingReviewCount = metrics.pendingReviewCount
    val unresolvedRedemptionCount = metrics.unresolvedRedemptionCount
    val expiringOfferCount = metrics.expiringOfferCount
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            BusinessHeroCard(
                businessName = businessName,
                businessCategories = businessCategories
            )
        }

        item { SectionHeader(text = "Manage drops") }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(
                    icon = Icons.Rounded.Inbox,
                    title = "Manage existing drops",
                    description = "Review performance and make changes to the drops you've shared.",
                    onClick = onViewMyDrops,
                    trailingContent = {
                        BusinessActionSummary(
                            liveDropCount = activeDropCount,
                            pendingReviewCount = pendingReviewCount,
                            unresolvedRedemptionCount = unresolvedRedemptionCount,
                            expiringOfferCount = expiringOfferCount
                        )
                    }
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
                    onClick = onViewDashboard,
                    trailingContent = {
                        BusinessActionSummary(
                            liveDropCount = activeDropCount,
                            pendingReviewCount = pendingReviewCount,
                            unresolvedRedemptionCount = unresolvedRedemptionCount,
                            expiringOfferCount = expiringOfferCount
                        )
                    }
                )

                ActionCard(
                    icon = Icons.Rounded.Edit,
                    title = "Update business name",
                    description = "Adjust how your brand appears across GeoDrop experiences.",
                    onClick = onUpdateBusinessProfile
                )
            }
        }

        statusMessage?.let { message ->
            item { StatusCard(message = message) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BusinessHeroCard(
    businessName: String?,
    businessCategories: List<BusinessCategory>,
    brandImageUrl: String? = null
) {
    val title = businessName?.takeIf { it.isNotBlank() }?.let { "$it on GeoDrop" }
        ?: "Welcome to GeoDrop Business"
    val subtitle = if (businessName.isNullOrBlank()) {
        "Share exclusive offers, stories, and tours to reach explorers right when they're nearby."
    } else {
        "Keep explorers engaged with timely offers and experiences from your team."
    }

    val sortedCategories = businessCategories
        .sortedWith(compareBy({ it.group.displayName }, { it.displayName }))

    val avatarBackground = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)
    val avatarBorder = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
    val chipContainer = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val chipBorder = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(avatarBackground)
                        .border(width = 1.dp, color = avatarBorder, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!brandImageUrl.isNullOrBlank()) {
                        val context = LocalContext.current
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(brandImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = businessName?.let { "$it brand logo" },
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

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
            }

            if (sortedCategories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Business categories",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )

                    sortedCategories
                        .groupBy { it.group }
                        .forEach { (group, categories) ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = group.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    categories.forEach { category ->
                                        AssistChip(
                                            onClick = {},
                                            enabled = false,
                                            label = {
                                                Text(
                                                    text = category.displayName,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = chipContainer,
                                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                disabledContainerColor = chipContainer,
                                                disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = chipBorder
                                            )
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
private fun ReadOnlyModeCard(message: String) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
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
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
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
            }

            trailingContent?.let {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.Start)
                ) {
                    it()
                }
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
private fun MetricPill(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private enum class StatusChipTone { Accent, Warning }

@Composable
private fun StatusChip(
    text: String,
    tone: StatusChipTone,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor) = when (tone) {
        StatusChipTone.Accent -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        StatusChipTone.Warning -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BusinessActionSummary(
    liveDropCount: Int,
    pendingReviewCount: Int,
    unresolvedRedemptionCount: Int,
    expiringOfferCount: Int
) {
    if (liveDropCount <= 0 && pendingReviewCount <= 0 && unresolvedRedemptionCount <= 0 && expiringOfferCount <= 0) {
        return
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (liveDropCount > 0) {
            CountBadge(count = liveDropCount)
        }
        if (pendingReviewCount > 0) {
            MetricPill(
                label = stringResource(R.string.metric_pending_reviews),
                value = pendingReviewCount
            )
        }
        if (unresolvedRedemptionCount > 0) {
            MetricPill(
                label = stringResource(R.string.metric_open_redemptions),
                value = unresolvedRedemptionCount
            )
        }
        if (expiringOfferCount > 0) {
            StatusChip(
                text = stringResource(R.string.metric_expiring_offers, expiringOfferCount),
                tone = StatusChipTone.Warning
            )
        }
    }
}

@Composable
private fun StatusCard(message: String) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            ),
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
    businessCategories: List<BusinessCategory>,
    userProfileLoading: Boolean,
    userProfileError: String?,
    dropType: DropType,
    onDropTypeChange: (DropType) -> Unit,
    dropContentType: DropContentType,
    onDropContentTypeChange: (DropContentType) -> Unit,
    note: TextFieldValue,
    onNoteChange: (TextFieldValue) -> Unit,
    description: TextFieldValue,
    onDescriptionChange: (TextFieldValue) -> Unit,
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
    dropAnonymously: Boolean,
    onDropAnonymouslyChange: (Boolean) -> Unit,
    groupCodeInput: TextFieldValue,
    onGroupCodeInputChange: (TextFieldValue) -> Unit,
    joinedGroups: List<String>,
    onSelectGroupCode: (String) -> Unit,
    redemptionCodeInput: TextFieldValue,
    onRedemptionCodeChange: (TextFieldValue) -> Unit,
    redemptionLimitInput: TextFieldValue,
    onRedemptionLimitChange: (TextFieldValue) -> Unit,
    decayDaysInput: TextFieldValue,
    onDecayDaysChange: (TextFieldValue) -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
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

                Text(
                    text = "Complete the steps below to share something new with nearby explorers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (userProfileLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                userProfileError?.let { errorMessage ->
                    Surface(
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }

                if (isBusinessUser) {
                    var currentStep by rememberSaveable { mutableStateOf(BusinessComposerStep.PLAN) }
                    val availableSteps = remember(dropType) {
                        buildList {
                            add(BusinessComposerStep.PLAN)
                            add(BusinessComposerStep.CONTENT)
                            if (dropType == DropType.RESTAURANT_COUPON) {
                                add(BusinessComposerStep.OFFER)
                            }
                            add(BusinessComposerStep.SETTINGS)
                        }
                    }
                    LaunchedEffect(availableSteps) {
                        if (!availableSteps.contains(currentStep)) {
                            currentStep = availableSteps.first()
                        }
                    }
                    val templateSuggestions = remember(businessCategories) {
                        dropTemplatesFor(businessCategories)
                            .take(MAX_BUSINESS_TEMPLATE_SUGGESTIONS)
                    }

                    BusinessComposerStepIndicator(
                        steps = availableSteps,
                        currentStep = currentStep,
                        onStepSelected = { selected ->
                            if (availableSteps.contains(selected)) {
                                currentStep = selected
                            }
                        }
                    )

                    Text(
                        text = currentStep.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = currentStep.helper,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    when (currentStep) {
                        BusinessComposerStep.PLAN -> {
                            DropComposerSection(
                                title = "Business goal",
                                description = "Choose the purpose for this drop. Options are tailored to your business categories.",
                                leadingIcon = Icons.Rounded.Storefront
                            ) {
                                BusinessDropTypeSection(
                                    dropType = dropType,
                                    onDropTypeChange = onDropTypeChange,
                                    businessName = businessName,
                                    businessCategories = businessCategories,
                                    showHeader = false
                                )
                            }

                            if (templateSuggestions.isNotEmpty()) {
                                DropComposerSection(
                                    title = "Need inspiration?",
                                    description = "Browse suggested ideas based on your business categories.",
                                    leadingIcon = Icons.Rounded.Info
                                ) {
                                    BusinessDropTemplatesSection(
                                        templates = templateSuggestions,
                                        onApply = { template ->
                                            onDropTypeChange(template.dropType)
                                            onDropContentTypeChange(template.contentType)
                                            onNoteChange(TextFieldValue(template.caption.ifBlank { "" }))
                                            onDescriptionChange(TextFieldValue(template.note))
                                        },
                                        showHeader = false
                                    )
                                }
                            }
                        }

                        BusinessComposerStep.CONTENT -> {
                            DropContentFormatSection(
                                dropContentType = dropContentType,
                                onDropContentTypeChange = onDropContentTypeChange
                            )
                            DropNoteAndDescriptionSection(
                                dropContentType = dropContentType,
                                note = note,
                                onNoteChange = onNoteChange,
                                description = description,
                                onDescriptionChange = onDescriptionChange
                            )
                            DropMediaAttachmentsSection(
                                context = context,
                                dropContentType = dropContentType,
                                capturedPhotoPath = capturedPhotoPath,
                                onCapturePhoto = onCapturePhoto,
                                onClearPhoto = onClearPhoto,
                                capturedAudioUri = capturedAudioUri,
                                onRecordAudio = onRecordAudio,
                                onClearAudio = onClearAudio,
                                capturedVideoUri = capturedVideoUri,
                                onRecordVideo = onRecordVideo,
                                onClearVideo = onClearVideo
                            )
                        }

                        BusinessComposerStep.OFFER -> {
                            DropComposerSection(
                                title = "Offer security",
                                description = "Set a redemption code and optional limit so each guest redeems only once.",
                                leadingIcon = Icons.Rounded.Flag
                            ) {
                                BusinessRedemptionSection(
                                    redemptionCode = redemptionCodeInput,
                                    onRedemptionCodeChange = onRedemptionCodeChange,
                                    redemptionLimit = redemptionLimitInput,
                                    onRedemptionLimitChange = onRedemptionLimitChange,
                                    showHeader = false
                                )
                            }
                        }

                        BusinessComposerStep.SETTINGS -> {
                            DropAutoDeleteSection(
                                decayDaysInput = decayDaysInput,
                                onDecayDaysChange = onDecayDaysChange
                            )
                            DropVisibilitySectionCard(
                                dropVisibility = dropVisibility,
                                onDropVisibilityChange = onDropVisibilityChange,
                                groupCodeInput = groupCodeInput,
                                onGroupCodeInputChange = onGroupCodeInputChange,
                                joinedGroups = joinedGroups,
                                onSelectGroupCode = onSelectGroupCode,
                                onManageGroupCodes = onManageGroupCodes,
                                isSubmitting = isSubmitting
                            )
                        }
                    }

                    val currentIndex = availableSteps.indexOf(currentStep).coerceAtLeast(0)
                    val previousStep = availableSteps.getOrNull(currentIndex - 1)
                    val nextStep = availableSteps.getOrNull(currentIndex + 1)

                    BusinessComposerStepNavigation(
                        previousStep = previousStep,
                        nextStep = nextStep,
                        isSubmitting = isSubmitting,
                        onBack = { step -> currentStep = step },
                        onNext = { step -> currentStep = step },
                        onSubmit = onSubmit
                    )
                } else {
                    DropContentFormatSection(
                        dropContentType = dropContentType,
                        onDropContentTypeChange = onDropContentTypeChange
                    )
                    DropNoteAndDescriptionSection(
                        dropContentType = dropContentType,
                        note = note,
                        onNoteChange = onNoteChange,
                        description = description,
                        onDescriptionChange = onDescriptionChange
                    )
                    DropAutoDeleteSection(
                        decayDaysInput = decayDaysInput,
                        onDecayDaysChange = onDecayDaysChange
                    )
                    DropMediaAttachmentsSection(
                        context = context,
                        dropContentType = dropContentType,
                        capturedPhotoPath = capturedPhotoPath,
                        onCapturePhoto = onCapturePhoto,
                        onClearPhoto = onClearPhoto,
                        capturedAudioUri = capturedAudioUri,
                        onRecordAudio = onRecordAudio,
                        onClearAudio = onClearAudio,
                        capturedVideoUri = capturedVideoUri,
                        onRecordVideo = onRecordVideo,
                        onClearVideo = onClearVideo
                    )
                    DropIdentitySection(
                        dropAnonymously = dropAnonymously,
                        onDropAnonymouslyChange = onDropAnonymouslyChange,
                        isSubmitting = isSubmitting
                    )
                    DropVisibilitySectionCard(
                        dropVisibility = dropVisibility,
                        onDropVisibilityChange = onDropVisibilityChange,
                        groupCodeInput = groupCodeInput,
                        onGroupCodeInputChange = onGroupCodeInputChange,
                        joinedGroups = joinedGroups,
                        onSelectGroupCode = onSelectGroupCode,
                        onManageGroupCodes = onManageGroupCodes,
                        isSubmitting = isSubmitting
                    )
                    DropSubmitButton(
                        isSubmitting = isSubmitting,
                        onSubmit = onSubmit
                    )
                }
            }
        }
    }
}

@Composable
private fun DropComposerSection(
    title: String,
    description: String?,
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                leadingIcon?.let { icon ->
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier.padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    description?.let { helper ->
                        Text(
                            text = helper,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            content()
        }
    }
}

@Composable
private fun ColumnScope.DropContentFormatSection(
    dropContentType: DropContentType,
    onDropContentTypeChange: (DropContentType) -> Unit
) {
    DropComposerSection(
        title = "Content format",
        description = "Pick what explorers experience when they discover this drop.",
        leadingIcon = Icons.Rounded.Edit
    ) {
        DropContentTypeSection(
            selected = dropContentType,
            onSelect = onDropContentTypeChange,
            showHeader = false
        )
    }
}

@Composable
private fun ColumnScope.DropNoteAndDescriptionSection(
    dropContentType: DropContentType,
    note: TextFieldValue,
    onNoteChange: (TextFieldValue) -> Unit,
    description: TextFieldValue,
    onDescriptionChange: (TextFieldValue) -> Unit
) {
    val noteLabel = when (dropContentType) {
        DropContentType.TEXT -> "Your note"
        DropContentType.PHOTO, DropContentType.AUDIO, DropContentType.VIDEO -> "Caption (optional)"
    }
    val noteSupporting = when (dropContentType) {
        DropContentType.TEXT -> "Share a friendly message, hint, or story for people who find this drop."
        DropContentType.PHOTO -> "Add a short caption to go with your photo."
        DropContentType.AUDIO -> "Add a short caption to go with your audio clip."
        DropContentType.VIDEO -> "Add a short caption to go with your video clip."
    }
    val noteMinLines = if (dropContentType == DropContentType.TEXT) 3 else 1

    DropComposerSection(
        title = noteLabel,
        description = noteSupporting,
        leadingIcon = Icons.Rounded.Edit
    ) {
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            label = null,
            placeholder = { Text("Write something memorable…") },
            minLines = noteMinLines,
            modifier = Modifier.fillMaxWidth()
        )
    }

    DropComposerSection(
        title = "Description",
        description = "Add more context so explorers know what to expect when they find this drop.",
        leadingIcon = Icons.Rounded.Description
    ) {
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = null,
            placeholder = { Text("Share more details…") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ColumnScope.DropAutoDeleteSection(
    decayDaysInput: TextFieldValue,
    onDecayDaysChange: (TextFieldValue) -> Unit
) {
    DropComposerSection(
        title = "Auto-delete",
        description = "Choose how long this drop should stay visible.",
        leadingIcon = Icons.Rounded.Refresh
    ) {
        OutlinedTextField(
            value = decayDaysInput,
            onValueChange = onDecayDaysChange,
            label = { Text("Auto-delete after (days)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            supportingText = {
                Text("Leave blank to keep this drop forever (max $MAX_DECAY_DAYS days).")
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ColumnScope.DropMediaAttachmentsSection(
    context: Context,
    dropContentType: DropContentType,
    capturedPhotoPath: String?,
    onCapturePhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    capturedAudioUri: String?,
    onRecordAudio: () -> Unit,
    onClearAudio: () -> Unit,
    capturedVideoUri: String?,
    onRecordVideo: () -> Unit,
    onClearVideo: () -> Unit
) {
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
            DropComposerSection(
                title = "Photo attachment",
                description = "Capture a shot to pair with your drop.",
                leadingIcon = Icons.Rounded.PhotoCamera
            ) {
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
            DropComposerSection(
                title = "Audio attachment",
                description = "Record a quick voice message for discoverers.",
                leadingIcon = Icons.Rounded.Mic
            ) {
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
            DropComposerSection(
                title = "Video attachment",
                description = "Record a short clip to share at this location.",
                leadingIcon = Icons.Rounded.Videocam
            ) {
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
        }

        DropContentType.TEXT -> Unit
    }
}

@Composable
private fun ColumnScope.DropIdentitySection(
    dropAnonymously: Boolean,
    onDropAnonymouslyChange: (Boolean) -> Unit,
    isSubmitting: Boolean
) {
    DropComposerSection(
        title = "Identity",
        description = "Choose whether your username appears to people who collect this drop.",
        leadingIcon = Icons.Rounded.AccountCircle
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Drop anonymously",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Hides your username from the drop while keeping ownership in your account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = dropAnonymously,
                onCheckedChange = onDropAnonymouslyChange,
                enabled = !isSubmitting
            )
        }
    }
}

@Composable
private fun ColumnScope.DropVisibilitySectionCard(
    dropVisibility: DropVisibility,
    onDropVisibilityChange: (DropVisibility) -> Unit,
    groupCodeInput: TextFieldValue,
    onGroupCodeInputChange: (TextFieldValue) -> Unit,
    joinedGroups: List<String>,
    onSelectGroupCode: (String) -> Unit,
    onManageGroupCodes: () -> Unit,
    isSubmitting: Boolean
) {
    DropComposerSection(
        title = "Visibility",
        description = "Decide who can discover this drop.",
        leadingIcon = Icons.Rounded.Public
    ) {
        DropVisibilitySection(
            visibility = dropVisibility,
            onVisibilityChange = onDropVisibilityChange,
            groupCodeInput = groupCodeInput,
            onGroupCodeInputChange = onGroupCodeInputChange,
            joinedGroups = joinedGroups,
            onSelectGroupCode = onSelectGroupCode,
            showHeader = false
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
    }
}

@Composable
private fun DropSubmitButton(
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Button(
        enabled = !isSubmitting,
        onClick = onSubmit,
        modifier = modifier
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

private enum class BusinessComposerStep(
    val title: String,
    val helper: String,
    val shortLabel: String
) {
    PLAN(
        title = "Plan your drop",
        helper = "Choose the goal that best matches what your business wants to share.",
        shortLabel = "Plan"
    ),
    CONTENT(
        title = "Create your content",
        helper = "Select a format and add the story that will engage nearby explorers.",
        shortLabel = "Content"
    ),
    OFFER(
        title = "Secure your offer",
        helper = "Protect promotions with a code and limit redemptions to avoid misuse.",
        shortLabel = "Offer"
    ),
    SETTINGS(
        title = "Finalize settings",
        helper = "Decide how long the drop lasts and who should be able to discover it.",
        shortLabel = "Settings"
    )
}

private enum class BusinessStepStatus { Completed, Active, Upcoming }

@Composable
private fun BusinessComposerStepIndicator(
    steps: List<BusinessComposerStep>,
    currentStep: BusinessComposerStep,
    onStepSelected: (BusinessComposerStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val currentIndex = steps.indexOf(currentStep).coerceAtLeast(0)
        steps.forEachIndexed { index, step ->
            val status = when {
                index < currentIndex -> BusinessStepStatus.Completed
                index == currentIndex -> BusinessStepStatus.Active
                else -> BusinessStepStatus.Upcoming
            }
            BusinessComposerStepItem(
                index = index,
                step = step,
                status = status,
                onClick = { onStepSelected(step) }
            )
        }
    }
}

@Composable
private fun BusinessComposerStepItem(
    index: Int,
    step: BusinessComposerStep,
    status: BusinessStepStatus,
    onClick: () -> Unit
) {
    val (containerColor, contentColor) = when (status) {
        BusinessStepStatus.Active -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        BusinessStepStatus.Completed -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        BusinessStepStatus.Upcoming -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val labelColor = if (status == BusinessStepStatus.Upcoming) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .widthIn(min = 72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = CircleShape,
            tonalElevation = if (status == BusinessStepStatus.Active) 4.dp else 0.dp
        ) {
            Text(
                text = (index + 1).toString(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = step.title,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 160.dp)
        )
    }
}

@Composable
private fun BusinessComposerStepNavigation(
    previousStep: BusinessComposerStep?,
    nextStep: BusinessComposerStep?,
    isSubmitting: Boolean,
    onBack: (BusinessComposerStep) -> Unit,
    onNext: (BusinessComposerStep) -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (previousStep != null) {
            OutlinedButton(
                onClick = { onBack(previousStep) },
                enabled = !isSubmitting
            ) {
                Text("Back")
            }
        }

        Spacer(Modifier.weight(1f))

        if (nextStep != null) {
            Button(
                onClick = { onNext(nextStep) },
                enabled = !isSubmitting
            ) {
                Text("Next: ${nextStep.shortLabel}")
            }
        } else {
            DropSubmitButton(
                isSubmitting = isSubmitting,
                onSubmit = onSubmit,
                modifier = Modifier
            )
        }
    }
}

@Composable
private fun DialogMessageContent(
    message: String,
    primaryLabel: String?,
    onPrimary: (() -> Unit)?,
    onDismiss: (() -> Unit)? = null
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

        onDismiss?.let {
            TextButton(onClick = it) {
                Text("Back to main page")
            }
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
                "Google Vision SafeSearch flagged this drop as potentially unsafe content."
            }
        }
    }
}

@Composable
private fun NotificationRadiusDialog(
    initialRadius: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val minRadius = NotificationPreferences.MIN_RADIUS_METERS.toFloat()
    val maxRadius = NotificationPreferences.MAX_RADIUS_METERS.toFloat()
    val step = NOTIFICATION_RADIUS_STEP_METERS
    val rawSteps = ((maxRadius - minRadius) / step).roundToInt()
    val steps = (rawSteps - 1).coerceAtLeast(0)
    var sliderValue by remember(initialRadius) {
        mutableStateOf(initialRadius.toFloat().coerceIn(minRadius, maxRadius))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Map, contentDescription = null) },
        title = { Text("Nearby notification radius") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Choose how close a drop should be before we alert you.")
                Text(
                    text = "${sliderValue.roundToInt()} meters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { value ->
                        val snapped = (value / step).roundToInt() * step
                        sliderValue = snapped.coerceIn(minRadius, maxRadius)
                    },
                    valueRange = minRadius..maxRadius,
                    steps = steps,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sliderValue.toDouble()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private const val NOTIFICATION_RADIUS_STEP_METERS = 50f
private const val DROP_PICKUP_RADIUS_METERS = 30.0
private const val MAX_BUSINESS_TEMPLATE_SUGGESTIONS = 6

private fun formatCoordinate(value: Double): String {
    return String.format(Locale.US, "%.5f", value)
}


@Composable
private fun CollectedDropsContent(
    modifier: Modifier = Modifier,
    notes: List<CollectedNote>,
    hiddenNsfwCount: Int,
    canReportDrops: Boolean,
    reportedDropIds: Set<String>,
    reportingDropId: String?,
    isReportProcessing: Boolean,
    sortOption: DropSortOption,
    sortOptions: List<DropSortOption>,
    onSortOptionChange: (DropSortOption) -> Unit,
    onReport: (CollectedNote) -> Unit,
    onView: (CollectedNote) -> Unit,
    onRemove: (CollectedNote) -> Unit,
    emptyMessage: String? = null,
    topContentPadding: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(vertical = 16.dp)
) {
    if (notes.isEmpty()) {
        val message = if (hiddenNsfwCount > 0) {
            val plural = if (hiddenNsfwCount == 1) "drop" else "drops"
            "Your NSFW settings are hiding $hiddenNsfwCount collected $plural."
        } else {
            emptyMessage ?: "You haven't collected any drops yet."
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(top = topContentPadding)
        ) {
            DialogMessageContent(
                message = message,
                primaryLabel = null,
                onPrimary = null,
                onDismiss = null
            )
        }
        return
    }

    var highlightedId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(notes) {
        highlightedId = highlightedId?.takeIf { id -> notes.any { it.id == id } }
    }

    val highlightedNote = notes.firstOrNull { it.id == highlightedId }

    val listState = rememberLazyListState()
    var lastSortOption by remember { mutableStateOf(sortOption) }
    var skipScrollForSortChange by remember { mutableStateOf(false) }

    LaunchedEffect(sortOption) {
        if (lastSortOption != sortOption) {
            skipScrollForSortChange = true
            listState.scrollToItem(0)
            lastSortOption = sortOption
        }
    }

    LaunchedEffect(highlightedId, notes) {
        if (skipScrollForSortChange) {
            skipScrollForSortChange = false
            return@LaunchedEffect
        }
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
                "Map occupies ${(mapWeight * 100).roundToInt()} percent of the available height"
            setProgress { target ->
                val coerced = target.coerceIn(minMapWeight, maxMapWeight)
                if (coerced != mapWeight) {
                    mapWeight = coerced
                }
                true
            }
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .onSizeChanged { containerHeight = it.height }
            .padding(top = topContentPadding)
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
                highlightedId = highlightedId,
                onNoteClick = { note ->
                    highlightedId = if (highlightedId == note.id) null else note.id
                }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DropSortMenu(
                    modifier = Modifier.weight(1f),
                    current = sortOption,
                    options = sortOptions,
                    onSelect = onSortOptionChange
                )

                Spacer(Modifier.width(12.dp))
                CountBadge(count = notes.size)
            }

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
                    val alreadyReported = reportedDropIds.contains(note.id)
                    val restrictionMessage = when {
                        alreadyReported -> "Thanks for your report. We'll review it soon."
                        !canReportDrops -> "Sign in to report drops."
                        else -> null
                    }
                    val isReporting = isReportProcessing && reportingDropId == note.id
                    CollectedNoteCard(
                        note = note,
                        selected = isHighlighted,
                        expanded = isHighlighted,
                        onSelect = {
                            highlightedId = if (isHighlighted) null else note.id
                        },
                        canReport = canReportDrops,
                        alreadyReported = alreadyReported,
                        reportRestrictionMessage = restrictionMessage,
                        isReporting = isReporting,
                        onReport = { onReport(note) },
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

@Composable
private fun GeoDropHeader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExplorerDestinationTabs(
    modifier: Modifier = Modifier,
    current: ExplorerDestination,
    onSelect: (ExplorerDestination) -> Unit,
    showMyDrops: Boolean,
    showCollected: Boolean
) {
    val destinations = remember(showMyDrops, showCollected) {
        ExplorerDestination.values().filter { destination ->
            when (destination) {
                ExplorerDestination.MyDrops -> showMyDrops
                ExplorerDestination.Collected -> showCollected
                ExplorerDestination.Discover -> true
            }
        }
    }
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        destinations.forEachIndexed { index, destination ->
            val selected = destination == current
            val shape = SegmentedButtonDefaults.itemShape(index, destinations.size)
            val (label, icon) = when (destination) {
                ExplorerDestination.Discover -> Pair(
                    stringResource(R.string.action_browse_map_title),
                    Icons.Rounded.Map
                )

                ExplorerDestination.MyDrops -> Pair(
                    stringResource(R.string.action_my_drops_title),
                    Icons.Rounded.Inbox
                )

                ExplorerDestination.Collected -> Pair(
                    stringResource(R.string.action_collected_drops_title),
                    Icons.Rounded.Bookmark
                )
            }
            SegmentedButton(
                selected = selected,
                onClick = { onSelect(destination) },
                shape = shape,
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null
                    )
                },
                label = { Text(label) }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSignInDialog(
    accountType: AccountType,
    canChangeAccountType: Boolean,
    onAccountTypeChange: (AccountType) -> Unit,
    mode: AccountAuthMode,
    onModeChange: (AccountAuthMode) -> Unit,
    email: TextFieldValue,
    onEmailChange: (TextFieldValue) -> Unit,
    password: TextFieldValue,
    onPasswordChange: (TextFieldValue) -> Unit,
    confirmPassword: TextFieldValue,
    onConfirmPasswordChange: (TextFieldValue) -> Unit,
    username: TextFieldValue,
    onUsernameChange: (TextFieldValue) -> Unit,
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val scrollState = rememberScrollState()

    val hideKeyboardAndClearFocus = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }
    val submitWithKeyboardDismiss = {
        hideKeyboardAndClearFocus()
        onSubmit()
    }
    val dismissWithKeyboardDismiss = {
        hideKeyboardAndClearFocus()
        onDismiss()
    }
    val forgotPasswordWithKeyboardDismiss = {
        hideKeyboardAndClearFocus()
        onForgotPassword()
    }
    val googleSignInWithKeyboardDismiss = {
        hideKeyboardAndClearFocus()
        onGoogleSignIn()
    }

    Dialog(
        onDismissRequest = {
            if (!isBusy) {
                dismissWithKeyboardDismiss()
            }
        },
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
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = when (accountType) {
                        AccountType.EXPLORER -> "Explorer account"
                        AccountType.BUSINESS -> "Business account"
                    },
                    style = MaterialTheme.typography.titleLarge
                )

                if (canChangeAccountType) {
                    SingleChoiceSegmentedButtonRow {
                        AccountType.entries.forEachIndexed { index, type ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index, AccountType.entries.size),
                                selected = accountType == type,
                                onClick = { onAccountTypeChange(type) }
                            ) {
                                Text(
                                    text = when (type) {
                                        AccountType.EXPLORER -> "Explorer"
                                        AccountType.BUSINESS -> "Business"
                                    }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = when (accountType) {
                        AccountType.EXPLORER -> "Explorer accounts let you drop, like, and collect rewards."
                        AccountType.BUSINESS -> "Business accounts can publish offers and require business details."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                BoxWithConstraints {
                    val shouldStackVertically = maxWidth < 360.dp

                    if (shouldStackVertically) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AccountAuthMode.entries.forEach { option ->
                                val selected = mode == option
                                val buttonColors = if (selected) {
                                    ButtonDefaults.filledTonalButtonColors()
                                } else {
                                    ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                FilledTonalButton(
                                    onClick = { onModeChange(option) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isBusy,
                                    colors = buttonColors,
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Text(
                                        text = when (option) {
                                            AccountAuthMode.SIGN_IN -> "Sign in"
                                            AccountAuthMode.REGISTER -> "Create account"
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        SingleChoiceSegmentedButtonRow {
                            AccountAuthMode.entries.forEachIndexed { index, option ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index, AccountAuthMode.entries.size),
                                    selected = mode == option,
                                    onClick = { onModeChange(option) }
                                ) {
                                    Text(
                                        text = when (option) {
                                            AccountAuthMode.SIGN_IN -> "Sign in"
                                            AccountAuthMode.REGISTER -> "Create account"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                val isRegister = mode == AccountAuthMode.REGISTER
                val requiresExplorerUsername = isRegister && accountType == AccountType.EXPLORER

                if (requiresExplorerUsername) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = onUsernameChange,
                        label = { Text(stringResource(R.string.explorer_profile_username_label)) },
                        placeholder = { Text(stringResource(R.string.explorer_profile_username_placeholder)) },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) }
                        )
                    )

                    Text(
                        text = stringResource(R.string.explorer_profile_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email address") },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
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
                    ),
                    keyboardActions = if (isRegister) {
                        KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) }
                        )
                    } else {
                        KeyboardActions(
                            onDone = { submitWithKeyboardDismiss() }
                        )
                    }
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
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitWithKeyboardDismiss() }
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
                        onClick = forgotPasswordWithKeyboardDismiss,
                        enabled = !isBusy,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot password?")
                    }
                }

                if (isRegister) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = submitWithKeyboardDismiss,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
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
                                        AccountAuthMode.SIGN_IN -> "Sign in"
                                        AccountAuthMode.REGISTER -> "Create account"
                                    }
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = dismissWithKeyboardDismiss,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = dismissWithKeyboardDismiss,
                            enabled = !isBusy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = submitWithKeyboardDismiss,
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
                                        AccountAuthMode.SIGN_IN -> "Sign in"
                                        AccountAuthMode.REGISTER -> "Create account"
                                    }
                                )
                            }
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
                    onClick = googleSignInWithKeyboardDismiss,
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
private fun ExplorerProfileDialog(
    currentUsername: String?,
    username: TextFieldValue,
    onUsernameChange: (TextFieldValue) -> Unit,
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
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.explorer_profile_title),
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = stringResource(R.string.explorer_profile_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                currentUsername?.takeIf { it.isNotBlank() }?.let { existing ->
                    Text(
                        text = stringResource(R.string.explorer_profile_current_username, "@$existing"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text(stringResource(R.string.explorer_profile_username_label)) },
                    placeholder = { Text(stringResource(R.string.explorer_profile_username_placeholder)) },
                    singleLine = true,
                    enabled = !isSubmitting,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.explorer_profile_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        } else {
                            Text(stringResource(R.string.explorer_profile_save))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessOnboardingDialog(
    name: TextFieldValue,
    selectedCategories: Set<BusinessCategory>,
    onNameChange: (TextFieldValue) -> Unit,
    onToggleCategory: (BusinessCategory) -> Unit,
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
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(scrollState),
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

                Text(
                    text = "Select the categories that best describe your business.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val groupedEntries = remember {
                    BusinessCategory.entries.groupBy { it.group }.entries.toList()
                }

                groupedEntries.forEachIndexed { index, (group, options) ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = group.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        options.forEach { category ->
                            BusinessCategoryOptionRow(
                                category = category,
                                selected = selectedCategories.contains(category),
                                enabled = !isSubmitting,
                                onToggle = { onToggleCategory(category) }
                            )
                        }
                    }

                    if (index != groupedEntries.lastIndex) {
                        Spacer(Modifier.height(8.dp))
                    }
                }

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
private fun BusinessCategoryOptionRow(
    category: BusinessCategory,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable(enabled = enabled, role = Role.Checkbox) { onToggle() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = null,
            enabled = enabled
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
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

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            maxItemsInEachRow = 2
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
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
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
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ),
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
                DropTitleText(
                    drop = drop,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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

            val expirationLabel = drop.decayAtMillis()?.let { expireAt ->
                val now = System.currentTimeMillis()
                if (expireAt <= now) {
                    "Auto-deleted"
                } else {
                    val remainingMillis = expireAt - now
                    val remainingDays = ceil(
                        remainingMillis.toDouble() /
                                TimeUnit.DAYS.toMillis(1).toDouble()
                    ).toInt().coerceAtLeast(1)
                    if (remainingDays == 1) "Expires in 1 day" else "Expires in $remainingDays days"
                }
            }
            expirationLabel?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val reactionSummary = "Likes: ${drop.likeCount} · Dislikes: ${drop.dislikeCount}"
            Text(
                text = reactionSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun OtherDropsExplorerSection(
    modifier: Modifier = Modifier,
    topContentPadding: Dp = 0.dp,
    loading: Boolean,
    drops: List<Drop>,
    currentLocation: LatLng?,
    notificationRadiusMeters: Double,
    error: String?,
    emptyMessage: String? = null,
    selectedId: String?,
    onSelect: (Drop) -> Unit,
    onPickUp: (Drop) -> Unit,
    currentUserId: String?,
    votingDropIds: Set<String>,
    collectedDropIds: Set<String>,
    canCollectDrops: Boolean,
    collectRestrictionMessage: String?,
    sortOption: DropSortOption,
    sortOptions: List<DropSortOption>,
    onSortOptionChange: (DropSortOption) -> Unit,
    canLikeDrops: Boolean,
    likeRestrictionMessage: String?,
    onLike: (Drop, DropLikeStatus) -> Unit,
    onReport: (Drop) -> Unit,
    reportingDropId: String?,
    dismissedBrowseDropIds: SnapshotStateList<String>,
    snackbar: SnackbarHostState,
    scope: CoroutineScope,
    onRefresh: () -> Unit,
    listState: LazyListState,
    mapWeight: Float,
    onMapWeightChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val clampedMapWeight = mapWeight.coerceIn(MAP_LIST_MIN_WEIGHT, MAP_LIST_MAX_WEIGHT)
    var internalWeight by remember { mutableStateOf(clampedMapWeight) }
    internalWeight = clampedMapWeight
    val listWeight = 1f - internalWeight
    var containerHeight by remember { mutableStateOf(0) }
    val dividerInteraction = remember { MutableInteractionSource() }
    val dividerDragState = rememberDraggableState { delta ->
        val height = containerHeight.takeIf { it > 0 }?.toFloat() ?: return@rememberDraggableState
        val deltaWeight = delta / height
        val updated = (internalWeight + deltaWeight).coerceIn(MAP_LIST_MIN_WEIGHT, MAP_LIST_MAX_WEIGHT)
        if (updated != internalWeight) {
            internalWeight = updated
            onMapWeightChange(updated)
        }
    }
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
                current = internalWeight,
                range = MAP_LIST_MIN_WEIGHT..MAP_LIST_MAX_WEIGHT
            )
            stateDescription =
                "Map occupies ${'$'}{(internalWeight * 100).roundToInt()} percent of the available height"
            setProgress { target ->
                val coerced = target.coerceIn(MAP_LIST_MIN_WEIGHT, MAP_LIST_MAX_WEIGHT)
                if (coerced != internalWeight) {
                    internalWeight = coerced
                    onMapWeightChange(coerced)
                    true
                } else {
                    false
                }
            }
        }

    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
                loading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = topContentPadding),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        OutlinedButton(onClick = onRefresh) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_retry_generic))
                        }
                    }
                }

                drops.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = topContentPadding),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = emptyMessage ?: "No drops from other users are available right now.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = onRefresh) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_retry_generic))
                        }
                    }
                }

                else -> {
                    var lastSortOption by remember { mutableStateOf(sortOption) }
                    var skipSelectionScroll by remember { mutableStateOf(false) }

                    LaunchedEffect(sortOption) {
                        if (lastSortOption != sortOption) {
                            skipSelectionScroll = true
                            listState.scrollToItem(0)
                            lastSortOption = sortOption
                        }
                    }

                    LaunchedEffect(selectedId, drops) {
                        if (skipSelectionScroll) {
                            skipSelectionScroll = false
                            return@LaunchedEffect
                        }
                        val targetId = selectedId ?: return@LaunchedEffect
                        val index = drops.indexOfFirst { it.id == targetId }
                        if (index >= 0) {
                            listState.animateScrollToItem(index)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .onSizeChanged { containerHeight = it.height }
                            .padding(top = topContentPadding)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(internalWeight)
                        ) {
                            OtherDropsMap(
                                drops = drops,
                                selectedDropId = selectedId,
                                currentLocation = currentLocation,
                                notificationRadiusMeters = notificationRadiusMeters,
                                onDropClick = onSelect
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DropSortMenu(
                                        modifier = Modifier.weight(1f),
                                        current = sortOption,
                                        options = sortOptions,
                                        onSelect = onSortOptionChange
                                )

                                if (drops.isNotEmpty()) {
                                    Spacer(Modifier.width(12.dp))
                                    CountBadge(count = drops.size)
                                }
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(drops, key = { it.id }) { drop ->
                                    val hasCollected = collectedDropIds.contains(drop.id)
                                    val canLike = canLikeDrops && isSignedIn && hasCollected
                                    val withinPickupRange = currentLocation?.let { location ->
                                        distanceBetweenMeters(
                                            location.latitude,
                                            location.longitude,
                                            drop.lat,
                                            drop.lng
                                        ) <= DROP_PICKUP_RADIUS_METERS
                                    } ?: false
                                    val likeMessage = when {
                                        !canLikeDrops -> likeRestrictionMessage
                                        !isSignedIn -> "Sign in to react to drops."
                                        !hasCollected -> "Collect this drop to react to it."
                                        else -> null
                                    }
                                    val isOwnDrop = currentUserId != null && drop.createdBy == currentUserId
                                    val alreadyReported = currentUserId?.let { drop.reportedBy.containsKey(it) } == true
                                    val canReport = isSignedIn && !isOwnDrop && (hasCollected || withinPickupRange)
                                    val reportMessage = when {
                                        isOwnDrop -> "You created this drop."
                                        !isSignedIn -> "Sign in to report drops."
                                        alreadyReported -> "Thanks for your report. We'll review it soon."
//                                        !hasCollected && !withinPickupRange ->
//                                            "Move within ${DROP_PICKUP_RADIUS_METERS.roundToInt()} meters to report this drop, or collect it first."
                                        else -> null
                                    }
                                    val showReportButton = !isOwnDrop
                                    val isReporting = reportingDropId == drop.id
                                    val canIgnoreForNow = !withinPickupRange
                                    OtherDropRow(
                                        drop = drop,
                                        isSelected = drop.id == selectedId,
                                        currentLocation = currentLocation,
                                        userLike = drop.userLikeStatus(currentUserId),
                                        canLike = canLike,
                                        likeRestrictionMessage = likeMessage,
                                        canPickUp = canCollectDrops,
                                        pickupRestrictionMessage = collectRestrictionMessage,
                                        isVoting = votingDropIds.contains(drop.id),
                                        showReport = showReportButton,
                                        canReport = canReport,
                                        alreadyReported = alreadyReported,
                                        reportRestrictionMessage = reportMessage,
                                        isReporting = isReporting,
                                        canIgnoreForNow = canIgnoreForNow,
                                        onIgnoreForNow = {
                                            if (!dismissedBrowseDropIds.contains(drop.id)) {
                                                dismissedBrowseDropIds.add(drop.id)
                                                snackbar.showMessage(
                                                    scope,
                                                    context.getString(R.string.browse_ignore_drop_snackbar)
                                                )
                                            }
                                        },
                                        onSelect = { onSelect(drop) },
                                        onLike = { status -> onLike(drop, status) },
                                        onPickUp = { onPickUp(drop) },
                                        onReport = { onReport(drop) }
                                    )
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class DropSortOption(val displayName: String) {
    NEAREST("Nearest"),
    MOST_POPULAR("Most popular"),
    NEWEST("Newest"),
    ENDING_SOON("Ending soon")
}

private fun sortDrops(
    drops: List<Drop>,
    option: DropSortOption,
    currentLocation: LatLng?
): List<Drop> {
    return when (option) {
        DropSortOption.NEAREST -> {
            val location = currentLocation ?: return drops
            drops.sortedBy { drop ->
                distanceBetweenMeters(
                    location.latitude,
                    location.longitude,
                    drop.lat,
                    drop.lng
                )
            }
        }

        DropSortOption.MOST_POPULAR -> drops.sortedWith(
            compareByDescending<Drop> { it.likeCount - it.dislikeCount }
                .thenByDescending { it.createdAt }
        )

        DropSortOption.NEWEST -> drops.sortedByDescending { it.createdAt }

        DropSortOption.ENDING_SOON -> drops.sortedWith(
            compareBy<Drop> { drop -> drop.decayAtMillis() ?: Long.MAX_VALUE }
                .thenByDescending { it.createdAt }
        )
    }
}

private fun sortCollectedNotes(
    notes: List<CollectedNote>,
    option: DropSortOption,
    currentLocation: LatLng?
): List<CollectedNote> {
    return when (option) {
        DropSortOption.NEAREST -> {
            val location = currentLocation ?: return notes
            notes.sortedWith(
                compareBy<CollectedNote> { note ->
                    val lat = note.lat
                    val lng = note.lng
                    if (lat == null || lng == null) {
                        Double.MAX_VALUE
                    } else {
                        distanceBetweenMeters(
                            location.latitude,
                            location.longitude,
                            lat,
                            lng
                        )
                    }
                }.thenByDescending { it.collectedAt }
            )
        }

        DropSortOption.MOST_POPULAR -> notes.sortedWith(
            compareByDescending<CollectedNote> { it.likeCount - it.dislikeCount }
                .thenByDescending { it.collectedAt }
        )

        DropSortOption.NEWEST -> notes.sortedByDescending { note ->
            note.dropCreatedAt ?: note.collectedAt
        }

        DropSortOption.ENDING_SOON -> notes.sortedWith(
            compareBy<CollectedNote> { note -> note.decayAtMillis() ?: Long.MAX_VALUE }
                .thenByDescending { note -> note.dropCreatedAt ?: note.collectedAt }
        )
    }
}

@Composable
private fun DropSortMenu(
    modifier: Modifier = Modifier,
    current: DropSortOption,
    options: List<DropSortOption>,
    onSelect: (DropSortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        AssistChip(
            onClick = { expanded = true },
            label = { Text("Sort: ${current.displayName}") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Sort,
                    contentDescription = null
                )
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    trailingIcon = if (option == current) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

@Composable
private fun CollectedDropsMap(
    notes: List<CollectedNote>,
    highlightedId: String?,
    modifier: Modifier = Modifier,
    onNoteClick: (CollectedNote) -> Unit
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
            modifier = modifier
                .fillMaxSize()
                .consumeMapGesturesInParent(),
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
                    zIndex = if (note.id == highlightedId) 1f else 0f,
                    onClick = {
                        onNoteClick(note)
                        false
                    }
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
    canReport: Boolean,
    alreadyReported: Boolean,
    reportRestrictionMessage: String?,
    isReporting: Boolean,
    onReport: () -> Unit,
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
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
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

            val preview = note.description?.takeIf { it.isNotBlank() }
                ?: note.text.takeIf { it.isNotBlank() }
                ?: when (note.contentType) {
                    DropContentType.TEXT -> "(No message)"
                    DropContentType.PHOTO -> "Photo drop"
                    DropContentType.AUDIO -> "Audio drop"
                    DropContentType.VIDEO -> "Video drop"
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
                    when {
                        note.contentType == DropContentType.PHOTO && mediaUrl != null -> {
                            val context = LocalContext.current
                            val imageRequest = remember(mediaUrl) {
                                ImageRequest.Builder(context)
                                    .data(mediaUrl)
                                    .crossfade(true)
                                    .build()
                            }

                            AsyncImage(
                                model = imageRequest,
                                contentDescription = preview,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 160.dp, max = 280.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        note.contentType == DropContentType.VIDEO && mediaUrl != null -> {
                            val videoUri = remember(mediaUrl) { Uri.parse(mediaUrl) }

                            DropVideoPlayer(
                                videoUri = videoUri,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Tap play to watch this clip.",
                                style = MaterialTheme.typography.bodySmall,
                                color = supportingColor
                            )
                        }
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

                    note.dropperUsername?.takeIf { it.isNotBlank() }?.let { username ->
                        Text(
                            text = "Dropped by @${username}",
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

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onReport,
                        enabled = canReport && !alreadyReported && !isReporting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isReporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Rounded.Report, contentDescription = null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when {
                                isReporting -> "Reporting..."
                                alreadyReported -> "Reported"
                                else -> "Report drop"
                            }
                        )
                    }

                    reportRestrictionMessage?.let { message ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
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
    groups: List<GroupMembership>,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onSubscribe: (String) -> Unit,
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
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.background,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            actionIconContentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Filled.ArrowBack,
                                    contentDescription = "Back to main page"
                                )
                            }
                        }
                    )
                },
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }
            ) { padding ->
                var createCode by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                    mutableStateOf(TextFieldValue(""))
                }
                var subscribeCode by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                    mutableStateOf(TextFieldValue(""))
                }

                val ownedGroups = groups.filter { it.role == GroupRole.OWNER }
                val subscribedGroups = groups.filter { it.role == GroupRole.SUBSCRIBER }
                val createNormalized = GroupPreferences.normalizeGroupCode(createCode.text)
                val subscribeNormalized = GroupPreferences.normalizeGroupCode(subscribeCode.text)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Create a group",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Pick a code to share with your crew. Only you can add or remove drops.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = createCode,
                                onValueChange = { createCode = it },
                                label = { Text("Group code") },
                                supportingText = {
                                    Text("Codes stay on this device. Share with people you trust.")
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    createNormalized?.let {
                                        onCreate(it)
                                        createCode = TextFieldValue("")
                                    }
                                },
                                enabled = createNormalized != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Create group")
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Groups you created",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (ownedGroups.isEmpty()) {
                                Text(
                                    text = "You haven't created any groups yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ownedGroups.forEach { membership ->
                                        GroupCodeRow(
                                            membership = membership,
                                            onRemove = onRemove
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Subscribe to a group",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Enter a code you received to follow drops from others.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = subscribeCode,
                                onValueChange = { subscribeCode = it },
                                label = { Text("Group code") },
                                supportingText = {
                                    Text("Codes stay on this device. You can leave anytime.")
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    subscribeNormalized?.let {
                                        onSubscribe(it)
                                        subscribeCode = TextFieldValue("")
                                    }
                                },
                                enabled = subscribeNormalized != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Subscribe")
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Groups you're subscribed to",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (subscribedGroups.isEmpty()) {
                                Text(
                                    text = "You haven't subscribed to any groups yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    subscribedGroups.forEach { membership ->
                                        GroupCodeRow(
                                            membership = membership,
                                            onRemove = onRemove
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

@Composable
private fun MyDropsContent(
    modifier: Modifier = Modifier,
    loading: Boolean,
    drops: List<Drop>,
    currentLocation: LatLng?,
    deletingId: String?,
    error: String?,
    emptyMessage: String? = null,
    selectedId: String?,
    sortOption: DropSortOption,
    sortOptions: List<DropSortOption>,
    onSortOptionChange: (DropSortOption) -> Unit,
    onSelect: (Drop) -> Unit,
    onRetry: () -> Unit,
    onView: (Drop) -> Unit,
    onDelete: (Drop) -> Unit,
    topContentPadding: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(vertical = 16.dp)
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topContentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topContentPadding)
                ) {
                    DialogMessageContent(
                        message = error,
                        primaryLabel = "Retry",
                        onPrimary = onRetry,
                        onDismiss = null
                    )
                }
            }

            drops.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topContentPadding)
                ) {
                    DialogMessageContent(
                        message = emptyMessage ?: "You haven't dropped any notes yet.",
                        primaryLabel = null,
                        onPrimary = null,
                        onDismiss = null
                    )
                }
            }

            else -> {
                val listState = rememberLazyListState()
                var lastSortOption by remember { mutableStateOf(sortOption) }
                var skipSelectionScroll by remember { mutableStateOf(false) }

                LaunchedEffect(sortOption) {
                    if (lastSortOption != sortOption) {
                        skipSelectionScroll = true
                        listState.scrollToItem(0)
                        lastSortOption = sortOption
                    }
                }

                LaunchedEffect(selectedId, drops) {
                    if (skipSelectionScroll) {
                        skipSelectionScroll = false
                        return@LaunchedEffect
                    }
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
                            "Map occupies ${(mapWeight * 100).roundToInt()} percent of the available height"
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
                        .padding(top = topContentPadding)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(mapWeight)
                    ) {
                        MyDropsMap(
                            drops = drops,
                            selectedDropId = selectedId,
                            currentLocation = currentLocation,
                            onDropClick = onSelect
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DropSortMenu(
                                modifier = Modifier.weight(1f),
                                current = sortOption,
                                options = sortOptions,
                                onSelect = onSortOptionChange
                            )

                            Spacer(Modifier.width(12.dp))
                            CountBadge(count = drops.size)
                        }

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
    membership: GroupMembership,
    onRemove: (String) -> Unit
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
            val code = membership.code
            val isOwner = membership.role == GroupRole.OWNER
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isOwner) {
                        "You created this group. Only you can add or remove drops."
                    } else {
                        "Subscribed to this group's drops and updates."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = { onRemove(code) }) {
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
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isReady) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
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
                OutlinedButton(
                    onClick = onSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = contentColor
                    )
                ) {
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
    currentLocation: LatLng?,
    onDropClick: (Drop) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState()
    val uiSettings = remember { MapUiSettings(zoomControlsEnabled = true) }

    LaunchedEffect(drops, selectedDropId, currentLocation) {
        val targetDrop = drops.firstOrNull { it.id == selectedDropId }
        val target = targetDrop?.let { LatLng(it.lat, it.lng) }
            ?: currentLocation
            ?: drops.firstOrNull()?.let { LatLng(it.lat, it.lng) }
        if (target != null) {
            val zoomLevel = if (targetDrop != null) 18f else 15f
            val update = CameraUpdateFactory.newLatLngZoom(target, zoomLevel)
            cameraPositionState.animate(update)
        }
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxSize()
            .consumeMapGesturesInParent(),
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
            snippetParts.add("Likes: ${drop.likeCount}")
            snippetParts.add("Dislikes: ${drop.dislikeCount}")
            if (drop.isNsfw) {
                snippetParts.add("Marked as adult content")
            }

            val isSelected = drop.id == selectedDropId
            val markerIcon = when {
                isSelected -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                drop.isNsfw -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
                else -> BitmapDescriptorFactory.defaultMarker(likeHueFor(drop.likeCount))
            }

            Marker(
                state = MarkerState(position),
                title = drop.displayTitle(),
                snippet = snippetParts.joinToString("\n"),
                icon = markerIcon,
                alpha = if (isSelected) 1f else 0.9f,
                zIndex = if (isSelected) 2f else 0f,
                onClick = {
                    onDropClick(drop)
                    false
                }
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
private fun DropTitleText(
    drop: Drop,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val (handle, baseTitle) = drop.displayTitleParts()
    val annotatedTitle = remember(handle, baseTitle) {
        if (handle != null) {
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(handle)
                }
                append(" dropped ")
                append(baseTitle)
            }
        } else {
            AnnotatedString(baseTitle)
        }
    }

    Text(
        text = annotatedTitle,
        style = style,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow
    )
}

@Composable
private fun OtherDropRow(
    drop: Drop,
    isSelected: Boolean,
    currentLocation: LatLng?,
    userLike: DropLikeStatus,
    canLike: Boolean,
    likeRestrictionMessage: String?,
    canPickUp: Boolean,
    pickupRestrictionMessage: String?,
    isVoting: Boolean,
    showReport: Boolean,
    canReport: Boolean,
    alreadyReported: Boolean,
    reportRestrictionMessage: String?,
    isReporting: Boolean,
    canIgnoreForNow: Boolean,
    onIgnoreForNow: () -> Unit,
    onSelect: () -> Unit,
    onLike: (DropLikeStatus) -> Unit,
    onPickUp: () -> Unit,
    onReport: () -> Unit
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
    val withinPickupRange =
        distanceMeters != null && distanceMeters <= DROP_PICKUP_RADIUS_METERS
    val canPreviewContent = withinPickupRange
    val context = LocalContext.current
    val mediaAttachment = remember(
        context,
        drop.id,
        drop.mediaUrl,
        drop.mediaData,
        drop.mediaMimeType,
        drop.contentType
    ) {
        if (drop.contentType == DropContentType.AUDIO || drop.contentType == DropContentType.VIDEO) {
            resolveDropMediaAttachment(context, drop)
        } else {
            null
        }
    }
    val previewText = drop.description?.takeIf { it.isNotBlank() }
        ?: drop.text.takeIf { it.isNotBlank() }
        ?: when (drop.contentType) {
            DropContentType.PHOTO -> "Preview the photo below."
            DropContentType.AUDIO -> "Use the player below to listen to this drop."
            DropContentType.VIDEO -> "Use the player below to watch this drop."
            DropContentType.TEXT -> null
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DropTitleText(
                    drop = drop,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = if (isSelected) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
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

            if (canPreviewContent && !previewText.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = supportingColor,
                    maxLines = if (isSelected) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (!canPreviewContent) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Move closer to preview this drop.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = supportingColor
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

                    if (canPreviewContent && drop.contentType == DropContentType.PHOTO) {
                        val mediaUrl = drop.mediaLabel()
                        if (!mediaUrl.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
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
                    }

                    if (canPreviewContent && (drop.contentType == DropContentType.AUDIO || drop.contentType == DropContentType.VIDEO)) {
                        Spacer(Modifier.height(12.dp))
                        AttachmentPreviewSection(
                            contentType = drop.contentType,
                            attachment = mediaAttachment,
                            onOpen = { attachment -> openDropMediaAttachment(context, attachment) }
                        )
                    }

                    Spacer(Modifier.height(4.dp))

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
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LikeToggleButton(
                                        icon = Icons.Rounded.ThumbUp,
                                        selected = userLike == DropLikeStatus.LIKED,
                                        enabled = canLike && !isVoting,
                                        onClick = {
                                            val nextStatus = if (userLike == DropLikeStatus.LIKED) {
                                                DropLikeStatus.NONE
                                            } else {
                                                DropLikeStatus.LIKED
                                            }
                                            onLike(nextStatus)
                                        },
                                        contentDescription = if (userLike == DropLikeStatus.LIKED) {
                                            "Unlike drop"
                                        } else {
                                            "Like drop"
                                        }
                                    )

                                    Text(
                                        text = drop.likeCount.toString(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LikeToggleButton(
                                        icon = Icons.Rounded.ThumbDown,
                                        selected = userLike == DropLikeStatus.DISLIKED,
                                        enabled = canLike && !isVoting,
                                        onClick = {
                                            val nextStatus = if (userLike == DropLikeStatus.DISLIKED) {
                                                DropLikeStatus.NONE
                                            } else {
                                                DropLikeStatus.DISLIKED
                                            }
                                            onLike(nextStatus)
                                        },
                                        contentDescription = if (userLike == DropLikeStatus.DISLIKED) {
                                            "Remove dislike"
                                        } else {
                                            "Dislike drop"
                                        }
                                    )

                                    Text(
                                        text = drop.dislikeCount.toString(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (showReport) {
                                        OutlinedButton(
                                            onClick = onReport,
                                            enabled = canReport && !alreadyReported && !isReporting
                                        ) {
                                            if (isReporting) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Rounded.Report,
                                                    contentDescription = null
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = when {
                                                    isReporting -> "Reporting..."
                                                    alreadyReported -> "Reported"
                                                    else -> "Report"
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (isVoting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
//                    if (!canLike) {
//                        Spacer(Modifier.height(4.dp))
//                        Text(
//                            text = likeRestrictionMessage ?: "Collect this drop to react to it.",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = supportingColor
//                        )
//                    }
                    if (showReport) {
                        reportRestrictionMessage?.let { message ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = supportingColor
                            )
                        }
                    }
                    if (withinPickupRange) {
                        Spacer(Modifier.height(12.dp))
                        if (canPickUp) {
                            Button(
                                onClick = onPickUp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Pick up drop")
                            }
                        } else {
                            Text(
                                text = pickupRestrictionMessage
                                    ?: "Preview this drop while browsing as a guest. Sign in to pick it up nearby.",
                                style = MaterialTheme.typography.bodySmall,
                                color = supportingColor
                            )
                        }
                    }
                    if (withinPickupRange && canIgnoreForNow) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onIgnoreForNow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_ignore_drop_for_now))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.browse_ignore_drop_explainer),
                            style = MaterialTheme.typography.bodySmall,
                            color = supportingColor
                        )
                    }
                }
            }
        }
    }
}

private const val REPORT_SOURCE_BROWSE_MAP = "browse_map"
private const val REPORT_SOURCE_COLLECTED = "collected_inventory"

private data class ReportableDrop(
    val id: String,
    val contentType: DropContentType,
    val dropType: DropType,
    val hasMedia: Boolean,
    val source: String
)

private fun Drop.toReportableDrop(source: String): ReportableDrop {
    return ReportableDrop(
        id = id,
        contentType = contentType,
        dropType = dropType,
        hasMedia = !mediaUrl.isNullOrBlank() || !mediaData.isNullOrBlank(),
        source = source
    )
}

private fun CollectedNote.toReportableDrop(source: String): ReportableDrop {
    return ReportableDrop(
        id = id,
        contentType = contentType,
        dropType = dropType,
        hasMedia = !mediaUrl.isNullOrBlank() || !mediaData.isNullOrBlank(),
        source = source
    )
}

@Composable
private fun AttachmentPreviewSection(
    contentType: DropContentType,
    attachment: DropMediaAttachment?,
    onOpen: (DropMediaAttachment) -> Unit
) {
    when (contentType) {
        DropContentType.AUDIO -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Audio clip",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap to listen to this recording.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { attachment?.let(onOpen) },
                enabled = attachment != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Play audio")
            }

            if (attachment == null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Attachment unavailable.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        DropContentType.VIDEO -> {
            val videoUri = attachment?.asUriOrNull()

            if (videoUri != null) {
                DropVideoPlayer(
                    videoUri = videoUri,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Tap play to watch this clip.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Video clip",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (videoUri != null) {
                                "Watch here or open it in another app."
                            } else {
                                "Attachment unavailable."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { attachment?.let(onOpen) },
                enabled = attachment != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (videoUri != null) "Open video externally" else "Play video")
            }
        }

        else -> return
    }
}

@Composable
private fun OtherDropsMap(
    drops: List<Drop>,
    selectedDropId: String?,
    currentLocation: LatLng?,
    notificationRadiusMeters: Double,
    onDropClick: (Drop) -> Unit
) {
    val context = LocalContext.current
    val baseMarkerBitmap = remember {
        BitmapFactory.decodeResource(
            context.resources,
            R.drawable.explorer_drop_marker
        )?.let { bitmap ->
            if (bitmap.config == Bitmap.Config.ARGB_8888) {
                bitmap
            } else {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            }
        }
    }
    val businessMarkerDescriptor = remember {
        runCatching {
            BitmapFactory.decodeResource(
                context.resources,
                R.drawable.business_drop_marker
            )?.let { bitmap ->
                val argbBitmap = if (bitmap.config == Bitmap.Config.ARGB_8888) {
                    bitmap
                } else {
                    bitmap.copy(Bitmap.Config.ARGB_8888, false)
                }
                BitmapDescriptorFactory.fromBitmap(argbBitmap)
            }
        }.getOrElse { error ->
            Log.e("GeoDrop", "Failed to load business drop marker", error)
            null
        }
    }
    val markerDescriptorCache = remember(baseMarkerBitmap) { mutableMapOf<Float, BitmapDescriptor>() }

    fun descriptorForHue(hue: Float): BitmapDescriptor {
        markerDescriptorCache[hue]?.let { return it }
        val descriptor = baseMarkerBitmap?.let { base ->
            val tinted = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(tinted)
            canvas.drawBitmap(base, 0f, 0f, null)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = PorterDuffColorFilter(
                    android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.8f, 1f)),
                    PorterDuff.Mode.SRC_ATOP
                )
                alpha = 200
            }
            canvas.drawBitmap(base, 0f, 0f, paint)
            BitmapDescriptorFactory.fromBitmap(tinted)
        } ?: BitmapDescriptorFactory.defaultMarker(hue)
        markerDescriptorCache[hue] = descriptor
        return descriptor
    }

    val cameraPositionState = rememberCameraPositionState()
    val uiSettings = remember { MapUiSettings(zoomControlsEnabled = true) }

    LaunchedEffect(drops, selectedDropId, currentLocation) {
        val targetDrop = drops.firstOrNull { it.id == selectedDropId }
        val target = targetDrop?.let { LatLng(it.lat, it.lng) }
            ?: currentLocation
            ?: drops.firstOrNull()?.let { LatLng(it.lat, it.lng) }
        if (target != null) {
            val zoomLevel = if (targetDrop != null) 18f else 15f
            val update = CameraUpdateFactory.newLatLngZoom(target, zoomLevel)
            cameraPositionState.animate(update)
        }
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxSize()
            .consumeMapGesturesInParent(),
        cameraPositionState = cameraPositionState,
        uiSettings = uiSettings
    ) {
        currentLocation?.let { location ->
            if (notificationRadiusMeters > 0.0) {
                Circle(
                    center = location,
                    radius = notificationRadiusMeters,
                    strokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    strokeWidth = 2f,
                    fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    zIndex = 0f
                )
            }

            Marker(
                state = MarkerState(location),
                title = "Your current location",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                zIndex = 1f
            )
        }

        val selectedDrop = selectedDropId?.let { id -> drops.firstOrNull { it.id == id } }
        selectedDrop?.let { drop ->
            val dropPosition = LatLng(drop.lat, drop.lng)
            Circle(
                center = dropPosition,
                radius = DROP_PICKUP_RADIUS_METERS,
                strokeColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                strokeWidth = 2f,
                fillColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                zIndex = 1f
            )
        }

        drops.forEach { drop ->
            val position = LatLng(drop.lat, drop.lng)
            val snippetParts = mutableListOf<String>()
            val snippetDescription = drop.description?.takeIf { it.isNotBlank() }
                ?: drop.text.takeIf { it.isNotBlank() }
                ?: when (drop.contentType) {
                    DropContentType.PHOTO -> "Preview the photo in the drop list."
                    DropContentType.AUDIO -> "Open the drop list to play this recording."
                    DropContentType.VIDEO -> "Open the drop list to watch this clip."
                    DropContentType.TEXT -> ""
                }
            if (!snippetDescription.isNullOrBlank()) {
                snippetParts.add(snippetDescription)
            }
            formatTimestamp(drop.createdAt)?.let { snippetParts.add("Dropped $it") }
            drop.groupCode?.takeIf { !it.isNullOrBlank() }?.let { snippetParts.add("Group $it") }
            snippetParts.add("Lat: %.5f, Lng: %.5f".format(drop.lat, drop.lng))
            snippetParts.add("Likes: ${drop.likeCount}")
            if (drop.isNsfw) {
                snippetParts.add("Marked as adult content")
            }

            val isSelected = drop.id == selectedDropId

            val markerIcon = when {
                drop.isBusinessDrop() && businessMarkerDescriptor != null -> businessMarkerDescriptor
                isSelected -> descriptorForHue(BitmapDescriptorFactory.HUE_BLUE)
                drop.isNsfw -> descriptorForHue(BitmapDescriptorFactory.HUE_MAGENTA)
                else -> descriptorForHue(likeHueFor(drop.likeCount))
            }

            Marker(
                state = MarkerState(position),
                title = drop.displayTitle(),
                snippet = snippetParts.joinToString("\n"),
                icon = markerIcon,
                alpha = if (isSelected) 1f else 0.9f,
                zIndex = if (isSelected) 2f else 0f,
                onClick = {
                    onDropClick(drop)
                    false
                }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Modifier.consumeMapGesturesInParent(): Modifier {
    val view = LocalView.current
    return pointerInteropFilter { event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_POINTER_DOWN -> {
                view.parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_POINTER_UP -> {
                view.parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        false
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

private fun likeHueFor(likes: Long): Float {
    return when {
        likes >= 25 -> BitmapDescriptorFactory.HUE_AZURE
        likes >= 10 -> BitmapDescriptorFactory.HUE_GREEN
        likes >= 5 -> BitmapDescriptorFactory.HUE_YELLOW
        likes >= 1 -> BitmapDescriptorFactory.HUE_ORANGE
        else -> BitmapDescriptorFactory.HUE_RED
    }
}

private fun formatReactionSummary(likeCount: Long, dislikeCount: Long): String {
    val likeLabel = if (likeCount == 1L) {
        "1 like"
    } else {
        "$likeCount likes"
    }
    val dislikeLabel = if (dislikeCount == 1L) {
        "1 dislike"
    } else {
        "$dislikeCount dislikes"
    }
    return "$likeLabel · $dislikeLabel"
}

private fun resolveDropMediaAttachment(context: Context, drop: Drop): DropMediaAttachment? {
    val data = drop.mediaData?.takeIf { it.isNotBlank() }
    val preferredMime = drop.mediaMimeType?.takeIf { it.isNotBlank() }
    if (data != null) {
        val (subDir, defaultMime, defaultExtension) = when (drop.contentType) {
            DropContentType.AUDIO -> Triple("audio", preferredMime ?: "audio/mpeg", "m4a")
            DropContentType.VIDEO -> Triple("video", preferredMime ?: "video/mp4", "mp4")
            else -> Triple("media", preferredMime ?: "application/octet-stream", "bin")
        }

        decodeDropMediaToTempFile(
            context = context,
            base64Data = data,
            mimeType = preferredMime,
            subDir = subDir,
            defaultMime = defaultMime,
            defaultExtension = defaultExtension
        )?.let { decoded ->
            return DropMediaAttachment.Local(decoded.uri, decoded.mimeType)
        }
    }

    val url = drop.mediaUrl?.takeIf { it.isNotBlank() } ?: return null
    return DropMediaAttachment.Link(url)
}

private fun decodeDropMediaToTempFile(
    context: Context,
    base64Data: String,
    mimeType: String?,
    subDir: String,
    defaultMime: String,
    defaultExtension: String
): DropDecodedMedia? {
    return try {
        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
        val resolvedMime = mimeType?.takeIf { it.isNotBlank() } ?: defaultMime
        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(resolvedMime)
            ?.takeIf { it.isNotBlank() }
            ?: defaultExtension
        val directory = File(context.cacheDir, subDir).apply { if (!exists()) mkdirs() }
        val file = File.createTempFile("geodrop_media_", ".${extension}", directory)
        FileOutputStream(file).use { output -> output.write(bytes) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        DropDecodedMedia(uri, resolvedMime)
    } catch (error: Exception) {
        Log.e("GeoDrop", "Failed to decode drop media", error)
        null
    }
}

private fun openDropMediaAttachment(context: Context, attachment: DropMediaAttachment) {
    when (attachment) {
        is DropMediaAttachment.Link -> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(attachment.url))
            runCatching { context.startActivity(intent) }
                .onFailure {
                    Toast.makeText(context, "No app found to open this media.", Toast.LENGTH_SHORT).show()
                }
        }

        is DropMediaAttachment.Local -> {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(attachment.uri, attachment.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.grantUriPermission(
                context.packageName,
                attachment.uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            runCatching { context.startActivity(intent) }
                .onFailure {
                    Toast.makeText(context, "No app found to open this media.", Toast.LENGTH_SHORT).show()
                }
        }
    }
}

private data class DropDecodedMedia(val uri: Uri, val mimeType: String)

private sealed class DropMediaAttachment {
    data class Link(val url: String) : DropMediaAttachment()
    data class Local(val uri: Uri, val mimeType: String) : DropMediaAttachment()
}

private fun DropMediaAttachment.asUriOrNull(): Uri? = when (this) {
    is DropMediaAttachment.Link -> url.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
    is DropMediaAttachment.Local -> uri
}

@Composable
private fun LikeToggleButton(
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val buttonModifier = modifier.heightIn(min = 40.dp)
    if (selected) {
        FilledTonalButton(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier
        ) {
            Icon(icon, contentDescription = contentDescription)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier
        ) {
            Icon(icon, contentDescription = contentDescription)
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
                DropTitleText(
                    drop = drop,
                    style = MaterialTheme.typography.bodyLarge,
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
                val context = LocalContext.current
                val mediaAttachment = remember(
                    drop.id,
                    drop.mediaUrl,
                    drop.mediaData,
                    drop.mediaMimeType
                ) {
                    resolveDropMediaAttachment(context, drop)
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(Modifier.height(4.dp))

                    val typeLabel = when (drop.contentType) {
                        DropContentType.TEXT -> "Text note"
                        DropContentType.PHOTO -> "Photo drop"
                        DropContentType.AUDIO -> "Audio drop"
                        DropContentType.VIDEO -> "Video drop"
                    }

                    val descriptionText = drop.description?.takeIf { it.isNotBlank() }
                    descriptionText?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = supportingColor
                        )
                    }

                    when (drop.contentType) {
                        DropContentType.PHOTO -> {
                            val imageData = when (mediaAttachment) {
                                is DropMediaAttachment.Link -> mediaAttachment.url
                                is DropMediaAttachment.Local -> mediaAttachment.uri
                                else -> drop.mediaLabel()
                            }
                            if (imageData != null) {
                                val imageRequest = remember(imageData) {
                                    ImageRequest.Builder(context)
                                        .data(imageData)
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
                        }

                        DropContentType.VIDEO -> {
                            val videoUri = mediaAttachment?.asUriOrNull()
                                ?: drop.mediaLabel()?.let { Uri.parse(it) }
                            if (videoUri != null) {
                                DropVideoPlayer(
                                    videoUri = videoUri,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        DropContentType.AUDIO -> {
                            val audioUri = mediaAttachment?.asUriOrNull()
                                ?: drop.mediaLabel()?.let { Uri.parse(it) }
                            if (audioUri != null) {
                                DropAudioPlayer(
                                    audioUri = audioUri,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        DropContentType.TEXT -> Unit
                    }

                    formatTimestamp(drop.createdAt)?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = supportingColor
                        )
                    }

                    Text(
                        text = formatReactionSummary(drop.likeCount, drop.dislikeCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = supportingColor
                    )

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

@Composable
private fun DropAudioPlayer(
    audioUri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mediaItem = remember(audioUri) { MediaItem.fromUri(audioUri) }
    val exoPlayer = remember(mediaItem) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(mediaItem)
            playWhenReady = false
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Audio clip",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    PlayerControlView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        player = exoPlayer
                        setShowTimeoutMs(0)
                    }
                },
                update = { controlView ->
                    if (controlView.player !== exoPlayer) {
                        controlView.player = exoPlayer
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropContentTypeSection(
    selected: DropContentType,
    onSelect: (DropContentType) -> Unit,
    showHeader: Boolean = true
) {
    val options = remember {
        listOf(
            DropContentTypeOption(
                type = DropContentType.TEXT,
                title = "Text",
                description = "Share a written message for people nearby.",
                icon = Icons.Rounded.Edit
            ),
            DropContentTypeOption(
                type = DropContentType.PHOTO,
                title = "Photo",
                description = "Capture a photo with your camera that others can open.",
                icon = Icons.Rounded.PhotoCamera
            ),
            DropContentTypeOption(
                type = DropContentType.VIDEO,
                title = "Video",
                description = "Record a short clip for nearby explorers to watch.",
                icon = Icons.Rounded.Videocam
            ),
            DropContentTypeOption(
                type = DropContentType.AUDIO,
                title = "Audio",
                description = "Record a quick voice message for nearby explorers.",
                icon = Icons.Rounded.Mic
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showHeader) {
            Text("Drop content", style = MaterialTheme.typography.titleSmall)
        }

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
    businessName: String?,
    businessCategories: List<BusinessCategory>,
    showHeader: Boolean = true
) {
    val options = remember(businessCategories) {
        businessDropTypeOptionsFor(businessCategories).map { optionCopy ->
            BusinessDropTypeOption(
                type = optionCopy.type,
                title = optionCopy.title,
                description = optionCopy.description,
                icon = when (optionCopy.type) {
                    DropType.RESTAURANT_COUPON -> Icons.Rounded.Storefront
                    DropType.TOUR_STOP -> Icons.Rounded.Flag
                    DropType.COMMUNITY -> Icons.Rounded.Public
                }
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val header = businessName?.takeIf { it.isNotBlank() }?.let { "Business tools for $it" }
            ?: "Business tools"
        if (showHeader) {
            Text(header, style = MaterialTheme.typography.titleSmall)
        }

        val selectedOption = options.firstOrNull { it.type == dropType } ?: options.firstOrNull()

        if (options.size > 1) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        onClick = { onDropTypeChange(option.type) },
                        selected = option.type == dropType,
                        modifier = Modifier.weight(1f),
                        label = { Text(option.title) },
                        icon = { Icon(option.icon, contentDescription = null) }
                    )
                }
            }

            Crossfade(targetState = selectedOption, label = "businessDropTypeDescription") { option ->
                val message = option?.description.orEmpty()
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (selectedOption != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = selectedOption.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Based on your categories, we'll publish this as ${selectedOption.title}.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = selectedOption.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BusinessDropTemplatesSection(
    templates: List<BusinessDropTemplate>,
    onApply: (BusinessDropTemplate) -> Unit,
    showHeader: Boolean = true
) {
    var showSuggestions by remember(templates) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showHeader) {
            Text("Drop ideas for your categories", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Use a template to pre-fill your drop with a ready-made idea.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedButton(
            onClick = { showSuggestions = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Explore personalized ideas",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Preview templates curated for your business categories.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showSuggestions) {
        BusinessDropTemplatesDialog(
            templates = templates,
            onApply = onApply,
            onDismiss = { showSuggestions = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BusinessDropTemplatesDialog(
    templates: List<BusinessDropTemplate>,
    onApply: (BusinessDropTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Personalized drop ideas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close drop ideas"
                        )
                    }
                }

                var currentTemplateIndex by remember(templates) { mutableStateOf(0) }
                LaunchedEffect(templates) {
                    currentTemplateIndex = 0
                }
                val activeTemplate = templates.getOrNull(currentTemplateIndex)

                if (activeTemplate != null) {
                    Crossfade(
                        targetState = activeTemplate,
                        label = "activeBusinessTemplate"
                    ) { template ->
                        BusinessDropTemplateCard(
                            template = template,
                            onApply = { chosenTemplate ->
                                onApply(chosenTemplate)
                                onDismiss()
                            }
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Idea ${currentTemplateIndex + 1} of ${templates.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                currentTemplateIndex = (currentTemplateIndex + 1) % templates.size
                            },
                            enabled = templates.size > 1,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Next idea")
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "No ideas available yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BusinessDropTemplateCard(
    template: BusinessDropTemplate,
    onApply: (BusinessDropTemplate) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Inspired by ${template.category.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val dropTypeIcon = when (template.dropType) {
                DropType.COMMUNITY -> Icons.Rounded.Public
                DropType.RESTAURANT_COUPON -> Icons.Rounded.Storefront
                DropType.TOUR_STOP -> Icons.Rounded.Flag
            }
            val dropTypeLabel = when (template.dropType) {
                DropType.COMMUNITY -> "Community drop"
                DropType.RESTAURANT_COUPON -> "Business offer"
                DropType.TOUR_STOP -> "Tour stop"
            }
            val contentTypeIcon = when (template.contentType) {
                DropContentType.TEXT -> Icons.Rounded.Edit
                DropContentType.PHOTO -> Icons.Rounded.PhotoCamera
                DropContentType.AUDIO -> Icons.Rounded.Mic
                DropContentType.VIDEO -> Icons.Rounded.Videocam
            }
            val contentTypeLabel = when (template.contentType) {
                DropContentType.TEXT -> "Text"
                DropContentType.PHOTO -> "Photo"
                DropContentType.AUDIO -> "Audio"
                DropContentType.VIDEO -> "Video"
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TemplateTag(
                    text = dropTypeLabel,
                    icon = dropTypeIcon,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                TemplateTag(
                    text = "$contentTypeLabel content",
                    icon = contentTypeIcon,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Text(
                text = template.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            template.callToAction?.let { message ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (template.caption.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Suggested caption",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = template.caption,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Suggested message",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = template.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Applies the drop type, format, and copy automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            FilledTonalButton(
                onClick = { onApply(template) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use this idea")
            }
        }
    }
}

@Composable
private fun TemplateTag(
    text: String,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun BusinessRedemptionSection(
    redemptionCode: TextFieldValue,
    onRedemptionCodeChange: (TextFieldValue) -> Unit,
    redemptionLimit: TextFieldValue,
    onRedemptionLimitChange: (TextFieldValue) -> Unit,
    showHeader: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showHeader) {
            Text("Offer security", style = MaterialTheme.typography.titleSmall)
        }

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
private const val MAX_DECAY_DAYS = 365

@Composable
private fun DropVisibilitySection(
    visibility: DropVisibility,
    onVisibilityChange: (DropVisibility) -> Unit,
    groupCodeInput: TextFieldValue,
    onGroupCodeInputChange: (TextFieldValue) -> Unit,
    joinedGroups: List<String>,
    onSelectGroupCode: (String) -> Unit,
    showHeader: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showHeader) {
            Text("Drop visibility", style = MaterialTheme.typography.titleSmall)
        }

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
                    Text("Only groups you created appear here. Create and manage codes from the menu.")
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
                "Create a group in Manage group codes to share private drops with your crew."
            } else {
                "Groups you created: ${joinedGroups.joinToString()}."
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
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }
    val borderWidth = if (selected) 2.dp else 1.dp

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .border(BorderStroke(borderWidth, borderColor), cardShape),
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

private enum class ExplorerDestination { Discover, MyDrops, Collected }

private enum class AccountAuthMode {
    SIGN_IN,
    REGISTER
}

private enum class AccountType { EXPLORER, BUSINESS }

private enum class DropVisibility { Public, GroupOnly }

/** Tiny helper to show snackbars from non-suspend places. */
private fun SnackbarHostState.showMessage(scope: kotlinx.coroutines.CoroutineScope, msg: String) {
    scope.launch { showSnackbar(msg) }
}
