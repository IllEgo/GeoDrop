package com.kitheapp.ui.participant

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kitheapp.data.R6CollectionReceipt
import com.kitheapp.data.R6DiscoveryState
import com.kitheapp.data.R6DropDiscovery
import com.kitheapp.data.R6ParticipantException
import com.kitheapp.data.R6ParticipantPolicy
import com.kitheapp.data.R6TrailProgress
import com.kitheapp.data.R6UnlockResult
import com.kitheapp.ui.components.DropCard
import com.kitheapp.ui.components.DropVisualState
import com.kitheapp.ui.components.ResultSheet
import com.kitheapp.ui.components.ResultSheetState
import com.kitheapp.ui.components.TrailStrip
import com.kitheapp.ui.components.UnlockButton
import com.kitheapp.ui.components.UnlockButtonState
import com.kitheapp.ui.theme.GeoDropSize
import com.kitheapp.ui.theme.GeoDropSpacing
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.DateFormat
import java.util.Date

enum class R6BrowseMode { MAP, LIST }

data class R6DiscoveryPresentation(
    val drop: R6DropDiscovery,
    val state: R6DiscoveryState,
    val distanceLabel: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun R6NearbyContent(
    loading: Boolean,
    refreshing: Boolean,
    error: String?,
    items: List<R6DiscoveryPresentation>,
    selectedDropId: String?,
    unlockingDropId: String?,
    trailProgress: R6TrailProgress?,
    currentLocation: LatLng?,
    approximateLocationEnabled: Boolean,
    networkAvailable: Boolean = true,
    topPadding: Dp,
    unlockResult: R6UnlockResult?,
    unlockError: R6ParticipantException?,
    onSelect: (R6DropDiscovery?) -> Unit,
    onUnlock: (R6DropDiscovery) -> Unit,
    onRequestLocation: () -> Unit,
    onRefresh: () -> Unit,
    onDismissUnlockResult: () -> Unit,
    onReport: (R6DropDiscovery, String, String?) -> Unit,
    onBlockHost: (R6DropDiscovery) -> Unit,
    mapsAvailable: Boolean = true,
    initialBrowseMode: R6BrowseMode = R6BrowseMode.MAP,
    modifier: Modifier = Modifier
) {
    var browseModeName by rememberSaveable { mutableStateOf(initialBrowseMode.name) }
    val requestedBrowseMode = runCatching { R6BrowseMode.valueOf(browseModeName) }
        .getOrDefault(R6BrowseMode.MAP)
    val browseMode = if (!mapsAvailable && requestedBrowseMode == R6BrowseMode.MAP) {
        R6BrowseMode.LIST
    } else {
        requestedBrowseMode
    }
    val selected = items.firstOrNull { it.drop.id == selectedDropId }
    var reportTarget by remember { mutableStateOf<R6DropDiscovery?>(null) }
    var blockTarget by remember { mutableStateOf<R6DropDiscovery?>(null) }
    val discoveryListState = rememberLazyListState()

    LaunchedEffect(networkAvailable, browseMode) {
        if (!networkAvailable && browseMode == R6BrowseMode.LIST) {
            discoveryListState.scrollToItem(0)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            error != null && items.isEmpty() -> R6ErrorState(error, onRefresh, Modifier.align(Alignment.Center))
            items.isEmpty() -> R6EmptyState(Modifier.align(Alignment.Center))
            else -> Column(Modifier.fillMaxSize().padding(top = topPadding)) {
                if (trailProgress != null) {
                    val next = items.firstOrNull {
                        it.drop.trailId == trailProgress.trailId &&
                            it.drop.trailStepIndex == trailProgress.currentStepIndex
                    }
                    TrailStrip(
                        title = "Main Trail",
                        currentStep = trailProgress.currentStepIndex,
                        totalSteps = items.firstNotNullOfOrNull { candidate ->
                            candidate.drop.takeIf { it.trailId == trailProgress.trailId }
                                ?.trailTotalSteps
                        } ?: trailProgress.currentStepIndex.coerceAtLeast(1),
                        nextLabel = next?.drop?.participantLabel(),
                        modifier = Modifier.padding(horizontal = GeoDropSpacing.screenGutter)
                    )
                    Spacer(Modifier.height(GeoDropSpacing.sm))
                }

                R6BrowseModeSelector(
                    mode = browseMode,
                    onChange = { browseModeName = it.name },
                    mapsAvailable = mapsAvailable,
                    refreshing = refreshing,
                    onRefresh = onRefresh
                )

                if (!approximateLocationEnabled && browseMode == R6BrowseMode.MAP) {
                    R6LocationUnavailableNotice(
                        onRequestLocation = onRequestLocation,
                        modifier = Modifier.padding(
                            horizontal = GeoDropSpacing.screenGutter,
                            vertical = GeoDropSpacing.sm
                        )
                    )
                }

                if (!networkAvailable && browseMode == R6BrowseMode.MAP) {
                    R6OfflineNotice(
                        modifier = Modifier.padding(
                            horizontal = GeoDropSpacing.screenGutter,
                            vertical = GeoDropSpacing.sm
                        )
                    )
                }

                when (browseMode) {
                    R6BrowseMode.MAP -> R6DiscoveryMap(
                        items = items,
                        currentLocation = currentLocation,
                        onSelect = { onSelect(it) },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                    R6BrowseMode.LIST -> LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        state = discoveryListState,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = GeoDropSpacing.screenGutter,
                            top = GeoDropSpacing.sm,
                            end = GeoDropSpacing.screenGutter,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)
                    ) {
                        if (!networkAvailable) {
                            item(key = "offline") { R6OfflineNotice() }
                        }
                        if (!mapsAvailable) {
                            item(key = "map-unavailable") { R6MapUnavailableNotice() }
                        }
                        if (!approximateLocationEnabled) {
                            item(key = "location-unavailable") {
                                R6LocationUnavailableNotice(onRequestLocation = onRequestLocation)
                            }
                        }
                        items(items, key = { it.drop.id }) { item ->
                            DropCard(
                                title = item.drop.participantLabel(),
                                hostLabel = item.drop.hostLabel,
                                distanceLabel = item.distanceLabel,
                                visualState = item.state.visualState(),
                                onClick = { onSelect(item.drop) }
                            )
                        }
                    }
                }
            }
        }

        if (refreshing && !loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = topPadding + 12.dp, end = 16.dp).size(20.dp),
                strokeWidth = 2.dp
            )
        }
    }

    if (selected != null && unlockResult == null && unlockError == null) {
        ModalBottomSheet(onDismissRequest = { onSelect(null) }) {
            R6DropDetail(
                item = selected,
                isUnlocking = unlockingDropId == selected.drop.id,
                onUnlock = { onUnlock(selected.drop) },
                onReport = { reportTarget = selected.drop },
                onBlock = { blockTarget = selected.drop }
            )
        }
    }

    if (unlockResult != null || unlockError != null) {
        ModalBottomSheet(onDismissRequest = onDismissUnlockResult) {
            val resultState = unlockResult?.let { result ->
                val trail = result.receipt.trail
                val snapshot = result.receipt.snapshot
                val rewardCopy = result.receipt.reward?.let { reward ->
                    buildString {
                        append("Reward code: ${reward.code}")
                        snapshot.rewardPresentation["instructions"]?.let { instructions ->
                            append("\n")
                            append(instructions)
                        }
                        append("\nSaved in Collection for later.")
                    }
                } ?: if (result.rewardUnavailable) {
                    "This reward has run out, but the content is still saved in Collection."
                } else {
                    null
                }
                ResultSheetState.Found(
                    title = snapshot.title,
                    body = listOfNotNull(
                        snapshot.body?.takeIf { it.isNotBlank() },
                        snapshot.mediaAltText?.let { "Photo: $it" },
                        rewardCopy
                    ).joinToString("\n\n"),
                    nextStep = when {
                        trail?.completedAtUnlock == true -> "Trail complete."
                        trail != null -> "Next: Trail stop ${trail.stepIndex + 2} of ${trail.totalSteps}"
                        else -> null
                    }
                )
            } ?: ResultSheetState.Failure(
                title = "Couldn't unlock",
                message = R6ParticipantPolicy.failureMessage(unlockError!!),
                canRetry = unlockError.retryable
            )
            ResultSheet(
                state = resultState,
                onRetry = { selected?.drop?.let(onUnlock) },
                modifier = Modifier.padding(bottom = GeoDropSpacing.lg)
            )
        }
    }

    reportTarget?.let { target ->
        R6ReportDialog(
            onDismiss = { reportTarget = null },
            onSubmit = { reason, narrative ->
                onReport(target, reason, narrative)
                reportTarget = null
            }
        )
    }

    blockTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { blockTarget = null },
            title = { Text("Block this host?") },
            text = { Text("Their drops will no longer appear in Nearby. You can manage blocked hosts from Account.") },
            confirmButton = {
                Button(onClick = { onBlockHost(target); blockTarget = null }) { Text("Block host") }
            },
            dismissButton = {
                TextButton(onClick = { blockTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun R6MapUnavailableNotice() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Map, contentDescription = null)
        Text(
            "Map setup is pending. Use List to browse drops.",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun R6OfflineNotice(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.CloudOff, contentDescription = null)
        Text(
            "You're offline. Showing saved drops. Reconnect, then Refresh.",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun R6LocationUnavailableNotice(
    onRequestLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.LocationOff, contentDescription = null)
        Text(
            "Distances are off because location is off.",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        TextButton(
            onClick = onRequestLocation,
            modifier = Modifier.heightIn(min = GeoDropSize.minimumTouchTarget)
        ) {
            Text("Turn on")
        }
    }
}

@Composable
fun R6CollectionContent(
    loading: Boolean,
    error: String?,
    receipts: List<R6CollectionReceipt>,
    topPadding: Dp,
    onRefresh: () -> Unit,
    onReport: (R6CollectionReceipt, String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var reportTarget by remember { mutableStateOf<R6CollectionReceipt?>(null) }
    when {
        loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        error != null && receipts.isEmpty() -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            R6ErrorState(error, onRefresh)
        }
        receipts.isEmpty() -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Your found drops will appear here.", style = MaterialTheme.typography.bodyLarge)
        }
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = GeoDropSpacing.screenGutter,
                top = topPadding + GeoDropSpacing.sm,
                end = GeoDropSpacing.screenGutter,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
        ) {
            items(receipts, key = { it.receiptId }) { receipt ->
                R6CollectionCard(receipt = receipt, onReport = { reportTarget = receipt })
            }
        }
    }

    reportTarget?.let { target ->
        R6ReportDialog(
            onDismiss = { reportTarget = null },
            onSubmit = { reason, narrative ->
                onReport(target, reason, narrative)
                reportTarget = null
            }
        )
    }
}

@Composable
private fun R6DiscoveryMap(
    items: List<R6DiscoveryPresentation>,
    currentLocation: LatLng?,
    onSelect: (R6DropDiscovery) -> Unit,
    modifier: Modifier = Modifier
) {
    val camera = rememberCameraPositionState()
    val target = currentLocation ?: items.firstOrNull()?.drop?.let { LatLng(it.lat, it.lng) }
    LaunchedEffect(target) {
        target?.let { camera.move(CameraUpdateFactory.newLatLngZoom(it, 15f)) }
    }
    GoogleMap(modifier = modifier, cameraPositionState = camera) {
        items.forEach { item ->
            Marker(
                state = MarkerState(LatLng(item.drop.lat, item.drop.lng)),
                title = item.drop.participantLabel(),
                snippet = "${item.state.accessibilityLabel()}, ${item.distanceLabel}",
                onClick = { onSelect(item.drop); true }
            )
        }
    }
}

@Composable
internal fun R6BrowseModeSelector(
    mode: R6BrowseMode,
    onChange: (R6BrowseMode) -> Unit,
    mapsAvailable: Boolean,
    refreshing: Boolean,
    onRefresh: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(horizontal = GeoDropSpacing.screenGutter),
    ) {
        val useStackedControls = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.5f
        if (useStackedControls) {
            Column(verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)
                ) {
                    R6ModeChip(
                        label = "Map",
                        selected = mode == R6BrowseMode.MAP,
                        enabled = mapsAvailable,
                        icon = { Icon(Icons.Rounded.Map, contentDescription = null) },
                        onClick = { onChange(R6BrowseMode.MAP) },
                        modifier = Modifier.weight(1f)
                    )
                    R6ModeChip(
                        label = "List",
                        selected = mode == R6BrowseMode.LIST,
                        icon = { Icon(Icons.Rounded.List, contentDescription = null) },
                        onClick = { onChange(R6BrowseMode.LIST) },
                        modifier = Modifier.weight(1f)
                    )
                }
                R6RefreshButton(
                    refreshing = refreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                R6ModeChip(
                    label = "Map",
                    selected = mode == R6BrowseMode.MAP,
                    enabled = mapsAvailable,
                    icon = { Icon(Icons.Rounded.Map, contentDescription = null) },
                    onClick = { onChange(R6BrowseMode.MAP) }
                )
                R6ModeChip(
                    label = "List",
                    selected = mode == R6BrowseMode.LIST,
                    icon = { Icon(Icons.Rounded.List, contentDescription = null) },
                    onClick = { onChange(R6BrowseMode.LIST) }
                )
                Spacer(Modifier.weight(1f))
                R6RefreshButton(refreshing = refreshing, onRefresh = onRefresh)
            }
        }
    }
}

@Composable
private fun R6ModeChip(
    label: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label, maxLines = 1) },
        leadingIcon = icon,
        modifier = modifier.heightIn(min = GeoDropSize.minimumTouchTarget)
    )
}

@Composable
private fun R6RefreshButton(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onRefresh,
        enabled = !refreshing,
        modifier = modifier.heightIn(min = GeoDropSize.minimumTouchTarget)
    ) {
        Icon(Icons.Rounded.Refresh, contentDescription = null)
        Spacer(Modifier.width(4.dp))
        Text("Refresh", maxLines = 1)
    }
}

@Composable
internal fun R6DropDetail(
    item: R6DiscoveryPresentation,
    isUnlocking: Boolean,
    onUnlock: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(GeoDropSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
    ) {
        Text(item.drop.participantLabel(), style = MaterialTheme.typography.headlineLarge)
        Text("Hosted by ${item.drop.hostLabel}", style = MaterialTheme.typography.bodyLarge)
        Text("${item.state.accessibilityLabel()} · ${item.distanceLabel}")
        Text(
            "The content stays private until the server confirms this unlock.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        UnlockButton(
            state = when {
                isUnlocking -> UnlockButtonState.Checking
                item.state == R6DiscoveryState.EXPIRED -> UnlockButtonState.Disabled("This drop has expired.")
                item.state == R6DiscoveryState.FOUND -> UnlockButtonState.Disabled("Already found — open it from Collection.")
                item.state == R6DiscoveryState.TRAIL_LOCKED -> UnlockButtonState.Disabled("Find the previous Trail stop first.")
                else -> UnlockButtonState.Idle
            },
            onUnlock = onUnlock
        )
        R6SafetyActions(onReport = onReport, onBlock = onBlock)
    }
}

@Composable
internal fun R6SafetyActions(
    onReport: () -> Unit,
    onBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)
    ) {
        OutlinedButton(
            onClick = onReport,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = GeoDropSize.minimumTouchTarget)
        ) {
            Icon(Icons.Rounded.Flag, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Report")
        }
        OutlinedButton(
            onClick = onBlock,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = GeoDropSize.minimumTouchTarget)
        ) {
            Icon(Icons.Rounded.Block, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Block host")
        }
    }
}

@Composable
private fun R6CollectionCard(receipt: R6CollectionReceipt, onReport: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${receipt.snapshot.title}, found, Experience ${receipt.experienceCode}"
            }
    ) {
        Column(
            modifier = Modifier.padding(GeoDropSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)
        ) {
            Text(receipt.snapshot.title, style = MaterialTheme.typography.titleLarge)
            Text(
                "${receipt.snapshot.hostLabel} · ${receipt.experienceCode} · ${DateFormat.getDateInstance().format(Date(receipt.unlockedAtMillis))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            receipt.snapshot.body?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            receipt.snapshot.mediaAltText?.let {
                Text("Photo: $it", style = MaterialTheme.typography.bodyMedium)
            }
            receipt.reward?.let { reward ->
                Text("Reward code", style = MaterialTheme.typography.labelLarge)
                Text(
                    reward.code,
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    if (reward.usedAtMillis == null) {
                        "Ready to use · available here after it loads, even offline"
                    } else {
                        "Used ${DateFormat.getDateInstance().format(Date(reward.usedAtMillis))}"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
                receipt.snapshot.rewardPresentation["instructions"]?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            receipt.trail?.let { trail ->
                Text(
                    if (trail.completedAtUnlock) "Trail complete" else "Trail stop ${trail.stepIndex + 1} of ${trail.totalSteps}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            TextButton(onClick = onReport, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Rounded.Flag, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Report")
            }
        }
    }
}

@Composable
internal fun R6ReportDialog(onDismiss: () -> Unit, onSubmit: (String, String?) -> Unit) {
    val reasons = listOf(
        "SPAM" to "Spam or misleading",
        "HARASSMENT" to "Harassment",
        "NSFW" to "Sexual content",
        "VIOLENCE" to "Violence or danger",
        "OTHER" to "Something else"
    )
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var narrative by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report this drop") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs)
            ) {
                reasons.forEach { (code, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected == code,
                                onClick = { selected = code },
                                role = Role.RadioButton
                            )
                            .padding(vertical = GeoDropSpacing.xs)
                            .semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == code, onClick = null)
                        Spacer(Modifier.width(GeoDropSpacing.xs))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                OutlinedTextField(
                    value = narrative,
                    onValueChange = { narrative = it.take(500) },
                    label = { Text("Details (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { selected?.let { onSubmit(it, narrative.takeIf(String::isNotBlank)) } },
                enabled = selected != null
            ) { Text("Send report") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun R6ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .semantics { liveRegion = LiveRegionMode.Assertive }
            .padding(GeoDropSpacing.screenGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
private fun R6EmptyState(modifier: Modifier = Modifier) {
    Text(
        "No drops are available in this Experience yet.",
        modifier = modifier.padding(GeoDropSpacing.screenGutter),
        style = MaterialTheme.typography.bodyLarge
    )
}

private fun R6DiscoveryState.visualState(): DropVisualState = when (this) {
    R6DiscoveryState.NEAR -> DropVisualState.NEAR
    R6DiscoveryState.FOUND -> DropVisualState.FOUND
    R6DiscoveryState.EXPIRED -> DropVisualState.EXPIRED
    R6DiscoveryState.LOCKED,
    R6DiscoveryState.TRAIL_LOCKED -> DropVisualState.LOCKED
}

private fun R6DiscoveryState.accessibilityLabel(): String = when (this) {
    R6DiscoveryState.LOCKED -> "Locked"
    R6DiscoveryState.NEAR -> "Nearby — ready to unlock"
    R6DiscoveryState.FOUND -> "Found"
    R6DiscoveryState.EXPIRED -> "Expired"
    R6DiscoveryState.TRAIL_LOCKED -> "Locked — previous Trail stop required"
}

fun r6DistanceLabel(distanceM: Double?): String = when {
    distanceM == null || !distanceM.isFinite() -> "Distance unavailable"
    distanceM <= 100.0 -> "Nearby"
    distanceM <= 800.0 -> "A short walk"
    else -> "Farther out"
}
