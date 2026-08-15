package com.kitheapp.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kitheapp.R
import com.kitheapp.data.R7OrganizerAccessState
import com.kitheapp.data.R7OrganizerAccessStatus
import com.kitheapp.data.R9AccountPolicy
import com.kitheapp.data.R9ExperienceAvailability
import com.kitheapp.data.R9JoinedExperience
import com.kitheapp.data.R9ReportStatus
import com.kitheapp.ui.components.EmptyState
import com.kitheapp.ui.theme.GeoDropSize
import com.kitheapp.ui.theme.GeoDropSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class ParticipantDestination {
    NEARBY,
    COLLECTION,
    ACCOUNT
}

data class ExperienceNavigationItem(
    val code: String,
    val isOwned: Boolean
)

object R4NavigationPolicy {
    fun resolveDestination(raw: String?): ParticipantDestination =
        ParticipantDestination.entries.firstOrNull { it.name == raw }
            ?: ParticipantDestination.NEARBY

    fun resolveActiveExperience(currentCode: String?, availableCodes: List<String>): String? {
        if (availableCodes.isEmpty()) return null
        return currentCode?.takeIf { it in availableCodes } ?: availableCodes.first()
    }

    fun stateKey(destination: ParticipantDestination, activeExperienceCode: String?): String =
        when (destination) {
            ParticipantDestination.NEARBY -> "nearby:${activeExperienceCode ?: "none"}"
            ParticipantDestination.COLLECTION -> "collection"
            ParticipantDestination.ACCOUNT -> "account"
        }
}

/**
 * Keeps each participant tab's saveable UI state, with a separate Nearby state per Experience.
 */
@Composable
fun GeoDropParticipantStateHost(
    destination: ParticipantDestination,
    activeExperienceCode: String?,
    content: @Composable () -> Unit
) {
    val stateHolder = rememberSaveableStateHolder()
    stateHolder.SaveableStateProvider(
        key = R4NavigationPolicy.stateKey(destination, activeExperienceCode),
        content = content
    )
}

@Composable
fun GeoDropParticipantNavigationBar(
    selected: ParticipantDestination,
    onSelect: (ParticipantDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val labelStyle = if (LocalDensity.current.fontScale >= 1.5f) {
        MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.sp
        )
    } else {
        MaterialTheme.typography.labelSmall
    }
    val items = listOf(
        NavigationItem(
            destination = ParticipantDestination.NEARBY,
            label = stringResource(R.string.r4_tab_nearby),
            icon = Icons.Default.Map
        ),
        NavigationItem(
            destination = ParticipantDestination.COLLECTION,
            label = stringResource(R.string.r4_tab_collection),
            icon = Icons.Default.Bookmarks
        ),
        NavigationItem(
            destination = ParticipantDestination.ACCOUNT,
            label = stringResource(R.string.r4_tab_account),
            icon = Icons.Default.AccountCircle
        )
    )
    NavigationBar(modifier = modifier, containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { item ->
            NavigationBarItem(
                selected = selected == item.destination,
                onClick = { onSelect(item.destination) },
                icon = { Icon(item.icon, contentDescription = null) },
                label = {
                    Text(
                        item.label,
                        style = labelStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}

private data class NavigationItem(
    val destination: ParticipantDestination,
    val label: String,
    val icon: ImageVector
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun GeoDropExperienceTopBar(
    experiences: List<ExperienceNavigationItem>,
    activeCode: String?,
    onSelectExperience: (String) -> Unit,
    onJoinExperience: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val active = experiences.firstOrNull { it.code == activeCode }
    TopAppBar(
        modifier = modifier,
        title = {
            TextButton(
                onClick = { if (experiences.isNotEmpty()) expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = GeoDropSize.minimumTouchTarget)
            ) {
                Text(
                    active?.let { stringResource(R.string.r4_experience_name, it.code) }
                        ?: stringResource(R.string.r4_no_active_experience),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (experiences.isNotEmpty()) {
                    Spacer(Modifier.width(GeoDropSpacing.xs))
                    Icon(Icons.Default.ExpandMore, contentDescription = stringResource(R.string.r4_switch_experience))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    experiences.forEach { experience ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(stringResource(R.string.r4_experience_name, experience.code))
                                    Text(
                                        stringResource(
                                            if (experience.isOwned) R.string.r4_experience_hosted
                                            else R.string.r4_experience_joined
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                expanded = false
                                onSelectExperience(experience.code)
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.r4_join_another_experience)) },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            expanded = false
                            onJoinExperience()
                        }
                    )
                }
            }
        },
        actions = {
            if (experiences.isEmpty()) {
                TextButton(onClick = onJoinExperience) {
                    Text(stringResource(R.string.r4_join_experience))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun GeoDropNoExperienceState(
    onJoinExperience: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = stringResource(R.string.r4_no_experience_title),
        message = stringResource(R.string.r4_no_experience_body),
        actionLabel = stringResource(R.string.r4_join_experience),
        onAction = onJoinExperience,
        modifier = modifier
    )
}

/** Transitional R4 code-entry surface. R5 replaces this with the complete QR/code flow. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun GeoDropJoinExperienceDialog(
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    val normalizedCode = code.trim().uppercase().takeIf { it.isNotBlank() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.r4_join_experience_title)) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.r4_join_back)
                                )
                            }
                        }
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding)
                        .padding(GeoDropSpacing.screenGutter),
                    verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
                ) {
                    Text(
                        stringResource(R.string.r4_experience_code_help),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text(stringResource(R.string.r4_experience_code)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { normalizedCode?.let(onJoin) },
                        enabled = normalizedCode != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = GeoDropSize.minimumTouchTarget)
                    ) {
                        Text(stringResource(R.string.r4_join))
                    }
                }
            }
        }
    }
}

@Composable
fun GeoDropAccountDestination(
    identityLabel: String,
    identitySupportingText: String?,
    isGuest: Boolean,
    locationGranted: Boolean,
    notificationsGranted: Boolean,
    joinedExperiences: List<ExperienceNavigationItem>,
    experienceHistory: List<R9JoinedExperience>,
    reportStatuses: List<R9ReportStatus>,
    blockedHostCount: Int,
    accountDetailsLoading: Boolean,
    accountDetailsError: String?,
    organizerAccessState: R7OrganizerAccessState,
    signingOut: Boolean,
    onSignIn: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenOrganizerAccess: () -> Unit,
    onOpenOrganizerTools: () -> Unit,
    onOpenBlockedCreators: () -> Unit,
    onOpenData: () -> Unit,
    onRetryAccountDetails: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val organizerApproved = organizerAccessState.status == R7OrganizerAccessStatus.APPROVED
    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = GeoDropSpacing.screenGutter,
            top = GeoDropSpacing.lg,
            end = GeoDropSpacing.screenGutter,
            bottom = GeoDropSpacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.lg)
    ) {
        item {
            AccountSection(title = stringResource(R.string.r4_account_identity)) {
                AccountIdentityCard(
                    identityLabel = identityLabel,
                    supportingText = identitySupportingText,
                    isGuest = isGuest,
                    onSignIn = onSignIn,
                    onEditProfile = onEditProfile
                )
            }
        }
        item {
            AccountSection(title = stringResource(R.string.r4_account_permissions)) {
                PermissionStatusRow(
                    icon = Icons.Default.LocationOn,
                    title = stringResource(R.string.r4_permission_location),
                    granted = locationGranted,
                    onClick = onOpenLocationSettings
                )
                PermissionStatusRow(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.r4_permission_notifications),
                    granted = notificationsGranted,
                    onClick = onOpenNotificationSettings
                )
            }
        }
        item {
            AccountSection(title = stringResource(R.string.r4_account_organizer)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(GeoDropSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(
                                        if (organizerApproved) R.string.r4_organizer_tools
                                        else R.string.r4_organizer_access
                                    ),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    when (organizerAccessState.status) {
                                        R7OrganizerAccessStatus.APPROVED ->
                                            stringResource(R.string.r4_organizer_approved_body)
                                        R7OrganizerAccessStatus.PENDING ->
                                            "Application under review. We'll follow up by email."
                                        R7OrganizerAccessStatus.DENIED ->
                                            "This application was not approved at this time."
                                        R7OrganizerAccessStatus.NOT_APPLIED -> if (isGuest) {
                                            "Sign in to request Organizer access."
                                        } else {
                                            stringResource(R.string.r4_organizer_unapproved_body)
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (organizerApproved) {
                            Button(
                                onClick = onOpenOrganizerTools,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = GeoDropSize.minimumTouchTarget)
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                                Spacer(Modifier.width(GeoDropSpacing.xs))
                                Text(stringResource(R.string.r4_open_organizer_tools))
                            }
                        } else if (!isGuest) {
                            OutlinedButton(
                                onClick = onOpenOrganizerAccess,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = GeoDropSize.minimumTouchTarget)
                            ) {
                                Text(
                                    when (organizerAccessState.status) {
                                        R7OrganizerAccessStatus.NOT_APPLIED -> "Request Organizer Access"
                                        R7OrganizerAccessStatus.PENDING -> "View application status"
                                        R7OrganizerAccessStatus.DENIED -> "View decision"
                                        R7OrganizerAccessStatus.APPROVED -> "Open Organizer tools"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            AccountSection(title = stringResource(R.string.r4_joined_experiences)) {
                if (accountDetailsLoading && experienceHistory.isEmpty()) {
                    Row(
                        modifier = Modifier.padding(vertical = GeoDropSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Loading Experience history…")
                    }
                } else if (experienceHistory.isNotEmpty()) {
                    experienceHistory.forEach { experience ->
                        ExperienceHistoryRow(experience)
                    }
                } else if (joinedExperiences.isEmpty()) {
                    Text(
                        stringResource(R.string.r4_joined_experiences_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    joinedExperiences.forEach { experience ->
                        ListItem(
                            headlineContent = {
                                Text(stringResource(R.string.r4_experience_name, experience.code))
                            },
                            supportingContent = {
                                Text(
                                    stringResource(
                                        if (experience.isOwned) R.string.r4_experience_hosted
                                        else R.string.r4_experience_joined
                                    )
                                )
                            },
                            leadingContent = { Icon(Icons.Default.History, contentDescription = null) }
                        )
                    }
                }
            }
        }
        if (!isGuest) {
            item {
                AccountSection(title = "Safety") {
                    if (reportStatuses.isEmpty()) {
                        Text(
                            "Reports you submit will appear here with their review status.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        reportStatuses.take(3).forEach { report ->
                            ListItem(
                                headlineContent = {
                                    Text(R9AccountPolicy.reportStatusLabel(report.state))
                                },
                                supportingContent = {
                                    Text(
                                        report.updatedAtMillis?.let(::formatAccountDate)
                                            ?: "Status updated"
                                    )
                                },
                                leadingContent = {
                                    Icon(Icons.Default.Report, contentDescription = null)
                                }
                            )
                        }
                    }
                    AccountActionRow(
                        icon = Icons.Default.Block,
                        label = if (blockedHostCount == 0) {
                            stringResource(R.string.r4_blocked_creators)
                        } else {
                            "Blocked hosts ($blockedHostCount)"
                        },
                        onClick = onOpenBlockedCreators
                    )
                }
            }
            item {
                AccountSection(title = stringResource(R.string.r4_account_privacy)) {
                    AccountActionRow(
                        icon = Icons.Default.Lock,
                        label = stringResource(R.string.r4_your_data),
                        onClick = onOpenData
                    )
                }
            }
            accountDetailsError?.let { message ->
                item {
                    Card(
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Assertive
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(GeoDropSpacing.md),
                            horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                message,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            TextButton(onClick = onRetryAccountDetails) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(GeoDropSpacing.xs))
                                Text("Retry")
                            }
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = onSignOut,
                    enabled = !signingOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = GeoDropSize.minimumTouchTarget)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(Modifier.width(GeoDropSpacing.xs))
                    Text(
                        stringResource(
                            if (signingOut) R.string.status_signing_out else R.string.menu_sign_out
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ExperienceHistoryRow(experience: R9JoinedExperience) {
    ListItem(
        headlineContent = { Text(experience.name) },
        supportingContent = {
            Column {
                Text("${experience.hostLabel} · ${formatExperienceDates(experience)}")
                Text(
                    "Code ${experience.code}${if (experience.isOwned) " · Hosted by you" else ""}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
        trailingContent = {
            Text(
                when (experience.availability) {
                    R9ExperienceAvailability.ACTIVE -> "Active"
                    R9ExperienceAvailability.UPCOMING -> "Upcoming"
                    R9ExperienceAvailability.ENDED -> "Ended"
                    R9ExperienceAvailability.CANCELLED -> "Cancelled"
                },
                style = MaterialTheme.typography.labelLarge,
                color = when (experience.availability) {
                    R9ExperienceAvailability.ACTIVE -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    )
}

private val accountDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

private fun formatExperienceDates(experience: R9JoinedExperience): String {
    val zone = runCatching { ZoneId.of(experience.timeZone) }.getOrDefault(ZoneId.systemDefault())
    val start = Instant.ofEpochMilli(experience.startsAtMillis).atZone(zone).toLocalDate()
    val end = Instant.ofEpochMilli(experience.endsAtMillis).atZone(zone).toLocalDate()
    val startLabel = start.format(accountDateFormatter)
    return if (start == end) startLabel else "$startLabel – ${end.format(accountDateFormatter)}"
}

private fun formatAccountDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(accountDateFormatter)

@Composable
private fun AccountSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() }
        )
        content()
    }
}

@Composable
private fun AccountIdentityCard(
    identityLabel: String,
    supportingText: String?,
    isGuest: Boolean,
    onSignIn: () -> Unit,
    onEditProfile: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(GeoDropSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(32.dp))
                Column(Modifier.weight(1f)) {
                    Text(identityLabel, style = MaterialTheme.typography.titleLarge)
                    supportingText?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Button(
                onClick = if (isGuest) onSignIn else onEditProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = GeoDropSize.minimumTouchTarget)
            ) {
                Icon(if (isGuest) Icons.Default.Login else Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(GeoDropSpacing.xs))
                Text(
                    stringResource(
                        if (isGuest) R.string.r4_sign_in_action else R.string.r4_edit_profile
                    )
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    icon: ImageVector,
    title: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = GeoDropSize.minimumTouchTarget)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick),
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                stringResource(
                    if (granted) R.string.r4_permission_granted
                    else R.string.r4_permission_not_granted
                )
            )
        },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.Default.Settings, contentDescription = null) }
    )
}

@Composable
private fun AccountActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = GeoDropSize.minimumTouchTarget)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick),
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
    )
}
