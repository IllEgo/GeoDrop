package com.e3hi.geodrop.ui.organizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.e3hi.geodrop.data.R7Experience
import com.e3hi.geodrop.data.R7OrganizerDropSummary
import com.e3hi.geodrop.data.R7OrganizerException
import com.e3hi.geodrop.data.R8CorrectionReason
import com.e3hi.geodrop.data.R8ExperienceResults
import com.e3hi.geodrop.data.R8OrganizerGateway
import com.e3hi.geodrop.data.R8RewardCode
import com.e3hi.geodrop.data.R8RewardCodeState
import com.e3hi.geodrop.data.R8RewardPolicy
import com.e3hi.geodrop.ui.theme.GeoDropSize
import com.e3hi.geodrop.ui.theme.GeoDropSpacing
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@Composable
fun R8RewardOperationsContent(
    drop: R7OrganizerDropSummary,
    gateway: R8OrganizerGateway,
    localDemo: Boolean,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var filterName by rememberSaveable(drop.id) { mutableStateOf(R8RewardCodeState.ISSUED.name) }
    val filter = runCatching { R8RewardCodeState.valueOf(filterName) }
        .getOrDefault(R8RewardCodeState.ISSUED)
    var searchText by rememberSaveable(drop.id) { mutableStateOf("") }
    var appliedSearch by rememberSaveable(drop.id) { mutableStateOf<String?>(null) }
    var codes by remember { mutableStateOf<List<R8RewardCode>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshToken by remember { mutableIntStateOf(0) }
    var target by remember { mutableStateOf<R8RewardCode?>(null) }
    var correction by remember { mutableStateOf(false) }
    var correctionReasonName by rememberSaveable {
        mutableStateOf(R8CorrectionReason.MARKED_BY_MISTAKE.name)
    }
    var mutationLoading by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(drop.id, filter, appliedSearch, refreshToken) {
        loading = true
        error = null
        runCatching {
            gateway.loadRewardCodes(
                dropId = drop.id,
                state = if (appliedSearch == null) filter else null,
                searchCode = appliedSearch
            ).filter { it.state != R8RewardCodeState.AVAILABLE }
        }.onSuccess { codes = it }
            .onFailure { error = it.r8Message() }
        loading = false
    }

    target?.let { selected ->
        AlertDialog(
            onDismissRequest = { if (!mutationLoading) target = null },
            title = { Text(if (correction) "Correct reward status?" else "Mark reward used?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
                    Text(selected.code, fontFamily = FontFamily.Monospace)
                    if (correction) {
                        Text("This returns the code to Issued. The original Used event remains in its history.")
                        R8CorrectionReason.entries.forEach { reason ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = correctionReasonName == reason.name,
                                    onClick = { correctionReasonName = reason.name }
                                )
                                Text(
                                    if (reason == R8CorrectionReason.MARKED_BY_MISTAKE) {
                                        "Marked by mistake"
                                    } else {
                                        "Business correction"
                                    }
                                )
                            }
                        }
                    } else {
                        Text("Only confirm after manually matching the guest's displayed code. This records the time but no guest identity.")
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !mutationLoading,
                    onClick = {
                        scope.launch {
                            mutationLoading = true
                            runCatching {
                                if (correction) {
                                    gateway.correctRewardCodeUse(
                                        drop.id,
                                        selected.code,
                                        R8CorrectionReason.valueOf(correctionReasonName)
                                    )
                                } else {
                                    gateway.markRewardCodeUsed(drop.id, selected.code)
                                }
                            }.onSuccess { changed ->
                                notice = when {
                                    correction && changed -> "Correction recorded."
                                    correction -> "Code was already Issued."
                                    changed -> "Code marked Used."
                                    else -> "Code was already Used."
                                }
                                target = null
                                refreshToken += 1
                            }.onFailure { error = it.r8Message() }
                            mutationLoading = false
                        }
                    }
                ) {
                    if (mutationLoading) CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                    else Text(if (correction) "Record correction" else "Mark Used")
                }
            },
            dismissButton = {
                TextButton(enabled = !mutationLoading, onClick = { target = null }) { Text("Cancel") }
            }
        )
    }

    Column(Modifier.fillMaxSize()) {
        R7InternalHeader("Reward codes", onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(GeoDropSpacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
                    Text(drop.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Pilot 1 uses manual code validation. Status changes require an internet connection; there is no merchant or employee account.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (localDemo) {
                        Card(colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )) {
                            Text(
                                "Local demo: the code pool is simulated in this running app. Production pools are pre-generated and loaded by the pilot operator.",
                                modifier = Modifier.padding(GeoDropSpacing.md)
                            )
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
                    FilterChip(
                        selected = appliedSearch == null && filter == R8RewardCodeState.ISSUED,
                        onClick = {
                            filterName = R8RewardCodeState.ISSUED.name
                            appliedSearch = null
                        },
                        label = { Text("Issued") }
                    )
                    FilterChip(
                        selected = appliedSearch == null && filter == R8RewardCodeState.USED,
                        onClick = {
                            filterName = R8RewardCodeState.USED.name
                            appliedSearch = null
                        },
                        label = { Text("Used") }
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs)) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it.uppercase().take(32) },
                        label = { Text("Find exact reward code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
                        Button(
                            onClick = {
                                val message = R8RewardPolicy.validateSearchCode(searchText)
                                if (message == null && searchText.isNotBlank()) {
                                    appliedSearch = R8RewardPolicy.normalizeCode(searchText)
                                } else {
                                    error = message ?: "Enter a reward code to search."
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = null)
                            Spacer(Modifier.width(GeoDropSpacing.xs))
                            Text("Search")
                        }
                        if (appliedSearch != null) {
                            OutlinedButton(onClick = {
                                searchText = ""
                                appliedSearch = null
                            }) { Text("Clear") }
                        }
                    }
                }
            }
            notice?.let { message -> item {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )) { Text(message, Modifier.fillMaxWidth().padding(GeoDropSpacing.md)) }
            } }
            error?.let { message -> item { R7InlineError(message) { refreshToken += 1 } } }
            when {
                loading -> item { Box(Modifier.fillMaxWidth().padding(GeoDropSpacing.xl)) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } }
                codes.isEmpty() -> item {
                    Text(
                        if (appliedSearch != null) "No issued or used code matches that search."
                        else "No ${filter.name.lowercase()} codes yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> items(codes, key = { it.code }) { code ->
                    R8RewardCodeCard(
                        code = code,
                        onMarkUsed = {
                            correction = false
                            target = code
                        },
                        onCorrect = {
                            correction = true
                            target = code
                        }
                    )
                }
            }
            item {
                OutlinedButton(
                    onClick = { refreshToken += 1 },
                    modifier = Modifier.fillMaxWidth().height(GeoDropSize.minimumTouchTarget)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(Modifier.width(GeoDropSpacing.xs))
                    Text("Refresh")
                }
            }
        }
    }
}

@Composable
private fun R8RewardCodeCard(
    code: R8RewardCode,
    onMarkUsed: () -> Unit,
    onCorrect: () -> Unit
) {
    Card {
        Column(
            Modifier.fillMaxWidth().padding(GeoDropSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    code.code,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                Text(code.state.name.lowercase().replaceFirstChar(Char::uppercase))
            }
            code.issuedAtMillis?.let { Text("Issued ${r8DateTime(it)}") }
            code.usedAtMillis?.let { Text("Used ${r8DateTime(it)}") }
            if (code.history.isNotEmpty()) {
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.History, contentDescription = null)
                    Spacer(Modifier.width(GeoDropSpacing.xs))
                    Text("Status history", style = MaterialTheme.typography.labelLarge)
                }
                code.history.take(3).forEach { event ->
                    Text(
                        "${event.transition.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)} · ${r8DateTime(event.occurredAtMillis)}" +
                            event.reason?.takeUnless { it.startsWith("demo-reward-") }?.let {
                                " · ${it.replace('_', ' ').lowercase()}"
                            }.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = if (code.state == R8RewardCodeState.USED) onCorrect else onMarkUsed,
                modifier = Modifier.fillMaxWidth().height(GeoDropSize.minimumTouchTarget)
            ) {
                Icon(
                    if (code.state == R8RewardCodeState.USED) Icons.Rounded.History
                    else Icons.Rounded.CheckCircle,
                    contentDescription = null
                )
                Spacer(Modifier.width(GeoDropSpacing.xs))
                Text(if (code.state == R8RewardCodeState.USED) "Correct status" else "Mark Used")
            }
        }
    }
}

@Composable
fun R8ResultsContent(
    experience: R7Experience,
    drops: List<R7OrganizerDropSummary>,
    gateway: R8OrganizerGateway,
    localDemo: Boolean,
    onBack: () -> Unit
) {
    var results by remember { mutableStateOf<R8ExperienceResults?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshToken by remember { mutableIntStateOf(0) }
    LaunchedEffect(experience.code, refreshToken) {
        loading = true
        error = null
        runCatching { gateway.loadResults(experience.code) }
            .onSuccess { results = it }
            .onFailure { error = it.r8Message() }
        loading = false
    }
    Column(Modifier.fillMaxSize()) {
        R7InternalHeader("Results", onBack)
        when {
            loading && results == null -> Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            error != null && results == null -> R7CenteredError(error!!, { refreshToken += 1 })
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(GeoDropSpacing.screenGutter),
                verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs)) {
                        Text(experience.name, style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { heading() })
                        Text(
                            "Private aggregate Results only. No participant names, email addresses, or location history are shown.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (localDemo) {
                            Text(
                                "Local demo Results reflect actions in this running app only.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                results?.let { value ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
                            R8MetricRow("Joined", value.joinedParticipants, "Published drops", value.publishedDrops)
                            R8MetricRow("Unique finders", value.uniqueUnlockers, "Total finds", value.unlocks)
                            R8MetricRow("Codes issued", value.codesIssued, "Codes used", value.codesUsed)
                            R8MetricRow("Main Trail completions", value.mainTrailCompletions, "", null)
                        }
                    }
                    item {
                        Card(colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )) {
                            Column(
                                Modifier.fillMaxWidth().padding(GeoDropSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs)
                            ) {
                                Text("What these numbers mean", style = MaterialTheme.typography.titleMedium)
                                Text("Joined: participant memberships created for this Experience.")
                                Text("Unique finders: distinct accounts with at least one confirmed unlock.")
                                Text("Total finds: confirmed unlock receipts; duplicate retries do not add another.")
                                Text("Codes issued and used are separate. A correction lowers Used but keeps its history.")
                            }
                        }
                    }
                    item {
                        Text("Per-drop Results", style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() })
                    }
                    if (value.drops.isEmpty()) {
                        item { Text("No per-drop activity yet.") }
                    } else {
                        items(value.drops, key = { it.dropId }) { result ->
                            Card {
                                Column(
                                    Modifier.fillMaxWidth().padding(GeoDropSpacing.md),
                                    verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs)
                                ) {
                                    Text(
                                        drops.firstOrNull { it.id == result.dropId }?.title
                                            ?: "Drop ${result.dropId.take(8)}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text("${result.unlocks} finds · ${result.codesIssued} codes issued · ${result.codesUsed} used")
                                }
                            }
                        }
                    }
                    item {
                        Card(colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )) {
                            Column(
                                Modifier.fillMaxWidth().padding(GeoDropSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs)
                            ) {
                                Text("Pilot report", style = MaterialTheme.typography.titleMedium)
                                Text("The founder report is prepared outside the app from the same aggregate definitions. In-app export is deferred for Pilot 1.")
                                value.updatedAtMillis?.let { Text("Updated ${r8DateTime(it)}") }
                                value.reconciledAtMillis?.let { Text("Last reconciled ${r8DateTime(it)}") }
                            }
                        }
                    }
                }
                error?.let { message -> item { R7InlineError(message) { refreshToken += 1 } } }
                item {
                    OutlinedButton(
                        onClick = { refreshToken += 1 },
                        modifier = Modifier.fillMaxWidth().height(GeoDropSize.minimumTouchTarget)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.width(GeoDropSpacing.xs))
                        Text("Refresh Results")
                    }
                }
            }
        }
    }
}

@Composable
private fun R8MetricRow(
    firstLabel: String,
    firstValue: Long,
    secondLabel: String,
    secondValue: Long?
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
        R8MetricCard(firstLabel, firstValue, Modifier.weight(1f))
        if (secondValue != null) R8MetricCard(secondLabel, secondValue, Modifier.weight(1f))
        else Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun R8MetricCard(label: String, value: Long, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(GeoDropSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs)
        ) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun Throwable.r8Message(): String = when (this) {
    is R7OrganizerException -> userMessage
    else -> message?.takeIf(String::isNotBlank) ?: "Couldn't load that right now."
}

private fun r8DateTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
