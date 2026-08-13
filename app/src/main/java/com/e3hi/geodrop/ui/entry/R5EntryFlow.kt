package com.e3hi.geodrop.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.e3hi.geodrop.R
import com.e3hi.geodrop.data.R5EntryException
import com.e3hi.geodrop.data.R5EntryFailureReason
import com.e3hi.geodrop.data.R5EntryGateway
import com.e3hi.geodrop.data.R5EntryRequest
import com.e3hi.geodrop.data.R5ExperienceAvailability
import com.e3hi.geodrop.data.R5ExperiencePreview
import com.e3hi.geodrop.ui.theme.GeoDropSpacing
import com.e3hi.geodrop.util.R5EntryParser
import kotlinx.coroutines.launch

private sealed interface R5EntryUiState {
    data object Manual : R5EntryUiState
    data class Loading(val joining: Boolean) : R5EntryUiState
    data class Preview(val experience: R5ExperiencePreview) : R5EntryUiState
    data class Error(val reason: R5EntryFailureReason, val retryable: Boolean) : R5EntryUiState
}

@Composable
fun R5EntryFlow(
    initialRequest: R5EntryRequest?,
    gateway: R5EntryGateway,
    onRequestResolved: (R5EntryRequest) -> Unit,
    onClearRequest: () -> Unit,
    onEntered: (R5EntryRequest, R5ExperiencePreview) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var codeInput by remember(initialRequest?.code) {
        mutableStateOf(initialRequest?.code?.let(R5EntryParser::displayCode).orEmpty())
    }
    var request by remember(initialRequest) { mutableStateOf(initialRequest) }
    var state by remember(initialRequest) {
        mutableStateOf<R5EntryUiState>(
            if (initialRequest == null) R5EntryUiState.Manual else R5EntryUiState.Loading(false)
        )
    }
    var manualValidationError by remember { mutableStateOf(false) }

    fun resolve(candidate: R5EntryRequest) {
        request = candidate
        onRequestResolved(candidate)
        state = R5EntryUiState.Loading(joining = false)
        scope.launch {
            runCatching {
                gateway.ensureGuestSession(candidate.entrySessionId)
                gateway.resolve(candidate)
            }.onSuccess { preview ->
                state = R5EntryUiState.Preview(preview)
            }.onFailure { error ->
                val entryError = error as? R5EntryException
                state = R5EntryUiState.Error(
                    reason = entryError?.reason ?: R5EntryFailureReason.UNKNOWN,
                    retryable = entryError?.retryable ?: true
                )
            }
        }
    }

    fun submitManualCode() {
        focusManager.clearFocus(force = true)
        val candidate = R5EntryParser.manual(codeInput)
        manualValidationError = candidate == null
        if (candidate != null) resolve(candidate)
    }

    LaunchedEffect(initialRequest) {
        if (initialRequest != null) resolve(initialRequest)
    }

    Surface(modifier = modifier.fillMaxSize()) {
        when (val current = state) {
            R5EntryUiState.Manual -> R5ManualEntry(
                code = codeInput,
                onCodeChange = {
                    codeInput = it.take(40)
                    manualValidationError = false
                },
                validationError = manualValidationError,
                onSubmit = ::submitManualCode
            )

            is R5EntryUiState.Loading -> R5EntryLoading(joining = current.joining)

            is R5EntryUiState.Preview -> R5ExperiencePreviewContent(
                preview = current.experience,
                joining = false,
                onStart = {
                    val activeRequest = request ?: return@R5ExperiencePreviewContent
                    state = R5EntryUiState.Loading(joining = true)
                    scope.launch {
                        runCatching { gateway.join(activeRequest) }
                            .onSuccess { joined -> onEntered(activeRequest, joined) }
                            .onFailure { error ->
                                val entryError = error as? R5EntryException
                                state = R5EntryUiState.Error(
                                    reason = entryError?.reason ?: R5EntryFailureReason.UNKNOWN,
                                    retryable = entryError?.retryable ?: true
                                )
                            }
                    }
                },
                onDifferentCode = {
                    request = null
                    onClearRequest()
                    state = R5EntryUiState.Manual
                }
            )

            is R5EntryUiState.Error -> R5EntryErrorContent(
                reason = current.reason,
                retryable = current.retryable,
                onRetry = { request?.let(::resolve) },
                onDifferentCode = {
                    request = null
                    onClearRequest()
                    state = R5EntryUiState.Manual
                    manualValidationError = false
                }
            )
        }
    }
}

@Composable
private fun R5ManualEntry(
    code: String,
    onCodeChange: (String) -> Unit,
    validationError: Boolean,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = GeoDropSpacing.screenGutter, vertical = GeoDropSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
    ) {
        Icon(
            imageVector = Icons.Rounded.Explore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = stringResource(R.string.r5_entry_eyebrow),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.r5_entry_title),
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = stringResource(R.string.r5_entry_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(GeoDropSpacing.sm))
        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("r5_entry_code"),
            label = { Text(stringResource(R.string.r5_entry_code_label)) },
            placeholder = { Text(stringResource(R.string.r5_entry_code_hint)) },
            isError = validationError,
            supportingText = if (validationError) {
                { Text(stringResource(R.string.r5_entry_invalid_code)) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onSubmit() })
        )
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("r5_preview_experience")
        ) {
            Text(stringResource(R.string.r5_entry_preview))
        }
    }
}

@Composable
private fun R5EntryLoading(joining: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(GeoDropSpacing.screenGutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(GeoDropSpacing.md))
        Text(
            text = stringResource(
                if (joining) R.string.r5_entry_joining else R.string.r5_entry_loading
            ),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun R5ExperiencePreviewContent(
    preview: R5ExperiencePreview,
    joining: Boolean,
    onStart: () -> Unit,
    onDifferentCode: () -> Unit
) {
    val canEnter = preview.availability == R5ExperienceAvailability.ACTIVE ||
        preview.availability == R5ExperienceAvailability.UPCOMING
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = GeoDropSpacing.screenGutter, vertical = GeoDropSpacing.xl)
            .testTag("r5_experience_preview"),
        verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
    ) {
        Icon(
            imageVector = Icons.Rounded.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = preview.name,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.testTag("r5_preview_name")
        )
        Text(
            text = stringResource(R.string.r5_preview_hosted_by, preview.hostLabel),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
            PreviewPill(
                text = stringResource(
                    when (preview.availability) {
                        R5ExperienceAvailability.ACTIVE -> R.string.r5_preview_active
                        R5ExperienceAvailability.UPCOMING -> R.string.r5_preview_upcoming
                        R5ExperienceAvailability.ENDED -> R.string.r5_preview_ended
                        R5ExperienceAvailability.CANCELLED -> R.string.r5_preview_cancelled
                    }
                )
            )
            PreviewPill(
                text = stringResource(R.string.r5_preview_drop_count, preview.availableDropCount)
            )
        }
        preview.description?.let { description ->
            Text(description, style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            text = stringResource(
                R.string.r5_preview_code,
                R5EntryParser.displayCode(preview.code)
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(GeoDropSpacing.md),
                verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs)
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null)
                    Text(
                        text = stringResource(R.string.r5_preview_what_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = stringResource(R.string.r5_preview_what_body),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(Modifier.height(GeoDropSpacing.sm))
        Button(
            onClick = onStart,
            enabled = canEnter && !joining,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("r5_start_exploring")
        ) {
            Text(stringResource(R.string.r5_preview_start))
        }
        OutlinedButton(
            onClick = onDifferentCode,
            enabled = !joining,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(stringResource(R.string.r5_preview_different_code))
        }
    }
}

@Composable
private fun PreviewPill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = GeoDropSpacing.sm, vertical = GeoDropSpacing.xs)
        )
    }
}

@Composable
private fun R5EntryErrorContent(
    reason: R5EntryFailureReason,
    retryable: Boolean,
    onRetry: () -> Unit,
    onDifferentCode: () -> Unit
) {
    val message = when (reason) {
        R5EntryFailureReason.INVALID_CODE,
        R5EntryFailureReason.EXPERIENCE_NOT_FOUND -> R.string.r5_entry_error_not_found
        R5EntryFailureReason.EXPERIENCE_CANCELLED -> R.string.r5_entry_error_cancelled
        R5EntryFailureReason.EXPERIENCE_ENDED -> R.string.r5_entry_error_ended
        R5EntryFailureReason.RATE_LIMITED -> R.string.r5_entry_error_rate_limited
        R5EntryFailureReason.OFFLINE -> R.string.r5_entry_error_offline
        R5EntryFailureReason.UNAVAILABLE,
        R5EntryFailureReason.UNKNOWN -> R.string.r5_entry_error_unavailable
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(GeoDropSpacing.screenGutter)
            .testTag("r5_entry_error"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(GeoDropSpacing.md))
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(GeoDropSpacing.lg))
        if (retryable) {
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(stringResource(R.string.r5_entry_retry))
            }
            Spacer(Modifier.height(GeoDropSpacing.sm))
        }
        OutlinedButton(
            onClick = onDifferentCode,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(stringResource(R.string.r5_preview_different_code))
        }
    }
}
