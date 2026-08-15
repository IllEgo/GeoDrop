package com.kitheapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitheapp.R
import com.kitheapp.ui.theme.GeoDropSize
import com.kitheapp.ui.theme.GeoDropSpacing
import com.kitheapp.ui.theme.GeoDropThemeTokens
import com.kitheapp.ui.theme.RewardCodeTextStyle

sealed interface ComponentState {
    data object Ready : ComponentState
    data object Loading : ComponentState
    data class Empty(val message: String) : ComponentState
    data class Error(val message: String) : ComponentState
    data class Disabled(val reason: String) : ComponentState
}

enum class DropVisualState {
    LOCKED,
    NEAR,
    FOUND,
    EXPIRED
}

@Immutable
private data class VisualStateStyle(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val containerColor: Color,
    val contentColor: Color
)

@Composable
private fun visualStyle(state: DropVisualState): VisualStateStyle {
    val colors = GeoDropThemeTokens.stateColors
    return when (state) {
        DropVisualState.LOCKED -> VisualStateStyle(
            label = stringResource(R.string.r3_state_locked),
            icon = Icons.Default.Lock,
            color = colors.locked,
            containerColor = colors.lockedContainer,
            contentColor = colors.onLockedContainer
        )
        DropVisualState.NEAR -> VisualStateStyle(
            label = stringResource(R.string.r3_state_near),
            icon = Icons.Default.LocationOn,
            color = colors.near,
            containerColor = colors.nearContainer,
            contentColor = colors.onNearContainer
        )
        DropVisualState.FOUND -> VisualStateStyle(
            label = stringResource(R.string.r3_state_found),
            icon = Icons.Default.CheckCircle,
            color = colors.found,
            containerColor = colors.foundContainer,
            contentColor = colors.onFoundContainer
        )
        DropVisualState.EXPIRED -> VisualStateStyle(
            label = stringResource(R.string.r3_state_expired),
            icon = Icons.Default.Schedule,
            color = MaterialTheme.colorScheme.outline,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ComponentStateFrame(
    state: ComponentState,
    modifier: Modifier = Modifier,
    ready: @Composable () -> Unit
) {
    when (state) {
        ComponentState.Ready -> ready()
        ComponentState.Loading -> StateMessage(
            title = stringResource(R.string.r3_loading),
            message = stringResource(R.string.r3_loading_description),
            icon = null,
            modifier = modifier,
            showProgress = true
        )
        is ComponentState.Empty -> StateMessage(
            title = stringResource(R.string.r3_empty_title),
            message = state.message,
            icon = Icons.Default.Info,
            modifier = modifier
        )
        is ComponentState.Error -> StateMessage(
            title = stringResource(R.string.r3_error_title),
            message = state.message,
            icon = Icons.Default.ErrorOutline,
            modifier = modifier,
            isError = true
        )
        is ComponentState.Disabled -> StateMessage(
            title = stringResource(R.string.r3_disabled_title),
            message = state.reason,
            icon = Icons.Default.Close,
            modifier = modifier
        )
    }
}

@Composable
private fun StateMessage(
    title: String,
    message: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    isError: Boolean = false
) {
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(GeoDropSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                showProgress -> CircularProgressIndicator(
                    modifier = Modifier
                        .size(GeoDropSize.icon)
                        .semantics {
                            contentDescription = message
                        },
                    strokeWidth = 3.dp
                )
                icon != null -> Icon(icon, contentDescription = null)
            }
            Column(verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xxs)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun DropCard(
    title: String,
    hostLabel: String,
    distanceLabel: String,
    visualState: DropVisualState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    componentState: ComponentState = ComponentState.Ready
) {
    ComponentStateFrame(componentState, modifier) {
        val style = visualStyle(visualState)
        val description = stringResource(
            R.string.r3_drop_card_description,
            title,
            style.label,
            distanceLabel
        )
        Card(
            modifier = modifier
                .fillMaxWidth()
                .sizeIn(
                    minWidth = GeoDropSize.minimumTouchTarget,
                    minHeight = GeoDropSize.minimumTouchTarget
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    stateDescription = style.label
                    role = Role.Button
                }
                .clickable(onClick = onClick),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(GeoDropSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(GeoDropSize.minimumTouchTarget),
                    shape = CircleShape,
                    color = style.containerColor,
                    contentColor = style.contentColor,
                    border = BorderStroke(2.dp, style.color)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(style.icon, contentDescription = null)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xxs)
                ) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Text(hostLabel, style = MaterialTheme.typography.bodyMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            style.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = style.color
                        )
                        Text(style.label, style = MaterialTheme.typography.labelLarge)
                        Text(distanceLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun DropPin(
    label: String,
    visualState: DropVisualState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val style = visualStyle(visualState)
    val description = stringResource(R.string.r3_drop_pin_description, label, style.label)
    Surface(
        modifier = modifier
            .sizeIn(
                minWidth = GeoDropSize.minimumTouchTarget,
                minHeight = GeoDropSize.minimumTouchTarget
            )
            .semantics(mergeDescendants = true) {
                contentDescription = description
                stateDescription = style.label
                role = Role.Button
            }
            .clickable(enabled = enabled, onClick = onClick),
        color = style.containerColor,
        contentColor = style.contentColor,
        shape = CircleShape,
        border = BorderStroke(2.dp, style.color)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = GeoDropSpacing.sm, vertical = GeoDropSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(style.icon, contentDescription = null)
            Text(style.label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

sealed interface UnlockButtonState {
    data object Idle : UnlockButtonState
    data object Checking : UnlockButtonState
    data class Disabled(val reason: String) : UnlockButtonState
}

@Composable
fun UnlockButton(
    state: UnlockButtonState,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val disabledReason = (state as? UnlockButtonState.Disabled)?.reason
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs)
    ) {
        Button(
            onClick = onUnlock,
            enabled = state == UnlockButtonState.Idle,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = GeoDropSize.minimumTouchTarget)
                .semantics {
                    if (disabledReason != null) stateDescription = disabledReason
                },
            shape = MaterialTheme.shapes.small
        ) {
            if (state == UnlockButtonState.Checking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(GeoDropSize.icon),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 3.dp
                )
                Spacer(Modifier.size(GeoDropSpacing.xs))
                Text(stringResource(R.string.r3_unlock_checking))
            } else {
                Icon(Icons.Default.LockOpen, contentDescription = null)
                Spacer(Modifier.size(GeoDropSpacing.xs))
                Text(stringResource(R.string.r3_unlock))
            }
        }
        if (disabledReason != null) {
            Text(
                disabledReason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

sealed interface ResultSheetState {
    data object Loading : ResultSheetState
    data class Found(
        val title: String,
        val body: String,
        val nextStep: String? = null
    ) : ResultSheetState
    data class Failure(
        val title: String,
        val message: String,
        val canRetry: Boolean = true
    ) : ResultSheetState
    data class Empty(val message: String) : ResultSheetState
}

@Composable
fun ResultSheet(
    state: ResultSheetState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val liveRegionMode = if (state is ResultSheetState.Failure) {
        LiveRegionMode.Assertive
    } else {
        LiveRegionMode.Polite
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = liveRegionMode },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(GeoDropSpacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
        ) {
            when (state) {
                ResultSheetState.Loading -> StateMessage(
                    title = stringResource(R.string.r3_loading),
                    message = stringResource(R.string.r3_loading_description),
                    icon = null,
                    showProgress = true
                )
                is ResultSheetState.Found -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GeoDropThemeTokens.stateColors.found,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(state.title, style = MaterialTheme.typography.headlineLarge)
                    Text(state.body, style = MaterialTheme.typography.bodyLarge)
                    state.nextStep?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is ResultSheetState.Failure -> {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(state.title, style = MaterialTheme.typography.headlineLarge)
                    Text(state.message, style = MaterialTheme.typography.bodyLarge)
                    if (state.canRetry) {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = GeoDropSize.minimumTouchTarget),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.size(GeoDropSpacing.xs))
                            Text(stringResource(R.string.r3_try_again))
                        }
                    }
                }
                is ResultSheetState.Empty -> StateMessage(
                    title = stringResource(R.string.r3_empty_title),
                    message = state.message,
                    icon = Icons.Default.Info
                )
            }
        }
    }
}

enum class PermissionPrimerVariant {
    FULL_SCREEN,
    SHEET
}

@Composable
fun PermissionPrimer(
    title: String,
    explanation: String,
    privacyPromise: String,
    variant: PermissionPrimerVariant,
    onAllow: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
    allowLabel: String? = null,
    notNowLabel: String? = null,
    componentState: ComponentState = ComponentState.Ready
) {
    ComponentStateFrame(componentState, modifier) {
        val containerModifier = if (variant == PermissionPrimerVariant.FULL_SCREEN) {
            modifier.fillMaxSize()
        } else {
            modifier.fillMaxWidth()
        }
        Surface(
            modifier = containerModifier,
            shape = if (variant == PermissionPrimerVariant.SHEET) {
                MaterialTheme.shapes.large
            } else {
                MaterialTheme.shapes.extraSmall
            }
        ) {
            Column(
                modifier = Modifier.padding(GeoDropSpacing.screenGutter),
                verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
            ) {
                val explanationModifier = if (variant == PermissionPrimerVariant.FULL_SCREEN) {
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                } else {
                    Modifier.verticalScroll(rememberScrollState())
                }
                Column(
                    modifier = explanationModifier,
                    verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(title, style = MaterialTheme.typography.headlineLarge)
                    Text(explanation, style = MaterialTheme.typography.bodyLarge)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(GeoDropSpacing.md),
                            horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Text(privacyPromise, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                Button(
                    onClick = onAllow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = GeoDropSize.minimumTouchTarget),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(allowLabel ?: stringResource(R.string.r3_permission_allow))
                }
                OutlinedButton(
                    onClick = onNotNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = GeoDropSize.minimumTouchTarget),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(notNowLabel ?: stringResource(R.string.r3_permission_not_now))
                }
            }
        }
    }
}

@Composable
fun TrailStrip(
    title: String,
    currentStep: Int,
    totalSteps: Int,
    nextLabel: String?,
    modifier: Modifier = Modifier,
    componentState: ComponentState = ComponentState.Ready
) {
    ComponentStateFrame(componentState, modifier) {
        val safeTotal = totalSteps.coerceAtLeast(1)
        val safeCurrent = currentStep.coerceIn(0, safeTotal)
        val progress = safeCurrent.toFloat() / safeTotal.toFloat()
        val progressLabel = stringResource(R.string.r3_trail_progress, safeCurrent, safeTotal)
        val description = stringResource(R.string.r3_trail_description, title, progressLabel)
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(GeoDropSpacing.md),
                verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Route, contentDescription = null)
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.weight(1f))
                    Text(progressLabel, style = MaterialTheme.typography.labelLarge)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
                nextLabel?.let {
                    Text(
                        stringResource(R.string.r3_trail_next, it),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    supportingText: String?,
    modifier: Modifier = Modifier,
    componentState: ComponentState = ComponentState.Ready
) {
    ComponentStateFrame(componentState, modifier) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(GeoDropSpacing.md),
                verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs)
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                Text(value, style = MaterialTheme.typography.displayLarge)
                supportingText?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CodeDisplay(
    code: String,
    businessLabel: String?,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    componentState: ComponentState = ComponentState.Ready
) {
    ComponentStateFrame(componentState, modifier) {
        val spokenCode = code.toCharArray().joinToString(separator = " ")
        val description = stringResource(R.string.r3_code_description, spokenCode)
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                },
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(GeoDropSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xxs)
                ) {
                    Text(code, style = RewardCodeTextStyle, letterSpacing = 2.sp)
                    businessLabel?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.sizeIn(
                        minWidth = GeoDropSize.minimumTouchTarget,
                        minHeight = GeoDropSize.minimumTouchTarget
                    )
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.r3_copy_code)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
    componentState: ComponentState = ComponentState.Ready
) {
    ComponentStateFrame(componentState, modifier) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(GeoDropSpacing.screenGutter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
        ) {
            Surface(
                modifier = Modifier.size(GeoDropSize.minimumTouchTarget),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                FilledTonalButton(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = GeoDropSize.minimumTouchTarget),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
