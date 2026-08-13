package com.e3hi.geodrop.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.e3hi.geodrop.R
import com.e3hi.geodrop.data.FirebaseR5EntryGateway
import com.e3hi.geodrop.data.GroupMembership
import com.e3hi.geodrop.data.GroupRole
import com.e3hi.geodrop.data.LegalConsentGateway
import com.e3hi.geodrop.data.R5EntryGateway
import com.e3hi.geodrop.data.R5EntryRequest
import com.e3hi.geodrop.data.R5ExperienceMembership
import com.e3hi.geodrop.data.R6ParticipantGateway
import com.e3hi.geodrop.data.R7OrganizerGateway
import com.e3hi.geodrop.data.R9AccountGateway
import com.e3hi.geodrop.ui.DropHereScreen
import com.e3hi.geodrop.ui.theme.GeoDropSpacing
import com.e3hi.geodrop.util.GroupPreferences
import com.e3hi.geodrop.util.R5EntryParser
import com.e3hi.geodrop.util.R5EntryStore

private enum class R5ShellBootstrapState {
    LOADING,
    READY,
    ERROR
}

@Composable
fun R5AppRoot(
    incomingRequest: R5EntryRequest?,
    onIncomingRequestConsumed: () -> Unit,
    onNearbyAlertsEnabled: () -> Unit,
    onNearbyAlertsDisabled: () -> Unit,
    modifier: Modifier = Modifier,
    gatewayOverride: R5EntryGateway? = null,
    r6GatewayOverride: R6ParticipantGateway? = null,
    r7GatewayOverride: R7OrganizerGateway? = null,
    r9GatewayOverride: R9AccountGateway? = null,
    legalConsentGatewayOverride: LegalConsentGateway? = null,
    debugDeviceDemoEnabled: Boolean = false
) {
    val context = LocalContext.current
    val store = remember(context) { R5EntryStore(context) }
    val groupPreferences = remember(context) { GroupPreferences(context) }
    val gateway = gatewayOverride ?: remember { FirebaseR5EntryGateway() }
    var memberships by remember { mutableStateOf(groupPreferences.getMemberships()) }
    var activeRequest by remember {
        mutableStateOf(incomingRequest ?: store.pendingEntry())
    }
    var manualEntryRequested by remember { mutableStateOf(false) }
    var bootstrapState by remember { mutableStateOf(R5ShellBootstrapState.LOADING) }
    var bootstrapAttempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(incomingRequest) {
        if (incomingRequest != null) {
            store.savePendingEntry(incomingRequest)
            activeRequest = incomingRequest
            manualEntryRequested = false
            onIncomingRequestConsumed()
        }
    }

    val needsEntry = activeRequest != null || memberships.isEmpty() || manualEntryRequested
    if (needsEntry) {
        R5EntryFlow(
            initialRequest = activeRequest,
            gateway = gateway,
            onRequestResolved = { request ->
                store.savePendingEntry(request)
                activeRequest = request
            },
            onClearRequest = {
                store.clearPendingEntry()
                activeRequest = null
                manualEntryRequested = true
            },
            onEntered = { request, preview ->
                val role = if (preview.membership == R5ExperienceMembership.OWNER) {
                    GroupRole.OWNER
                } else {
                    GroupRole.SUBSCRIBER
                }
                groupPreferences.addGroup(
                    GroupMembership(
                        code = preview.code.ifBlank { request.code },
                        ownerId = null,
                        role = role
                    )
                )
                store.completeEntry(request)
                memberships = groupPreferences.getMemberships()
                activeRequest = null
                manualEntryRequested = false
                bootstrapState = R5ShellBootstrapState.READY
            },
            modifier = modifier
        )
        return
    }

    LaunchedEffect(bootstrapAttempt, memberships) {
        bootstrapState = R5ShellBootstrapState.LOADING
        val session = store.activeEntrySessionId() ?: R5EntryParser.newEntrySessionId()
        runCatching {
            gateway.ensureGuestSession(session)
            runCatching {
                gateway.recordClientEvent(
                    eventName = "app_first_open",
                    entrySessionId = session,
                    experienceCode = store.activeExperienceCode(),
                    installKey = store.installKey()
                )
            }
        }.onSuccess {
            bootstrapState = R5ShellBootstrapState.READY
        }.onFailure {
            bootstrapState = R5ShellBootstrapState.ERROR
        }
    }

    when (bootstrapState) {
        R5ShellBootstrapState.LOADING -> R5BootstrapMessage(
            message = stringResource(R.string.r5_entry_boot_loading)
        )

        R5ShellBootstrapState.ERROR -> R5BootstrapMessage(
            message = stringResource(R.string.r5_entry_boot_error),
            action = stringResource(R.string.r5_entry_retry),
            onAction = { bootstrapAttempt += 1 }
        )

        R5ShellBootstrapState.READY -> DropHereScreen(
            onNearbyAlertsEnabled = onNearbyAlertsEnabled,
            onNearbyAlertsDisabled = onNearbyAlertsDisabled,
            skipFirstRunOnboarding = true,
            r5EntrySessionId = store.activeEntrySessionId(),
            r5ExperienceCode = store.activeExperienceCode(),
            r5EntryStore = store,
            r5EntryGateway = gateway,
            r6ParticipantGateway = r6GatewayOverride,
            r7OrganizerGateway = r7GatewayOverride,
            r9AccountGateway = r9GatewayOverride,
            legalConsentGateway = legalConsentGatewayOverride,
            debugDeviceDemoEnabled = debugDeviceDemoEnabled
        )
    }
}

@Composable
private fun R5BootstrapMessage(
    message: String,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(GeoDropSpacing.screenGutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (action == null) {
            CircularProgressIndicator()
            Spacer(Modifier.height(GeoDropSpacing.md))
        }
        Text(message, style = MaterialTheme.typography.bodyLarge)
        if (action != null) {
            Spacer(Modifier.height(GeoDropSpacing.lg))
            Button(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(action)
            }
        }
    }
}
