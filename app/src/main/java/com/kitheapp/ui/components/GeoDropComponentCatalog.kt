package com.kitheapp.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.kitheapp.R
import com.kitheapp.ui.theme.GeoDropSpacing
import com.kitheapp.ui.theme.GeoDropTheme

private data class CatalogSection(
    val titleResource: Int,
    val content: @Composable () -> Unit
)

/**
 * Development-only visual inventory for the nine shared R3 components.
 * It intentionally is not connected to app navigation before the R4 gate.
 */
@Composable
fun GeoDropComponentCatalog(modifier: Modifier = Modifier) {
    val sections = listOf(
        CatalogSection(R.string.r3_catalog_section_drop_card) {
            Column(verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
                DropVisualState.entries.forEach { state ->
                    DropCard(
                        title = stringResource(R.string.r3_catalog_drop_title),
                        hostLabel = stringResource(R.string.r3_catalog_host),
                        distanceLabel = stringResource(R.string.r3_catalog_distance),
                        visualState = state,
                        onClick = {}
                    )
                }
            }
        },
        CatalogSection(R.string.r3_catalog_section_drop_pin) {
            Column(verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
                DropVisualState.entries.forEach { state ->
                    DropPin(
                        label = stringResource(R.string.r3_catalog_drop_title),
                        visualState = state,
                        onClick = {}
                    )
                }
            }
        },
        CatalogSection(R.string.r3_catalog_section_unlock_button) {
            Column(verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
                UnlockButton(UnlockButtonState.Idle, onUnlock = {})
                UnlockButton(UnlockButtonState.Checking, onUnlock = {})
                UnlockButton(
                    UnlockButtonState.Disabled(stringResource(R.string.r3_catalog_disabled)),
                    onUnlock = {}
                )
            }
        },
        CatalogSection(R.string.r3_catalog_section_result_sheet) {
            Column(verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
                ResultSheet(ResultSheetState.Loading, onRetry = {})
                ResultSheet(
                    ResultSheetState.Found(
                        title = stringResource(R.string.r3_catalog_found_title),
                        body = stringResource(R.string.r3_catalog_found_body),
                        nextStep = stringResource(R.string.r3_catalog_next_step)
                    ),
                    onRetry = {}
                )
                ResultSheet(
                    ResultSheetState.Failure(
                        title = stringResource(R.string.r3_catalog_failure_title),
                        message = stringResource(R.string.r3_catalog_error)
                    ),
                    onRetry = {}
                )
                ResultSheet(
                    ResultSheetState.Empty(stringResource(R.string.r3_catalog_empty)),
                    onRetry = {}
                )
            }
        },
        CatalogSection(R.string.r3_catalog_section_permission_primer) {
            PermissionPrimer(
                title = stringResource(R.string.r3_catalog_permission_title),
                explanation = stringResource(R.string.r3_catalog_permission_body),
                privacyPromise = stringResource(R.string.r3_catalog_permission_privacy),
                variant = PermissionPrimerVariant.SHEET,
                onAllow = {},
                onNotNow = {}
            )
        },
        CatalogSection(R.string.r3_catalog_section_trail_strip) {
            TrailStrip(
                title = stringResource(R.string.r3_catalog_trail_title),
                currentStep = 2,
                totalSteps = 5,
                nextLabel = stringResource(R.string.r3_catalog_trail_next)
            )
        },
        CatalogSection(R.string.r3_catalog_section_stat_card) {
            StatCard(
                label = stringResource(R.string.r3_catalog_stat_label),
                value = stringResource(R.string.r3_catalog_stat_value),
                supportingText = stringResource(R.string.r3_catalog_stat_support)
            )
        },
        CatalogSection(R.string.r3_catalog_section_code_display) {
            CodeDisplay(
                code = stringResource(R.string.r3_catalog_code),
                businessLabel = stringResource(R.string.r3_catalog_code_business),
                onCopy = {}
            )
        },
        CatalogSection(R.string.r3_catalog_section_empty_state) {
            EmptyState(
                title = stringResource(R.string.r3_catalog_empty_title),
                message = stringResource(R.string.r3_catalog_empty_body),
                actionLabel = stringResource(R.string.r3_catalog_empty_action),
                onAction = {}
            )
        },
        CatalogSection(R.string.r3_catalog_section_shared_states) {
            Column(verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm)) {
                StatCard(
                    label = stringResource(R.string.r3_catalog_stat_label),
                    value = stringResource(R.string.r3_catalog_stat_value),
                    supportingText = null,
                    componentState = ComponentState.Loading
                )
                StatCard(
                    label = stringResource(R.string.r3_catalog_stat_label),
                    value = stringResource(R.string.r3_catalog_stat_value),
                    supportingText = null,
                    componentState = ComponentState.Empty(stringResource(R.string.r3_catalog_empty))
                )
                StatCard(
                    label = stringResource(R.string.r3_catalog_stat_label),
                    value = stringResource(R.string.r3_catalog_stat_value),
                    supportingText = null,
                    componentState = ComponentState.Error(stringResource(R.string.r3_catalog_error))
                )
                StatCard(
                    label = stringResource(R.string.r3_catalog_stat_label),
                    value = stringResource(R.string.r3_catalog_stat_value),
                    supportingText = null,
                    componentState = ComponentState.Disabled(stringResource(R.string.r3_catalog_disabled))
                )
            }
        }
    )

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(GeoDropSpacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xl)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs)) {
                    Text(
                        stringResource(R.string.r3_component_catalog_title),
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        stringResource(R.string.r3_component_catalog_description),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(sections) { section ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)
                ) {
                    Text(
                        stringResource(section.titleResource),
                        style = MaterialTheme.typography.titleLarge
                    )
                    section.content()
                }
            }
        }
    }
}

@Preview(name = "R3 light", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun GeoDropComponentCatalogLightPreview() {
    GeoDropTheme(darkTheme = false) { GeoDropComponentCatalog() }
}

@Preview(
    name = "R3 dark",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun GeoDropComponentCatalogDarkPreview() {
    GeoDropTheme(darkTheme = true) { GeoDropComponentCatalog() }
}

@Preview(
    name = "R3 compact at 200 percent",
    widthDp = 320,
    heightDp = 640,
    fontScale = 2f,
    showBackground = true
)
@Composable
private fun GeoDropComponentCatalogLargeTextPreview() {
    GeoDropTheme(darkTheme = false) { GeoDropComponentCatalog() }
}

@Preview(name = "R3 reduced motion", widthDp = 320, heightDp = 640, showBackground = true)
@Composable
private fun GeoDropComponentCatalogReducedMotionPreview() {
    GeoDropTheme(darkTheme = false, reducedMotion = true) { GeoDropComponentCatalog() }
}
