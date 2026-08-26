package com.kitheapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.location.Location
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
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
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Help
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
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material3.*
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SheetValue
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kitheapp.ui.theme.GeoDropThemeTokens
import com.kitheapp.ui.account.EditProfileDialog
import com.kitheapp.ui.account.AccountAuthDialog
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kitheapp.BuildConfig
import com.kitheapp.R
import com.kitheapp.data.AccountDeletionReceipt
import com.kitheapp.data.BusinessCategory
import com.kitheapp.data.CollectedNote
import com.kitheapp.data.BusinessDropTemplate
import com.kitheapp.data.Drop
import com.kitheapp.data.DropContentType
import com.kitheapp.data.ExperienceAnalytics
import com.kitheapp.data.GuestAccountUpgrade
import com.kitheapp.data.GroupMembership
import com.kitheapp.data.GroupAlreadyExistsException
import com.kitheapp.data.GroupRole
import com.kitheapp.data.displayTitle
import com.kitheapp.data.displayTitleParts
import com.kitheapp.data.mediaLabel
import com.kitheapp.data.FirestoreRepo
import com.kitheapp.data.DropLikeStatus
import com.kitheapp.data.LegalConsentRepo
import com.kitheapp.data.LegalPolicyManifest
import com.kitheapp.data.LegalConsentGateway
import com.kitheapp.data.DebugDemoR5EntryGateway
import com.kitheapp.data.MediaStorageRepo
import com.kitheapp.data.NoteInventory
import com.kitheapp.data.UserDataSyncRepository
import com.kitheapp.data.DropType
import com.kitheapp.data.DropReleaseAvailability
import com.kitheapp.data.releaseAvailability
import com.kitheapp.data.ownerExplanation
import com.kitheapp.data.HuntBuilderState
import com.kitheapp.data.HuntChain
import com.kitheapp.data.HuntStepDraft
import com.kitheapp.data.isHuntDrop
import com.kitheapp.data.huntStepLabel
import com.kitheapp.data.UserProfile
import com.kitheapp.data.ExplorerUsername
import com.kitheapp.data.UserMode
import com.kitheapp.data.dropTemplatesFor
import com.kitheapp.data.businessDropTypeOptionsFor
import com.kitheapp.data.UserRole
import com.kitheapp.data.RedemptionResult
import com.kitheapp.data.FirebaseR5EntryGateway
import com.kitheapp.data.R5EntryGateway
import com.kitheapp.data.R5EntryChannel
import com.kitheapp.data.R5EntryException
import com.kitheapp.data.R5EntryFailureReason
import com.kitheapp.data.R5EntryRequest
import com.kitheapp.data.R5ExperienceAvailability
import com.kitheapp.data.R5ExperienceMembership
import com.kitheapp.data.R5PendingUnlock
import com.kitheapp.data.FirebaseR6ParticipantGateway
import com.kitheapp.data.R6CollectionReceipt
import com.kitheapp.data.R6DiscoveryState
import com.kitheapp.data.R6DropDiscovery
import com.kitheapp.data.R6ParticipantException
import com.kitheapp.data.R6ParticipantGateway
import com.kitheapp.data.R6ParticipantPolicy
import com.kitheapp.data.R6TrailProgress
import com.kitheapp.data.R6UnlockFailureReason
import com.kitheapp.data.R6UnlockRequest
import com.kitheapp.data.R6UnlockResult
import com.kitheapp.data.FirebaseR7OrganizerGateway
import com.kitheapp.data.FirebaseR9AccountGateway
import com.kitheapp.data.R7OrganizerAccessState
import com.kitheapp.data.R7OrganizerAccessStatus
import com.kitheapp.data.R7OrganizerGateway
import com.kitheapp.data.R9AccountGateway
import com.kitheapp.data.R9BlockedHost
import com.kitheapp.data.R9JoinedExperience
import com.kitheapp.data.R9ReportStatus
import com.kitheapp.data.applyUserLike
import com.kitheapp.data.isBusinessDrop
import com.kitheapp.data.isRedeemedBy
import com.kitheapp.data.remainingRedemptions
import com.kitheapp.data.requiresRedemption
import com.kitheapp.data.userLikeStatus
import com.kitheapp.data.isBusiness
import com.kitheapp.ui.navigation.ExperienceNavigationItem
import com.kitheapp.ui.navigation.GeoDropAccountDestination
import com.kitheapp.ui.navigation.GeoDropExperienceTopBar
import com.kitheapp.ui.navigation.GeoDropJoinExperienceDialog
import com.kitheapp.ui.navigation.GeoDropNoExperienceState
import com.kitheapp.ui.navigation.GeoDropParticipantNavigationBar
import com.kitheapp.ui.navigation.GeoDropParticipantStateHost
import com.kitheapp.ui.navigation.ParticipantDestination
import com.kitheapp.ui.navigation.R4NavigationPolicy
import com.kitheapp.ui.participant.R6CollectionContent
import com.kitheapp.ui.participant.R6DiscoveryPresentation
import com.kitheapp.ui.participant.R6NearbyContent
import com.kitheapp.ui.participant.r6DistanceLabel
import com.kitheapp.ui.organizer.R7OrganizerAccessDialog
import com.kitheapp.ui.organizer.R7OrganizerContent
import kotlinx.coroutines.delay
import com.kitheapp.data.isExpired
import com.kitheapp.data.remainingDecayMillis
import com.kitheapp.data.decayAtMillis
import com.kitheapp.data.likeStatus
import com.kitheapp.geo.DropCollectionResult
import com.kitheapp.geo.DropCollector
import com.kitheapp.geo.pickupFailureMessage
import com.kitheapp.geo.toCollectionRequest
import com.kitheapp.util.ExplorerAccountStore
import com.kitheapp.util.GroupPreferences
import com.kitheapp.util.PilotFeatureFlags
import com.kitheapp.util.NotificationPreferences
import com.kitheapp.util.ContextualPermissionAction
import com.kitheapp.util.ContextualPermissionIntent
import com.kitheapp.util.ContextualPermissionPolicy
import com.kitheapp.util.PermissionGrantState
import com.kitheapp.util.R5EntryStore
import com.kitheapp.util.R5EntryParser
import com.kitheapp.util.formatTimestamp
import com.kitheapp.util.TermsPreferences
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.ktx.Firebase
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
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
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
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
import com.kitheapp.ui.components.PermissionPrimer
import com.kitheapp.ui.components.PermissionPrimerVariant

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropHereScreen(
    onNearbyAlertsEnabled: () -> Unit = {},
    onNearbyAlertsDisabled: () -> Unit = {},
    skipFirstRunOnboarding: Boolean = false,
    r5EntrySessionId: String? = null,
    r5ExperienceCode: String? = null,
    r5EntryStore: R5EntryStore? = null,
    r5EntryGateway: R5EntryGateway? = null,
    r6ParticipantGateway: R6ParticipantGateway? = null,
    r7OrganizerGateway: R7OrganizerGateway? = null,
    r9AccountGateway: R9AccountGateway? = null,
    legalConsentGateway: LegalConsentGateway? = null,
    debugDeviceDemoEnabled: Boolean = false,
    initialParticipantDestination: ParticipantDestination = ParticipantDestination.NEARBY,
    openOrganizerAccessOnLaunch: Boolean = false
) {
    val ctx = LocalContext.current
    val networkAvailable = rememberNetworkAvailable()
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val auth = remember { FirebaseAuth.getInstance() }
    val effectiveR5Store = remember(ctx, r5EntryStore) { r5EntryStore ?: R5EntryStore(ctx) }
    val effectiveR5Gateway = r5EntryGateway ?: remember { FirebaseR5EntryGateway() }
    val effectiveR6Gateway = r6ParticipantGateway ?: remember { FirebaseR6ParticipantGateway() }
    val effectiveR7Gateway = r7OrganizerGateway ?: remember { FirebaseR7OrganizerGateway() }
    val effectiveR9Gateway = r9AccountGateway ?: remember { FirebaseR9AccountGateway() }
    val r6TargetEnabled = r6ParticipantGateway != null || PilotFeatureFlags.redesignBackendEnabled
    val r7TargetEnabled = r7OrganizerGateway != null || PilotFeatureFlags.redesignBackendEnabled
    val r9TargetEnabled = r9AccountGateway != null || PilotFeatureFlags.redesignBackendEnabled
    val localDeviceDemoActive = debugDeviceDemoEnabled &&
        r5ExperienceCode == DebugDemoR5EntryGateway.DEVICE_DEMO_CODE
    // Declared up here, ahead of the sign-in helpers below, because the guest
    // upgrade at sign-in needs the merge callable (task 4.6).
    val repo = remember { FirestoreRepo() }
    // Same reason: the sign-in path reports the guest-upgrade outcome through
    // `status` and refreshes My Drops after a merge.
    var status by remember { mutableStateOf<String?>(null) }
    var myDropsRefreshToken by remember { mutableStateOf(0) }
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    val termsPrefs = remember(ctx) { TermsPreferences(ctx) }
    val effectiveLegalConsentGateway = legalConsentGateway ?: remember { LegalConsentRepo() }
    var legalManifest by remember { mutableStateOf<LegalPolicyManifest?>(null) }
    var legalManifestError by remember { mutableStateOf<String?>(null) }
    var legalManifestLoading by remember { mutableStateOf(true) }
    var legalAcceptanceSubmitting by remember { mutableStateOf(false) }
    var legalManifestRefreshToken by remember { mutableIntStateOf(0) }
    var legalAcceptanceRequestToken by remember { mutableIntStateOf(0) }
    var hasAcceptedTerms by remember { mutableStateOf(false) }
    var hasViewedOnboarding by remember(skipFirstRunOnboarding) {
        mutableStateOf(skipFirstRunOnboarding || termsPrefs.hasViewedFirstRunOnboarding())
    }
    var showOnboardingHelp by remember { mutableStateOf(false) }
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
    var r5UnlockAccountGate by rememberSaveable { mutableStateOf(false) }
    var r5AuthCompletionPath by remember { mutableStateOf<String?>(null) }
    var r5PendingUnlockDropId by rememberSaveable {
        mutableStateOf(effectiveR5Store.pendingUnlock()?.dropId)
    }
    var r5PreciseGrantResumeToken by remember { mutableIntStateOf(0) }
    var r5UnlockResumeStartedForDrop by rememberSaveable { mutableStateOf<String?>(null) }
    var showR5NearbyPrimer by remember { mutableStateOf(false) }
    var showR5PrecisePrimer by remember { mutableStateOf(false) }
    var r5PrecisePrimerDismissedForDrop by rememberSaveable { mutableStateOf<String?>(null) }
    var showR5NotificationPrimer by remember { mutableStateOf(false) }
    var pendingExplorerUsername by remember { mutableStateOf<String?>(null) }
    var showBusinessOnboarding by remember { mutableStateOf(false) }
    var showBusinessWelcome by remember { mutableStateOf(false) }
    var accountGoogleSigningIn by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }
    var showAccountDataDialog by remember { mutableStateOf(false) }
    var accountDeletionReceipt by remember { mutableStateOf<AccountDeletionReceipt?>(null) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var termsPrivacyDialogTab by remember { mutableStateOf<Int?>(null) }
    var permissionStateVersion by remember { mutableIntStateOf(0) }
    var foregroundLocationRequested by rememberSaveable { mutableStateOf(false) }
    var preciseLocationRequested by rememberSaveable { mutableStateOf(false) }
    var notificationPermissionRequested by rememberSaveable { mutableStateOf(false) }
    var alertPermissionFlowToken by remember { mutableIntStateOf(0) }
    var resumeAlertPermissionFlow by remember { mutableStateOf(false) }
    var showExplorerProfile by remember { mutableStateOf(false) }
    var explorerDestination by rememberSaveable { mutableStateOf(ExplorerDestination.Discover.name) }
    var explorerUsernameField by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var explorerDisplayNameField by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var explorerProfileSubmitting by remember { mutableStateOf(false) }
    var explorerProfileError by remember { mutableStateOf<String?>(null) }
    var signingOut by remember { mutableStateOf(false) }
    var pickupCelebrationDrop by remember { mutableStateOf<Drop?>(null) }
    var pickupCelebrationVisible by remember { mutableStateOf(false) }
    var waitingForEmailVerification by remember { mutableStateOf(false) }
    var verificationAccountType by remember { mutableStateOf<AccountType?>(null) }
    val defaultWebClientId = remember(ctx) {
        val buildConfigClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()

        val resourceClientId = runCatching { ctx.getString(R.string.default_web_client_id).trim() }
            .getOrDefault("")

        when {
            buildConfigClientId.isNotBlank() -> buildConfigClientId
            resourceClientId.isNotBlank() -> resourceClientId
            else -> ""
        }
    }
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

    LaunchedEffect(pickupCelebrationDrop) {
        if (pickupCelebrationDrop != null) {
            pickupCelebrationVisible = true
            delay(2200)
            pickupCelebrationVisible = false
            delay(300)
            pickupCelebrationDrop = null
        }
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
    }

    /**
     * Task 4.6 — say what happened to the guest's activity.
     *
     * Linking is the silent case: the uid never changed, so there is nothing to
     * report. A merge is worth confirming, and a *failed* merge has to be said
     * out loud — the user is signed in, so the flow looks like it worked, and
     * the drops they made as a guest are the thing they would notice missing.
     */
    fun reportGuestContentOutcome(outcome: GuestAccountUpgrade.GuestContent?) {
        when (outcome) {
            GuestAccountUpgrade.GuestContent.MERGED -> {
                myDropsRefreshToken += 1
                status = ctx.getString(R.string.guest_upgrade_content_merged)
            }

            GuestAccountUpgrade.GuestContent.MERGE_FAILED -> {
                status = ctx.getString(R.string.guest_upgrade_content_not_merged)
            }

            else -> Unit
        }
    }

    fun dismissAccountAuthDialog() {
        if (accountAuthSubmitting || accountGoogleSigningIn) return
        showAccountSignIn = false
        r5UnlockAccountGate = false
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
        val needsExplorerUsername = accountAuthMode == AccountAuthMode.REGISTER &&
            accountType == AccountType.EXPLORER &&
            !r5UnlockAccountGate
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
            // Task 4.6 — registering as a guest links the anonymous account in
            // place, so the uid and everything attached to it survives. Signing
            // in to an account that already exists cannot link, so the guest's
            // content is handed over server-side instead.
            when (selectedMode) {
                AccountAuthMode.SIGN_IN ->
                    GuestAccountUpgrade.signInWithEmail(auth, repo, email, password)

                AccountAuthMode.REGISTER ->
                    GuestAccountUpgrade.registerWithEmail(auth, email, password)
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
                val current = auth.currentUser
                val guestContent = authTask.result?.guestContent
                reportGuestContentOutcome(guestContent)

                if (r5UnlockAccountGate) {
                    r5AuthCompletionPath = when (guestContent) {
                        GuestAccountUpgrade.GuestContent.LINKED -> "LINK"
                        GuestAccountUpgrade.GuestContent.MERGED,
                        GuestAccountUpgrade.GuestContent.MERGE_FAILED -> "MERGE"
                        else -> null
                    }
                    accountAuthSubmitting = false
                    resetAccountAuthFields(clearEmail = true)
                    showAccountSignIn = false
                    r5UnlockAccountGate = false
                    return@addOnCompleteListener
                }

                if (selectedMode == AccountAuthMode.SIGN_IN) {
                    if (current?.isEmailVerified == false) {
                        current.sendEmailVerification()
                            .addOnCompleteListener { verificationTask ->
                                verificationAccountType = selectedType
                                waitingForEmailVerification = verificationTask.isSuccessful
                                accountAuthSubmitting = verificationTask.isSuccessful
                                accountAuthError = null
                                accountAuthStatus = if (verificationTask.isSuccessful) {
                                    "Waiting for email verification. We sent a link to ${current.email ?: "your inbox"}."
                                } else {
                                    waitingForEmailVerification = false
                                    accountAuthSubmitting = false
                                    "Verify your email to continue. Couldn't send a link automatically—try again later."
                                }
                            }
                        return@addOnCompleteListener
                    }

                    if (sanitizedExplorerUsername != null) {
                        accountAuthStatus = ctx.getString(R.string.explorer_profile_status_claiming)
                        pendingExplorerUsername = sanitizedExplorerUsername
                        return@addOnCompleteListener
                    }

                    accountAuthSubmitting = false
                    resetAccountAuthFields(clearEmail = true)
                    showAccountSignIn = false
                    if (selectedType == AccountType.BUSINESS) {
                        showBusinessOnboarding = true
                        showBusinessWelcome = true
                    }
                } else {
                    val newUser = current
                    if (newUser == null) {
                        accountAuthSubmitting = false
                        accountAuthError = "Couldn't create your account. Try again."
                        return@addOnCompleteListener
                    }

                    newUser.sendEmailVerification()
                        .addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                pendingExplorerUsername = sanitizedExplorerUsername
                                verificationAccountType = selectedType
                                waitingForEmailVerification = true
                                accountAuthSubmitting = true
                                accountAuthStatus = "Waiting for email verification. Check ${newUser.email ?: "your inbox"} for a verification link."
                                accountAuthError = null
                            } else {
                                waitingForEmailVerification = false
                                accountAuthSubmitting = false
                                accountAuthError = "Couldn't send a verification email. Try again."
                            }
                        }
                }
            } else {
                accountAuthSubmitting = false
                val exception = authTask.exception
                val message = when {
                    selectedMode == AccountAuthMode.SIGN_IN &&
                            exception is FirebaseAuthInvalidCredentialsException -> {
                        "Incorrect password. Please try again."
                    }

                    selectedMode == AccountAuthMode.SIGN_IN &&
                            exception is FirebaseAuthInvalidUserException -> {
                        "We couldn't find an account with that email."
                    }

                    else -> exception?.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: if (selectedMode == AccountAuthMode.REGISTER) {
                            "Couldn't create your account. Try again."
                        } else {
                            "Couldn't sign you in. Check your email and password."
                        }
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
        fun googleSignInErrorMessage(
            apiException: ApiException?,
            defaultMessage: String = "Google sign-in failed. Try again."
        ): String {
            val statusMessage = apiException?.statusCode?.let { statusCode ->
                when (statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Google sign-in was cancelled."
                    GoogleSignInStatusCodes.DEVELOPER_ERROR,
                    GoogleSignInStatusCodes.INVALID_ACCOUNT -> "Google sign-in is misconfigured. Provide a valid web client ID."
                    GoogleSignInStatusCodes.SIGN_IN_FAILED,
                    GoogleSignInStatusCodes.SIGN_IN_REQUIRED,
                    GoogleSignInStatusCodes.NETWORK_ERROR -> "Google sign-in failed. Try again."
                    else -> null
                }
            }

            return statusMessage
                ?: apiException?.localizedMessage?.takeIf { it.isNotBlank() }
                ?: defaultMessage
        }
        if (result.resultCode != Activity.RESULT_OK) {
            accountGoogleSigningIn = false
            accountAuthError = googleSignInErrorMessage(
                apiException = GoogleSignIn.getSignedInAccountFromIntent(result.data).exception as? ApiException,
                defaultMessage = if (result.resultCode == Activity.RESULT_CANCELED) {
                    "Google sign-in was cancelled."
                } else {
                    "Google sign-in failed. Try again."
                }
            )
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
            runCatching {
                // Task 4.6 — link the guest session in place where possible, and
                // move its content server-side where the credential already
                // belongs to an account.
                GuestAccountUpgrade.signInWithCredential(auth, repo, credential)
                    .addOnCompleteListener { authTask ->
                        accountAuthSubmitting = false
                        accountGoogleSigningIn = false
                        if (authTask.isSuccessful) {
                            resetAccountAuthFields(clearEmail = true)
                            val selectedType = accountType
                            showAccountSignIn = false
                            val guestContent = authTask.result?.guestContent
                            reportGuestContentOutcome(guestContent)
                            if (r5UnlockAccountGate) {
                                r5AuthCompletionPath = when (guestContent) {
                                    GuestAccountUpgrade.GuestContent.LINKED -> "LINK"
                                    GuestAccountUpgrade.GuestContent.MERGED,
                                    GuestAccountUpgrade.GuestContent.MERGE_FAILED -> "MERGE"
                                    else -> null
                                }
                                r5UnlockAccountGate = false
                                return@addOnCompleteListener
                            }
                            // `createdAccount` rather than `isNewUser`: linking a
                            // guest to a fresh Google credential is how that
                            // account comes into existence, but Firebase reports
                            // isNewUser = false for a link.
                            val isNewUser = authTask.result?.createdAccount == true
                            if (selectedType == AccountType.BUSINESS && isNewUser) {
                                showBusinessOnboarding = true
                                showBusinessWelcome = true
                            }
                        } else {
                            val message = authTask.exception?.localizedMessage?.takeIf { it.isNotBlank() }
                                ?: "Couldn't sign you in with Google. Try again."
                            accountAuthError = message
                        }
                    }
            }.onFailure { error ->
                accountAuthSubmitting = false
                accountGoogleSigningIn = false
                accountAuthError = error.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Couldn't sign you in with Google. Try again."
            }
        } catch (error: ApiException) {
            accountGoogleSigningIn = false
            accountAuthError = googleSignInErrorMessage(error)
        }
    }

    fun startAccountGoogleSignIn() {
        if (accountAuthSubmitting || accountGoogleSigningIn) return
        if (defaultWebClientId.isBlank()) {
            accountAuthError = "Google sign-in isn't configured. Provide a valid web client ID via google-services.json or the GOOGLE_WEB_CLIENT_ID property."
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

    LaunchedEffect(legalManifestRefreshToken) {
        legalManifestLoading = true
        legalManifestError = null
        hasAcceptedTerms = false
        runCatching { effectiveLegalConsentGateway.fetchManifest() }
            .onSuccess { manifest ->
                legalManifest = manifest
            }
            .onFailure { error ->
                legalManifest = null
                legalManifestError = error.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Kithe's approved legal policies are unavailable."
            }
        legalManifestLoading = false
    }

    LaunchedEffect(legalManifest?.version, currentUser?.uid) {
        val manifest = legalManifest ?: run {
            hasAcceptedTerms = false
            return@LaunchedEffect
        }
        if (!termsPrefs.hasAcceptedTerms(manifest.version)) {
            hasAcceptedTerms = false
            return@LaunchedEffect
        }

        val userId = currentUser?.uid
        if (userId == null) {
            hasAcceptedTerms = true
            return@LaunchedEffect
        }
        if (termsPrefs.hasRecordedServerAcceptance(userId, manifest.version)) {
            hasAcceptedTerms = true
            return@LaunchedEffect
        }

        hasAcceptedTerms = false
        legalAcceptanceSubmitting = true
        runCatching { effectiveLegalConsentGateway.recordAcceptance(manifest.version) }
            .onSuccess {
                termsPrefs.recordServerAcceptance(userId, manifest.version)
                hasAcceptedTerms = true
                legalManifestError = null
            }
            .onFailure { error ->
                legalManifestError = error.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Couldn't record acceptance. Try again."
            }
        legalAcceptanceSubmitting = false
    }

    LaunchedEffect(legalAcceptanceRequestToken) {
        if (legalAcceptanceRequestToken == 0) return@LaunchedEffect
        val manifest = legalManifest ?: run {
            hasAcceptedTerms = false
            legalManifestError = "Kithe's approved legal policies are unavailable."
            return@LaunchedEffect
        }

        legalAcceptanceSubmitting = true
        legalManifestError = null
        val userId = currentUser?.uid
        val result = runCatching {
            if (userId != null) {
                effectiveLegalConsentGateway.recordAcceptance(manifest.version)
            }
        }
        if (result.isSuccess) {
            termsPrefs.recordAcceptance(manifest.version)
            if (userId != null) {
                termsPrefs.recordServerAcceptance(userId, manifest.version)
            }
            hasAcceptedTerms = true
        } else {
            hasAcceptedTerms = false
            legalManifestError = result.exceptionOrNull()?.localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Couldn't record acceptance. Try again."
        }
        legalAcceptanceSubmitting = false
    }

    val verifiedUser = currentUser?.takeIf { user ->
        !user.isAnonymous && (skipFirstRunOnboarding || user.isEmailVerified)
    }

    // The approved R5 flow is Experience-first: a missing or unverified account is
    // view-only guest state, never an account-choice landing page.
    val userMode = if (verifiedUser != null) UserMode.SIGNED_IN else UserMode.GUEST

    LaunchedEffect(userMode) {
        when (userMode) {
            UserMode.SIGNED_IN -> Unit

            UserMode.GUEST -> {
                explorerDestination = ExplorerDestination.Discover.name
            }

        }
    }

    LaunchedEffect(
        currentUser,
        accountAuthSubmitting,
        accountGoogleSigningIn,
        signingOut,
        hasAcceptedTerms,
        hasViewedOnboarding,
        showAccountSignIn
    ) {
        if (currentUser == null) {
            showAccountMenu = false
            showAccountDataDialog = false
            if (!accountAuthSubmitting && !accountGoogleSigningIn && !showAccountSignIn) {
                accountAuthMode = AccountAuthMode.SIGN_IN
                accountType = AccountType.EXPLORER
                resetAccountAuthFields(clearEmail = true)
                waitingForEmailVerification = false
                verificationAccountType = null
            }
        }
    }

    val noteInventory = remember { NoteInventory(ctx) }
    var collectedNotes by remember { mutableStateOf(noteInventory.getCollectedNotes()) }
    var ignoredDropIds by remember { mutableStateOf(noteInventory.getIgnoredDropIds()) }
    val collectedDropIds = remember(collectedNotes) { collectedNotes.map { it.id }.toSet() }
    var collectedPendingRemove by remember { mutableStateOf<CollectedNote?>(null) }
    var collectedSelectedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser?.uid) {
        noteInventory.setActiveUser(currentUser?.uid)
        collectedNotes = noteInventory.getCollectedNotes()
        ignoredDropIds = noteInventory.getIgnoredDropIds()
    }

    LaunchedEffect(waitingForEmailVerification, currentUser) {
        if (!waitingForEmailVerification) return@LaunchedEffect

        val user = currentUser ?: run {
            waitingForEmailVerification = false
            accountAuthSubmitting = false
            accountAuthStatus = null
            verificationAccountType = null
            return@LaunchedEffect
        }

        while (waitingForEmailVerification) {
            val reloadResult = runCatching { user.reload().await() }
            if (reloadResult.isSuccess && user.isEmailVerified) {
                waitingForEmailVerification = false
                accountAuthSubmitting = pendingExplorerUsername != null
                accountAuthError = null
                verificationAccountType?.let { selectedType ->
                    if (pendingExplorerUsername == null) {
                        resetAccountAuthFields(clearEmail = true)
                        showAccountSignIn = false
                        if (selectedType == AccountType.BUSINESS) {
                            showBusinessOnboarding = true
                            showBusinessWelcome = true
                        }
                    }
                }
                accountAuthStatus = if (pendingExplorerUsername != null) {
                    ctx.getString(R.string.explorer_profile_status_claiming)
                } else {
                    null
                }
                verificationAccountType = null
                break
            }

            if (reloadResult.isFailure) {
                waitingForEmailVerification = false
                accountAuthSubmitting = false
                verificationAccountType = null
                accountAuthError = reloadResult.exceptionOrNull()?.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Couldn't confirm email verification. Try signing in again."
                break
            }

            delay(3000)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                collectedNotes = noteInventory.getCollectedNotes()
                ignoredDropIds = noteInventory.getIgnoredDropIds()
                permissionStateVersion += 1
                if (resumeAlertPermissionFlow) {
                    resumeAlertPermissionFlow = false
                    alertPermissionFlowToken += 1
                }
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

    accountDeletionReceipt?.let { receipt ->
        AlertDialog(
            onDismissRequest = { accountDeletionReceipt = null },
            title = { Text("Account deleted") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your Kithe account deletion completed successfully.")
                    Text("Receipt: ${receipt.receiptId}")
                    Text("Completed: ${receipt.completedAt}")
                    Text("Policy: ${receipt.policyVersion}")
                    Text(
                        "Removed ${receipt.deletedDrops} drops and ${receipt.deletedMediaObjects} media objects.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { accountDeletionReceipt = null }) {
                    Text("Done")
                }
            }
        )
    }

    val legalUserId = currentUser?.uid
    val legalConsentSatisfied = legalManifest?.let { manifest ->
        hasAcceptedTerms &&
            termsPrefs.hasAcceptedTerms(manifest.version) &&
            (legalUserId == null || termsPrefs.hasRecordedServerAcceptance(
                legalUserId,
                manifest.version
            ))
    } == true

    if (!legalConsentSatisfied) {
        TermsAcceptanceScreen(
            manifest = legalManifest,
            isLoading = legalManifestLoading,
            isAccepting = legalAcceptanceSubmitting,
            errorMessage = legalManifestError,
            onRetry = { legalManifestRefreshToken += 1 },
            onOpenPolicy = { url ->
                runCatching {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }.onFailure {
                    legalManifestError = "No browser is available to open this policy."
                }
            },
            onAccept = { legalAcceptanceRequestToken += 1 },
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

    if (showOnboardingHelp) {
        FirstRunOnboardingScreen(
            onContinue = { showOnboardingHelp = false },
            onExit = { showOnboardingHelp = false },
            showExitButton = false
        )
        return
    }

    if (showAccountSignIn) {
        AccountAuthDialog(
            unlockGate = r5UnlockAccountGate,
            isRegister = accountAuthMode == AccountAuthMode.REGISTER,
            onRegisterChanged = { register ->
                if (!accountAuthSubmitting && !accountGoogleSigningIn) {
                    accountType = AccountType.EXPLORER
                    accountAuthMode = if (register) {
                        AccountAuthMode.REGISTER
                    } else {
                        AccountAuthMode.SIGN_IN
                    }
                    accountAuthError = null
                    accountAuthStatus = null
                }
            },
            isGuestUpgrade = accountTypeSelectionLocked || r5UnlockAccountGate,
            showOrganizerGuidance = !accountTypeSelectionLocked,
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

    val snackbar = remember { SnackbarHostState() }
    val manageGroupsSnackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(r5AuthCompletionPath, currentUser?.uid) {
        val upgradePath = r5AuthCompletionPath ?: return@LaunchedEffect
        if (currentUser?.isAnonymous != false) return@LaunchedEffect
        runCatching {
            effectiveR5Gateway.recordAuthCompletion(
                entrySessionId = r5EntrySessionId
                    ?: effectiveR5Store.activeEntrySessionId()
                    ?: return@runCatching,
                upgradePath = upgradePath,
                pendingUnlockResumed = r5PendingUnlockDropId != null
            )
        }
        r5AuthCompletionPath = null
    }

    val fused = remember { LocationServices.getFusedLocationProviderClient(ctx) }
    val mediaStorage = remember { MediaStorageRepo() }
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
    var dropContentType by remember { mutableStateOf(DropContentType.TEXT) }
    var dropType by remember { mutableStateOf(DropType.COMMUNITY) }
    var note by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var capturedPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var capturedAudioUri by rememberSaveable { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var groupCodeInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var redemptionLimitInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var decayDaysInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var showOtherDropsMap by remember { mutableStateOf(false) }
    var otherDrops by remember { mutableStateOf<List<Drop>>(emptyList()) }
    var otherDropsLoading by remember { mutableStateOf(false) }
    var otherDropsRefreshing by remember { mutableStateOf(false) }
    var otherDropsError by remember { mutableStateOf<String?>(null) }
    var otherDropsCurrentLocation by remember { mutableStateOf<LatLng?>(null) }
    var otherDropsLocationAccuracyMeters by remember { mutableStateOf<Double?>(null) }
    // Drops whose proximity was proven this session by attemptUnlock. Content is revealed
    // for these; nothing about *where* the user was is kept (task 3.5).
    var unlockedDropIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var unlockInProgressDropId by remember { mutableStateOf<String?>(null) }
    var r6Discoveries by remember { mutableStateOf<List<R6DropDiscovery>>(emptyList()) }
    var r6Collection by remember { mutableStateOf<List<R6CollectionReceipt>>(emptyList()) }
    var r6TrailProgress by remember { mutableStateOf<List<R6TrailProgress>>(emptyList()) }
    var r6BlockedHostIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var r6DiscoveryLoading by remember { mutableStateOf(false) }
    var r6DiscoveryRefreshing by remember { mutableStateOf(false) }
    var r6DiscoveryError by remember { mutableStateOf<String?>(null) }
    var r6CollectionLoading by remember { mutableStateOf(false) }
    var r6CollectionError by remember { mutableStateOf<String?>(null) }
    var r6SelectedDropId by rememberSaveable { mutableStateOf<String?>(null) }
    var r6UnlockingDropId by remember { mutableStateOf<String?>(null) }
    var r6UnlockResult by remember { mutableStateOf<R6UnlockResult?>(null) }
    var r6UnlockError by remember { mutableStateOf<R6ParticipantException?>(null) }
    var r6RefreshToken by remember { mutableIntStateOf(0) }
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
    var votingDropIds by remember { mutableStateOf(setOf<String>()) }
    val dropReportReasons = remember { DefaultReportReasons }
    var browseReportDialogOpen by remember { mutableStateOf(false) }
    var browseReportSelectedReasons by remember { mutableStateOf(setOf<String>()) }
    var browseReportError by remember { mutableStateOf<String?>(null) }
    var browseReportProcessing by remember { mutableStateOf(false) }
    var browseReportTarget by remember { mutableStateOf<ReportableDrop?>(null) }
    var browseReportingDropId by remember { mutableStateOf<String?>(null) }

    var showBlockedCreators by remember { mutableStateOf(false) }
    var blockedCreatorIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var blockedCreatorsLoading by remember { mutableStateOf(false) }
    var r9ExperienceHistory by remember { mutableStateOf<List<R9JoinedExperience>>(emptyList()) }
    var r9BlockedHosts by remember { mutableStateOf<List<R9BlockedHost>>(emptyList()) }
    var r9ReportStatuses by remember { mutableStateOf<List<R9ReportStatus>>(emptyList()) }
    var r9AccountLoading by remember { mutableStateOf(false) }
    var r9AccountError by remember { mutableStateOf<String?>(null) }
    var r9RefreshToken by remember { mutableIntStateOf(0) }
    var myDrops by remember { mutableStateOf<List<Drop>>(emptyList()) }
    var myDropsLoading by remember { mutableStateOf(false) }
    var myDropsError by remember { mutableStateOf<String?>(null) }
    var myDropsCurrentLocation by remember { mutableStateOf<LatLng?>(null) }
    var myDropsLocationAccuracyMeters by remember { mutableStateOf<Double?>(null) }
    var myDropsDeletingId by remember { mutableStateOf<String?>(null) }
    var myDropsPendingDelete by remember { mutableStateOf<Drop?>(null) }
    var myDropsSelectedId by remember { mutableStateOf<String?>(null) }
    var myDropsSortKey by rememberSaveable { mutableStateOf(DropSortOption.NEWEST.name) }
    var myDropCountHint by remember { mutableStateOf<Int?>(null) }
    var myDropPendingReviewHint by remember { mutableStateOf<Int?>(null) }
    var showManageGroups by remember { mutableStateOf(false) }
    var showGroupMenu by remember { mutableStateOf(false) }
    var showDropComposer by remember { mutableStateOf(false) }
    var showHuntBuilder by remember { mutableStateOf(false) }
    var huntBuilderState by remember { mutableStateOf<HuntBuilderState?>(null) }
    var huntBuilderSubmitting by remember { mutableStateOf(false) }
    var huntBuilderError by remember { mutableStateOf<String?>(null) }
    var showBusinessDashboard by remember { mutableStateOf(false) }
    var businessDrops by remember { mutableStateOf<List<Drop>>(emptyList()) }
    var businessExperienceAnalytics by remember {
        mutableStateOf<List<ExperienceAnalytics>>(emptyList())
    }
    var businessDashboardLoading by remember { mutableStateOf(false) }
    var businessDashboardError by remember { mutableStateOf<String?>(null) }
    var businessDashboardRefreshToken by remember { mutableStateOf(0) }
    var selectedParticipantDestination by rememberSaveable(initialParticipantDestination) {
        mutableStateOf(initialParticipantDestination.name)
    }
    var showOrganizerTools by rememberSaveable { mutableStateOf(false) }
    var organizerSignInPrompted by rememberSaveable(openOrganizerAccessOnLaunch) {
        mutableStateOf(false)
    }
    var organizerToolsAutoOpened by rememberSaveable(openOrganizerAccessOnLaunch) {
        mutableStateOf(false)
    }
    var nearbyAlertsEnabled by remember { mutableStateOf(notificationPrefs.areNearbyAlertsEnabled()) }
    var showNotificationPermissionRecovery by remember { mutableStateOf(false) }

    val permissionActivity = ctx as? Activity
    val hasFineLocation = remember(permissionStateVersion) {
        ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    val hasCoarseLocation = remember(permissionStateVersion) {
        ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    val hasForegroundLocation = hasFineLocation || hasCoarseLocation
    val hasNotificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    // Browsing needs approximate location only (task 3.2), so COARSE alone is a full
    // grant here. Precise is a separate, later ask — see preciseLocationState.
    val foregroundLocationState = when {
        hasForegroundLocation -> PermissionGrantState.GRANTED
        !foregroundLocationRequested -> PermissionGrantState.REQUESTABLE
        permissionActivity != null && (
            ActivityCompat.shouldShowRequestPermissionRationale(
                permissionActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                permissionActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) -> PermissionGrantState.REQUESTABLE
        else -> PermissionGrantState.BLOCKED
    }
    // Precise is asked for at the unlock attempt, so its state is tracked separately
    // from the coarse grant that browsing runs on.
    val preciseLocationState = when {
        hasFineLocation -> PermissionGrantState.GRANTED
        !preciseLocationRequested -> PermissionGrantState.REQUESTABLE
        permissionActivity != null && ActivityCompat.shouldShowRequestPermissionRationale(
            permissionActivity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) -> PermissionGrantState.REQUESTABLE
        else -> PermissionGrantState.BLOCKED
    }
    val notificationPermissionState = when {
        hasNotificationPermission -> PermissionGrantState.GRANTED
        !notificationPermissionRequested -> PermissionGrantState.REQUESTABLE
        permissionActivity != null && ActivityCompat.shouldShowRequestPermissionRationale(
            permissionActivity,
            Manifest.permission.POST_NOTIFICATIONS
        ) -> PermissionGrantState.REQUESTABLE
        else -> PermissionGrantState.BLOCKED
    }

    val foregroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        foregroundLocationRequested = true
        permissionStateVersion += 1
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            otherDropsRefreshToken += 1
            snackbar.showMessage(scope, "Location enabled for Nearby.")
        } else {
            snackbar.showMessage(
                scope,
                "Location is off. You can keep browsing and enable it later from Nearby."
            )
        }
        scope.launch {
            runCatching {
                effectiveR5Gateway.recordClientEvent(
                    eventName = "location_permission_result",
                    entrySessionId = r5EntrySessionId ?: effectiveR5Store.activeEntrySessionId(),
                    experienceCode = r5ExperienceCode ?: selectedExplorerGroupCode,
                    params = mapOf(
                        "precision" to "APPROXIMATE",
                        "result" to if (granted) "GRANTED" else "DENIED",
                        "context" to "NEARBY"
                    )
                )
            }
        }
    }
    // Precise location is requested only when the user attempts to unlock a drop
    // (task 3.3). Android may retain the permission choice, but nothing starts a stream
    // and the precise fix is discarded once the proximity question is answered.
    val preciseLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        preciseLocationRequested = true
        permissionStateVersion += 1
        if (granted) {
            r5PreciseGrantResumeToken += 1
        } else {
            r5PrecisePrimerDismissedForDrop = r5PendingUnlockDropId
            snackbar.showMessage(
                scope,
                "Unlocking needs precise location. Browsing still works without it."
            )
        }
        scope.launch {
            runCatching {
                effectiveR5Gateway.recordClientEvent(
                    eventName = "location_permission_result",
                    entrySessionId = r5EntrySessionId ?: effectiveR5Store.activeEntrySessionId(),
                    experienceCode = r5ExperienceCode ?: selectedExplorerGroupCode,
                    dropId = r5PendingUnlockDropId,
                    params = mapOf(
                        "precision" to "PRECISE",
                        "result" to if (granted) "GRANTED" else "DENIED",
                        "context" to "UNLOCK"
                    )
                )
            }
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionRequested = true
        permissionStateVersion += 1
        if (granted) {
            alertPermissionFlowToken += 1
        } else {
            showNotificationPermissionRecovery = true
        }
    }

    fun openApplicationSettings() {
        ctx.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", ctx.packageName, null)
            )
        )
    }

    fun performNearbyLocationAccessRequest() {
        when (
            ContextualPermissionPolicy.nextAction(
                intent = ContextualPermissionIntent.NEARBY_DISCOVERY,
                onboardingComplete = hasViewedOnboarding,
                foregroundLocation = foregroundLocationState
            )
        ) {
            ContextualPermissionAction.REQUEST_FOREGROUND_LOCATION -> {
                foregroundLocationRequested = true
                // Coarse only. Precise is requested at the moment of an unlock attempt
                // (task 3.3), not to browse a map.
                foregroundLocationLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }
            ContextualPermissionAction.OPEN_FOREGROUND_LOCATION_SETTINGS ->
                openApplicationSettings()
            else -> Unit
        }
    }

    fun requestNearbyLocationAccess() {
        if (foregroundLocationState == PermissionGrantState.BLOCKED) {
            openApplicationSettings()
        } else if (foregroundLocationState == PermissionGrantState.REQUESTABLE) {
            showR5NearbyPrimer = true
        }
    }

    LaunchedEffect(
        alertPermissionFlowToken,
        notificationPermissionState
    ) {
        if (alertPermissionFlowToken == 0) return@LaunchedEffect
        when (
            ContextualPermissionPolicy.nextAction(
                intent = ContextualPermissionIntent.ENABLE_NEARBY_ALERTS,
                onboardingComplete = hasViewedOnboarding,
                notificationsRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                notifications = notificationPermissionState
            )
        ) {
            ContextualPermissionAction.REQUEST_NOTIFICATIONS -> {
                alertPermissionFlowToken = 0
                notificationPermissionRequested = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            ContextualPermissionAction.OPEN_NOTIFICATION_SETTINGS -> {
                alertPermissionFlowToken = 0
                showNotificationPermissionRecovery = true
            }
            ContextualPermissionAction.ENABLE_NEARBY_ALERTS -> {
                alertPermissionFlowToken = 0
                nearbyAlertsEnabled = true
                notificationPrefs.setNearbyAlertsEnabled(true)
                onNearbyAlertsEnabled()
                snackbar.showMessage(scope, "Alerts are on for the experiences you've joined.")
            }
            else -> alertPermissionFlowToken = 0
        }
    }

    DisposableEffect(groupPrefs) {
        val listener = GroupPreferences.ChangeListener { groups, _ ->
            joinedGroups = groups
        }
        groupPrefs.addChangeListener(listener)
        joinedGroups = groupPrefs.getMemberships()
        onDispose { groupPrefs.removeChangeListener(listener) }
    }

    LaunchedEffect(localDeviceDemoActive) {
        if (localDeviceDemoActive) {
            groupPrefs.addGroup(
                GroupMembership(
                    code = DebugDemoR5EntryGateway.DEVICE_DEMO_CODE,
                    ownerId = null,
                    role = GroupRole.SUBSCRIBER
                )
            )
            joinedGroups = groupPrefs.getMemberships()
        }
    }

    LaunchedEffect(explorerGroups) {
        val availableCodes = explorerGroups.map { it.code }
        val resolved = R4NavigationPolicy.resolveActiveExperience(
            currentCode = selectedExplorerGroupCode,
            availableCodes = availableCodes
        )
        if (resolved != selectedExplorerGroupCode) {
            selectedExplorerGroupCode = resolved
        }
    }

    DisposableEffect(currentUser?.uid, localDeviceDemoActive) {
        val uid = currentUser?.uid
        if (uid.isNullOrBlank() || localDeviceDemoActive) {
            userDataSync.stop()
        } else {
            userDataSync.start(uid)
        }
        onDispose { userDataSync.stop() }
    }

    // Task 4.6 — record who is signed in, for the notification receivers that
    // resolve a uid from this store when they run without an activity.
    //
    // Guest content no longer moves here. It used to try, and in the wrong
    // direction: the whole block ran only when the *current* user was anonymous,
    // so guest→account never migrated at all — what it actually did was copy a
    // real account's display name onto a fresh guest session on sign-out, then
    // throw when rules refused the drops half. Continuity now happens at the
    // sign-in call site, which is the only place that still holds the guest's
    // session token, by linking the account in place or merging server-side.
    LaunchedEffect(currentUser?.uid) {
        val user = currentUser ?: return@LaunchedEffect
        explorerAccountStore.setLastExplorerUid(user.uid)
    }

    fun handleSignOut() {
        if (signingOut) return

        showAccountMenu = false
        signingOut = true
        showBusinessDashboard = false
        showBusinessOnboarding = false
        showDropComposer = false
        showManageGroups = false
        showAccountSignIn = false
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
                auth.signInAnonymously().await()
            }

            signingOut = false

            if (result.isSuccess) {
                selectedParticipantDestination = ParticipantDestination.NEARBY.name
                showOrganizerTools = false
                explorerDestination = ExplorerDestination.Discover.name
                snackbar.showMessage(scope, "Browsing as a guest.")
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
    var r7OrganizerAccessState by remember { mutableStateOf(R7OrganizerAccessState()) }
    var r7OrganizerAccessLoading by remember { mutableStateOf(false) }
    var showR7OrganizerAccess by remember { mutableStateOf(false) }

    val businessCategories = userProfile?.businessCategories.orEmpty()

    LaunchedEffect(businessCategories, dropType) {
        val permittedTypes = businessDropTypeOptionsFor(businessCategories).map { it.type }
        if (permittedTypes.isNotEmpty() && dropType !in permittedTypes) {
            dropType = permittedTypes.first()
        }
    }

    val currentUserId = currentUser?.uid

    LaunchedEffect(
        currentUserId,
        currentUser?.isAnonymous,
        explorerGroups,
        r9TargetEnabled,
        r9RefreshToken
    ) {
        val userId = currentUserId
        if (
            userId.isNullOrBlank() || currentUser?.isAnonymous != false || !r9TargetEnabled
        ) {
            r9ExperienceHistory = emptyList()
            r9BlockedHosts = emptyList()
            r9ReportStatuses = emptyList()
            r9AccountLoading = false
            r9AccountError = null
            return@LaunchedEffect
        }
        r9AccountLoading = true
        r9AccountError = null
        val history = runCatching {
            effectiveR9Gateway.loadExperienceHistory(userId, explorerGroups)
        }
        val blocks = runCatching { effectiveR9Gateway.loadBlockedHosts(userId) }
        val reports = runCatching { effectiveR9Gateway.loadReportStatuses(userId) }
        history.onSuccess { r9ExperienceHistory = it }
        blocks.onSuccess {
            r9BlockedHosts = it
            r6BlockedHostIds = it.mapTo(mutableSetOf(), R9BlockedHost::hostId)
        }
        reports.onSuccess { r9ReportStatuses = it }
        val firstFailure = listOf(history, blocks, reports).firstOrNull(Result<*>::isFailure)
        r9AccountError = firstFailure?.exceptionOrNull()?.localizedMessage
            ?.takeIf(String::isNotBlank)
            ?.let { "Some account details could not be loaded. Choose Retry below." }
        r9AccountLoading = false
    }

    LaunchedEffect(currentUserId, currentUser?.isAnonymous, r7TargetEnabled, permissionStateVersion) {
        val userId = currentUserId
        if (userId.isNullOrBlank() || currentUser?.isAnonymous != false || !r7TargetEnabled) {
            r7OrganizerAccessState = R7OrganizerAccessState()
            r7OrganizerAccessLoading = false
            return@LaunchedEffect
        }
        r7OrganizerAccessLoading = true
        runCatching { effectiveR7Gateway.loadAccessState(userId) }
            .onSuccess { r7OrganizerAccessState = it }
            .onFailure {
                r7OrganizerAccessState = R7OrganizerAccessState()
                userProfileError = it.localizedMessage ?: "Failed to load Organizer access."
            }
        r7OrganizerAccessLoading = false
    }

    LaunchedEffect(currentUserId) {
        notificationPrefs.setActiveUser(currentUserId)
        nearbyAlertsEnabled = notificationPrefs.areNearbyAlertsEnabled()
    }

    LaunchedEffect(
        permissionStateVersion,
        hasNotificationPermission,
        nearbyAlertsEnabled
    ) {
        if (nearbyAlertsEnabled && !hasNotificationPermission) {
            nearbyAlertsEnabled = false
            notificationPrefs.setNearbyAlertsEnabled(false)
            onNearbyAlertsDisabled()
        }
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
            explorerDisplayNameField = TextFieldValue(userProfile?.displayName.orEmpty())
            explorerProfileError = message
            explorerProfileSubmitting = false
            showExplorerProfile = true
            snackbar.showMessage(scope, message)
        }
    }

    /**
     * Precise, one-shot. Used where the user's own position *is* the content being
     * authored — placing a drop or a hunt step. Never used to browse.
     */
    suspend fun getLatestLocation(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (!hasForegroundLocation) return@withContext null
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

    /**
     * Approximate, one-shot. Everything the map and the nearby lists need: distance
     * labels, sorting, and centring. Task 3.2 — browsing never asks for GPS-grade
     * precision, and nothing here is retained or streamed.
     */
    suspend fun getApproximateLocation(): ApproximateLocationFix? = withContext(Dispatchers.IO) {
        if (!hasForegroundLocation) return@withContext null
        val fresh = try {
            val cts = CancellationTokenSource()
            Tasks.await(
                fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            )
        } catch (_: Exception) {
            null
        }

        val loc = fresh ?: try {
            Tasks.await(fused.lastLocation)
        } catch (_: Exception) {
            null
        }

        loc?.let { location ->
            ApproximateLocationFix(
                position = LatLng(location.latitude, location.longitude),
                accuracyMeters = location.accuracy
                    .takeIf { location.hasAccuracy() && it > 0f }
                    ?.toDouble()
            )
        }
    }

    /**
     * Precise, one-shot, requested at the moment of an unlock attempt and discarded as
     * soon as the proximity question is answered (task 3.3, direction doc steps 2–5).
     *
     * Returns null when the fix is missing, stale, or too imprecise to decide a
     * [DROP_PICKUP_RADIUS_METERS] question — callers must fail closed, exactly as
     * [DropCollector] does for the authoritative check.
     */
    suspend fun getPreciseFixForUnlock(): Location? = withContext(Dispatchers.IO) {
        if (!hasFineLocation) return@withContext null
        val fix = try {
            val cts = CancellationTokenSource()
            Tasks.await(fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token))
        } catch (_: Exception) {
            null
        } ?: return@withContext null

        val ageMillis = fix.elapsedRealtimeNanos.takeIf { it > 0L }?.let { nanos ->
            (SystemClock.elapsedRealtimeNanos() - nanos) / 1_000_000
        } ?: Long.MAX_VALUE
        if (ageMillis > UNLOCK_LOCATION_STALE_THRESHOLD_MILLIS) return@withContext null

        val accuracy = fix.accuracy.takeIf { fix.hasAccuracy() && it > 0f } ?: return@withContext null
        if (accuracy > DROP_PICKUP_RADIUS_METERS) return@withContext null

        fix
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

    fun saveExplorerProfile() {
        if (explorerProfileSubmitting) return

        val userId = currentUserId
        if (userId.isNullOrBlank()) {
            explorerProfileError = ctx.getString(R.string.explorer_profile_error_sign_in)
            return
        }

        val desiredUsername = explorerUsernameField.text
        val desiredDisplayName = explorerDisplayNameField.text.trim()
        val currentUsername = userProfile?.username

        // Only validate/update username if it has changed or is newly set
        val sanitizedUsername: String? = if (desiredUsername.isNotEmpty() && desiredUsername != currentUsername) {
            try {
                ExplorerUsername.sanitize(desiredUsername)
            } catch (error: ExplorerUsername.InvalidUsernameException) {
                explorerProfileError = when (error.reason) {
                    ExplorerUsername.ValidationError.TOO_SHORT -> ctx.getString(R.string.explorer_profile_error_too_short)
                    ExplorerUsername.ValidationError.TOO_LONG -> ctx.getString(R.string.explorer_profile_error_too_long)
                    ExplorerUsername.ValidationError.INVALID_CHARACTERS ->
                        ctx.getString(R.string.explorer_profile_error_invalid_characters)
                }
                return
            }
        } else if (desiredUsername.isNotEmpty()) {
            desiredUsername
        } else {
            null
        }

        explorerProfileSubmitting = true
        explorerProfileError = null

        scope.launch {
            try {
                var updated = repo.updateDisplayName(userId, desiredDisplayName)
                if (sanitizedUsername != null && sanitizedUsername != currentUsername) {
                    updated = repo.updateExplorerUsername(userId, sanitizedUsername)
                } else {
                    updated = updated.copy(username = currentUsername)
                }
                userProfile = updated
                showExplorerProfile = false
                val profileLabel = updated.username?.takeIf { it.isNotBlank() }?.let { "@$it" }
                    ?: updated.displayName?.takeIf { it.isNotBlank() }
                    ?: "your account"
                snackbar.showMessage(
                    scope,
                    ctx.getString(R.string.explorer_profile_status_saved, profileLabel)
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

    fun attemptR6Unlock(drop: R6DropDiscovery) {
        if (!r6TargetEnabled || r6UnlockingDropId != null) return
        if (!canParticipate) {
            val pendingTarget = R5PendingUnlock(
                experienceCode = drop.experienceCode,
                dropId = drop.id
            )
            effectiveR5Store.savePendingUnlock(pendingTarget)
            r5PendingUnlockDropId = drop.id
            r5UnlockResumeStartedForDrop = null
            r5PrecisePrimerDismissedForDrop = null
            r5UnlockAccountGate = true
            openAccountAuthDialog(
                initialType = AccountType.EXPLORER,
                initialMode = AccountAuthMode.REGISTER,
                lockAccountType = true
            )
            return
        }
        if (!hasFineLocation) {
            effectiveR5Store.savePendingUnlock(
                R5PendingUnlock(experienceCode = drop.experienceCode, dropId = drop.id)
            )
            r5PendingUnlockDropId = drop.id
            r5UnlockResumeStartedForDrop = null
            r5PrecisePrimerDismissedForDrop = null
            showR5PrecisePrimer = true
            return
        }

        scope.launch {
            r5UnlockResumeStartedForDrop = drop.id
            r6UnlockingDropId = drop.id
            r6UnlockResult = null
            r6UnlockError = null
            Firebase.crashlytics.apply {
                setCustomKey("participant_unlock_path", "R6_SERVER")
                setCustomKey("participant_unlock_stage", "LOCATION_FIX")
                log("R6 unlock started")
            }
            try {
                val fix = getPreciseFixForUnlock() ?: throw R6ParticipantException(
                    reason = R6UnlockFailureReason.ACCURACY_INSUFFICIENT,
                    retryable = true
                )
                Firebase.crashlytics.setCustomKey("participant_unlock_stage", "SERVER_CHECK")
                val result = effectiveR6Gateway.unlock(
                    R6UnlockRequest(
                        dropId = drop.id,
                        entrySessionId = r5EntrySessionId ?: effectiveR5Store.activeEntrySessionId(),
                        latitude = fix.latitude,
                        longitude = fix.longitude,
                        accuracyM = fix.accuracy.toDouble(),
                        capturedAtMillis = fix.time.takeIf { it > 0L } ?: System.currentTimeMillis()
                    )
                )
                r6Collection = listOf(result.receipt) + r6Collection.filterNot {
                    it.dropId == result.receipt.dropId
                }
                r6UnlockResult = result
                effectiveR5Store.clearPendingUnlock()
                r5PendingUnlockDropId = null
                r5UnlockResumeStartedForDrop = null
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Firebase.crashlytics.apply {
                    setCustomKey("participant_unlock_stage", "FOUND")
                    log("R6 unlock succeeded")
                }
                val notificationExperience = drop.experienceCode
                if (
                    PilotFeatureFlags.notificationsEnabled &&
                    !nearbyAlertsEnabled &&
                    !effectiveR5Store.notificationPrimerSeen(notificationExperience)
                ) {
                    effectiveR5Store.markNotificationPrimerSeen(notificationExperience)
                    showR5NotificationPrimer = true
                }
                r6RefreshToken += 1
            } catch (error: R6ParticipantException) {
                r6UnlockError = error
                r5UnlockResumeStartedForDrop = null
                Firebase.crashlytics.apply {
                    setCustomKey("participant_unlock_stage", "FAILED_${error.reason.name}")
                    log("R6 unlock failed: ${error.reason.name}")
                }
            } catch (error: Exception) {
                r6UnlockError = R6ParticipantException(
                    reason = R6UnlockFailureReason.UNKNOWN,
                    retryable = true,
                    cause = error
                )
                r5UnlockResumeStartedForDrop = null
                Firebase.crashlytics.recordException(error)
            } finally {
                r6UnlockingDropId = null
            }
        }
    }

    fun submitR6ReportById(dropId: String, reason: String, narrative: String?) {
        scope.launch {
            runCatching { effectiveR6Gateway.submitReport(dropId, reason, narrative) }
                .onSuccess {
                    r9RefreshToken += 1
                    snackbar.showMessage(scope, "Report submitted. Track its status in Account.")
                }
                .onFailure { error ->
                    snackbar.showMessage(scope, error.localizedMessage ?: "Couldn't submit the report.")
                }
        }
    }

    fun submitR6Report(drop: R6DropDiscovery, reason: String, narrative: String?) {
        submitR6ReportById(drop.id, reason, narrative)
    }

    fun blockR6Host(drop: R6DropDiscovery) {
        scope.launch {
            runCatching { effectiveR6Gateway.blockHost(drop.id) }
                .onSuccess {
                    r6BlockedHostIds = r6BlockedHostIds + drop.ownerId
                    r9RefreshToken += 1
                    r6SelectedDropId = null
                    snackbar.showMessage(scope, "Host blocked.")
                }
                .onFailure { error ->
                    snackbar.showMessage(scope, error.localizedMessage ?: "Couldn't block this host.")
                }
        }
    }

    /**
     * The unlock attempt (task 3.3, direction doc steps 2–5): request precise location
     * *now*, answer the proximity question, then let the fix go. A success is remembered
     * as an unlocked drop id — the record of the unlock, not of where the user was.
     */
    fun attemptUnlock(drop: Drop) {
        if (!canParticipate) {
            val pendingTarget = R5PendingUnlock(
                experienceCode = r5ExperienceCode ?: selectedExplorerGroupCode,
                dropId = drop.id
            )
            effectiveR5Store.savePendingUnlock(pendingTarget)
            r5PendingUnlockDropId = drop.id
            r5UnlockResumeStartedForDrop = null
            r5PrecisePrimerDismissedForDrop = null
            scope.launch {
                runCatching {
                    effectiveR5Gateway.recordClientEvent(
                        eventName = "unlock_attempted",
                        entrySessionId = r5EntrySessionId ?: effectiveR5Store.activeEntrySessionId(),
                        experienceCode = pendingTarget.experienceCode,
                        dropId = drop.id,
                        params = mapOf(
                            "accountState" to "GUEST",
                            "accountGateShown" to true
                        )
                    )
                }
            }
            r5UnlockAccountGate = true
            openAccountAuthDialog(
                initialType = AccountType.EXPLORER,
                initialMode = AccountAuthMode.REGISTER,
                lockAccountType = true
            )
            return
        }
        val expiresAt = drop.decayAtMillis()
        if (expiresAt != null && expiresAt <= System.currentTimeMillis()) {
            snackbar.showMessage(scope, "This drop has already expired.")
            return
        }
        if (!hasFineLocation) {
            val pendingTarget = R5PendingUnlock(
                experienceCode = r5ExperienceCode ?: selectedExplorerGroupCode,
                dropId = drop.id
            )
            effectiveR5Store.savePendingUnlock(pendingTarget)
            r5PendingUnlockDropId = drop.id
            r5UnlockResumeStartedForDrop = null
            r5PrecisePrimerDismissedForDrop = null
            showR5PrecisePrimer = true
            return
        }

        scope.launch {
            if (r5PendingUnlockDropId == drop.id) {
                r5UnlockResumeStartedForDrop = drop.id
                effectiveR5Store.clearPendingUnlock()
                r5PendingUnlockDropId = null
            }
            unlockInProgressDropId = drop.id
            val fix = try {
                getPreciseFixForUnlock()
            } finally {
                unlockInProgressDropId = null
            }
            if (fix == null) {
                snackbar.showMessage(
                    scope,
                    "Couldn't confirm your location accurately enough. Step outside or try again."
                )
                return@launch
            }
            val distance = distanceBetweenMeters(fix.latitude, fix.longitude, drop.lat, drop.lng)
            if (distance > DROP_PICKUP_RADIUS_METERS + fix.accuracy) {
                snackbar.showMessage(
                    scope,
                    "Move within ${DROP_PICKUP_RADIUS_METERS.roundToInt()} meters to unlock this drop."
                )
                return@launch
            }
            unlockedDropIds = unlockedDropIds + drop.id
        }
    }

    LaunchedEffect(
        currentUser?.uid,
        currentUser?.isAnonymous,
        r5PendingUnlockDropId,
        r6TargetEnabled,
        r6Discoveries,
        otherDrops,
        hasFineLocation,
        r5PreciseGrantResumeToken
    ) {
        if (currentUser?.isAnonymous != false) return@LaunchedEffect
        val targetId = r5PendingUnlockDropId ?: return@LaunchedEffect
        if (r5UnlockResumeStartedForDrop == targetId) return@LaunchedEffect
        if (r6TargetEnabled) {
            val target = r6Discoveries.firstOrNull { it.id == targetId } ?: return@LaunchedEffect
            if (hasFineLocation) {
                attemptR6Unlock(target)
            } else if (r5PrecisePrimerDismissedForDrop != targetId) {
                showR5PrecisePrimer = true
            }
            return@LaunchedEffect
        }
        val target = otherDrops.firstOrNull { it.id == targetId } ?: return@LaunchedEffect
        if (hasFineLocation) {
            attemptUnlock(target)
        } else if (r5PrecisePrimerDismissedForDrop != targetId) {
            showR5PrecisePrimer = true
        }
    }

    if (showR5NearbyPrimer) {
        ModalBottomSheet(onDismissRequest = { showR5NearbyPrimer = false }) {
            PermissionPrimer(
                title = stringResource(R.string.r5_location_nearby_title),
                explanation = stringResource(R.string.r5_location_nearby_body),
                privacyPromise = stringResource(R.string.r5_location_nearby_privacy),
                variant = PermissionPrimerVariant.SHEET,
                onAllow = {
                    showR5NearbyPrimer = false
                    performNearbyLocationAccessRequest()
                },
                onNotNow = { showR5NearbyPrimer = false }
            )
        }
    }

    if (showR5PrecisePrimer) {
        ModalBottomSheet(
            onDismissRequest = {
                r5PrecisePrimerDismissedForDrop = r5PendingUnlockDropId
                showR5PrecisePrimer = false
            }
        ) {
            PermissionPrimer(
                title = stringResource(R.string.r5_location_precise_title),
                explanation = stringResource(R.string.r5_location_precise_body),
                privacyPromise = stringResource(R.string.r5_location_precise_privacy),
                variant = PermissionPrimerVariant.SHEET,
                allowLabel = if (preciseLocationState == PermissionGrantState.BLOCKED) {
                    stringResource(R.string.r5_open_settings)
                } else {
                    null
                },
                onAllow = {
                    showR5PrecisePrimer = false
                    if (preciseLocationState == PermissionGrantState.BLOCKED) {
                        r5PrecisePrimerDismissedForDrop = r5PendingUnlockDropId
                        openApplicationSettings()
                    } else {
                        preciseLocationRequested = true
                        preciseLocationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                onNotNow = {
                    r5PrecisePrimerDismissedForDrop = r5PendingUnlockDropId
                    showR5PrecisePrimer = false
                }
            )
        }
    }

    if (showR5NotificationPrimer) {
        AlertDialog(
            onDismissRequest = { showR5NotificationPrimer = false },
            title = { Text(stringResource(R.string.r5_notifications_title)) },
            text = { Text(stringResource(R.string.r5_notifications_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showR5NotificationPrimer = false
                        alertPermissionFlowToken += 1
                    }
                ) {
                    Text(stringResource(R.string.r5_notifications_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { showR5NotificationPrimer = false }) {
                    Text(stringResource(R.string.r5_notifications_not_now))
                }
            }
        )
    }

    fun pickUpDrop(drop: Drop) {
        if (!canParticipate) {
            attemptUnlock(drop)
            return
        }
        val expiresAt = drop.decayAtMillis()
        if (expiresAt != null && expiresAt <= System.currentTimeMillis()) {
            snackbar.showMessage(scope, "This drop has already expired.")
            return
        }
        // Proximity was proven by attemptUnlock, which is the only way to reach this
        // button. DropCollector re-checks with its own precise fix and fails closed,
        // so nothing here is load-bearing for correctness.
        if (drop.id !in unlockedDropIds) {
            attemptUnlock(drop)
            return
        }

        val appContext = ctx.applicationContext
        val userId = currentUserId
        scope.launch {
            val result = DropCollector.collect(
                context = appContext,
                request = drop.toCollectionRequest(),
                userId = userId
            )
            if (result != DropCollectionResult.Collected) {
                snackbar.showMessage(scope, pickupFailureMessage(result))
                return@launch
            }

            val remaining = otherDrops.filterNot { it.id == drop.id }
            otherDrops = remaining
            if (otherDropsSelectedId == drop.id) {
                otherDropsSelectedId = remaining.firstOrNull()?.id
            }
            snackbar.showMessage(scope, "Drop added to your collection.")
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            pickupCelebrationDrop = drop
            val notificationExperience = r5ExperienceCode
                ?: selectedExplorerGroupCode
                ?: effectiveR5Store.activeExperienceCode()
            if (
                PilotFeatureFlags.notificationsEnabled &&
                !nearbyAlertsEnabled &&
                !notificationExperience.isNullOrBlank() &&
                !effectiveR5Store.notificationPrimerSeen(notificationExperience)
            ) {
                effectiveR5Store.markNotificationPrimerSeen(notificationExperience)
                showR5NotificationPrimer = true
            }
            // Refresh the browse map so a newly-unlocked hunt step appears.
            if (drop.isHuntDrop()) {
                otherDropsRefreshToken += 1
            }
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

    fun submitCollectedLike(note: CollectedNote, desiredStatus: DropLikeStatus) {
        if (!canParticipate) {
            snackbar.showMessage(scope, participationRestriction("react to drops"))
            return
        }

        val userId = currentUserId
        if (userId.isNullOrBlank()) {
            snackbar.showMessage(scope, "Sign in to react to drops.")
            return
        }

        val dropId = note.id
        if (dropId.isBlank()) return

        val previousStatus = note.likeStatus()
        if (previousStatus == desiredStatus) return

        val previousLikeCount = note.likeCount

        var updatedLikeCount = previousLikeCount

        when (previousStatus) {
            DropLikeStatus.LIKED -> updatedLikeCount = (updatedLikeCount - 1).coerceAtLeast(0L)
            DropLikeStatus.NONE -> Unit
        }

        when (desiredStatus) {
            DropLikeStatus.LIKED -> updatedLikeCount += 1
            DropLikeStatus.NONE -> Unit
        }

        noteInventory.updateLikeStatus(dropId, updatedLikeCount, desiredStatus)
        collectedNotes = noteInventory.getCollectedNotes()

        votingDropIds = votingDropIds + dropId

        scope.launch {
            try {
                repo.setDropLike(dropId, userId, desiredStatus)
            } catch (e: Exception) {
                noteInventory.updateLikeStatus(dropId, previousLikeCount, previousStatus)
                collectedNotes = noteInventory.getCollectedNotes()
                if (e is FirebaseFirestoreException &&
                    e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                ) {
                    Log.w(
                        "DropHere",
                        "Permission denied while updating reaction for collected drop $dropId for $userId",
                        e
                    )
                } else {
                    Log.e(
                        "DropHere",
                        "Failed to update reaction for collected drop $dropId for $userId",
                        e
                    )
                }
                snackbar.showMessage(scope, "Couldn't update your reaction. Try again.")
            } finally {
                votingDropIds = votingDropIds - dropId
            }
        }
    }

    // Task 3.4 — alerts are membership-scoped and sent by the server, so this only has
    // to keep the messaging token registered. Radius and group codes are no longer part
    // of it: nothing on the device is watching a radius any more.
    LaunchedEffect(nearbyAlertsEnabled) {
        if (nearbyAlertsEnabled && FirebaseAuth.getInstance().currentUser != null) {
            onNearbyAlertsEnabled()
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

    LaunchedEffect(
        r7OrganizerAccessState.status,
        currentUser?.isAnonymous,
        r7TargetEnabled,
        userProfile?.role
    ) {
        val approvedForActiveSurface = if (r7TargetEnabled) {
            r7OrganizerAccessState.status == R7OrganizerAccessStatus.APPROVED &&
                currentUser?.isAnonymous == false
        } else {
            userProfile?.isBusiness() == true
        }
        if (!approvedForActiveSurface) {
            dropType = DropType.COMMUNITY
            showOrganizerTools = false
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

    LaunchedEffect(dropType) {
        if (dropType != DropType.RESTAURANT_COUPON) {
            redemptionLimitInput = TextFieldValue("")
        }
    }

    var pendingPhotoPath by remember { mutableStateOf<String?>(null) }
    var pendingPermissionAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val path = pendingPhotoPath
        if (success && path != null) {
            capturedPhotoPath = path
        } else if (path != null) {
            runCatching { File(path).delete() }
        }
        pendingPhotoPath = null
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
        showDropComposer = false
        if (dropType == DropType.RESTAURANT_COUPON) {
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
            groupCode != null -> "Your drop is out there for the group."
            dropType == DropType.RESTAURANT_COUPON -> "Your offer is live — go find some explorers."
            dropType == DropType.TOUR_STOP -> "Tour stop is out in the world."
            !dropTypeTitle.isNullOrBlank() -> "Your ${dropTypeTitle.lowercase()} is out there."
            else -> when (contentType) {
                DropContentType.TEXT -> "Your note is out in the world."
                DropContentType.PHOTO -> "Your photo is out there waiting to be found."
                DropContentType.AUDIO -> "Your audio drop is out there."
            }
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
        mediaStoragePath: String?,
        redemptionLimit: Int?,
        decayDays: Int?
    ) {
        // Task 2.1 — no fabricated creator id. A drop is authored by a real,
        // non-anonymous account or it is not authored at all. The repository
        // enforces the same rule, and so do the Firestore rules (task 1.2).
        val uid = FirebaseAuth.getInstance().currentUser
            ?.takeIf { !it.isAnonymous }
            ?.uid
            ?: error("Creating a drop requires a signed-in account.")
        val sanitizedMedia = mediaInput?.takeIf { it.isNotBlank() }
        val sanitizedMime = mediaMimeType?.takeIf { it.isNotBlank() }
        val sanitizedRedemptionLimit = redemptionLimit?.takeIf { it > 0 }
        val sanitizedData = mediaData?.takeIf { it.isNotBlank() }
        val sanitizedDecayDays = decayDays?.takeIf { it > 0 }
        val sanitizedText = noteText.trim()
        val sanitizedDescription = descriptionText?.trim()?.takeIf { it.isNotEmpty() }
        val dropperUsername = userProfile?.username?.trim()?.takeIf { it.isNotEmpty() }
        val d = Drop(
            text = sanitizedText,
            description = sanitizedDescription,
            lat = lat,
            lng = lng,
            createdBy = uid,
            createdAt = System.currentTimeMillis(),
            dropperUsername = dropperUsername,
            groupCode = groupCode,
            dropType = dropType,
            businessId = if (dropType != DropType.COMMUNITY) uid else null,
            businessName = if (dropType != DropType.COMMUNITY) userProfile?.businessName else null,
            contentType = contentType,
            mediaUrl = sanitizedMedia,
            mediaMimeType = sanitizedMime,
            mediaData = sanitizedData,
            mediaStoragePath = mediaStoragePath?.takeIf { it.isNotBlank() },
            redemptionLimit = if (dropType == DropType.RESTAURANT_COUPON) sanitizedRedemptionLimit else null,
            decayDays = sanitizedDecayDays
        )

        // Task 2.2 - no client-side NSFW classification. isNsfw/nsfwLabels are
        // server-owned: only the backend moderation pipeline (analyzeOnUpload)
        // may set them. The client always writes the safe values, which the
        // Firestore rules require to be present and false on create.
        val dropToSave = d.copy(
            isNsfw = false,
            nsfwLabels = emptyList()
        )

        repo.addDrop(dropToSave) // suspend (uses Firestore .await() internally)
        uiDone(lat, lng, groupCode, contentType, dropType)
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
                    if (dropContentType != DropContentType.TEXT) {
                        isSubmitting = false
                        snackbar.showMessage(
                            scope,
                            "Private group drops are text-only during the market pilot."
                        )
                        return@launch
                    }
                }
                var mediaUrlResult: String? = null
                var mediaStoragePathResult: String? = null
                var mediaMimeTypeResult: String? = null
                var mediaDataResult: String? = null
                var dropNoteText = note.text
                var dropDescriptionText = description.text
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
                        mediaStoragePathResult = uploadResult.storagePath
                    }

                }

                if (dropType == DropType.RESTAURANT_COUPON) {

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
                addDropAt(
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
                    mediaStoragePath = mediaStoragePathResult,
                    redemptionLimit = redemptionLimitResult,
                    decayDays = decayDaysResult
                )
            } catch (e: Exception) {
                isSubmitting = false
                snackbar.showMessage(scope, "Error: ${e.message}")
            }
        }
    }


    val isBusinessUser = userProfile?.isBusiness() == true
    val r7OrganizerApproved =
        r7OrganizerAccessState.status == R7OrganizerAccessStatus.APPROVED &&
            currentUser?.isAnonymous == false
    val organizerToolsAvailable = r7OrganizerApproved || (!r7TargetEnabled && isBusinessUser)
    val accountOrganizerAccessState = if (!r7TargetEnabled && isBusinessUser) {
        R7OrganizerAccessState(status = R7OrganizerAccessStatus.APPROVED)
    } else {
        r7OrganizerAccessState
    }
    val currentParticipantDestination = R4NavigationPolicy.resolveDestination(
        selectedParticipantDestination
    )

    LaunchedEffect(
        openOrganizerAccessOnLaunch,
        currentUser?.uid,
        currentUser?.isAnonymous,
        r7OrganizerAccessLoading,
        organizerToolsAvailable
    ) {
        if (!openOrganizerAccessOnLaunch) return@LaunchedEffect

        selectedParticipantDestination = ParticipantDestination.ACCOUNT.name
        if (currentUser?.isAnonymous != false) {
            if (!organizerSignInPrompted) {
                organizerSignInPrompted = true
                openAccountAuthDialog(
                    initialType = AccountType.EXPLORER,
                    initialMode = AccountAuthMode.SIGN_IN,
                    lockAccountType = false
                )
            }
            return@LaunchedEffect
        }

        if (
            !r7OrganizerAccessLoading &&
            organizerToolsAvailable &&
            !organizerToolsAutoOpened
        ) {
            organizerToolsAutoOpened = true
            showOrganizerTools = true
        }
    }

    LaunchedEffect(
        currentParticipantDestination,
        selectedExplorerGroupCode,
        unlockInProgressDropId,
        preciseLocationState
    ) {
        Firebase.crashlytics.apply {
            setCustomKey("participant_destination", currentParticipantDestination.name)
            setCustomKey("participant_has_active_experience", selectedExplorerGroupCode != null)
            setCustomKey("participant_unlock_in_progress", unlockInProgressDropId != null)
            setCustomKey("participant_precise_permission", preciseLocationState.name)
            log("Participant surface changed to ${currentParticipantDestination.name}")
        }
    }
    val nearbyVisible = currentParticipantDestination == ParticipantDestination.NEARBY &&
        !showOrganizerTools
    val r6ActiveExperienceCode = selectedExplorerGroupCode
        ?: r5ExperienceCode
        ?: explorerGroups.firstOrNull()?.code

    LaunchedEffect(
        r6TargetEnabled,
        nearbyVisible,
        r6ActiveExperienceCode,
        currentUserId,
        currentUser?.isAnonymous,
        r6RefreshToken
    ) {
        if (!r6TargetEnabled || !nearbyVisible || r6ActiveExperienceCode.isNullOrBlank()) {
            if (!r6TargetEnabled) {
                r6Discoveries = emptyList()
                r6Collection = emptyList()
                r6TrailProgress = emptyList()
                r6BlockedHostIds = emptySet()
            }
            r6DiscoveryLoading = false
            r6DiscoveryRefreshing = false
            return@LaunchedEffect
        }

        val hadDiscoveries = r6Discoveries.isNotEmpty()
        r6DiscoveryLoading = !hadDiscoveries
        r6DiscoveryRefreshing = hadDiscoveries
        r6DiscoveryError = null
        try {
            r6Discoveries = effectiveR6Gateway.loadDiscoveries(r6ActiveExperienceCode)
            val userId = currentUserId
            if (!userId.isNullOrBlank()) {
                r6BlockedHostIds = effectiveR6Gateway.loadBlockedHostIds(userId)
                if (currentUser?.isAnonymous == false) {
                    r6Collection = effectiveR6Gateway.loadCollection(userId)
                    r6TrailProgress = effectiveR6Gateway.loadTrailProgress(
                        userId,
                        r6ActiveExperienceCode
                    )
                }
            }
            r6SelectedDropId = r6SelectedDropId?.takeIf { selected ->
                r6Discoveries.any { it.id == selected }
            }
        } catch (error: Exception) {
            r6DiscoveryError = error.localizedMessage
                ?: "Couldn't load this Experience. Try again."
        } finally {
            r6DiscoveryLoading = false
            r6DiscoveryRefreshing = false
        }
    }

    LaunchedEffect(
        r6TargetEnabled,
        currentParticipantDestination,
        currentUserId,
        currentUser?.isAnonymous,
        r6RefreshToken
    ) {
        if (
            !r6TargetEnabled ||
            currentParticipantDestination != ParticipantDestination.COLLECTION ||
            currentUser?.isAnonymous != false ||
            currentUserId.isNullOrBlank()
        ) {
            r6CollectionLoading = false
            return@LaunchedEffect
        }
        r6CollectionLoading = r6Collection.isEmpty()
        r6CollectionError = null
        try {
            r6Collection = effectiveR6Gateway.loadCollection(currentUserId)
        } catch (error: Exception) {
            r6CollectionError = error.localizedMessage ?: "Couldn't load your Collection."
        } finally {
            r6CollectionLoading = false
        }
    }

    LaunchedEffect(
        nearbyVisible,
        hasForegroundLocation,
        joinedGroups,
        otherDropsRefreshToken,
        collectedDropIds,
        ignoredDropIds,
        dismissedBrowseDropIds.toList()
    ) {
        if (nearbyVisible && !r6TargetEnabled) {
            val hadExistingDrops = otherDrops.isNotEmpty()
            if (hadExistingDrops) {
                otherDropsRefreshing = true
                otherDropsError = null
            } else {
                otherDropsLoading = true
                otherDropsError = null
                otherDropsCurrentLocation = null
                otherDropsLocationAccuracyMeters = null
            }
            val rawUid = FirebaseAuth.getInstance().currentUser?.uid
            val effectiveUid = when (userMode) {
                UserMode.GUEST -> null
                else -> rawUid
            }
            if (userMode != UserMode.GUEST && effectiveUid == null) {
                if (hadExistingDrops) {
                    snackbar.showMessage(scope, "Sign-in is still in progress. Try again in a moment.")
                } else {
                    otherDrops = emptyList()
                    otherDropsError = "Sign-in is still in progress. Try again in a moment."
                }
                otherDropsLoading = false
                otherDropsRefreshing = false
            } else {
                try {
                    val drops = repo.getVisibleDropsForUser(
                        effectiveUid,
                        joinedGroups.map { it.code }.toSet()
                    )
                        .sortedByDescending { it.createdAt }
                    val approximateFix = if (hasForegroundLocation) {
                        getApproximateLocation()
                    } else {
                        null
                    }
                    val latestLocation = approximateFix?.position
                    dismissedBrowseDropIds.removeAll { id -> drops.none { it.id == id } }
                    val filteredDrops = drops.filterNot { drop ->
                        val id = drop.id
                        when {
                            id in collectedDropIds || id in ignoredDropIds -> true
                            dismissedBrowseDropIds.contains(id) -> {
                                val nearbyAgain = latestLocation?.let { location ->
                                    distanceBetweenMeters(
                                        location.latitude,
                                        location.longitude,
                                        drop.lat,
                                        drop.lng
                                    ) <= BROWSE_NEARBY_THRESHOLD_METERS
                                } ?: false
                                !nearbyAgain
                            }
                            else -> false
                        }
                    }
                    otherDrops = filteredDrops
                    otherDropsCurrentLocation = latestLocation
                    otherDropsLocationAccuracyMeters = approximateFix?.accuracyMeters
                    otherDropsSelectedId = otherDropsSelectedId?.takeIf { id -> filteredDrops.any { it.id == id } }
                    otherDropsError = null
                } catch (e: Exception) {
                    if (hadExistingDrops) {
                        snackbar.showMessage(scope, e.message ?: "Failed to load nearby drops.")
                    } else {
                        otherDrops = emptyList()
                        otherDropsError = e.message ?: "Failed to load nearby drops."
                    }
                } finally {
                    otherDropsLoading = false
                    otherDropsRefreshing = false
                }
            }
        } else {
            otherDrops = emptyList()
            otherDropsError = null
            otherDropsLoading = false
            otherDropsRefreshing = false
            otherDropsCurrentLocation = null
            otherDropsLocationAccuracyMeters = null
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

    // Task 3.2 — the explorer surface used to hold a continuous PRIORITY_HIGH_ACCURACY
    // stream (5 s interval) for as long as it was visible, purely to keep distance
    // labels fresh. That is precise location for browsing, held indefinitely, which the
    // direction doc's steps 1 and 5 exclude. Distances now come from a coarse one-shot
    // refreshed with the list, and precision is requested only at an unlock attempt.
    LaunchedEffect(nearbyVisible, hasForegroundLocation, otherDropsRefreshToken) {
        if (!nearbyVisible || !hasForegroundLocation) {
            otherDropsCurrentLocation = null
            otherDropsLocationAccuracyMeters = null
            return@LaunchedEffect
        }
        val approximateFix = getApproximateLocation()
        otherDropsCurrentLocation = approximateFix?.position
        otherDropsLocationAccuracyMeters = approximateFix?.accuracyMeters
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

    LaunchedEffect(showOrganizerTools, currentExplorerDestination, myDropsRefreshToken) {
        val shouldLoad = showOrganizerTools &&
                currentExplorerDestination == ExplorerDestination.MyDrops
        if (shouldLoad) {
            myDropsLoading = true
            myDropsError = null
            myDropsDeletingId = null
            myDropsCurrentLocation = null
            myDropsLocationAccuracyMeters = null
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
                    val approximateFix = getApproximateLocation()
                    myDropsCurrentLocation = approximateFix?.position
                    myDropsLocationAccuracyMeters = approximateFix?.accuracyMeters
                    myDropsSelectedId = myDropsSelectedId?.takeIf { id -> drops.any { it.id == id } }
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
            myDropsLocationAccuracyMeters = null
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
        selectedParticipantDestination = when (destination) {
            ExplorerDestination.Collected -> ParticipantDestination.COLLECTION.name
            ExplorerDestination.Discover -> ParticipantDestination.NEARBY.name
            ExplorerDestination.MyDrops -> ParticipantDestination.ACCOUNT.name
        }
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


    val filteredOtherDrops = remember(selectedExplorerGroupCode, otherDrops, explorerGroups, blockedCreatorIds) {
        val groupFiltered = selectedExplorerGroupCode?.takeIf { code -> explorerGroups.any { it.code == code } }?.let { code ->
            otherDrops.filter { drop -> drop.groupCode == code }
        } ?: otherDrops
        if (blockedCreatorIds.isEmpty()) groupFiltered
        else groupFiltered.filter { drop -> drop.createdBy !in blockedCreatorIds }
    }
    val r6VisibleDiscoveries = remember(r6Discoveries, r6BlockedHostIds) {
        r6Discoveries.filterNot { it.ownerId in r6BlockedHostIds }
    }
    val r6UnlockedDropIds = remember(r6Collection) { r6Collection.mapTo(mutableSetOf()) { it.dropId } }
    val r6DiscoveryPresentation = remember(
        r6VisibleDiscoveries,
        r6UnlockedDropIds,
        r6TrailProgress,
        otherDropsCurrentLocation,
        otherDropsLocationAccuracyMeters
    ) {
        r6VisibleDiscoveries.map { drop ->
            val progress = drop.trailId?.let { trailId ->
                r6TrailProgress.firstOrNull { it.trailId == trailId }
            }
            val distance = otherDropsCurrentLocation?.let { location ->
                distanceBetweenMeters(location.latitude, location.longitude, drop.lat, drop.lng)
            }
            R6DiscoveryPresentation(
                drop = drop,
                state = R6ParticipantPolicy.discoveryState(
                    drop = drop,
                    unlockedDropIds = r6UnlockedDropIds,
                    trailProgress = progress,
                    approximateDistanceM = distance,
                    approximateAccuracyM = otherDropsLocationAccuracyMeters,
                    nowMillis = System.currentTimeMillis(),
                    experienceEndsAtMillis = null
                ),
                distanceLabel = r6DistanceLabel(distance)
            )
        }
    }
    val r6ActiveTrailProgress = remember(r6TrailProgress, r6DiscoveryPresentation) {
        r6TrailProgress.firstOrNull { progress ->
            r6DiscoveryPresentation.any { it.drop.trailId == progress.trailId }
        } ?: r6DiscoveryPresentation.firstOrNull { it.drop.trailId != null }?.drop?.let { drop ->
            R6TrailProgress(
                experienceCode = drop.experienceCode,
                trailId = drop.trailId!!,
                currentStepIndex = 0,
                completedDropIds = emptyList(),
                completedAtMillis = null
            )
        }
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
    // R4 Collection is a complete cross-Experience history and is intentionally not
    // filtered by the active Nearby Experience.
    val filteredCollected = collectedNotes
    // Server-flagged content is hidden unconditionally; the viewer opt-out went with
    // the NSFW pilot flag at task 2.8.
    val visibleCollectedNotes =
        filteredCollected.filterNot { note -> note.isNsfw || note.nsfwLabels.isNotEmpty() }
    val hiddenNsfwCollectedCount = filteredCollected.size - visibleCollectedNotes.size
    val sortedCollectedNotes = remember(visibleCollectedNotes, collectedSortOption, collectedCurrentLocation) {
        sortCollectedNotes(visibleCollectedNotes, collectedSortOption, collectedCurrentLocation)
    }

    LaunchedEffect(sortedCollectedNotes) {
        collectedSelectedId = collectedSelectedId?.takeIf { id -> sortedCollectedNotes.any { it.id == id } }
            ?: sortedCollectedNotes.firstOrNull()?.id
    }

    LaunchedEffect(selectedExplorerGroupCode, sortedOtherDrops) {
        val current = otherDropsSelectedId
        if (current != null && sortedOtherDrops.none { drop -> drop.id == current }) {
            otherDropsSelectedId = null
        }
    }

    LaunchedEffect(selectedExplorerGroupCode, sortedMyDrops) {
        val current = myDropsSelectedId
        if (current != null && sortedMyDrops.none { drop -> drop.id == current }) {
            myDropsSelectedId = null
        }
    }

    LaunchedEffect(currentParticipantDestination) {
        val shouldUpdateLocation = currentParticipantDestination == ParticipantDestination.COLLECTION
        collectedCurrentLocation = if (shouldUpdateLocation) {
            getApproximateLocation()?.position
        } else {
            null
        }
    }

    LaunchedEffect(
        isBusinessUser,
        showOrganizerTools,
        showBusinessDashboard,
        businessDashboardRefreshToken,
        currentUserId
    ) {
        val userId = currentUserId
        val shouldFetch = isBusinessUser && !userId.isNullOrBlank() &&
                (showBusinessDashboard || showOrganizerTools)

        if (shouldFetch) {
            if (showBusinessDashboard) {
                businessDashboardLoading = true
                businessDashboardError = null
            }

            try {
                businessDrops = repo.getBusinessDrops(userId!!)
                businessExperienceAnalytics = repo.getOwnedExperienceAnalytics(userId)
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
                businessExperienceAnalytics = emptyList()
            }
        }
    }

    val isSignedIn = !currentUserId.isNullOrBlank()
    val collectRestrictionMessage = when (userMode) {
        UserMode.GUEST -> "Preview drops nearby, then create an account to pick them up when you're ready."
        UserMode.SIGNED_IN -> null
    }
    val collectedLikeRestrictionMessage = when {
        !isSignedIn -> "Sign in to react to drops."
        !canParticipate -> participationRestriction("react to drops")
        else -> null
    }

    val handleOtherDropReport: (Drop) -> Unit = report@{ drop ->
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
        if (drop.reportedBy.containsKey(userId)) {
            Toast.makeText(ctx, "You've already reported this drop.", Toast.LENGTH_SHORT).show()
            return@report
        }
        browseReportTarget = drop.toReportableDrop(source = REPORT_SOURCE_BROWSE_MAP)
        browseReportSelectedReasons = emptySet()
        browseReportError = null
        browseReportDialogOpen = true
    }

    fun ignoreDropForNow(drop: Drop) {
        if (!dismissedBrowseDropIds.contains(drop.id)) {
            dismissedBrowseDropIds.add(drop.id)
            snackbar.showMessage(
                scope,
                ctx.getString(R.string.browse_ignore_drop_snackbar)
            )
        }
    }

    val viewMyDrop: (Drop) -> Unit = { drop ->
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
            val userId = currentUserId
            when (drop.userLikeStatus(userId)) {
                DropLikeStatus.LIKED -> putExtra("dropIsLiked", true)
                DropLikeStatus.NONE -> Unit
            }
            putExtra("dropIsNsfw", drop.isNsfw)
            if (drop.nsfwLabels.isNotEmpty()) {
                putStringArrayListExtra("dropNsfwLabels", ArrayList(drop.nsfwLabels))
            }
            drop.decayDays?.let { putExtra("dropDecayDays", it) }
        }
        ctx.startActivity(intent)
    }

    val requestMyDropDeletion: (Drop) -> Unit = { drop ->
        if (drop.id.isBlank()) {
            snackbar.showMessage(scope, "Unable to delete this drop.")
        } else {
            myDropsPendingDelete = drop
        }
    }

    val viewCollectedNote: (CollectedNote) -> Unit = { note ->
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
            if (note.isLiked) {
                putExtra("dropIsLiked", true)
            }
            putExtra("dropIsNsfw", note.isNsfw)
            if (note.nsfwLabels.isNotEmpty()) {
                putStringArrayListExtra("dropNsfwLabels", ArrayList(note.nsfwLabels))
            }
            note.decayDays?.let { putExtra("dropDecayDays", it) }
        }
        ctx.startActivity(intent)
    }

    val requestCollectedRemoval: (CollectedNote) -> Unit = { note ->
        collectedPendingRemove = note
    }

    val handleBlockCreator: (creatorId: String) -> Unit = { creatorId ->
        val userId = currentUserId
        if (!userId.isNullOrBlank() && creatorId.isNotBlank()) {
            blockedCreatorIds = (blockedCreatorIds + creatorId).distinct()
            scope.launch {
                runCatching { repo.blockDropCreator(userId, creatorId) }
            snackbar.showMessage(scope, "Host blocked. Their drops won't appear for you.")
            }
        }
    }

    val handleCollectedReport: (CollectedNote) -> Unit = report@{ note ->
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
    }

    val handleCollectedLike: (CollectedNote, DropLikeStatus) -> Unit = { note, status ->
        submitCollectedLike(note, status)
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
    var fabRowBottomPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Organizer tools are nested under Account, never a separate mode or tab.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .onSizeChanged { size -> topBarHeightPx = size.height }
                .zIndex(1f)
        ) {
            if (showOrganizerTools && organizerToolsAvailable) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TopAppBar(
                            modifier = Modifier.fillMaxWidth(),
                            navigationIcon = {
                                IconButton(onClick = {
                                    showOrganizerTools = false
                                    selectedParticipantDestination = ParticipantDestination.ACCOUNT.name
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = stringResource(R.string.r4_back_to_account)
                                    )
                                }
                            },
                            title = { Text(stringResource(R.string.r4_organizer_tools_title)) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent
                            )
                        )
                        SideEffect { explorerNavigationHeightPx = 0 }
                    }
                }
                Divider()
            } else {
                when (currentParticipantDestination) {
                    ParticipantDestination.NEARBY -> GeoDropExperienceTopBar(
                        experiences = explorerGroups.map { membership ->
                            ExperienceNavigationItem(
                                code = membership.code,
                                isOwned = membership.role == GroupRole.OWNER
                            )
                        },
                        activeCode = selectedExplorerGroupCode,
                        onSelectExperience = { code -> selectedExplorerGroupCode = code },
                        onJoinExperience = { showManageGroups = true }
                    )
                    ParticipantDestination.COLLECTION -> TopAppBar(
                        title = { Text(stringResource(R.string.r4_tab_collection)) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    ParticipantDestination.ACCOUNT -> TopAppBar(
                        title = { Text(stringResource(R.string.r4_tab_account)) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
                SideEffect { explorerNavigationHeightPx = 0 }
            }
        }

        // Floating destination FABs — overlaid at the top of the map
        if (false) {
            val fabDestinations = remember(hasExplorerAccount) {
                ExplorerDestination.values().filter { d ->
                    when (d) {
                        ExplorerDestination.MyDrops   -> hasExplorerAccount
                        ExplorerDestination.Collected -> hasExplorerAccount
                        ExplorerDestination.Discover  -> true
                    }
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp)
                    .zIndex(2f)
                    .onGloballyPositioned { coords ->
                        fabRowBottomPx = coords.boundsInRoot().bottom
                    },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                fabDestinations.forEach { destination ->
                    val selected = effectiveExplorerDestination == destination
                    val (label, icon) = when (destination) {
                        ExplorerDestination.Discover  -> Pair(stringResource(R.string.action_browse_map_title),    Icons.Rounded.Map)
                        ExplorerDestination.MyDrops   -> Pair(stringResource(R.string.action_my_drops_title),      Icons.Rounded.Inbox)
                        ExplorerDestination.Collected -> Pair(stringResource(R.string.action_collected_drops_title), Icons.Rounded.Bookmark)
                    }
                    SmallFloatingActionButton(
                        onClick = { openExplorerDestination(destination) },
                        containerColor = if (selected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surface,
                        contentColor = if (selected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Icon(imageVector = icon, contentDescription = label)
                    }
                }
            }
        }

        val celebrationTopPadding = with(density) {
            (topBarHeightPx + explorerNavigationHeightPx).toDp()
        } + 16.dp
        PickupCelebrationBanner(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 24.dp)
                .zIndex(2f)
                .padding(top = celebrationTopPadding),
            visible = pickupCelebrationVisible && pickupCelebrationDrop != null,
            drop = pickupCelebrationDrop
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                GeoDropParticipantNavigationBar(
                    selected = currentParticipantDestination,
                    onSelect = { destination ->
                        showOrganizerTools = false
                        selectedParticipantDestination = destination.name
                        when (destination) {
                            ParticipantDestination.NEARBY -> {
                                explorerDestination = ExplorerDestination.Discover.name
                            }
                            ParticipantDestination.COLLECTION -> {
                                collectedNotes = noteInventory.getCollectedNotes()
                                explorerDestination = ExplorerDestination.Collected.name
                            }
                            ParticipantDestination.ACCOUNT -> Unit
                        }
                    },
                    modifier = Modifier.heightIn(min = 64.dp)
                )
                /* R4 removed the legacy Profile / Drop Something / Manage Groups bar.
                   It remains commented for this migration unit so the surrounding legacy
                   dialog callbacks can be removed safely in their later owning tasks.
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
                                            openAccountAuthDialog(
                                                initialType = AccountType.EXPLORER,
                                                initialMode = AccountAuthMode.SIGN_IN,
                                                lockAccountType = true
                                            )
                                        }
                                    )
                                }

                                UserMode.SIGNED_IN -> {
                                    if (isBusinessUser) {
                                        DropdownMenuItem(
                                            text = { Text("Business Profile") },
                                            leadingIcon = { Icon(Icons.Rounded.Storefront, contentDescription = null) },
                                            onClick = {
                                                showAccountMenu = false
                                                showBusinessOnboarding = true
                                            }
                                        )
                                    } else {
                                        val explorerUsername = userProfile?.username?.takeIf { it.isNotBlank() }
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    explorerUsername?.let {
                                                        stringResource(
                                                            R.string.menu_edit_username_with_value,
                                                            it
                                                        )
                                                    } ?: "Profile & preferences"
                                                )
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                            onClick = {
                                                showAccountMenu = false
                                                explorerProfileError = null
                                                explorerProfileSubmitting = false
                                                explorerUsernameField = TextFieldValue(explorerUsername.orEmpty())
                                                explorerDisplayNameField = TextFieldValue(userProfile?.displayName.orEmpty())
                                                showExplorerProfile = true
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Blocked hosts") },
                                        leadingIcon = { Icon(Icons.Rounded.Block, contentDescription = null) },
                                        onClick = {
                                            showAccountMenu = false
                                            val userId = currentUserId
                                            if (!userId.isNullOrBlank()) {
                                                blockedCreatorsLoading = true
                                                showBlockedCreators = true
                                                scope.launch {
                                                    blockedCreatorIds = runCatching {
                                                        repo.getBlockedCreatorIds(userId)
                                                    }.getOrElse { emptyList() }
                                                    blockedCreatorsLoading = false
                                                }
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Your data") },
                                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                                        onClick = {
                                            showAccountMenu = false
                                            showAccountDataDialog = true
                                        }
                                    )
                                }
                            }

                            if (canParticipate) {
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

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.info_menu_tutorial)) },
                                leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                                onClick = {
                                    showAccountMenu = false
                                    showOnboardingHelp = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.info_menu_faq)) },
                                leadingIcon = { Icon(Icons.Rounded.Help, contentDescription = null) },
                                onClick = {
                                    showAccountMenu = false
                                    showFaqDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.info_menu_terms)) },
                                leadingIcon = { Icon(Icons.Rounded.Description, contentDescription = null) },
                                onClick = {
                                    showAccountMenu = false
                                    termsPrivacyDialogTab = 0
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.info_menu_privacy)) },
                                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                                onClick = {
                                    showAccountMenu = false
                                    termsPrivacyDialogTab = 1
                                }
                            )
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
                */
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbar,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
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
            val fabClearanceDp = with(density) { fabRowBottomPx.toDp() } + 8.dp

            Box(modifier = Modifier.fillMaxSize()) {
                if (r7OrganizerApproved && showOrganizerTools) {
                    R7OrganizerContent(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(
                                start = startPadding,
                                top = navAwareTopPadding,
                                end = endPadding,
                                bottom = bottomPadding
                            ),
                        userId = currentUserId.orEmpty(),
                        gateway = effectiveR7Gateway,
                        localDemo = localDeviceDemoActive,
                        currentLocationProvider = { getLatestLocation() }
                    )
                } else if (!r7TargetEnabled && isBusinessUser && showOrganizerTools) {
                    BusinessHomeDestination(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(
                                start = startPadding,
                                top = navAwareTopPadding,
                                end = endPadding,
                                bottom = bottomPadding
                            ),
                        businessName = userProfile?.businessName,
                        businessCategories = businessCategories,
                        metrics = businessHomeMetrics,
                        onViewDashboard = {
                            if (!userProfileLoading) showBusinessDashboard = true
                        },
                        onUpdateBusinessProfile = { showBusinessOnboarding = true },
                        onViewMyDrops = { showBusinessDashboard = true }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(
                                start = startPadding,
                                end = endPadding,
                                bottom = bottomPadding
                            )
                    ) {
                        GeoDropParticipantStateHost(
                            destination = currentParticipantDestination,
                            activeExperienceCode = selectedExplorerGroupCode
                        ) {
                        if (currentParticipantDestination == ParticipantDestination.ACCOUNT) {
                            GeoDropAccountDestination(
                                identityLabel = when {
                                    userMode == UserMode.GUEST -> stringResource(R.string.r4_guest_identity)
                                    !userProfile?.displayName.isNullOrBlank() -> userProfile?.displayName.orEmpty()
                                    !userProfile?.username.isNullOrBlank() -> "@${userProfile?.username}"
                                    else -> currentUser?.email.orEmpty()
                                },
                                identitySupportingText = when {
                                    userMode == UserMode.GUEST -> stringResource(R.string.r4_guest_identity_body)
                                    isBusinessUser && !userProfile?.businessName.isNullOrBlank() -> userProfile?.businessName
                                    else -> currentUser?.email
                                },
                                isGuest = userMode == UserMode.GUEST,
                                locationGranted = hasForegroundLocation,
                                notificationsGranted = hasNotificationPermission,
                                joinedExperiences = explorerGroups.map { membership ->
                                    ExperienceNavigationItem(
                                        code = membership.code,
                                        isOwned = membership.role == GroupRole.OWNER
                                    )
                                },
                                experienceHistory = r9ExperienceHistory,
                                reportStatuses = r9ReportStatuses,
                                blockedHostCount = if (r9TargetEnabled) {
                                    r9BlockedHosts.size
                                } else {
                                    blockedCreatorIds.size
                                },
                                accountDetailsLoading = r9AccountLoading,
                                accountDetailsError = r9AccountError,
                                organizerAccessState = accountOrganizerAccessState,
                                signingOut = signingOut,
                                onSignIn = {
                                    openAccountAuthDialog(
                                        initialType = AccountType.EXPLORER,
                                        initialMode = AccountAuthMode.SIGN_IN,
                                        lockAccountType = true
                                    )
                                },
                                onEditProfile = {
                                    if (isBusinessUser) {
                                        showBusinessOnboarding = true
                                    } else {
                                        explorerProfileError = null
                                        explorerProfileSubmitting = false
                                        explorerUsernameField = TextFieldValue(userProfile?.username.orEmpty())
                                        explorerDisplayNameField = TextFieldValue(userProfile?.displayName.orEmpty())
                                        showExplorerProfile = true
                                    }
                                },
                                onOpenLocationSettings = {
                                    ctx.startActivity(
                                        Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:${ctx.packageName}")
                                        )
                                    )
                                },
                                onOpenNotificationSettings = {
                                    ctx.startActivity(
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                                        }
                                    )
                                },
                                onOpenOrganizerAccess = {
                                    if (r7OrganizerAccessLoading) {
                                        snackbar.showMessage(scope, "Loading Organizer access…")
                                    } else if (!r7TargetEnabled) {
                                        snackbar.showMessage(scope, "Organizer applications are not enabled in this build.")
                                    } else {
                                        showR7OrganizerAccess = true
                                    }
                                },
                                onOpenOrganizerTools = { showOrganizerTools = true },
                                onOpenBlockedCreators = {
                                    val userId = currentUserId
                                    if (!userId.isNullOrBlank()) {
                                        blockedCreatorsLoading = true
                                        showBlockedCreators = true
                                        scope.launch {
                                            if (r9TargetEnabled) {
                                                r9BlockedHosts = runCatching {
                                                    effectiveR9Gateway.loadBlockedHosts(userId)
                                                }.getOrElse { emptyList() }
                                            } else {
                                                blockedCreatorIds = runCatching {
                                                    repo.getBlockedCreatorIds(userId)
                                                }.getOrElse { emptyList() }
                                            }
                                            blockedCreatorsLoading = false
                                        }
                                    }
                                },
                                onOpenData = { showAccountDataDialog = true },
                                onRetryAccountDetails = { r9RefreshToken += 1 },
                                onSignOut = { handleSignOut() },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = navAwareTopPadding)
                            )
                        } else {
                        val shellDestination = if (
                            currentParticipantDestination == ParticipantDestination.COLLECTION
                        ) {
                            ExplorerDestination.Collected
                        } else {
                            ExplorerDestination.Discover
                        }
                        val participantMotion = GeoDropThemeTokens.motion
                        AnimatedContent(
                            targetState = shellDestination,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                if (participantMotion.reducedMotion) {
                                    fadeIn(tween(participantMotion.crossFadeMillis)) togetherWith
                                        fadeOut(tween(participantMotion.crossFadeMillis))
                                } else {
                                    val forward = targetState.ordinal > initialState.ordinal
                                    slideInHorizontally { w -> if (forward) w else -w } togetherWith
                                        slideOutHorizontally { w -> if (forward) -w else w }
                                }
                            },
                            label = "ParticipantDestination"
                        ) { destination ->
                        when (destination) {
                            ExplorerDestination.Discover -> {
                                if (explorerGroups.isEmpty()) {
                                    GeoDropNoExperienceState(
                                        onJoinExperience = { showManageGroups = true },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = navAwareTopPadding)
                                    )
                                } else if (r6TargetEnabled) {
                                    R6NearbyContent(
                                        loading = r6DiscoveryLoading,
                                        refreshing = r6DiscoveryRefreshing,
                                        error = r6DiscoveryError,
                                        items = r6DiscoveryPresentation,
                                        selectedDropId = r6SelectedDropId,
                                        unlockingDropId = r6UnlockingDropId,
                                        trailProgress = r6ActiveTrailProgress,
                                        currentLocation = otherDropsCurrentLocation,
                                        approximateLocationEnabled = hasCoarseLocation,
                                        networkAvailable = networkAvailable,
                                        topPadding = mapAwareTopPadding,
                                        unlockResult = r6UnlockResult,
                                        unlockError = r6UnlockError,
                                        onSelect = { drop -> r6SelectedDropId = drop?.id },
                                        onUnlock = { drop -> attemptR6Unlock(drop) },
                                        onRequestLocation = { requestNearbyLocationAccess() },
                                        onRefresh = { r6RefreshToken += 1 },
                                        onDismissUnlockResult = {
                                            r6UnlockResult = null
                                            r6UnlockError = null
                                            r6SelectedDropId = null
                                        },
                                        onReport = { drop, reason, narrative ->
                                            submitR6Report(drop, reason, narrative)
                                        },
                                        onBlockHost = { drop -> blockR6Host(drop) },
                                        mapsAvailable = BuildConfig.MAPS_CONFIGURED,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
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
                                                fabClearance = fabClearanceDp,
                                                destinationLabel = stringResource(R.string.action_browse_map_title),
                                                loading = otherDropsLoading,
                                                refreshing = otherDropsRefreshing,
                                                drops = sortedOtherDrops,
                                                currentLocation = otherDropsCurrentLocation,
                                                currentLocationAccuracyMeters = otherDropsLocationAccuracyMeters,
                                                unlockedDropIds = unlockedDropIds,
                                                unlockingDropId = unlockInProgressDropId,
                                                approximateLocationEnabled = hasCoarseLocation,
                                                locationNeedsSettings = foregroundLocationState == PermissionGrantState.BLOCKED,
                                                onRequestLocation = { requestNearbyLocationAccess() },
                                                error = otherDropsError,
                                                emptyMessage = selectedExplorerGroupCode?.let { code ->
                                                    "Nothing in $code yet — be the first to drop something here."
                                                },
                                                selectedId = otherDropsSelectedId,
                                                onSelect = { drop ->
                                                    otherDropsSelectedId = if (otherDropsSelectedId == drop.id) {
                                                        null
                                                    } else {
                                                        drop.id
                                                    }
                                                },
                                                sortOption = otherDropsSortOption,
                                                sortOptions = dropSortOptions,
                                                onSortOptionChange = { option ->
                                                    otherDropsSortKey = option.name
                                                },
                                                canLikeDrops = canParticipate,
                                                likeRestrictionMessage = if (canParticipate) null else participationRestriction("react to drops"),
                                                currentUserId = currentUserId,
                                                isSignedIn = isSignedIn,
                                                collectedDropIds = collectedDropIds,
                                                canParticipate = canParticipate,
                                                collectRestrictionMessage = collectRestrictionMessage,
                                                browseReportingDropId = browseReportingDropId,
                                                onPickUp = { pickUpDrop(it) },
                                                onReport = { handleOtherDropReport(it) },
                                                onIgnoreForNow = { ignoreDropForNow(it) },
                                                onBlock = { drop ->
                                                    if (!drop.createdBy.isNullOrBlank()) {
                                                        handleBlockCreator(drop.createdBy)
                                                    }
                                                },
                                                onRefresh = { otherDropsRefreshToken += 1 }
                                            )
                                        }
                                    }
                                }
                                }
                            }

                            ExplorerDestination.MyDrops -> {
                                fun performDropDeletion(drop: Drop) {
                                    if (drop.id.isBlank()) {
                                        snackbar.showMessage(scope, "Unable to delete this drop.")
                                        return
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

                                val pendingDeletion = myDropsPendingDelete
                                if (pendingDeletion != null) {
                                    AlertDialog(
                                        onDismissRequest = { myDropsPendingDelete = null },
                                        icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                        title = { Text("Delete drop?") },
                                        text = { Text("Are you sure you want to delete this drop?") },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    myDropsPendingDelete = null
                                                    performDropDeletion(pendingDeletion)
                                                }
                                            ) {
                                                Text("Delete")
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { myDropsPendingDelete = null }) {
                                                Text("Cancel")
                                            }
                                        }
                                    )
                                }

                                Column(
                                    modifier = Modifier.fillMaxSize(),
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
                                            fabClearance = fabClearanceDp,
                                            contentPadding = PaddingValues(bottom = 0.dp),
                                            loading = myDropsLoading,
                                            drops = sortedMyDrops,
                                            currentLocation = myDropsCurrentLocation,
                                            currentLocationAccuracyMeters = myDropsLocationAccuracyMeters,
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
                                            onView = viewMyDrop,
                                            onDelete = requestMyDropDeletion
                                        )
                                    }
                                }
                            }

                            ExplorerDestination.Collected -> {
                                if (r6TargetEnabled) {
                                    R6CollectionContent(
                                        loading = r6CollectionLoading,
                                        error = r6CollectionError,
                                        receipts = r6Collection,
                                        topPadding = mapAwareTopPadding,
                                        onRefresh = { r6RefreshToken += 1 },
                                        onReport = { receipt, reason, narrative ->
                                            submitR6ReportById(receipt.dropId, reason, narrative)
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        val isSignedIn = !currentUserId.isNullOrBlank()
                                        val collectedLikeRestrictionMessage = when {
                                            !isSignedIn -> "Sign in to react to drops."
                                            !canParticipate -> participationRestriction("react to drops")
                                            else -> null
                                        }
                                        val pendingRemoval = collectedPendingRemove
                                        if (pendingRemoval != null) {
                                            AlertDialog(
                                                onDismissRequest = { collectedPendingRemove = null },
                                                icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                                title = { Text("Delete saved drop?") },
                                                text = { Text("Are you sure you want to delete this saved drop?") },
                                                confirmButton = {
                                                    TextButton(
                                                        onClick = {
                                                            noteInventory.removeCollected(pendingRemoval.id)
                                                            collectedNotes = noteInventory.getCollectedNotes()
                                                            collectedPendingRemove = null
                                                        }
                                                    ) {
                                                        Text("Delete")
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { collectedPendingRemove = null }) {
                                                        Text("Cancel")
                                                    }
                                                }
                                            )
                                        }

                                        CollectedDropsContent(
                                            modifier = Modifier.fillMaxSize(),
                                            topContentPadding = mapAwareTopPadding,
                                            fabClearance = fabClearanceDp,
                                            contentPadding = PaddingValues(bottom = 0.dp),
                                            notes = sortedCollectedNotes,
                                            hiddenNsfwCount = hiddenNsfwCollectedCount,
                                            canReportDrops = isSignedIn,
                                            reportedDropIds = reportedCollectedDropIds.toSet(),
                                            reportingDropId = browseReportingDropId,
                                            isReportProcessing = browseReportProcessing,
                                            emptyMessage = "You haven't collected any drops yet.",
                                            sortOption = collectedSortOption,
                                            sortOptions = dropSortOptions,
                                            onSortOptionChange = { option ->
                                                collectedSortKey = option.name
                                            },
                                            canLikeDrops = canParticipate,
                                            isSignedIn = isSignedIn,
                                            likeRestrictionMessage = collectedLikeRestrictionMessage,
                                            votingDropIds = votingDropIds,
                                            selectedId = collectedSelectedId,
                                            onSelect = { note ->
                                                collectedSelectedId = if (collectedSelectedId == note.id) {
                                                    null
                                                } else {
                                                    note.id
                                                }
                                            },
                                            onLike = handleCollectedLike,
                                            onReport = handleCollectedReport,
                                            onView = viewCollectedNote,
                                            onRemove = requestCollectedRemoval
                                        )
                                    }
                                }
                                }
                            }
                        }
                        } // AnimatedContent
                        }
                        }
                    }
                }

            }
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
                onDropContentTypeChange = { selected ->
                    dropContentType = selected.takeUnless { it == DropContentType.AUDIO }
                        ?: DropContentType.TEXT
                },
                note = note,
                onNoteChange = { note = it },
                description = description,
                onDescriptionChange = { description = it },
                capturedPhotoPath = capturedPhotoPath,
                onCapturePhoto = { ensureCameraAndLaunch() },
                onClearPhoto = { clearPhoto() },
                capturedAudioUri = capturedAudioUri,
                onRecordAudio = {
                    snackbar.showMessage(scope, "Audio drops are not available in Pilot 1.")
                },
                onClearAudio = { clearAudio() },
                dropVisibility = dropVisibility,
                onDropVisibilityChange = { dropVisibility = it },
                groupCodeInput = groupCodeInput,
                onGroupCodeInputChange = { groupCodeInput = it },
                joinedGroups = createdGroups.map { it.code },
                onSelectGroupCode = { code -> groupCodeInput = TextFieldValue(code) },
                redemptionLimitInput = redemptionLimitInput,
                onRedemptionLimitChange = { redemptionLimitInput = it },
                decayDaysInput = decayDaysInput,
                onDecayDaysChange = { decayDaysInput = it },
                onManageGroupCodes = { showManageGroups = true },
                onSubmit = { submitDrop() },
                onCreateHunt = {
                    showDropComposer = false
                    huntBuilderError = null
                    scope.launch {
                        val loc = getLatestLocation()
                        val (lat, lng) = loc ?: Pair(0.0, 0.0)
                        huntBuilderState = HuntBuilderState(
                            steps = listOf(HuntStepDraft(stepIndex = 0, lat = lat, lng = lng))
                        )
                        showHuntBuilder = true
                    }
                },
                onDismiss = {
                    if (!isSubmitting) {
                        showDropComposer = false
                    }
                }
            )
        }

        if (showHuntBuilder) {
            val builderState = huntBuilderState
            HuntBuilderDialog(
                state = builderState ?: HuntBuilderState(),
                isSubmitting = huntBuilderSubmitting,
                error = huntBuilderError,
                isBusinessUser = userProfile?.isBusiness() == true,
                businessName = userProfile?.businessName,
                businessCategories = businessCategories,
                onStateChange = { huntBuilderState = it },
                onAddStep = {
                    scope.launch {
                        val loc = getLatestLocation()
                        val (lat, lng) = loc ?: Pair(0.0, 0.0)
                        val current = huntBuilderState ?: HuntBuilderState()
                        huntBuilderState = current.addStep(lat, lng)
                    }
                },
                onSubmit = { state ->
                    val userId = currentUserId
                    if (userId.isNullOrBlank()) return@HuntBuilderDialog
                    if (state.title.isBlank()) {
                        huntBuilderError = "Please give your hunt a title."
                        return@HuntBuilderDialog
                    }
                    if (state.steps.size < 2) {
                        huntBuilderError = "A scavenger hunt needs at least 2 steps."
                        return@HuntBuilderDialog
                    }
                    scope.launch {
                        huntBuilderSubmitting = true
                        huntBuilderError = null
                        try {
                            val totalSteps = state.steps.size
                            val now = System.currentTimeMillis()
                            val profile = userProfile
                            // Save all drops atomically
                            val dropIds = state.steps.mapIndexed { index, step ->
                                val drop = Drop(
                                    text = step.noteText,
                                    description = step.description.takeIf { it.isNotBlank() },
                                    lat = step.lat,
                                    lng = step.lng,
                                    createdBy = userId,
                                    createdAt = now,
                                    dropperUsername = profile?.username ?: profile?.displayName,
                                    dropType = step.dropType,
                                    businessId = if (step.dropType != DropType.COMMUNITY) userId else null,
                                    businessName = if (step.dropType != DropType.COMMUNITY) profile?.businessName else null,
                                    contentType = DropContentType.TEXT,
                                    decayDays = state.decayDays,
                                    redemptionLimit = step.redemptionLimit,
                                    huntStepIndex = index,
                                    huntTotalSteps = totalSteps
                                )
                                repo.addDrop(drop)
                            }
                            // Create the chain document with all drop IDs
                            val chain = HuntChain(
                                createdBy = userId,
                                createdAt = now,
                                businessId = if (userProfile?.isBusiness() == true) userId else null,
                                businessName = userProfile?.businessName,
                                title = state.title.trim(),
                                description = state.description.trim().takeIf { it.isNotBlank() },
                                dropIds = dropIds,
                                isActive = true,
                                decayDays = state.decayDays,
                                totalSteps = totalSteps
                            )
                            val huntId = repo.createHuntChain(chain)
                            // Update each drop with the huntId (needed for visibility filtering)
                            dropIds.forEachIndexed { index, dropId ->
                                try {
                                    repo.updateDropHuntId(dropId, huntId)
                                } catch (_: Exception) { }
                            }
                            showHuntBuilder = false
                            huntBuilderState = null
                            snackbar.showMessage(scope, "Scavenger hunt created with ${totalSteps} stops!")
                            myDropsRefreshToken += 1
                        } catch (error: Exception) {
                            Firebase.crashlytics.recordException(error)
                            huntBuilderError = error.localizedMessage ?: "Couldn't create hunt."
                        } finally {
                            huntBuilderSubmitting = false
                        }
                    }
                },
                onDismiss = {
                    if (!huntBuilderSubmitting) {
                        showHuntBuilder = false
                        huntBuilderState = null
                        huntBuilderError = null
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

        if (showAccountDataDialog) {
            AccountDataDialog(
                scope = scope,
                onDismiss = { showAccountDataDialog = false },
                onDeleted = { receipt ->
                    showAccountDataDialog = false
                    showAccountMenu = false
                    showBusinessDashboard = false
                    showBusinessOnboarding = false
                    showDropComposer = false
                    showManageGroups = false
                    showExplorerProfile = false
                    nearbyAlertsEnabled = false
                    notificationPrefs.setNearbyAlertsEnabled(false)
                    onNearbyAlertsDisabled()
                    selectedParticipantDestination = ParticipantDestination.NEARBY.name
                    showOrganizerTools = false
                    explorerDestination = ExplorerDestination.Discover.name
                    accountDeletionReceipt = receipt
                    runCatching { googleSignInClient.signOut() }
                    runCatching { auth.signOut() }
                    currentUser = null
                    scope.launch {
                        runCatching { auth.signInAnonymously().await() }
                            .onFailure {
                                snackbar.showMessage(
                                    scope,
                                    "Account deleted. Guest browsing will reconnect when you're online."
                                )
                            }
                    }
                }
            )
        }

        if (showExplorerProfile) {
            EditProfileDialog(
                displayNameField = explorerDisplayNameField,
                onDisplayNameChange = { explorerDisplayNameField = it },
                username = explorerUsernameField,
                onUsernameChange = { explorerUsernameField = it },
                isSubmitting = explorerProfileSubmitting,
                error = explorerProfileError,
                onSubmit = { saveExplorerProfile() },
                onDismiss = {
                    if (!explorerProfileSubmitting) {
                        showExplorerProfile = false
                    }
                }
            )
        }

        if (showNotificationPermissionRecovery) {
            AlertDialog(
                onDismissRequest = { showNotificationPermissionRecovery = false },
                title = { Text("Notifications are off") },
                text = {
                    Text(
                        "Kithe only asks for notifications after you enable Nearby alerts. Turn them on in Settings to finish alert setup; Nearby browsing still works without them."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showNotificationPermissionRecovery = false
                            resumeAlertPermissionFlow = true
                            ctx.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                                }
                            )
                        }
                    ) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNotificationPermissionRecovery = false }) {
                        Text("Not now")
                    }
                }
            )
        }


        termsPrivacyDialogTab?.let { tab ->
            TermsPrivacyDialog(
                initialTab = tab,
                manifest = legalManifest,
                onOpenPolicy = { url ->
                    runCatching {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }.onFailure {
                        snackbar.showMessage(scope, "No browser is available to open this policy.")
                    }
                },
                onDismiss = { termsPrivacyDialogTab = null }
            )
        }

        if (showFaqDialog) {
            FaqDialog(onDismiss = { showFaqDialog = false })
        }

        if (showBusinessWelcome) {
            BusinessFirstRunDialog(onContinue = { showBusinessWelcome = false })
        }

        if (showR7OrganizerAccess) {
            R7OrganizerAccessDialog(
                accessState = r7OrganizerAccessState,
                onDismiss = { showR7OrganizerAccess = false },
                onContinueToApplication = {
                    scope.launch {
                        runCatching { effectiveR7Gateway.createApplicationLink() }
                            .onSuccess { link ->
                                runCatching {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
                                }.onFailure {
                                    snackbar.showMessage(scope, "Couldn't open the application form.")
                                }
                            }
                            .onFailure { error ->
                                snackbar.showMessage(
                                    scope,
                                    error.localizedMessage
                                        ?: "Couldn't create the application link. Try again."
                                )
                            }
                    }
                }
            )
        }

        if (showBusinessOnboarding && !showBusinessWelcome) {
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
                            Firebase.crashlytics.recordException(error)
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

        if (showBlockedCreators) {
            BlockedCreatorsDialog(
                hosts = if (r9TargetEnabled) {
                    r9BlockedHosts
                } else {
                    blockedCreatorIds.map { R9BlockedHost(it, "Blocked creator", null) }
                },
                loading = blockedCreatorsLoading,
                onUnblock = { host ->
                    val userId = currentUserId
                    if (!userId.isNullOrBlank()) {
                        scope.launch {
                            val result = if (r9TargetEnabled) {
                                runCatching { effectiveR9Gateway.unblockHost(host.hostId) }
                            } else {
                                runCatching {
                                    repo.unblockDropCreator(userId, host.hostId)
                                    true
                                }
                            }
                            result.onSuccess {
                                r9BlockedHosts = r9BlockedHosts.filter { it.hostId != host.hostId }
                                r6BlockedHostIds = r6BlockedHostIds - host.hostId
                                blockedCreatorIds = blockedCreatorIds.filter { it != host.hostId }
                                otherDropsRefreshToken += 1
                                r6RefreshToken += 1
                                r9RefreshToken += 1
                                snackbar.showMessage(scope, "Host unblocked.")
                            }.onFailure { error ->
                                snackbar.showMessage(
                                    scope,
                                    error.localizedMessage ?: "Couldn't unblock this host. Try again."
                                )
                            }
                        }
                    }
                },
                onDismiss = { showBlockedCreators = false }
            )
        }

        if (showBusinessDashboard) {
            BusinessDashboardDialog(
                drops = businessDrops,
                experiences = businessExperienceAnalytics,
                loading = businessDashboardLoading,
                error = businessDashboardError,
                onDismiss = { showBusinessDashboard = false },
                onRefresh = { businessDashboardRefreshToken += 1 },
                onDeleteDrop = { drop ->
                    scope.launch {
                        runCatching { repo.deleteDrop(drop.id) }
                        businessDrops = businessDrops.filter { it.id != drop.id }
                        snackbar.showMessage(scope, "Drop deleted.")
                    }
                }
            )
        }

        if (showManageGroups) {
            GeoDropJoinExperienceDialog(
                snackbarHostState = manageGroupsSnackbar,
                onDismiss = {
                    showManageGroups = false
                    showGroupMenu = false
                },
                onJoin = { code ->
                    scope.launch {
                        val normalized = GroupPreferences.normalizeGroupCode(code) ?: return@launch
                        try {
                            val request = R5EntryRequest(
                                code = normalized,
                                entrySessionId = R5EntryParser.newEntrySessionId(),
                                channel = R5EntryChannel.MANUAL
                            )
                            effectiveR5Gateway.ensureGuestSession(request.entrySessionId)
                            val preview = effectiveR5Gateway.join(request)
                            when (preview.availability) {
                                R5ExperienceAvailability.CANCELLED -> throw R5EntryException(
                                    R5EntryFailureReason.EXPERIENCE_CANCELLED,
                                    retryable = false
                                )
                                R5ExperienceAvailability.ENDED -> throw R5EntryException(
                                    R5EntryFailureReason.EXPERIENCE_ENDED,
                                    retryable = false
                                )
                                R5ExperienceAvailability.ACTIVE,
                                R5ExperienceAvailability.UPCOMING -> Unit
                            }
                            val membership = GroupMembership(
                                code = preview.code.ifBlank { normalized },
                                ownerId = null,
                                role = if (preview.membership == R5ExperienceMembership.OWNER) {
                                    GroupRole.OWNER
                                } else {
                                    GroupRole.SUBSCRIBER
                                }
                            )
                            groupPrefs.addGroup(membership)
                            effectiveR5Store.completeEntry(request.copy(code = membership.code))
                            joinedGroups = groupPrefs.getMemberships()
                            selectedExplorerGroupCode = membership.code
                            manageGroupsSnackbar.showMessage(
                                scope,
                                ctx.getString(R.string.r4_join_success, normalized)
                            )
                        } catch (error: Exception) {
                            val message = when (error) {
                                is R5EntryException -> when (error.reason) {
                                    R5EntryFailureReason.INVALID_CODE,
                                    R5EntryFailureReason.EXPERIENCE_NOT_FOUND ->
                                        ctx.getString(R.string.r4_join_not_found, normalized)
                                    R5EntryFailureReason.EXPERIENCE_CANCELLED ->
                                        ctx.getString(R.string.r5_entry_error_cancelled)
                                    R5EntryFailureReason.EXPERIENCE_ENDED ->
                                        ctx.getString(R.string.r5_entry_error_ended)
                                    R5EntryFailureReason.RATE_LIMITED ->
                                        ctx.getString(R.string.r5_entry_error_rate_limited)
                                    R5EntryFailureReason.OFFLINE ->
                                        ctx.getString(R.string.r5_entry_error_offline)
                                    R5EntryFailureReason.UNAVAILABLE,
                                    R5EntryFailureReason.UNKNOWN ->
                                        ctx.getString(R.string.r5_entry_error_unavailable)
                                }
                                else -> error.localizedMessage
                                    ?: ctx.getString(R.string.r4_join_error, normalized)
                            }
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
    manifest: LegalPolicyManifest?,
    isLoading: Boolean,
    isAccepting: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onOpenPolicy: (String) -> Unit,
    onAccept: () -> Unit,
    onExit: () -> Unit
) {
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
                    text = "Welcome to Kithe",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Before you explore Kithe, review the current approved Terms of Service and Privacy Policy. Acceptance is tied to the policy version shown below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider()

                when {
                    manifest != null -> {
                        Text(
                            text = "Policy version: ${manifest.version}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PolicyLinkButton("Terms of Service", manifest.terms, onOpenPolicy)
                            PolicyLinkButton("Privacy Policy", manifest.privacy, onOpenPolicy)
                            PolicyLinkButton(
                                "Community Guidelines",
                                manifest.communityGuidelines,
                                onOpenPolicy
                            )
                            PolicyLinkButton("Promotion Terms", manifest.promotionTerms, onOpenPolicy)
                            PolicyLinkButton("Data retention", manifest.retention, onOpenPolicy)
                            PolicyLinkButton("Subprocessors", manifest.processors, onOpenPolicy)
                            PolicyLinkButton("Minors policy", manifest.minors, onOpenPolicy)
                            PolicyLinkButton("Support", manifest.support, onOpenPolicy)
                        }
                    }
                    isLoading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    else -> {
                        Text(
                            text = errorMessage
                                ?: "Kithe's approved legal policies are unavailable.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        OutlinedButton(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }

                if (manifest != null && errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = "By tapping Accept & Continue you agree to the current version of these policies.",
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
                    Button(
                        onClick = onAccept,
                        enabled = manifest != null && !isLoading && !isAccepting
                    ) {
                        if (isAccepting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Accept & Continue")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicyLinkButton(
    label: String,
    url: String,
    onOpenPolicy: (String) -> Unit
) {
    OutlinedButton(
        onClick = { onOpenPolicy(url) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label)
    }
}

@Composable
private fun BusinessPlanStep(
    dropType: DropType,
    onDropTypeChange: (DropType) -> Unit,
    businessName: String?,
    businessCategories: List<BusinessCategory>,
    templateSuggestions: List<BusinessDropTemplate>,
    onDropContentTypeChange: (DropContentType) -> Unit,
    onNoteChange: (TextFieldValue) -> Unit,
    onDescriptionChange: (TextFieldValue) -> Unit
) {
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

@Composable
private fun BusinessOfferStep(
    redemptionLimitInput: TextFieldValue,
    onRedemptionLimitChange: (TextFieldValue) -> Unit
) {
    DropComposerSection(
        title = "Offer security",
        description = "Set a redemption code and optional limit so each guest redeems only once.",
        leadingIcon = Icons.Rounded.Flag
    ) {
        BusinessRedemptionSection(
            redemptionLimit = redemptionLimitInput,
            onRedemptionLimitChange = onRedemptionLimitChange,
            showHeader = false
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FirstRunOnboardingScreen(
    onContinue: () -> Unit,
    onExit: () -> Unit,
    showExitButton: Boolean = true
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
            ),
            OnboardingSlide(
                icon = Icons.Rounded.Groups,
                title = "Join community groups",
                description = "Follow local crews or start your own group to coordinate adventures and share exclusive drops."
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
                        text = "Welcome to Kithe",
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

        if (showExitButton) {
            TextButton(
                onClick = onExit,
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text("Exit app")
            }
        }
    }
}

private data class OnboardingSlide(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val TERMS_PRIVACY_TABS = listOf(
    "Terms of Service",
    "Privacy Policy"
)



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

private data class BusinessKpiTile(
    val title: String,
    val icon: ImageVector,
    val value: Int,
    val subtitle: String
)

@Composable
private fun BusinessHomeDestination(
    modifier: Modifier = Modifier,
    businessName: String?,
    businessCategories: List<BusinessCategory>,
    metrics: BusinessHomeMetrics,
    onViewDashboard: () -> Unit,
    onUpdateBusinessProfile: () -> Unit,
    onViewMyDrops: () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BusinessOverviewContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            businessName = businessName,
            businessCategories = businessCategories,
            metrics = metrics,
            onViewDashboard = onViewDashboard,
            onViewMyDrops = onViewMyDrops
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BusinessOverviewContent(
    modifier: Modifier = Modifier,
    businessName: String?,
    businessCategories: List<BusinessCategory>,
    metrics: BusinessHomeMetrics,
    onViewDashboard: () -> Unit,
    onViewMyDrops: () -> Unit
) {
    val kpiTiles = remember(metrics) {
        listOf(
            BusinessKpiTile("Live drops", Icons.Rounded.Place, metrics.liveDropCount, "Active offers nearby"),
            BusinessKpiTile("Pending review", Icons.Rounded.Flag, metrics.pendingReviewCount, "Reports to review"),
            BusinessKpiTile("Redemptions", Icons.Rounded.CheckCircle, metrics.unresolvedRedemptionCount, "Awaiting confirmation"),
            BusinessKpiTile("Expiring soon", Icons.Rounded.Refresh, metrics.expiringOfferCount, "Time-sensitive"),
        )
    }

    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = GridCells.Adaptive(180.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 144.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            BusinessHeroCard(
                businessName = businessName,
                businessCategories = businessCategories
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(text = "At a glance")
        }

        items(kpiTiles) { tile ->
            BusinessMetricCard(tile)
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            BusinessFulfillmentSection(
                metrics = metrics,
                onViewDashboard = onViewDashboard,
                onViewMyDrops = onViewMyDrops
            )
        }
    }
}

@Composable
private fun BusinessMetricCard(tile: BusinessKpiTile) {
    ElevatedCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(tile.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(tile.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = tile.value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = tile.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun BusinessFulfillmentSection(
    metrics: BusinessHomeMetrics,
    onViewDashboard: () -> Unit,
    onViewMyDrops: () -> Unit
) {
    ElevatedCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Queues & follow-ups", style = MaterialTheme.typography.titleMedium)
            BusinessActionRow(
                icon = Icons.Rounded.CheckCircle,
                title = "Pending redemptions",
                subtitle = "${metrics.unresolvedRedemptionCount} awaiting confirmation",
                onClick = onViewMyDrops
            )
            BusinessActionRow(
                icon = Icons.Rounded.Refresh,
                title = "Expiring offers",
                subtitle = "${metrics.expiringOfferCount} offers expiring soon",
                onClick = onViewMyDrops
            )
            if (metrics.pendingReviewCount > 0) {
                BusinessActionRow(
                    icon = Icons.Rounded.Flag,
                    title = "Flagged drops",
                    subtitle = "${metrics.pendingReviewCount} ${if (metrics.pendingReviewCount == 1) "drop has" else "drops have"} been reported",
                    onClick = onViewDashboard
                )
            }
            BusinessActionRow(
                icon = Icons.Rounded.Lightbulb,
                title = "Analytics",
                subtitle = "View performance across all your drops",
                onClick = onViewDashboard
            )
        }
    }
}


@Composable
private fun BusinessActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BusinessHeroCard(
    businessName: String?,
    businessCategories: List<BusinessCategory>,
    brandImageUrl: String? = null
) {
    val title = businessName?.takeIf { it.isNotBlank() }?.let { "$it on Kithe" }
        ?: "Welcome to Kithe Business"
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

@OptIn(ExperimentalMaterial3Api::class)
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
    dropVisibility: DropVisibility,
    onDropVisibilityChange: (DropVisibility) -> Unit,
    groupCodeInput: TextFieldValue,
    onGroupCodeInputChange: (TextFieldValue) -> Unit,
    joinedGroups: List<String>,
    onSelectGroupCode: (String) -> Unit,
    redemptionLimitInput: TextFieldValue,
    onRedemptionLimitChange: (TextFieldValue) -> Unit,
    decayDaysInput: TextFieldValue,
    onDecayDaysChange: (TextFieldValue) -> Unit,
    onManageGroupCodes: () -> Unit,
    onSubmit: () -> Unit,
    onCreateHunt: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )
    val bottomNavHeightPx = with(LocalDensity.current) { 36.dp.roundToPx() }
    val contentIsValid = remember(dropContentType, note, capturedPhotoPath, capturedAudioUri) {
        when (dropContentType) {
            DropContentType.TEXT -> note.text.isNotBlank()
            DropContentType.PHOTO -> capturedPhotoPath != null
            DropContentType.AUDIO -> capturedAudioUri != null
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isSubmitting) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        windowInsets = WindowInsets(0, 0, 0, bottomNavHeightPx)
    ) {
        LaunchedEffect(Unit) { sheetState.expand() }

        Box(modifier = Modifier.fillMaxWidth()) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Create a drop",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Complete the steps below to share something new with nearby explorers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting
                ) {
                    Text("Close")
                }
            }

            OutlinedCard(
                onClick = onCreateHunt,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Create Scavenger Hunt", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Drop a chain of connected clues for explorers to find.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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
                val templateSuggestions = remember(businessCategories) {
                    dropTemplatesFor(businessCategories)
                        .filter { it.contentType != DropContentType.AUDIO }
                        .take(MAX_BUSINESS_TEMPLATE_SUGGESTIONS)
                }
                var settingsExpanded by rememberSaveable { mutableStateOf(false) }
                val settingsSummary = remember(dropVisibility, decayDaysInput) {
                    buildString {
                        append(when (dropVisibility) {
                            DropVisibility.Public -> "Public"
                            DropVisibility.GroupOnly -> "Group only"
                        })
                        val days = decayDaysInput.text.toIntOrNull()
                        if (days != null && days > 0) append(" · Expires in $days days")
                    }
                }
                // Task 4.3 — offers no longer carry an author-set code; the server
                // issues one per redeemer, so there is nothing to validate here.
                val isOfferValid = true

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BusinessPlanStep(
                        dropType = dropType,
                        onDropTypeChange = onDropTypeChange,
                        businessName = businessName,
                        businessCategories = businessCategories,
                        templateSuggestions = templateSuggestions,
                        onDropContentTypeChange = onDropContentTypeChange,
                        onNoteChange = onNoteChange,
                        onDescriptionChange = onDescriptionChange
                    )
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
                    )
                    if (dropType == DropType.RESTAURANT_COUPON) {
                        BusinessOfferStep(
                            redemptionLimitInput = redemptionLimitInput,
                            onRedemptionLimitChange = onRedemptionLimitChange
                        )
                    }

                    // Collapsible settings
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { settingsExpanded = !settingsExpanded }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Settings", style = MaterialTheme.typography.titleSmall)
                                if (!settingsExpanded) {
                                    Text(
                                        text = settingsSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (settingsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (settingsExpanded) "Collapse settings" else "Expand settings"
                            )
                        }
                    }

                    if (settingsExpanded) {
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

                    DropSubmitButton(
                        isSubmitting = isSubmitting,
                        onSubmit = onSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (contentIsValid && isOfferValid) 1f else 0.6f),
                        enabled = contentIsValid && isOfferValid
                    )
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                var settingsExpanded by rememberSaveable { mutableStateOf(false) }
                val settingsSummary = remember(dropVisibility, decayDaysInput) {
                    buildString {
                        append(when (dropVisibility) {
                            DropVisibility.Public -> "Public"
                            DropVisibility.GroupOnly -> "Group only"
                        })
                        val days = decayDaysInput.text.toIntOrNull()
                        if (days != null && days > 0) append(" · Expires in $days days")
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                    )

                    // Collapsible settings
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { settingsExpanded = !settingsExpanded }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Settings", style = MaterialTheme.typography.titleSmall)
                                if (!settingsExpanded) {
                                    Text(
                                        text = settingsSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (settingsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (settingsExpanded) "Collapse settings" else "Expand settings"
                            )
                        }
                    }

                    if (settingsExpanded) {
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

                    DropSubmitButton(
                        isSubmitting = isSubmitting,
                        onSubmit = onSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (contentIsValid) 1f else 0.6f),
                        enabled = contentIsValid
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        } // Box
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
private fun DropContentFormatSection(
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
private fun DropNoteAndDescriptionSection(
    dropContentType: DropContentType,
    note: TextFieldValue,
    onNoteChange: (TextFieldValue) -> Unit,
    description: TextFieldValue,
    onDescriptionChange: (TextFieldValue) -> Unit
) {
    val noteLabel = when (dropContentType) {
        DropContentType.TEXT -> "Your note"
        DropContentType.PHOTO, DropContentType.AUDIO -> "Caption (optional)"
    }
    val noteSupporting = when (dropContentType) {
        DropContentType.TEXT -> "Share a friendly message, hint, or story for people who find this drop."
        DropContentType.PHOTO -> "Add a short caption to go with your photo."
        DropContentType.AUDIO -> "Add a short caption to go with your audio clip."
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
private fun DropAutoDeleteSection(
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
private fun DropMediaAttachmentsSection(
    context: Context,
    dropContentType: DropContentType,
    capturedPhotoPath: String?,
    onCapturePhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    capturedAudioUri: String?,
    onRecordAudio: () -> Unit,
    onClearAudio: () -> Unit,
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


        DropContentType.TEXT -> Unit
    }
}

@Composable
private fun DropVisibilitySectionCard(
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
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true
) {
    Button(
        enabled = !isSubmitting && enabled,
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
private fun FaqDialog(
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val faqEntries = remember {
        listOf(
            FaqEntry(
                question = R.string.faq_question_core_features,
                answer = R.string.faq_answer_core_features
            ),
            FaqEntry(
                question = R.string.faq_question_drop_contents,
                answer = R.string.faq_answer_drop_contents
            ),
            FaqEntry(
                question = R.string.faq_question_explorer_usage,
                answer = R.string.faq_answer_explorer_usage
            ),
            FaqEntry(
                question = R.string.faq_question_groups,
                answer = R.string.faq_answer_groups
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.faq_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.faq_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                faqEntries.forEach { entry ->
                    FaqEntryContent(
                        question = stringResource(entry.question),
                        answer = stringResource(entry.answer)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.faq_close))
                    }
                }
            }
        }
    }
}

private data class FaqEntry(
    val question: Int,
    val answer: Int
)

@Composable
private fun FaqEntryContent(
    question: String,
    answer: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = question,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = answer,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TermsPrivacyDialog(
    initialTab: Int,
    manifest: LegalPolicyManifest?,
    onOpenPolicy: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val tabCount = TERMS_PRIVACY_TABS.size
    var selectedTab by remember { mutableStateOf(initialTab.coerceIn(0, tabCount - 1)) }

    LaunchedEffect(initialTab) {
        val clamped = initialTab.coerceIn(0, tabCount - 1)
        selectedTab = clamped
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.terms_privacy_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = manifest?.let { "Approved policy version: ${it.version}" }
                        ?: "Approved policies are currently unavailable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (manifest == null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
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
                OutlinedButton(
                    onClick = {
                        val url = if (selectedTab == 0) manifest?.terms else manifest?.privacy
                        if (url != null) onOpenPolicy(url)
                    },
                    enabled = manifest != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (selectedTab == 0) {
                            "Open Terms of Service"
                        } else {
                            "Open Privacy Policy"
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.terms_privacy_dialog_close))
                    }
                }
            }
        }
    }
}

private data class ApproximateLocationFix(
    val position: LatLng,
    val accuracyMeters: Double?
)

internal fun approximateLocationZoom(accuracyMeters: Double?): Float = when {
    accuracyMeters == null -> 13f
    accuracyMeters <= 100.0 -> 15f
    accuracyMeters <= 500.0 -> 14f
    accuracyMeters <= 1_500.0 -> 13f
    else -> 12f
}

private const val MIN_APPROXIMATE_AREA_RADIUS_METERS = 250.0
private const val DEFAULT_APPROXIMATE_AREA_RADIUS_METERS = 1_000.0

internal fun approximateAreaRadiusMeters(accuracyMeters: Double?): Double {
    val reportedAccuracy = accuracyMeters?.takeIf { it.isFinite() && it > 0.0 }
        ?: return DEFAULT_APPROXIMATE_AREA_RADIUS_METERS
    return max(reportedAccuracy, MIN_APPROXIMATE_AREA_RADIUS_METERS)
}

internal enum class BrowseDistanceBand(val displayName: String) {
    NEARBY("Nearby"),
    SHORT_WALK("A short walk"),
    FARTHER_OUT("Farther out")
}

private const val NEARBY_DISTANCE_BAND_MAX_METERS = 300.0
private const val SHORT_WALK_DISTANCE_BAND_MAX_METERS = 1_000.0

internal fun browseDistanceBand(distanceMeters: Double): BrowseDistanceBand = when {
    distanceMeters <= NEARBY_DISTANCE_BAND_MAX_METERS -> BrowseDistanceBand.NEARBY
    distanceMeters <= SHORT_WALK_DISTANCE_BAND_MAX_METERS -> BrowseDistanceBand.SHORT_WALK
    else -> BrowseDistanceBand.FARTHER_OUT
}

private const val DROP_PICKUP_RADIUS_METERS = 30.0

/**
 * A fix older than this is not trusted for an unlock. Mirrors
 * `DropCollector.LOCATION_STALE_THRESHOLD_MILLIS`.
 */
private const val UNLOCK_LOCATION_STALE_THRESHOLD_MILLIS = 2 * 60 * 1000L

/**
 * Browse-scale proximity, answerable with an approximate fix. Used only for list
 * behaviour such as re-showing a drop the user dismissed earlier — never to unlock.
 */
private const val BROWSE_NEARBY_THRESHOLD_METERS = 150.0

private const val MAX_BUSINESS_TEMPLATE_SUGGESTIONS = 6

private fun formatCoordinate(value: Double): String {
    return String.format(Locale.US, "%.5f", value)
}

// Collected notes flagged as mature content are hidden for everyone; the viewer
// preference that used to explain this went away with the NSFW flag at task 2.8.
private fun hiddenFlaggedCollectedMessage(count: Int): String {
    val subject = if (count == 1) "1 collected drop is" else "$count collected drops are"
    return "$subject hidden after being flagged as mature content."
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
    canLikeDrops: Boolean,
    isSignedIn: Boolean,
    likeRestrictionMessage: String?,
    votingDropIds: Set<String>,
    selectedId: String?,
    onSelect: (CollectedNote) -> Unit,
    onLike: (CollectedNote, DropLikeStatus) -> Unit,
    onReport: (CollectedNote) -> Unit,
    onView: (CollectedNote) -> Unit,
    onRemove: (CollectedNote) -> Unit,
    emptyMessage: String? = null,
    topContentPadding: Dp = 0.dp,
    fabClearance: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(vertical = 16.dp)
) {
    if (notes.isEmpty()) {
        val message = if (hiddenNsfwCount > 0) {
            hiddenFlaggedCollectedMessage(hiddenNsfwCount)
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

    LaunchedEffect(selectedId, notes) {
        if (skipScrollForSortChange) {
            skipScrollForSortChange = false
            return@LaunchedEffect
        }
        val targetId = selectedId ?: return@LaunchedEffect
        val index = notes.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    val selectedNote = notes.firstOrNull { it.id == selectedId }

    val screenHeight = rememberScreenHeightDp()
    val panelState = rememberExplorerDropListPanelState()
    val panelTopPadding = if (fabClearance > 0.dp) {
        (fabClearance - topContentPadding).coerceAtLeast(topContentPadding)
    } else {
        topContentPadding
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topContentPadding)
        ) {
            CollectedDropsMap(
                notes = notes,
                selectedId = selectedId,
                onNoteClick = { note ->
                    onSelect(note)
                },
                modifier = Modifier.fillMaxSize()
            )

            if (selectedNote != null && (selectedNote.lat == null || selectedNote.lng == null)) {
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

            ExplorerDropListPanel(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding(),
                state = panelState,
                mapAwareTopPadding = panelTopPadding,
                panelMaxHeight = screenHeight,
                expandWhen = selectedId != null,
                listState = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                header = {
                    if (hiddenNsfwCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 2.dp,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = hiddenFlaggedCollectedMessage(hiddenNsfwCount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        DropSortMenu(
                            modifier = Modifier.align(Alignment.CenterStart),
                            current = sortOption,
                            options = sortOptions,
                            onSelect = onSortOptionChange
                        )
                        Text(
                            text = stringResource(R.string.action_collected_drops_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                            CountBadge(count = notes.size)
                        }
                    }
                },
                body = {
                    items(notes, key = { it.id }) { note ->
                        val isHighlighted = note.id == selectedId
                        val alreadyReported = reportedDropIds.contains(note.id)
                        val restrictionMessage = when {
                            alreadyReported -> "Thanks for your report. We'll review it soon."
                            !canReportDrops -> "Sign in to report drops."
                            else -> null
                        }
                        val isReporting = isReportProcessing && reportingDropId == note.id
                        val canReact = canLikeDrops && isSignedIn
                        val isVoting = votingDropIds.contains(note.id)
                        CollectedNoteCard(
                            note = note,
                            selected = isHighlighted,
                            expanded = isHighlighted,
                            onSelect = { onSelect(note) },
                            likeCount = note.likeCount,
                            userLike = note.likeStatus(),
                            canLike = canReact,
                            likeRestrictionMessage = likeRestrictionMessage,
                            isVoting = isVoting,
                            onLike = { status -> onLike(note, status) },
                            canReport = canReportDrops,
                            alreadyReported = alreadyReported,
                            reportRestrictionMessage = restrictionMessage,
                            isReporting = isReporting,
                            onReport = { onReport(note) },
                            onView = { onView(note) },
                            onRemove = { onRemove(note) }
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun GeoDropHeader(
    modifier: Modifier = Modifier,
    onShowTutorial: () -> Unit = {},
    onShowFaq: () -> Unit = {},
    onShowTerms: () -> Unit = {},
    onShowPrivacy: () -> Unit = {},
) {
    var infoMenuExpanded by remember { mutableStateOf(false) }
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

        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            IconButton(
                onClick = { infoMenuExpanded = true }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = stringResource(R.string.content_description_open_info_menu)
                )
            }

            DropdownMenu(
                expanded = infoMenuExpanded,
                onDismissRequest = { infoMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.info_menu_tutorial)) },
                    leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                    onClick = {
                        infoMenuExpanded = false
                        onShowTutorial()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.info_menu_faq)) },
                    leadingIcon = { Icon(Icons.Rounded.Help, contentDescription = null) },
                    onClick = {
                        infoMenuExpanded = false
                        onShowFaq()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.info_menu_terms)) },
                    leadingIcon = { Icon(Icons.Rounded.Description, contentDescription = null) },
                    onClick = {
                        infoMenuExpanded = false
                        onShowTerms()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.info_menu_privacy)) },
                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                    onClick = {
                        infoMenuExpanded = false
                        onShowPrivacy()
                    }
                )
            }
        }
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
    unlockGate: Boolean,
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
    val configuration = LocalConfiguration.current
    val maxDialogHeight = remember(configuration) {
        (configuration.screenHeightDp.dp * 0.9f).coerceAtLeast(0.dp)
    }

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
                    .imePadding()
                    .heightIn(max = maxDialogHeight)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (unlockGate) {
                        stringResource(R.string.r5_unlock_account_title)
                    } else when (accountType) {
                        AccountType.EXPLORER -> "Account"
                        AccountType.BUSINESS -> "Organizer account"
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
                                        AccountType.EXPLORER -> "Participant"
                                        AccountType.BUSINESS -> "Organizer"
                                    }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = if (unlockGate) {
                        stringResource(R.string.r5_unlock_account_body)
                    } else when (accountType) {
                        AccountType.EXPLORER -> "Save finds and join Experiences with an account."
                        AccountType.BUSINESS -> "Sign in with an approved Organizer account."
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
                val requiresExplorerUsername = isRegister &&
                    accountType == AccountType.EXPLORER &&
                    !unlockGate

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
private fun BusinessFirstRunDialog(onContinue: () -> Unit) {
    data class Step(val icon: ImageVector, val title: String, val desc: String)
    val steps = listOf(
        Step(Icons.Rounded.AddCircle, "Create a drop", "Place a message, photo, or offer at any real-world location — a storefront, event venue, or local landmark."),
        Step(Icons.Rounded.Map, "Explorers discover it", "Nearby users walking by see your drop on the map. They walk up to it to unlock and collect it."),
        Step(Icons.Rounded.Lightbulb, "Track performance", "See how many people collected your drop, liked it, and redeemed your offers in your analytics dashboard.")
    )
    AlertDialog(
        onDismissRequest = {},
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Rounded.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("Welcome to Kithe for Business", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Kithe lets you engage local customers by placing digital drops at real-world locations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                steps.forEach { step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp).padding(top = 2.dp)
                        )
                        Column {
                            Text(step.title, style = MaterialTheme.typography.titleSmall)
                            Text(step.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text("Get started")
            }
        }
    )
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
    experiences: List<ExperienceAnalytics>,
    loading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onDeleteDrop: (Drop) -> Unit
) {
    val maxDialogHeight = LocalConfiguration.current.screenHeightDp.dp * 0.9f
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxDialogHeight)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
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

                        Text(
                            text = "Experience totals",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Server-verified activity across invite-only experiences you own.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (experiences.isEmpty()) {
                            Text(
                                text = "Create an experience to see its aggregate activity here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(experiences, key = { "experience_${it.groupCode}" }) { analytics ->
                                    ExperienceAnalyticsCard(analytics)
                                }
                            }
                        }

                        Divider()

                        if (sorted.isEmpty()) {
                            Text(
                                text = "You haven't shared any business drops yet. Create one to see per-drop analytics here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val flaggedDrops = sorted.filter { it.reportCount > 0 }
                        if (flaggedDrops.isNotEmpty()) {
                            Text(
                                text = "Flagged by users (${flaggedDrops.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(flaggedDrops, key = { it.id }) { drop ->
                                    BusinessDropAnalyticsCard(
                                        drop = drop,
                                        onDeleteDrop = { onDeleteDrop(drop) },
                                        availability = drop.currentReleaseAvailability()
                                    )
                                }
                            }
                            Divider()
                            Text(
                                text = "All drops",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (sorted.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 360.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(sorted, key = { "all_${it.id}" }) { drop ->
                                    BusinessDropAnalyticsCard(
                                        drop = drop,
                                        onDeleteDrop = { onDeleteDrop(drop) },
                                        availability = drop.currentReleaseAvailability()
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
@OptIn(ExperimentalLayoutApi::class)
private fun ExperienceAnalyticsCard(analytics: ExperienceAnalytics) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Experience ${analytics.groupCode}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${analytics.drops} drops",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${analytics.collects} collects",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${analytics.redemptions} redemptions",
                    style = MaterialTheme.typography.bodyMedium
                )
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

/** The live flag values, resolved at render time rather than threaded through state. */
private fun Drop.currentReleaseAvailability(): DropReleaseAvailability = releaseAvailability(
    couponsEnabled = PilotFeatureFlags.couponsEnabled,
    mediaEnabled = PilotFeatureFlags.mediaEnabled,
    huntsEnabled = PilotFeatureFlags.huntsEnabled
)

@Composable
private fun BusinessDropAnalyticsCard(
    drop: Drop,
    onDeleteDrop: (() -> Unit)? = null,
    availability: DropReleaseAvailability = DropReleaseAvailability.AVAILABLE
) {
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

            availability.ownerExplanation()?.let { explanation ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Not visible to attendees",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

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

            val reactionSummary = "Likes: ${drop.likeCount}"
            Text(
                text = reactionSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (drop.reportCount > 0) {
                Text(
                    text = "${drop.reportCount} user ${if (drop.reportCount == 1L) "report" else "reports"} — review and delete if needed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (onDeleteDrop != null) {
                OutlinedButton(
                    onClick = onDeleteDrop,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete drop")
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun OtherDropsExplorerSection(
    modifier: Modifier = Modifier,
    unlockedDropIds: Set<String> = emptySet(),
    unlockingDropId: String? = null,
    topContentPadding: Dp = 0.dp,
    fabClearance: Dp = 0.dp,
    destinationLabel: String = "",
    loading: Boolean,
    refreshing: Boolean,
    drops: List<Drop>,
    currentLocation: LatLng?,
    currentLocationAccuracyMeters: Double?,
    approximateLocationEnabled: Boolean,
    locationNeedsSettings: Boolean,
    onRequestLocation: () -> Unit,
    error: String?,
    emptyMessage: String? = null,
    selectedId: String?,
    onSelect: (Drop) -> Unit,
    sortOption: DropSortOption,
    sortOptions: List<DropSortOption>,
    onSortOptionChange: (DropSortOption) -> Unit,
    canLikeDrops: Boolean,
    likeRestrictionMessage: String?,
    currentUserId: String?,
    isSignedIn: Boolean,
    collectedDropIds: Set<String>,
    canParticipate: Boolean,
    collectRestrictionMessage: String?,
    browseReportingDropId: String?,
    onPickUp: (Drop) -> Unit,
    onReport: (Drop) -> Unit,
    onIgnoreForNow: (Drop) -> Unit,
    onBlock: ((Drop) -> Unit)? = null,
    onRefresh: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp)
                .padding(top = topContentPadding + 76.dp)
                .zIndex(3f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (approximateLocationEnabled) {
                        if (currentLocation == null) "Finding your approximate area" else "Approximate area shown"
                    } else {
                        "Browse without sharing your location"
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (approximateLocationEnabled) {
                        "Your location is shown as a broad area. Drop distances are grouped as Nearby, A short walk, or Farther out. Precise location is requested only when you unlock a drop."
                    } else {
                        "All visible drops remain available. Turn on approximate location to group drops into broad distance bands and position the map."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!approximateLocationEnabled) {
                    TextButton(onClick = onRequestLocation) {
                        Text(if (locationNeedsSettings) "Open Settings" else "Use my location")
                    }
                }
            }
        }

        if (refreshing && !loading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp)
                    .padding(top = topContentPadding)
            )
        }

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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
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

            else -> {
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

                LaunchedEffect(selectedId, drops) {
                    if (skipScrollForSortChange) {
                        skipScrollForSortChange = false
                        return@LaunchedEffect
                    }
                    val targetId = selectedId ?: return@LaunchedEffect
                    val index = drops.indexOfFirst { it.id == targetId }
                    if (index >= 0) {
                        listState.animateScrollToItem(index)
                    }
                }

                val panelState = rememberExplorerDropListPanelState()
                val screenHeight = rememberScreenHeightDp()
                val panelTopPadding = if (fabClearance > 0.dp) {
                    (fabClearance - topContentPadding).coerceAtLeast(topContentPadding)
                } else {
                    topContentPadding
                }
                val panelMaxHeight = screenHeight
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topContentPadding)
                ) {
                    OtherDropsMap(
                        drops = drops,
                        selectedDropId = selectedId,
                        currentLocation = currentLocation,
                        currentLocationAccuracyMeters = currentLocationAccuracyMeters,
                        onDropClick = onSelect,
                        modifier = Modifier.fillMaxSize()
                    )

                    ExplorerDropListPanel(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding(),
                        state = panelState,
                        mapAwareTopPadding = panelTopPadding,
                        panelMaxHeight = panelMaxHeight,
                        expandWhen = selectedId != null,
                        listState = listState,
                        header = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                DropSortMenu(
                                    modifier = Modifier.align(Alignment.CenterStart),
                                    current = sortOption,
                                    options = sortOptions,
                                    onSelect = onSortOptionChange
                                )
                                if (destinationLabel.isNotBlank()) {
                                    Text(
                                        text = destinationLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (drops.isNotEmpty()) {
                                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                                        CountBadge(count = drops.size)
                                    }
                                }
                            }

                            if (!likeRestrictionMessage.isNullOrBlank() && !canLikeDrops) {
                                Text(
                                    text = likeRestrictionMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        body = {
                            items(drops, key = { it.id }) { drop ->
                                val userLike = drop.userLikeStatus(currentUserId)
                                val isOwnDrop = currentUserId != null && drop.createdBy == currentUserId
                                val alreadyReported = currentUserId?.let { id ->
                                    drop.reportedBy.containsKey(id)
                                } == true
                                val hasCollected = collectedDropIds.contains(drop.id)
                                val isUnlocked = hasCollected || drop.id in unlockedDropIds
                                val canReportDrop = isSignedIn && !isOwnDrop && isUnlocked
                                val reportRestrictionMessage = when {
                                    isOwnDrop -> "You created this drop."
                                    !isSignedIn -> "Sign in to report drops."
                                    alreadyReported -> "Thanks for your report. We'll review it soon."
                                    else -> null
                                }

                                OtherDropRow(
                                    drop = drop,
                                    isSelected = drop.id == selectedId,
                                    currentLocation = currentLocation,
                                    isUnlocked = isUnlocked,
                                    isUnlocking = unlockingDropId == drop.id,
                                    userLike = userLike,
                                    canPickUp = canParticipate,
                                    pickupRestrictionMessage = collectRestrictionMessage,
                                    showReport = !isOwnDrop,
                                    canReport = canReportDrop,
                                    alreadyReported = alreadyReported,
                                    reportRestrictionMessage = reportRestrictionMessage,
                                    isReporting = browseReportingDropId == drop.id,
                                    canIgnoreForNow = !isUnlocked,
                                    onIgnoreForNow = { onIgnoreForNow(drop) },
                                    onSelect = { onSelect(drop) },
                                    onPickUp = { onPickUp(drop) },
                                    onReport = { onReport(drop) },
                                    onBlock = if (!isOwnDrop && isSignedIn && !drop.createdBy.isNullOrBlank()) {
                                        { onBlock?.invoke(drop) }
                                    } else null
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

private enum class DropSortOption(val displayName: String) {
    // Keep the serialized key for saved-state compatibility, but group by broad bands.
    NEAREST("Nearby drops"),
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
            drops.sortedWith(
                compareBy<Drop> { drop ->
                    browseDistanceBand(
                        distanceBetweenMeters(
                            location.latitude,
                            location.longitude,
                            drop.lat,
                            drop.lng
                        )
                    ).ordinal
                }.thenByDescending { it.createdAt }
            )
        }

        // Popularity ranks on likes alone. The net-score weighting it replaced went
        // out with dislikes at task 2.6.
        DropSortOption.MOST_POPULAR -> drops.sortedWith(
            compareByDescending<Drop> { it.likeCount }
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
                        BrowseDistanceBand.entries.size
                    } else {
                        browseDistanceBand(
                            distanceBetweenMeters(
                                location.latitude,
                                location.longitude,
                                lat,
                                lng
                            )
                        ).ordinal
                    }
                }.thenByDescending { it.collectedAt }
            )
        }

        DropSortOption.MOST_POPULAR -> notes.sortedWith(
            compareByDescending<CollectedNote> { it.likeCount }
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
            label = {},
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Sort,
                    contentDescription = "Sort"
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

private enum class ExplorerDropListPanelValue { Collapsed, Expanded }

@OptIn(ExperimentalFoundationApi::class)
private class ExplorerDropListPanelState internal constructor(
    internal val anchoredState: AnchoredDraggableState<ExplorerDropListPanelValue>
) {
    val currentValue: ExplorerDropListPanelValue get() = anchoredState.currentValue
    val targetValue: ExplorerDropListPanelValue get() = anchoredState.targetValue
    val offset: Float get() = anchoredState.offset

    suspend fun animateTo(value: ExplorerDropListPanelValue) = anchoredState.animateTo(value)

    suspend fun snapTo(value: ExplorerDropListPanelValue) = anchoredState.snapTo(value)

    internal fun updateAnchors(anchors: DraggableAnchors<ExplorerDropListPanelValue>) {
        anchoredState.updateAnchors(anchors)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberExplorerDropListPanelState(
    initialValue: ExplorerDropListPanelValue = ExplorerDropListPanelValue.Collapsed
): ExplorerDropListPanelState {
    val density = LocalDensity.current
    val anchoredState = rememberAnchoredDraggableState(
        initialValue = initialValue,
        positionalThreshold = { distance -> distance * 0.5f },
        velocityThreshold = { with(density) { 80.dp.toPx() } },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )
    return remember(anchoredState) { ExplorerDropListPanelState(anchoredState) }
}

@Composable
private fun rememberScreenHeightDp(): Dp {
    val configuration = LocalConfiguration.current
    return remember(configuration) { configuration.screenHeightDp.dp }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> rememberAnchoredDraggableState(
    initialValue: T,
    animationSpec: AnimationSpec<Float>,
    confirmValueChange: (T) -> Boolean = { true },
    positionalThreshold: (distance: Float) -> Float,
    velocityThreshold: () -> Float
): AnchoredDraggableState<T> {
    return remember(initialValue, animationSpec, confirmValueChange, positionalThreshold, velocityThreshold) {
        AnchoredDraggableState(
            initialValue = initialValue,
            animationSpec = animationSpec,
            confirmValueChange = confirmValueChange,
            positionalThreshold = positionalThreshold,
            velocityThreshold = velocityThreshold
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExplorerDropListPanel(
    modifier: Modifier = Modifier,
    state: ExplorerDropListPanelState = rememberExplorerDropListPanelState(),
    mapAwareTopPadding: Dp = 0.dp,
    panelWidth: Dp = 360.dp,
    panelMaxHeight: Dp = 420.dp,
    expandWhen: Boolean? = null,
    handleWidth: Dp = 40.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    listState: LazyListState,
    header: @Composable ColumnScope.() -> Unit = {},
    body: LazyListScope.() -> Unit
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val handleLabel = stringResource(R.string.drop_list_handle_label)

    BoxWithConstraints(modifier = modifier.fillMaxHeight().heightIn(max = panelMaxHeight)) {
        val collapsedPanelWidth = remember(panelWidth, maxWidth) {
            panelWidth.coerceAtMost(maxWidth)
        }
        val expandedPanelWidth = remember(maxWidth) { maxWidth }
        val collapsedOffset = with(density) { collapsedPanelWidth.toPx() }
        val anchors = remember(collapsedOffset) {
            DraggableAnchors {
                ExplorerDropListPanelValue.Collapsed at collapsedOffset
                ExplorerDropListPanelValue.Expanded at 0f
            }
        }
        val panelTranslationX = finitePanelTranslation(state.offset, collapsedOffset)

        LaunchedEffect(anchors, expandWhen) {
            state.updateAnchors(anchors)
            if (expandWhen == true) {
                state.animateTo(ExplorerDropListPanelValue.Expanded)
            }
        }

        val isExpanded by remember {
            derivedStateOf {
                state.targetValue == ExplorerDropListPanelValue.Expanded ||
                        (state.currentValue == ExplorerDropListPanelValue.Expanded &&
                                state.targetValue == state.currentValue)
            }
        }
        val currentPanelWidth = remember(isExpanded, collapsedPanelWidth, expandedPanelWidth) {
            if (isExpanded) expandedPanelWidth else collapsedPanelWidth
        }

        val startPadding = contentPadding.calculateStartPadding(layoutDirection)
        val endPadding = contentPadding.calculateEndPadding(layoutDirection)
        val topPadding = contentPadding.calculateTopPadding()
        val bottomPadding = contentPadding.calculateBottomPadding()
        val availablePanelHeight = remember(maxHeight, mapAwareTopPadding) {
            (maxHeight - mapAwareTopPadding).coerceAtLeast(0.dp)
        }
        val minPanelHeight = 240.dp
        val effectiveMinPanelHeight = remember(availablePanelHeight) {
            minPanelHeight.coerceAtMost(availablePanelHeight)
        }
        val defaultPanelHeight = remember(availablePanelHeight) {
            (availablePanelHeight * 0.75f).coerceAtLeast(effectiveMinPanelHeight)
        }
        var panelHeightValue by rememberSaveable { mutableStateOf(defaultPanelHeight.value) }
        LaunchedEffect(availablePanelHeight, effectiveMinPanelHeight) {
            panelHeightValue = panelHeightValue.coerceIn(
                effectiveMinPanelHeight.value,
                availablePanelHeight.value
            )
        }
        val currentPanelHeight = panelHeightValue.dp

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(top = mapAwareTopPadding)
                .height(currentPanelHeight)
                .heightIn(min = effectiveMinPanelHeight, max = availablePanelHeight)
                .width(currentPanelWidth)
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(currentPanelWidth)
                    .height(currentPanelHeight)
                    .heightIn(min = effectiveMinPanelHeight, max = availablePanelHeight)
                    .graphicsLayer { translationX = panelTranslationX },
                shape = RectangleShape,
                tonalElevation = 8.dp,
                shadowElevation = 0.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            ) {
                val resizeHandleLabel = stringResource(R.string.drop_list_resize_handle)
                val resizeDragState = rememberDraggableState { delta ->
                    val deltaDp = with(density) { delta.toDp() }
                    panelHeightValue = (panelHeightValue - deltaDp.value).coerceIn(
                        effectiveMinPanelHeight.value,
                        availablePanelHeight.value
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .heightIn(min = effectiveMinPanelHeight, max = availablePanelHeight)
                        .padding(
                            start = startPadding,
                            top = topPadding,
                            end = endPadding,
                            bottom = bottomPadding
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .draggable(
                                state = resizeDragState,
                                orientation = Orientation.Vertical
                            )
                            .semantics { contentDescription = resizeHandleLabel },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DragHandle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        header()
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            top = 12.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        body()
                    }
                }
            }

            if (isExpanded) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            state.animateTo(ExplorerDropListPanelValue.Collapsed)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.content_description_close_drop_list_panel)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isExpanded,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                modifier = Modifier
                    .width(handleWidth)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        coroutineScope.launch {
                            state.animateTo(ExplorerDropListPanelValue.Expanded)
                        }
                    },
                shape = RectangleShape,
                tonalElevation = 8.dp,
                shadowElevation = 0.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Top)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ExpandLess,
                        contentDescription = null,
                        modifier = Modifier.rotate(-90f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = handleLabel,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectedDropsMap(
    notes: List<CollectedNote>,
    selectedId: String?,
    modifier: Modifier = Modifier,
    onNoteClick: (CollectedNote) -> Unit
) {
    val notesWithLocation = remember(notes) { notes.filter { it.lat != null && it.lng != null } }
    val cameraPositionState = rememberCameraPositionState()
    val uiSettings = remember { MapUiSettings(zoomControlsEnabled = true) }

    val highlightedNote = notesWithLocation.firstOrNull { it.id == selectedId }
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
                    }
                }

                val markerIcon = when {
                    note.isNsfw -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
                    note.id == selectedId -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                    else -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                }

                Marker(
                    state = MarkerState(position),
                    title = title,
                    snippet = snippetParts.joinToString("\n"),
                    icon = markerIcon,
                    zIndex = if (note.id == selectedId) 1f else 0f,
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
    likeCount: Long,
    userLike: DropLikeStatus,
    canLike: Boolean,
    likeRestrictionMessage: String?,
    isVoting: Boolean,
    onLike: (DropLikeStatus) -> Unit,
    canReport: Boolean,
    alreadyReported: Boolean,
    reportRestrictionMessage: String?,
    isReporting: Boolean,
    onReport: () -> Unit,
    onView: () -> Unit,
    onRemove: () -> Unit
) {
    val (containerColor, contentColor, supportingColor) = explorerDropCardColors(selected)
    val typeLabel = when (note.contentType) {
        DropContentType.TEXT -> "Text note"
        DropContentType.PHOTO -> "Photo drop"
        DropContentType.AUDIO -> "Audio drop"
    }
    val dropperHandle = note.dropperUsername?.takeIf { it.isNotBlank() }?.let { "@${it}" }
    val previewText = note.description?.takeIf { it.isNotBlank() }
        ?: note.text.takeIf { it.isNotBlank() }
        ?: when (note.contentType) {
            DropContentType.TEXT -> "(No message)"
            DropContentType.PHOTO -> "Photo drop"
            DropContentType.AUDIO -> "Audio drop"
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val annotatedTitle = remember(dropperHandle, typeLabel) {
                    if (dropperHandle != null) {
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(dropperHandle)
                            }
                            append(" dropped ")
                            append(typeLabel.lowercase())
                        }
                    } else {
                        AnnotatedString(typeLabel)
                    }
                }
                Text(
                    text = annotatedTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
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

            Text(
                text = previewText,
                style = MaterialTheme.typography.bodyMedium,
                color = supportingColor,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
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
                                contentDescription = previewText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 160.dp, max = 280.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                    }

//                    Text(
//                        text = "Collected: ${formatTimestamp(note.collectedAt)}",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = supportingColor
//                    )
//
//                    note.dropCreatedAt?.let {
//                        Text(
//                            text = "Dropped: ${formatTimestamp(it)}",
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = supportingColor
//                        )
//                    }

                    Text(
                        text = note.dropperUsername?.takeIf { it.isNotBlank() }
                            ?.let { "Dropped by @$it" }
                            ?: "Left by a stranger",
                        style = MaterialTheme.typography.bodyMedium,
                        color = supportingColor
                    )

                    note.groupCode?.let { group ->
                        Text(
                            text = "Group: $group",
                            style = MaterialTheme.typography.bodyMedium,
                            color = supportingColor
                        )
                    }

//                    if (note.lat != null && note.lng != null) {
//                        Text(
//                            text = "Location: ${formatCoordinate(note.lat)}, ${formatCoordinate(note.lng)}",
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = supportingColor
//                        )
//                    } else {
//                        Text(
//                            text = "Location: Unknown",
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = supportingColor
//                        )
//                    }

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
                                        text = likeCount.toString(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                if (canReport) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = onReport,
                                            enabled = !alreadyReported && !isReporting
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

                        if (likeRestrictionMessage != null) {
                            Text(
                                text = likeRestrictionMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = supportingColor
                            )
                        }

                        reportRestrictionMessage?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = supportingColor
                            )
                        }
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
    currentLocationAccuracyMeters: Double?,
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
    fabClearance: Dp = 0.dp,
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

                val screenHeight = rememberScreenHeightDp()
                val panelState = rememberExplorerDropListPanelState()
                val panelTopPadding = if (fabClearance > 0.dp) {
                    (fabClearance - topContentPadding).coerceAtLeast(topContentPadding)
                } else {
                    topContentPadding
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topContentPadding)
                ) {
                    MyDropsMap(
                        drops = drops,
                        selectedDropId = selectedId,
                        currentLocation = currentLocation,
                        currentLocationAccuracyMeters = currentLocationAccuracyMeters,
                        onDropClick = onSelect,
                        modifier = Modifier.fillMaxSize()
                    )

                    ExplorerDropListPanel(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding(),
                        state = panelState,
                        mapAwareTopPadding = panelTopPadding,
                        panelMaxHeight = screenHeight,
                        expandWhen = selectedId != null,
                        listState = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        header = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                DropSortMenu(
                                    modifier = Modifier.align(Alignment.CenterStart),
                                    current = sortOption,
                                    options = sortOptions,
                                    onSelect = onSortOptionChange
                                )
                                Text(
                                    text = stringResource(R.string.action_my_drops_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (drops.isNotEmpty()) {
                                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                                        CountBadge(count = drops.size)
                                    }
                                }
                            }

                        },
                        body = {
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
                    )
                }
            }
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

            if (!isOwner) {
                IconButton(onClick = { onRemove(code) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Leave group"
                    )
                }
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
    currentLocationAccuracyMeters: Double?,
    onDropClick: (Drop) -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState()
    val uiSettings = remember { MapUiSettings(zoomControlsEnabled = true) }
    var cameraCenteredOnUser by remember { mutableStateOf(false) }

    // An explicit drop selection wins. Without one, wait for the user's approximate
    // area; use the first drop only as a temporary fallback when location is unavailable.
    LaunchedEffect(drops, selectedDropId) {
        val targetDrop = drops.firstOrNull { it.id == selectedDropId }
        if (targetDrop != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(targetDrop.lat, targetDrop.lng), 18f)
            )
        } else if (!cameraCenteredOnUser && currentLocation == null) {
            drops.firstOrNull()?.let { firstDrop ->
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(firstDrop.lat, firstDrop.lng), 13f)
                )
            }
        }
    }

    // Center on the approximate area only the first time it becomes available. The
    // zoom reflects the fix's accuracy instead of implying GPS-grade precision.
    LaunchedEffect(currentLocation, currentLocationAccuracyMeters, selectedDropId) {
        if (cameraCenteredOnUser || selectedDropId != null) return@LaunchedEffect
        currentLocation?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    it,
                    approximateLocationZoom(currentLocationAccuracyMeters)
                )
            )
            cameraCenteredOnUser = true
        }
    }

    GoogleMap(
        modifier = modifier
            .fillMaxSize()
            .consumeMapGesturesInParent(),
        cameraPositionState = cameraPositionState,
        uiSettings = uiSettings
    ) {
        currentLocation?.let { location ->
            Circle(
                center = location,
                radius = approximateAreaRadiusMeters(currentLocationAccuracyMeters),
                strokeColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
                strokeWidth = 1f,
                fillColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                zIndex = 0f
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
            snippetParts.add("Likes: ${drop.likeCount}")
            if (drop.isNsfw) {
                snippetParts.add("Marked as adult content")
            }

            val isSelected = drop.id == selectedDropId
            val markerIcon = when {
                isSelected -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                drop.isNsfw -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
                drop.huntId != null -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
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
private fun DropHuntBadge(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = "Scavenger hunt",
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
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
private fun PickupCelebrationBanner(
    modifier: Modifier = Modifier,
    visible: Boolean,
    drop: Drop?
) {
    if (drop == null) return
    val isHuntComplete = drop.huntId != null &&
        drop.huntStepIndex != null &&
        drop.huntTotalSteps != null &&
        drop.huntStepIndex + 1 >= drop.huntTotalSteps

    val headline = when {
        isHuntComplete -> "Hunt complete!"
        drop.huntId != null -> "Clue collected — keep going!"
        drop.contentType == DropContentType.PHOTO -> "You found it!"
        drop.contentType == DropContentType.AUDIO -> "You found it!"
        else -> "You found something!"
    }
    val subline = when {
        isHuntComplete -> "You completed the entire trail. Well done."
        drop.huntId != null -> "The next step is now unlocked on the map."
        drop.dropperUsername.isNullOrBlank() ->
            "Left by a stranger just for you."
        else -> "Left by @${drop.dropperUsername}"
    }
    val icon = if (isHuntComplete) Icons.Rounded.EmojiEvents else Icons.Rounded.Star

    val infiniteTransition = rememberInfiniteTransition(label = "pickupCelebration")
    val sparkleOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pickupSparkleOffset"
    )
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pickupShimmerAlpha"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 200)) + scaleIn(
            initialScale = 0.9f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = fadeOut(animationSpec = tween(durationMillis = 250)) + scaleOut(
            targetScale = 0.95f,
            animationSpec = tween(durationMillis = 250)
        ),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isHuntComplete)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isHuntComplete) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer { translationY = sparkleOffset }
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = (if (isHuntComplete) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary).copy(alpha = shimmerAlpha),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun OtherDropRow(
    drop: Drop,
    isSelected: Boolean,
    currentLocation: LatLng?,
    isUnlocked: Boolean,
    isUnlocking: Boolean,
    userLike: DropLikeStatus,
    canPickUp: Boolean,
    pickupRestrictionMessage: String?,
    showReport: Boolean,
    canReport: Boolean,
    alreadyReported: Boolean,
    reportRestrictionMessage: String?,
    isReporting: Boolean,
    canIgnoreForNow: Boolean,
    onIgnoreForNow: () -> Unit,
    onSelect: () -> Unit,
    onPickUp: () -> Unit,
    onReport: () -> Unit,
    onBlock: (() -> Unit)? = null
) {
    val (containerColor, contentColor, supportingColor) = explorerDropCardColors(isSelected)
    val distanceMeters = currentLocation?.let { location ->
        distanceBetweenMeters(location.latitude, location.longitude, drop.lat, drop.lng)
    }
    // Task 3.2/3.3 — `currentLocation` is now an approximate fix, so it can no longer
    // decide a 30 m question. Content is revealed only once attemptUnlock has proven
    // proximity with a precise fix taken at the moment of the attempt.
    val canPreviewContent = isUnlocked
    val context = LocalContext.current
    val mediaAttachment = remember(
        context,
        drop.id,
        drop.mediaUrl,
        drop.mediaData,
        drop.mediaMimeType,
        drop.contentType
    ) {
        if (drop.contentType == DropContentType.AUDIO) {
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
                drop.huntStepLabel()?.let { label ->
                    Spacer(Modifier.width(4.dp))
                    DropHuntBadge(label = label)
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
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = drop.dropperUsername?.takeIf { it.isNotBlank() }
                            ?.let { "Left by @$it" }
                            ?: "Left by a stranger",
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

                    if (canPreviewContent && drop.contentType == DropContentType.AUDIO) {
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
                            text = browseDistanceBand(distance).displayName,
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
                                ReactionCount(
                                    icon = Icons.Rounded.ThumbUp,
                                    count = drop.likeCount,
                                    isHighlighted = userLike == DropLikeStatus.LIKED
                                )
                            }

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
                            if (onBlock != null) {
                                OutlinedButton(onClick = onBlock) {
                                    Icon(Icons.Rounded.Block, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Block")
                                }
                            }
                        }
                    }
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
                    if (!isUnlocked) {
                        Button(
                            onClick = onPickUp,
                            enabled = !isUnlocking,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isUnlocking) "Checking your location…" else "Unlock drop")
                        }
                    }

                    if (isUnlocked) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onPickUp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (canPickUp) "Pick up drop" else "Make an account to pick up")
                        }
                        if (!canPickUp) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = pickupRestrictionMessage
                                    ?: "Preview this drop while browsing as a guest. Sign in to pick it up nearby.",
                                style = MaterialTheme.typography.bodySmall,
                                color = supportingColor
                            )
                        }
                    }
                    if (isUnlocked && canIgnoreForNow) {
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

@Composable
private fun ReactionCount(
    icon: ImageVector,
    count: Long,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val highlightColor = MaterialTheme.colorScheme.primary
    val iconColor = if (isHighlighted) {
        highlightColor
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textColor = if (isHighlighted) {
        highlightColor
    } else {
        LocalContentColor.current
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor
        )

        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
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


        else -> return
    }
}

@Composable
private fun OtherDropsMap(
    drops: List<Drop>,
    selectedDropId: String?,
    currentLocation: LatLng?,
    currentLocationAccuracyMeters: Double?,
    onDropClick: (Drop) -> Unit,
    modifier: Modifier = Modifier
) {
    val businessMarkerDescriptor = remember {
        runCatching {
            BitmapDescriptorFactory.fromResource(R.drawable.business_drop_marker)
        }.getOrElse { error ->
            Log.e("GeoDrop", "Failed to load business drop marker", error)
            null
        }
    }

    val cameraPositionState = rememberCameraPositionState()
    val uiSettings = remember { MapUiSettings(zoomControlsEnabled = true) }
    var cameraCenteredOnUser by remember { mutableStateOf(false) }

    // An explicit drop selection wins. Without one, wait for the user's approximate
    // area; use the first drop only as a temporary fallback when location is unavailable.
    LaunchedEffect(drops, selectedDropId) {
        val targetDrop = drops.firstOrNull { it.id == selectedDropId }
        if (targetDrop != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(targetDrop.lat, targetDrop.lng), 18f)
            )
        } else if (!cameraCenteredOnUser && currentLocation == null) {
            drops.firstOrNull()?.let { firstDrop ->
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(firstDrop.lat, firstDrop.lng), 13f)
                )
            }
        }
    }

    // Center on the approximate area only the first time it becomes available. The
    // zoom reflects the fix's accuracy instead of implying GPS-grade precision.
    LaunchedEffect(currentLocation, currentLocationAccuracyMeters, selectedDropId) {
        if (cameraCenteredOnUser || selectedDropId != null) return@LaunchedEffect
        currentLocation?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    it,
                    approximateLocationZoom(currentLocationAccuracyMeters)
                )
            )
            cameraCenteredOnUser = true
        }
    }

    GoogleMap(
        modifier = modifier
            .fillMaxSize()
            .consumeMapGesturesInParent(),
        cameraPositionState = cameraPositionState,
        uiSettings = uiSettings
    ) {
        currentLocation?.let { location ->
            Circle(
                center = location,
                radius = approximateAreaRadiusMeters(currentLocationAccuracyMeters),
                strokeColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
                strokeWidth = 1f,
                fillColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                zIndex = 0.5f
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
                    DropContentType.PHOTO -> "Tap to preview this photo."
                    DropContentType.AUDIO -> "Tap to play this recording."
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
                isSelected -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                drop.isNsfw -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
                drop.huntId != null -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
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

private fun likeHueFor(likes: Long): Float {
    return when {
        likes >= 25 -> BitmapDescriptorFactory.HUE_AZURE
        likes >= 10 -> BitmapDescriptorFactory.HUE_GREEN
        likes >= 5 -> BitmapDescriptorFactory.HUE_YELLOW
        likes >= 1 -> BitmapDescriptorFactory.HUE_ORANGE
        else -> BitmapDescriptorFactory.HUE_RED
    }
}

private fun resolveDropMediaAttachment(context: Context, drop: Drop): DropMediaAttachment? {
    val data = drop.mediaData?.takeIf { it.isNotBlank() }
    val preferredMime = drop.mediaMimeType?.takeIf { it.isNotBlank() }
    if (data != null) {
        val (subDir, defaultMime, defaultExtension) = when (drop.contentType) {
            DropContentType.AUDIO -> Triple("audio", preferredMime ?: "audio/mpeg", "m4a")
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
    val (containerColor, contentColor, supportingColor) = explorerDropCardColors(isSelected)

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

            val previewText = drop.description?.takeIf { it.isNotBlank() }
                ?: drop.text.takeIf { it.isNotBlank() }
            previewText?.let { preview ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = supportingColor,
                    maxLines = if (isSelected) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
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

                    CompositionLocalProvider(LocalContentColor provides supportingColor) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ReactionCount(
                                icon = Icons.Rounded.ThumbUp,
                                count = drop.likeCount,
                                isHighlighted = false
                            )
                        }
                    }

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

@androidx.annotation.OptIn(UnstableApi::class)
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
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
            }
            val contentTypeLabel = when (template.contentType) {
                DropContentType.TEXT -> "Text"
                DropContentType.PHOTO -> "Photo"
                DropContentType.AUDIO -> "Audio"
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
private data class ExplorerDropCardColors(
    val container: Color,
    val content: Color,
    val supporting: Color
)

@Composable
private fun BlockedCreatorsDialog(
    hosts: List<R9BlockedHost>,
    loading: Boolean,
    onUnblock: (R9BlockedHost) -> Unit,
    onDismiss: () -> Unit
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
                        text = "Blocked hosts",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                when {
                    loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    hosts.isEmpty() -> {
                        Text(
                            text = "You haven't blocked any hosts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(hosts, key = R9BlockedHost::hostId) { host ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.AccountCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = host.hostLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    OutlinedButton(onClick = { onUnblock(host) }) {
                                        Text("Unblock")
                                    }
                                }
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun explorerDropCardColors(isSelected: Boolean): ExplorerDropCardColors {
    val container = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val supporting = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    return ExplorerDropCardColors(
        container = container,
        content = content,
        supporting = supporting
    )
}

// ── Scavenger Hunt Builder UI ────────────────────────────────────────────────

@Composable
private fun HuntBuilderDialog(
    state: HuntBuilderState,
    isSubmitting: Boolean,
    error: String?,
    isBusinessUser: Boolean,
    businessName: String?,
    businessCategories: List<BusinessCategory>,
    onStateChange: (HuntBuilderState) -> Unit,
    onAddStep: () -> Unit,
    onSubmit: (HuntBuilderState) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = !isSubmitting)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Create Scavenger Hunt",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }
                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Hunt meta
                    Text("Hunt details", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = { onStateChange(state.copy(title = it)) },
                        label = { Text("Title *") },
                        placeholder = { Text("e.g. Downtown Coffee Trail") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isSubmitting
                    )
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = { onStateChange(state.copy(description = it)) },
                        label = { Text("Description (optional)") },
                        placeholder = { Text("What's the hunt about?") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        enabled = !isSubmitting
                    )
                    OutlinedTextField(
                        value = state.decayDays?.toString() ?: "",
                        onValueChange = { input ->
                            onStateChange(state.copy(decayDays = input.toIntOrNull()?.takeIf { it > 0 }))
                        },
                        label = { Text("Expires after N days (optional)") },
                        placeholder = { Text("Leave blank for no expiry") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isSubmitting
                    )

                    HorizontalDivider()

                    // Steps
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Steps (${state.steps.size})",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onAddStep,
                            enabled = !isSubmitting && state.steps.size < 10
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add step")
                        }
                    }

                    Text(
                        text = "The first step is visible to all explorers. Each subsequent step unlocks when the previous one is collected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    state.steps.forEachIndexed { index, step ->
                        HuntStepEditor(
                            step = step,
                            stepNumber = index + 1,
                            totalSteps = state.steps.size,
                            isFirst = index == 0,
                            isLast = index == state.steps.lastIndex,
                            isBusinessUser = isBusinessUser,
                            businessName = businessName,
                            businessCategories = businessCategories,
                            enabled = !isSubmitting,
                            onUpdate = { updated -> onStateChange(state.updateStep(index, { updated })) },
                            onRemove = if (state.steps.size > 1) {
                                { onStateChange(state.removeStep(index)) }
                            } else null
                        )
                    }

                    if (error != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                    TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSubmit(state) },
                        enabled = !isSubmitting && state.title.isNotBlank() && state.steps.size >= 2
                    ) {
                        Text("Create Hunt (${state.steps.size} stops)")
                    }
                }
            }
        }
    }
}

@Composable
private fun HuntStepEditor(
    step: HuntStepDraft,
    stepNumber: Int,
    totalSteps: Int,
    isFirst: Boolean,
    isLast: Boolean,
    isBusinessUser: Boolean,
    businessName: String?,
    businessCategories: List<BusinessCategory>,
    enabled: Boolean,
    onUpdate: (HuntStepDraft) -> Unit,
    onRemove: (() -> Unit)?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        color = if (isLast) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
               else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isLast) Icons.Rounded.EmojiEvents else Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isLast && totalSteps > 1) "Step $stepNumber — Final prize" else "Step $stepNumber",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                if (onRemove != null) {
                    IconButton(onClick = onRemove, enabled = enabled, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Remove step", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Location — captured automatically from GPS when step was added
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = if (step.lat != 0.0 || step.lng != 0.0) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (step.lat != 0.0 || step.lng != 0.0)
                        "Location captured: %.5f, %.5f".format(step.lat, step.lng)
                    else
                        "Location not captured — GPS unavailable when step was added.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (step.lat != 0.0 || step.lng != 0.0) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.error
                )
            }

            OutlinedTextField(
                value = step.noteText,
                onValueChange = { onUpdate(step.copy(noteText = it)) },
                label = { Text(if (isLast && totalSteps > 1) "Prize / reward message *" else "Clue text *") },
                placeholder = {
                    Text(if (isLast && totalSteps > 1) "Congratulations! Here's your reward..." else "Head to the blue door on Main St...")
                },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                enabled = enabled
            )

            OutlinedTextField(
                value = step.description,
                onValueChange = { onUpdate(step.copy(description = it)) },
                label = { Text("Hint (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                enabled = enabled
            )

            if (isBusinessUser) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = step.redemptionLimit?.toString() ?: "",
                        onValueChange = { input -> onUpdate(step.copy(redemptionLimit = input.toIntOrNull()?.takeIf { it > 0 })) },
                        label = { Text("Limit") },
                        modifier = Modifier.width(88.dp),
                        singleLine = true,
                        enabled = enabled,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }
    }
}
