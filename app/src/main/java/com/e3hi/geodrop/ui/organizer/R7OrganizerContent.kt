package com.e3hi.geodrop.ui.organizer

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.e3hi.geodrop.BuildConfig
import com.e3hi.geodrop.data.R6DropKind
import com.e3hi.geodrop.data.R7DropContentKind
import com.e3hi.geodrop.data.R7DropDraft
import com.e3hi.geodrop.data.R7Experience
import com.e3hi.geodrop.data.R7ExperienceDraft
import com.e3hi.geodrop.data.R7ExperienceState
import com.e3hi.geodrop.data.R7ExpiryMode
import com.e3hi.geodrop.data.R7OrganizerAccessState
import com.e3hi.geodrop.data.R7OrganizerAccessStatus
import com.e3hi.geodrop.data.R7OrganizerDrop
import com.e3hi.geodrop.data.R7OrganizerDropSummary
import com.e3hi.geodrop.data.R7OrganizerException
import com.e3hi.geodrop.data.R7OrganizerGateway
import com.e3hi.geodrop.data.R7OrganizerPolicy
import com.e3hi.geodrop.ui.theme.GeoDropSize
import com.e3hi.geodrop.ui.theme.GeoDropSpacing
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.io.ByteArrayOutputStream
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class R7OrganizerScreen {
    EXPERIENCES,
    EXPERIENCE_FORM,
    EXPERIENCE_DETAIL,
    DROP_FORM,
    REWARD_CODES,
    RESULTS
}

@Composable
fun R7OrganizerAccessDialog(
    accessState: R7OrganizerAccessState,
    onDismiss: () -> Unit,
    onContinueToApplication: () -> Unit
) {
    val submitted = accessState.submittedAtMillis?.let(::formatDateTime)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (accessState.status) {
                    R7OrganizerAccessStatus.NOT_APPLIED -> "Organizer accounts"
                    R7OrganizerAccessStatus.PENDING -> "Your application is under review"
                    R7OrganizerAccessStatus.APPROVED -> "You're approved as an Organizer"
                    R7OrganizerAccessStatus.DENIED -> "Application not approved at this time"
                }
            )
        },
        text = {
            Text(
                when (accessState.status) {
                    R7OrganizerAccessStatus.NOT_APPLIED ->
                        "Organizer accounts let you build Experiences for an event, business, or organization. Every account is reviewed. Review typically takes a few business days, and we'll follow up by email."
                    R7OrganizerAccessStatus.PENDING ->
                        "${submitted?.let { "Submitted $it. " }.orEmpty()}We'll email you when a decision is made. There is no need to keep checking here."
                    R7OrganizerAccessStatus.APPROVED ->
                        "You can now create and manage Experiences."
                    R7OrganizerAccessStatus.DENIED ->
                        "Thanks for your interest. We're not able to approve this application right now. There is no automatic reapplication; contact the pilot team with questions."
                }
            )
        },
        confirmButton = {
            if (accessState.status == R7OrganizerAccessStatus.NOT_APPLIED) {
                Button(onClick = onContinueToApplication) {
                    Text("Continue to application")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        },
        dismissButton = if (accessState.status == R7OrganizerAccessStatus.NOT_APPLIED) {
            { TextButton(onClick = onDismiss) { Text("Not now") } }
        } else {
            null
        }
    )
}

@Composable
fun R7OrganizerContent(
    userId: String,
    gateway: R7OrganizerGateway,
    localDemo: Boolean,
    currentLocationProvider: suspend () -> Pair<Double, Double>?,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var screenName by rememberSaveable { mutableStateOf(R7OrganizerScreen.EXPERIENCES.name) }
    val screen = runCatching { R7OrganizerScreen.valueOf(screenName) }
        .getOrDefault(R7OrganizerScreen.EXPERIENCES)
    var experiences by remember { mutableStateOf<List<R7Experience>>(emptyList()) }
    var experiencesLoading by remember { mutableStateOf(true) }
    var experiencesError by remember { mutableStateOf<String?>(null) }
    var selectedExperience by remember { mutableStateOf<R7Experience?>(null) }
    var editingExperience by remember { mutableStateOf<R7Experience?>(null) }
    var drops by remember { mutableStateOf<List<R7OrganizerDropSummary>>(emptyList()) }
    var dropsLoading by remember { mutableStateOf(false) }
    var dropsError by remember { mutableStateOf<String?>(null) }
    var editingDrop by remember { mutableStateOf<R7OrganizerDrop?>(null) }
    var selectedRewardDrop by remember { mutableStateOf<R7OrganizerDropSummary?>(null) }
    var actionLoading by remember { mutableStateOf(false) }
    var refreshToken by remember { mutableIntStateOf(0) }

    fun refreshExperiences() {
        refreshToken += 1
    }

    LaunchedEffect(userId, refreshToken) {
        experiencesLoading = experiences.isEmpty()
        experiencesError = null
        runCatching { gateway.loadExperiences(userId) }
            .onSuccess { loaded ->
                experiences = loaded
                selectedExperience = selectedExperience?.let { selected ->
                    loaded.firstOrNull { it.code == selected.code } ?: selected
                }
            }
            .onFailure { experiencesError = it.r7Message() }
        experiencesLoading = false
    }

    LaunchedEffect(screen, selectedExperience?.code, refreshToken) {
        val experience = selectedExperience
        if (screen != R7OrganizerScreen.EXPERIENCE_DETAIL || experience == null) return@LaunchedEffect
        dropsLoading = drops.isEmpty()
        dropsError = null
        runCatching { gateway.loadDrops(userId, experience.code) }
            .onSuccess { drops = it }
            .onFailure { dropsError = it.r7Message() }
        dropsLoading = false
    }

    Box(modifier.fillMaxSize()) {
        when (screen) {
            R7OrganizerScreen.EXPERIENCES -> R7ExperiencesList(
                experiences = experiences,
                loading = experiencesLoading,
                error = experiencesError,
                localDemo = localDemo,
                onRetry = ::refreshExperiences,
                onCreate = {
                    editingExperience = null
                    screenName = R7OrganizerScreen.EXPERIENCE_FORM.name
                },
                onOpen = {
                    selectedExperience = it
                    drops = emptyList()
                    screenName = R7OrganizerScreen.EXPERIENCE_DETAIL.name
                }
            )

            R7OrganizerScreen.EXPERIENCE_FORM -> R7ExperienceForm(
                initial = editingExperience,
                submitting = actionLoading,
                onBack = { screenName = R7OrganizerScreen.EXPERIENCES.name },
                onSave = { draft ->
                    if (!actionLoading) scope.launch {
                        actionLoading = true
                        runCatching {
                            editingExperience?.let { gateway.updateExperience(it.code, draft) }
                                ?: gateway.createExperience(draft)
                        }.onSuccess { saved ->
                            selectedExperience = saved
                            editingExperience = null
                            drops = emptyList()
                            screenName = R7OrganizerScreen.EXPERIENCE_DETAIL.name
                            refreshExperiences()
                            snackbar.showSnackbar("Experience saved.")
                        }.onFailure { snackbar.showSnackbar(it.r7Message()) }
                        actionLoading = false
                    }
                }
            )

            R7OrganizerScreen.EXPERIENCE_DETAIL -> selectedExperience?.let { experience ->
                R7ExperienceDetail(
                    experience = experience,
                    drops = drops,
                    loading = dropsLoading,
                    error = dropsError,
                    onBack = { screenName = R7OrganizerScreen.EXPERIENCES.name },
                    onEditExperience = {
                        editingExperience = experience
                        screenName = R7OrganizerScreen.EXPERIENCE_FORM.name
                    },
                    onRetry = ::refreshExperiences,
                    onAddDrop = {
                        editingDrop = null
                        screenName = R7OrganizerScreen.DROP_FORM.name
                    },
                    onOpenResults = { screenName = R7OrganizerScreen.RESULTS.name },
                    onManageReward = { summary ->
                        selectedRewardDrop = summary
                        screenName = R7OrganizerScreen.REWARD_CODES.name
                    },
                    onEditDrop = { summary ->
                        if (!actionLoading) scope.launch {
                            actionLoading = true
                            runCatching { gateway.loadDrop(summary.id) }
                                .onSuccess {
                                    editingDrop = it
                                    screenName = R7OrganizerScreen.DROP_FORM.name
                                }
                                .onFailure { snackbar.showSnackbar(it.r7Message()) }
                            actionLoading = false
                        }
                    }
                )
            } ?: LaunchedEffect(Unit) { screenName = R7OrganizerScreen.EXPERIENCES.name }

            R7OrganizerScreen.DROP_FORM -> selectedExperience?.let { experience ->
                R7DropForm(
                    experience = experience,
                    initial = editingDrop,
                    submitting = actionLoading,
                    currentLocationProvider = currentLocationProvider,
                    onBack = { screenName = R7OrganizerScreen.EXPERIENCE_DETAIL.name },
                    onSave = { draft ->
                        if (!actionLoading) scope.launch {
                            actionLoading = true
                            runCatching { gateway.saveDrop(userId, draft) }
                                .onSuccess {
                                    editingDrop = null
                                    screenName = R7OrganizerScreen.EXPERIENCE_DETAIL.name
                                    refreshExperiences()
                                    snackbar.showSnackbar("Drop published.")
                                }
                                .onFailure { snackbar.showSnackbar(it.r7Message()) }
                            actionLoading = false
                        }
                    },
                    onDelete = editingDrop?.let { drop ->
                        {
                            if (!actionLoading) scope.launch {
                                actionLoading = true
                                runCatching { gateway.deleteDrop(drop.summary.id) }
                                    .onSuccess {
                                        editingDrop = null
                                        screenName = R7OrganizerScreen.EXPERIENCE_DETAIL.name
                                        refreshExperiences()
                                        snackbar.showSnackbar("Drop deleted. Past finds stay in guest collections.")
                                    }
                                    .onFailure { snackbar.showSnackbar(it.r7Message()) }
                                actionLoading = false
                            }
                        }
                    }
                )
            } ?: LaunchedEffect(Unit) { screenName = R7OrganizerScreen.EXPERIENCES.name }

            R7OrganizerScreen.REWARD_CODES -> selectedRewardDrop?.let { drop ->
                R8RewardOperationsContent(
                    drop = drop,
                    gateway = gateway,
                    localDemo = localDemo,
                    onBack = { screenName = R7OrganizerScreen.EXPERIENCE_DETAIL.name }
                )
            } ?: LaunchedEffect(Unit) { screenName = R7OrganizerScreen.EXPERIENCE_DETAIL.name }

            R7OrganizerScreen.RESULTS -> selectedExperience?.let { experience ->
                R8ResultsContent(
                    experience = experience,
                    drops = drops,
                    gateway = gateway,
                    localDemo = localDemo,
                    onBack = { screenName = R7OrganizerScreen.EXPERIENCE_DETAIL.name }
                )
            } ?: LaunchedEffect(Unit) { screenName = R7OrganizerScreen.EXPERIENCES.name }
        }

        if (actionLoading && screen == R7OrganizerScreen.EXPERIENCE_DETAIL) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(GeoDropSpacing.md)
        )
    }
}

@Composable
private fun R7ExperiencesList(
    experiences: List<R7Experience>,
    loading: Boolean,
    error: String?,
    localDemo: Boolean,
    onRetry: () -> Unit,
    onCreate: () -> Unit,
    onOpen: (R7Experience) -> Unit
) {
    when {
        loading -> Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        error != null && experiences.isEmpty() -> R7CenteredError(error, onRetry)
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(GeoDropSpacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
                    Text(
                        "Experiences",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                    if (localDemo) {
                        Card(colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )) {
                            Text(
                                "Local demo organizer — changes stay in this running debug app and do not sync to another device. Photo review is simulated locally.",
                                modifier = Modifier.padding(GeoDropSpacing.md),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Button(
                        onClick = onCreate,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(GeoDropSpacing.xs))
                        Text("Create an Experience")
                    }
                }
            }
            error?.let { message -> item { R7InlineError(message, onRetry) } }
            if (experiences.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )) {
                        Column(
                            Modifier.fillMaxWidth().padding(GeoDropSpacing.lg),
                            verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)
                        ) {
                            Text("No Experiences yet", style = MaterialTheme.typography.titleLarge)
                            Text("Create your first one to start adding drops.")
                        }
                    }
                }
            } else {
                items(experiences, key = { it.code }) { experience ->
                    Card(onClick = { onOpen(experience) }) {
                        Column(
                            Modifier.fillMaxWidth().padding(GeoDropSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs)
                        ) {
                            Text(experience.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                experienceStatus(experience),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("${experience.dropCount} published drops · Code ${experience.code}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun R7ExperienceForm(
    initial: R7Experience?,
    submitting: Boolean,
    onBack: () -> Unit,
    onSave: (R7ExperienceDraft) -> Unit
) {
    val now = remember { System.currentTimeMillis() }
    val defaultWindow = remember(now) { r7DefaultExperienceWindow(now) }
    var name by rememberSaveable(initial?.code) { mutableStateOf(initial?.name.orEmpty()) }
    var description by rememberSaveable(initial?.code) { mutableStateOf(initial?.description.orEmpty()) }
    var startsAt by rememberSaveable(initial?.code) {
        mutableStateOf(initial?.startsAtMillis ?: defaultWindow.first)
    }
    var endsAt by rememberSaveable(initial?.code) {
        mutableStateOf(initial?.endsAtMillis ?: defaultWindow.second)
    }
    var timeZone by rememberSaveable(initial?.code) {
        mutableStateOf(initial?.timeZone ?: TimeZone.getDefault().id)
    }
    var radius by rememberSaveable(initial?.code) {
        mutableIntStateOf(initial?.defaultRadiusM ?: R7OrganizerPolicy.DEFAULT_RADIUS_M)
    }
    var cancelled by rememberSaveable(initial?.code) {
        mutableStateOf(initial?.state == R7ExperienceState.CANCELLED)
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        R7InternalHeader(
            title = if (initial == null) "Create an Experience" else "Experience settings",
            onBack = onBack
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(GeoDropSpacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 100) name = it },
                    label = { Text("Experience name") },
                    placeholder = { Text("e.g. Hilo Garden Walk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 240) description = it },
                    label = { Text("Short description (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                R7DateTimeButton("Starts", startsAt, timeZoneId = timeZone) { startsAt = it }
            }
            item {
                R7DateTimeButton("Ends", endsAt, timeZoneId = timeZone) { endsAt = it }
            }
            item {
                OutlinedTextField(
                    value = timeZone,
                    onValueChange = { timeZone = it },
                    label = { Text("Time zone") },
                    supportingText = { Text("Defaults to this device; edit for the venue if needed.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                R7RadiusControl(
                    title = "Default unlock distance",
                    radius = radius,
                    onRadiusChanged = { radius = it }
                )
            }
            if (initial != null) {
                item {
                    FilterChip(
                        selected = cancelled,
                        onClick = { cancelled = !cancelled },
                        label = { Text(if (cancelled) "Experience cancelled" else "Experience published") }
                    )
                }
            }
            validationError?.let { message -> item { R7InlineError(message) } }
            item {
                Button(
                    onClick = {
                        val draft = R7ExperienceDraft(
                            name = name,
                            description = description,
                            startsAtMillis = startsAt,
                            endsAtMillis = endsAt,
                            timeZone = timeZone,
                            defaultRadiusM = radius,
                            state = if (cancelled) R7ExperienceState.CANCELLED else R7ExperienceState.PUBLISHED
                        )
                        validationError = R7OrganizerPolicy.validateExperience(draft)
                        if (validationError == null) onSave(draft)
                    },
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (submitting) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (initial == null) "Create Experience" else "Save settings")
                    }
                }
            }
        }
    }
}

@Composable
private fun R7ExperienceDetail(
    experience: R7Experience,
    drops: List<R7OrganizerDropSummary>,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onEditExperience: () -> Unit,
    onRetry: () -> Unit,
    onAddDrop: () -> Unit,
    onOpenResults: () -> Unit,
    onManageReward: (R7OrganizerDropSummary) -> Unit,
    onEditDrop: (R7OrganizerDropSummary) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxSize()) {
        R7InternalHeader(experience.name, onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(GeoDropSpacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )) {
                    Column(
                        Modifier.fillMaxWidth().padding(GeoDropSpacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)
                    ) {
                        Text("Event code", style = MaterialTheme.typography.labelLarge)
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(
                                experience.code,
                                style = MaterialTheme.typography.headlineLarge,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
                            OutlinedButton(onClick = {
                                clipboard.setText(AnnotatedString(experience.code))
                            }) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                                Spacer(Modifier.width(GeoDropSpacing.xs))
                                Text("Copy")
                            }
                            OutlinedButton(onClick = {
                                val body = if (BuildConfig.APP_LINK_CONFIGURED) {
                                    "Join ${experience.name}: https://${BuildConfig.APP_LINK_HOST}/e/${experience.code}\nCode: ${experience.code}"
                                } else {
                                    "Join ${experience.name} with event code ${experience.code}."
                                }
                                context.startActivity(Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, body)
                                    },
                                    "Share Experience code"
                                ))
                            }) {
                                Icon(Icons.Rounded.Share, contentDescription = null)
                                Spacer(Modifier.width(GeoDropSpacing.xs))
                                Text("Share")
                            }
                        }
                        if (!BuildConfig.APP_LINK_CONFIGURED) {
                            Text(
                                "QR sharing is postponed until the app-owned HTTPS host is configured. The event code remains available for device review.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)
                ) {
                    OutlinedButton(onClick = onEditExperience, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Edit, contentDescription = null)
                        Spacer(Modifier.width(GeoDropSpacing.xs))
                        Text("Settings")
                    }
                    Button(onClick = onAddDrop, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(GeoDropSpacing.xs))
                        Text("Add a drop")
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = onOpenResults,
                    modifier = Modifier.fillMaxWidth().height(GeoDropSize.minimumTouchTarget)
                ) {
                    Icon(Icons.Rounded.BarChart, contentDescription = null)
                    Spacer(Modifier.width(GeoDropSpacing.xs))
                    Text("View Results")
                }
            }
            if (System.currentTimeMillis() < experience.startsAtMillis) {
                item {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )) {
                        Text(
                            "Guests can join now. Published drops will appear when this Experience starts ${formatDateTime(experience.startsAtMillis, experience.timeZone)}.",
                            modifier = Modifier.fillMaxWidth().padding(GeoDropSpacing.md),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            item {
                Text("Published drops", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() })
            }
            when {
                loading -> item { Box(Modifier.fillMaxWidth().padding(GeoDropSpacing.xl)) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } }
                error != null && drops.isEmpty() -> item { R7InlineError(error, onRetry) }
                drops.isEmpty() -> item {
                    Text(
                        "No drops yet. Add the first drop while standing at its venue location, or place its pin on the map.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> items(drops, key = { it.id }) { drop ->
                    Card {
                        Column(Modifier.fillMaxWidth().padding(GeoDropSpacing.md)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
                            ) {
                                Icon(
                                    when {
                                        drop.dropKind == R6DropKind.REWARD -> Icons.Rounded.Redeem
                                        drop.contentKind == R7DropContentKind.PHOTO -> Icons.Rounded.Image
                                        else -> Icons.Rounded.LocationOn
                                    },
                                    contentDescription = null
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(drop.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        buildString {
                                            if (drop.dropKind == R6DropKind.REWARD) append("Reward · ")
                                            append(
                                                if (drop.moderationState == "PENDING") {
                                                    "${drop.radiusM} m · Pending review — not visible to guests"
                                                } else {
                                                    "${drop.radiusM} m · ${drop.moderationState.lowercase().replaceFirstChar { it.uppercase() }}"
                                                }
                                            )
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(GeoDropSpacing.sm))
                            Row(horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
                                OutlinedButton(
                                    onClick = { onEditDrop(drop) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = null)
                                    Spacer(Modifier.width(GeoDropSpacing.xs))
                                    Text("Edit")
                                }
                                if (drop.dropKind == R6DropKind.REWARD) {
                                    Button(
                                        onClick = { onManageReward(drop) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Redeem, contentDescription = null)
                                        Spacer(Modifier.width(GeoDropSpacing.xs))
                                        Text("Codes")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun R7DropForm(
    experience: R7Experience,
    initial: R7OrganizerDrop?,
    submitting: Boolean,
    currentLocationProvider: suspend () -> Pair<Double, Double>?,
    onBack: () -> Unit,
    onSave: (R7DropDraft) -> Unit,
    onDelete: (() -> Unit)?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initialLat = initial?.summary?.lat ?: 19.704
    val initialLng = initial?.summary?.lng ?: -155.0767777778
    var latitudeText by rememberSaveable(initial?.summary?.id) { mutableStateOf(initialLat.toString()) }
    var longitudeText by rememberSaveable(initial?.summary?.id) { mutableStateOf(initialLng.toString()) }
    var radius by rememberSaveable(initial?.summary?.id) {
        mutableIntStateOf(initial?.summary?.radiusM ?: experience.defaultRadiusM)
    }
    var contentKindName by rememberSaveable(initial?.summary?.id) {
        mutableStateOf((initial?.summary?.contentKind ?: R7DropContentKind.TEXT).name)
    }
    val contentKind = runCatching { R7DropContentKind.valueOf(contentKindName) }
        .getOrDefault(R7DropContentKind.TEXT)
    var dropKindName by rememberSaveable(initial?.summary?.id) {
        mutableStateOf((initial?.summary?.dropKind ?: R6DropKind.STANDARD).name)
    }
    val dropKind = runCatching { R6DropKind.valueOf(dropKindName) }
        .getOrDefault(R6DropKind.STANDARD)
    var title by rememberSaveable(initial?.summary?.id) { mutableStateOf(initial?.summary?.title.orEmpty()) }
    var body by rememberSaveable(initial?.summary?.id) { mutableStateOf(initial?.body.orEmpty()) }
    var altText by rememberSaveable(initial?.summary?.id) { mutableStateOf(initial?.mediaAltText.orEmpty()) }
    var rewardLabel by rememberSaveable(initial?.summary?.id) {
        mutableStateOf(initial?.rewardPresentation?.get("rewardLabel").orEmpty())
    }
    var businessLabel by rememberSaveable(initial?.summary?.id) {
        mutableStateOf(initial?.rewardPresentation?.get("businessLabel").orEmpty())
    }
    var rewardInstructions by rememberSaveable(initial?.summary?.id) {
        mutableStateOf(initial?.rewardPresentation?.get("instructions").orEmpty())
    }
    var rewardTerms by rememberSaveable(initial?.summary?.id) {
        mutableStateOf(initial?.rewardPresentation?.get("terms").orEmpty())
    }
    var inventoryText by rememberSaveable(initial?.summary?.id) {
        mutableStateOf(initial?.inventoryLimit?.toString() ?: "25")
    }
    var expiryModeName by rememberSaveable(initial?.summary?.id) {
        mutableStateOf((initial?.summary?.expiryMode ?: R7ExpiryMode.NONE).name)
    }
    val expiryMode = runCatching { R7ExpiryMode.valueOf(expiryModeName) }
        .getOrDefault(R7ExpiryMode.NONE)
    var expiresAt by rememberSaveable(initial?.summary?.id) {
        mutableStateOf(initial?.summary?.expiresAtMillis ?: experience.endsAtMillis)
    }
    var validationError by remember { mutableStateOf<String?>(null) }
    var locationLoading by remember { mutableStateOf(false) }
    var photoLoading by remember { mutableStateOf(false) }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var photoPreview by remember { mutableStateOf<Bitmap?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var placementMapTouchActive by remember { mutableStateOf(false) }

    fun acceptBitmap(bitmap: Bitmap) {
        scope.launch {
            photoLoading = true
            runCatching { withContext(Dispatchers.Default) { compressPhoto(bitmap) } }
                .onSuccess {
                    photoPreview = it.preview
                    photoBytes = it.bytes
                    contentKindName = R7DropContentKind.PHOTO.name
                }
                .onFailure { validationError = "That photo could not be prepared. Choose another image." }
            photoLoading = false
        }
    }

    val choosePhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            photoLoading = true
            runCatching {
                withContext(Dispatchers.IO) {
                    R7PhotoOrientation.decode(context.contentResolver, uri)
                }
            }.onSuccess(::acceptBitmap)
                .onFailure { validationError = "That photo could not be opened. Choose another image." }
            photoLoading = false
        }
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let(::acceptBitmap)
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) takePhoto.launch(null)
        else validationError = "Camera permission was not granted. Choose a photo instead."
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this drop?") },
            text = { Text("Guests who've already found it keep it in their Collection. This only removes it from the map going forward.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    Column(Modifier.fillMaxSize()) {
        R7InternalHeader(if (initial == null) "Add a drop" else "Edit drop", onBack)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(
                state = rememberScrollState(),
                enabled = !placementMapTouchActive
            )
                .padding(GeoDropSpacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
        ) {
            Text("Placement", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            val latitude = latitudeText.toDoubleOrNull()
            val longitude = longitudeText.toDoubleOrNull()
            if (latitude != null && longitude != null && latitude in -90.0..90.0 && longitude in -180.0..180.0) {
                R7PlacementMap(
                    latitude = latitude,
                    longitude = longitude,
                    onLocationChanged = { lat, lng ->
                        latitudeText = formatCoordinate(lat)
                        longitudeText = formatCoordinate(lng)
                    },
                    onTouchActiveChanged = { placementMapTouchActive = it }
                )
            }
            Button(
                onClick = {
                    scope.launch {
                        locationLoading = true
                        val location = currentLocationProvider()
                        if (location == null) {
                            validationError = "Location is unavailable. Place the pin on the map or enter coordinates instead."
                        } else {
                            latitudeText = formatCoordinate(location.first)
                            longitudeText = formatCoordinate(location.second)
                        }
                        locationLoading = false
                    }
                },
                enabled = !locationLoading,
                modifier = Modifier.fillMaxWidth().height(GeoDropSize.minimumTouchTarget)
            ) {
                Icon(Icons.Rounded.LocationOn, contentDescription = null)
                Spacer(Modifier.width(GeoDropSpacing.xs))
                Text(if (locationLoading) "Finding location…" else "Use my location")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
                OutlinedTextField(
                    value = latitudeText,
                    onValueChange = { latitudeText = it },
                    label = { Text("Latitude") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = longitudeText,
                    onValueChange = { longitudeText = it },
                    label = { Text("Longitude") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            R7RadiusControl(
                title = "How close someone needs to be",
                radius = radius,
                onRadiusChanged = { radius = it }
            )
            Text(
                "Most venues work well around 25 m. Drops under 30 m apart can overlap.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            Text("Drop type", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            Row(horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
                FilterChip(
                    selected = dropKind == R6DropKind.STANDARD,
                    enabled = initial == null,
                    onClick = { dropKindName = R6DropKind.STANDARD.name },
                    label = { Text("Standard") }
                )
                FilterChip(
                    selected = dropKind == R6DropKind.REWARD,
                    enabled = initial == null,
                    onClick = { dropKindName = R6DropKind.REWARD.name },
                    label = { Text("Reward") }
                )
            }
            Text(
                if (initial == null) {
                    "Reward drops assign one unique pre-generated code when a guest unlocks them."
                } else {
                    "Drop type is locked after publishing so issued reward history stays intact."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            Text("Content", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            Row(horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
                FilterChip(
                    selected = contentKind == R7DropContentKind.TEXT,
                    onClick = { contentKindName = R7DropContentKind.TEXT.name },
                    label = { Text("Text") }
                )
                FilterChip(
                    selected = contentKind == R7DropContentKind.PHOTO,
                    onClick = { contentKindName = R7DropContentKind.PHOTO.name },
                    label = { Text("Photo") }
                )
            }
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 80) title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = body,
                onValueChange = { if (it.length <= 2_000) body = it },
                label = { Text(if (contentKind == R7DropContentKind.TEXT) "Message" else "Caption (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            if (contentKind == R7DropContentKind.PHOTO) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                takePhoto.launch(null)
                            } else {
                                cameraPermission.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(GeoDropSpacing.xs))
                        Text("Take photo")
                    }
                    OutlinedButton(
                        onClick = { choosePhoto.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null)
                        Spacer(Modifier.width(GeoDropSpacing.xs))
                        Text("Choose photo")
                    }
                }
                when {
                    photoLoading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                    photoPreview != null -> {
                        val preview = photoPreview!!
                        Image(
                            bitmap = preview.asImageBitmap(),
                            contentDescription = altText.ifBlank { "Selected photo preview" },
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().height(
                                if (preview.height > preview.width) 320.dp else 180.dp
                            )
                        )
                    }
                    initial?.summary?.contentKind == R7DropContentKind.PHOTO -> Text(
                        "Choose a replacement photo to save changes. Existing published media remains private and is not loaded into the editor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = altText,
                    onValueChange = { if (it.length <= 240) altText = it },
                    label = { Text("Photo description for screen readers") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "One photo per drop. Images are compressed automatically before upload.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (dropKind == R6DropKind.REWARD) {
                HorizontalDivider()
                Text("Reward setup", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() })
                OutlinedTextField(
                    value = rewardLabel,
                    onValueChange = { if (it.length <= 240) rewardLabel = it },
                    label = { Text("Reward name") },
                    placeholder = { Text("Free small coffee") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = businessLabel,
                    onValueChange = { if (it.length <= 240) businessLabel = it },
                    label = { Text("Business or redemption location") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rewardInstructions,
                    onValueChange = { if (it.length <= 240) rewardInstructions = it },
                    label = { Text("How to use it") },
                    placeholder = { Text("Show the code at the counter before ordering.") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rewardTerms,
                    onValueChange = { if (it.length <= 500) rewardTerms = it },
                    label = { Text("Terms (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = inventoryText,
                    onValueChange = { value ->
                        if (initial == null && value.length <= 5 && value.all(Char::isDigit)) {
                            inventoryText = value
                        }
                    },
                    enabled = initial == null,
                    label = { Text("Code inventory") },
                    supportingText = {
                        Text(
                            if (initial == null) "1–10,000 unique codes. Inventory locks after publishing."
                            else "Inventory is locked after publishing."
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )) {
                    Text(
                        "For Pilot 1, the pilot operator loads the approved pre-generated code list. Organizers validate displayed codes manually; no merchant account is created.",
                        modifier = Modifier.fillMaxWidth().padding(GeoDropSpacing.md),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            HorizontalDivider()
            Text("Expiration", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            R7ExpiryOption("No expiration", R7ExpiryMode.NONE, expiryMode) { expiryModeName = it.name }
            R7ExpiryOption("Use the Experience end time", R7ExpiryMode.EXPERIENCE_END, expiryMode) { expiryModeName = it.name }
            R7ExpiryOption("Custom date and time", R7ExpiryMode.CUSTOM, expiryMode) { expiryModeName = it.name }
            if (expiryMode == R7ExpiryMode.CUSTOM) {
                R7DateTimeButton(
                    label = "Expires",
                    valueMillis = expiresAt,
                    timeZoneId = experience.timeZone,
                    onValueChanged = { expiresAt = it }
                )
            }
            Text(
                "Guests who already found this keep it in their Collection after it expires or is deleted.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            validationError?.let { R7InlineError(it) }
            Button(
                onClick = {
                    val draft = R7DropDraft(
                        experienceCode = experience.code,
                        dropId = initial?.summary?.id,
                        lat = latitudeText.toDoubleOrNull() ?: Double.NaN,
                        lng = longitudeText.toDoubleOrNull() ?: Double.NaN,
                        radiusM = radius,
                        expiryMode = expiryMode,
                        expiresAtMillis = if (expiryMode == R7ExpiryMode.CUSTOM) expiresAt else null,
                        contentKind = contentKind,
                        dropKind = dropKind,
                        title = title,
                        body = body,
                        mediaAltText = if (contentKind == R7DropContentKind.PHOTO) altText else null,
                        rewardLabel = if (dropKind == R6DropKind.REWARD) rewardLabel else null,
                        businessLabel = if (dropKind == R6DropKind.REWARD) businessLabel else null,
                        rewardInstructions = if (dropKind == R6DropKind.REWARD) rewardInstructions else null,
                        rewardTerms = if (dropKind == R6DropKind.REWARD) rewardTerms else null,
                        inventoryLimit = if (dropKind == R6DropKind.REWARD) {
                            inventoryText.toIntOrNull()
                        } else null,
                        photoBytes = if (contentKind == R7DropContentKind.PHOTO) photoBytes else null,
                        photoMimeType = if (contentKind == R7DropContentKind.PHOTO) "image/jpeg" else null
                    )
                    validationError = R7OrganizerPolicy.validateDrop(draft)
                    if (validationError == null) onSave(draft)
                },
                enabled = !submitting && !photoLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (submitting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(if (initial == null) "Publish drop" else "Save changes")
            }
            if (onDelete != null) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth().height(GeoDropSize.minimumTouchTarget)
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null)
                    Spacer(Modifier.width(GeoDropSpacing.xs))
                    Text("Delete drop")
                }
            }
            Spacer(Modifier.height(GeoDropSpacing.xl))
        }
    }
}

@Composable
private fun R7PlacementMap(
    latitude: Double,
    longitude: Double,
    onLocationChanged: (Double, Double) -> Unit,
    onTouchActiveChanged: (Boolean) -> Unit
) {
    val position = LatLng(latitude, longitude)
    val markerState = remember { MarkerState(position) }
    val camera = rememberCameraPositionState {
        this.position = CameraPosition.fromLatLngZoom(position, 17f)
    }
    LaunchedEffect(position) {
        markerState.position = position
        camera.animate(CameraUpdateFactory.newLatLng(position))
    }
    LaunchedEffect(markerState) {
        snapshotFlow { markerState.position }
            .drop(1)
            .collect { onLocationChanged(it.latitude, it.longitude) }
    }
    GoogleMap(
        modifier = Modifier.fillMaxWidth().height(240.dp)
            .semantics {
                contentDescription =
                    "Drop placement map. Drag with one finger to move the map. Tap to move the pin."
            }
            .reportMapTouchActivity(onTouchActiveChanged),
        cameraPositionState = camera,
        uiSettings = remember { MapUiSettings(scrollGesturesEnabled = true) },
        onMapClick = { onLocationChanged(it.latitude, it.longitude) }
    ) {
        Marker(
            state = markerState,
            title = "Drop location",
            draggable = true
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.reportMapTouchActivity(
    onTouchActiveChanged: (Boolean) -> Unit
): Modifier = motionEventSpy { event ->
    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> onTouchActiveChanged(true)
        MotionEvent.ACTION_UP,
        MotionEvent.ACTION_CANCEL -> onTouchActiveChanged(false)
    }
}

@Composable
private fun R7RadiusControl(
    title: String,
    radius: Int,
    onRadiusChanged: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onRadiusChanged((radius - 5).coerceAtLeast(R7OrganizerPolicy.MIN_RADIUS_M)) },
                enabled = radius > R7OrganizerPolicy.MIN_RADIUS_M,
                modifier = Modifier.semantics { contentDescription = "Decrease distance" }
            ) { Icon(Icons.Rounded.Remove, contentDescription = null) }
            Text(
                "$radius meters",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { contentDescription = "$radius meters" }
            )
            IconButton(
                onClick = { onRadiusChanged((radius + 5).coerceAtMost(R7OrganizerPolicy.MAX_RADIUS_M)) },
                enabled = radius < R7OrganizerPolicy.MAX_RADIUS_M,
                modifier = Modifier.semantics { contentDescription = "Increase distance" }
            ) { Icon(Icons.Rounded.Add, contentDescription = null) }
        }
    }
}

@Composable
private fun R7ExpiryOption(
    label: String,
    option: R7ExpiryMode,
    selected: R7ExpiryMode,
    onSelected: (R7ExpiryMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .selectable(
                selected = option == selected,
                onClick = { onSelected(option) },
                role = Role.RadioButton
            )
            .padding(vertical = GeoDropSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = option == selected, onClick = null)
        Spacer(Modifier.width(GeoDropSpacing.sm))
        Text(label)
    }
}

@Composable
private fun R7DateTimeButton(
    label: String,
    valueMillis: Long,
    timeZoneId: String? = null,
    onValueChanged: (Long) -> Unit
) {
    val context = LocalContext.current
    val timeZone = timeZoneId?.let(::resolvedTimeZone) ?: TimeZone.getDefault()
    OutlinedButton(
        onClick = {
            val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = valueMillis }
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    val selected = Calendar.getInstance(timeZone).apply {
                        timeInMillis = valueMillis
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                    }
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            selected.set(Calendar.HOUR_OF_DAY, hour)
                            selected.set(Calendar.MINUTE, minute)
                            selected.set(Calendar.SECOND, 0)
                            selected.set(Calendar.MILLISECOND, 0)
                            onValueChanged(selected.timeInMillis)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        },
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
        Spacer(Modifier.width(GeoDropSpacing.sm))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(formatDateTime(valueMillis, timeZone.id))
        }
    }
}

@Composable
internal fun R7InternalHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = GeoDropSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Text(title, style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).semantics { heading() })
    }
    HorizontalDivider()
}

@Composable
internal fun R7CenteredError(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.align(Alignment.Center).padding(GeoDropSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
        ) {
            Text(message)
            Button(onClick = onRetry) { Text("Try again") }
        }
    }
}

@Composable
internal fun R7InlineError(message: String, onRetry: (() -> Unit)? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(
            Modifier.fillMaxWidth().padding(GeoDropSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)
        ) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            onRetry?.let { TextButton(onClick = it) { Text("Try again") } }
        }
    }
}

private data class R7CompressedPhoto(val preview: Bitmap, val bytes: ByteArray)

internal fun r7DefaultExperienceWindow(nowMillis: Long): Pair<Long, Long> =
    nowMillis to (nowMillis + 4 * 60 * 60 * 1_000L)

private fun compressPhoto(source: Bitmap): R7CompressedPhoto {
    val maxSide = 1_600
    val scale = minOf(1f, maxSide.toFloat() / maxOf(source.width, source.height).toFloat())
    val preview = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true
        )
    } else {
        source
    }
    var quality = 84
    var bytes: ByteArray
    do {
        bytes = ByteArrayOutputStream().use { output ->
            preview.compress(Bitmap.CompressFormat.JPEG, quality, output)
            output.toByteArray()
        }
        quality -= 12
    } while (bytes.size > 8 * 1024 * 1024 && quality >= 48)
    return R7CompressedPhoto(preview, bytes)
}

private fun Throwable.r7Message(): String = when (this) {
    is R7OrganizerException -> userMessage
    else -> localizedMessage?.takeIf(String::isNotBlank) ?: "Couldn't complete that action. Try again."
}

private fun formatDateTime(millis: Long, timeZoneId: String? = null): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).apply {
        timeZone = timeZoneId?.let(::resolvedTimeZone) ?: TimeZone.getDefault()
    }.format(Date(millis))

private fun resolvedTimeZone(id: String): TimeZone = runCatching {
    java.time.ZoneId.of(id)
    TimeZone.getTimeZone(id)
}.getOrElse { TimeZone.getDefault() }

private fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)

private fun experienceStatus(experience: R7Experience): String {
    if (experience.state == R7ExperienceState.CANCELLED) return "Cancelled"
    val now = System.currentTimeMillis()
    return when {
        now < experience.startsAtMillis ->
            "Starts ${formatDateTime(experience.startsAtMillis, experience.timeZone)}"
        now >= experience.endsAtMillis ->
            "Ended ${formatDateTime(experience.endsAtMillis, experience.timeZone)}"
        else -> "Active until ${formatDateTime(experience.endsAtMillis, experience.timeZone)}"
    }
}
