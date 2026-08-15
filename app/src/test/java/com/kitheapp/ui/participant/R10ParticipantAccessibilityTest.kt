package com.kitheapp.ui.participant

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kitheapp.data.R6ContentKind
import com.kitheapp.data.R6DiscoveryState
import com.kitheapp.data.R6DropDiscovery
import com.kitheapp.data.R6DropKind
import com.kitheapp.data.R6ExpiryMode
import com.kitheapp.ui.components.NoOpFirebaseInitProviderShadow
import com.kitheapp.ui.components.R3TestApplication
import com.kitheapp.ui.theme.GeoDropTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    application = R3TestApplication::class,
    shadows = [NoOpFirebaseInitProviderShadow::class]
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class R10ParticipantAccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun safetyActionsGrowAndRemainReachableAtTwoHundredPercentFontScale() {
        var reportClicks = 0
        var blockClicks = 0

        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                GeoDropTheme {
                    Surface(Modifier.width(320.dp)) {
                        R6SafetyActions(
                            onReport = { reportClicks += 1 },
                            onBlock = { blockClicks += 1 }
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("Report")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithText("Block host")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, reportClicks)
            assertEquals(1, blockClicks)
        }
    }

    @Test
    fun constrainedDropSheetCanScrollToSafetyActionsAtTwoHundredPercentFontScale() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                GeoDropTheme {
                    Surface {
                        Box(Modifier.width(320.dp).height(480.dp)) {
                            R6DropDetail(
                                item = R6DiscoveryPresentation(
                                    drop = discovery(),
                                    state = R6DiscoveryState.NEAR,
                                    distanceLabel = "Nearby"
                                ),
                                isUnlocking = false,
                                onUnlock = {},
                                onReport = {},
                                onBlock = {}
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText("Report")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Block host")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun reportReasonsExposeOneLabeledSelectableRowAtTwoHundredPercentFontScale() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                GeoDropTheme {
                    R6ReportDialog(onDismiss = {}, onSubmit = { _, _ -> })
                }
            }
        }

        composeRule.onNodeWithText("Spam or misleading")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
            .assertIsSelected()
        composeRule.onNodeWithText("Something else")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun unavailableParticipantStateIsAnnouncedAndKeepsRetryReachable() {
        composeRule.setContent {
            GeoDropTheme {
                R6ErrorState(
                    message = "Kithe can't reach this Experience right now.",
                    onRetry = {}
                )
            }
        }

        composeRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Assertive
            )
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Try again").assertIsDisplayed()
    }

    @Test
    fun compactBrowseControlsDoNotCollapseAtTwoHundredPercentFontScale() {
        var refreshClicks = 0

        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                GeoDropTheme {
                    Surface(Modifier.width(320.dp)) {
                        R6BrowseModeSelector(
                            mode = R6BrowseMode.LIST,
                            onChange = {},
                            mapsAvailable = false,
                            refreshing = false,
                            onRefresh = { refreshClicks += 1 }
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("Map").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("List").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Refresh")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(64.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(1, refreshClicks) }
    }

    @Test
    fun compactListKeepsDropsReachableWhenMapIsUnavailableAtTwoHundredPercentFontScale() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                GeoDropTheme {
                    Surface {
                        Box(Modifier.width(320.dp).height(480.dp)) {
                            R6NearbyContent(
                                loading = false,
                                refreshing = false,
                                error = null,
                                items = listOf(
                                    R6DiscoveryPresentation(
                                        drop = discovery(),
                                        state = R6DiscoveryState.NEAR,
                                        distanceLabel = "Nearby"
                                    )
                                ),
                                selectedDropId = null,
                                unlockingDropId = null,
                                trailProgress = null,
                                currentLocation = null,
                                approximateLocationEnabled = false,
                                networkAvailable = false,
                                topPadding = 0.dp,
                                unlockResult = null,
                                unlockError = null,
                                onSelect = {},
                                onUnlock = {},
                                onRequestLocation = {},
                                onRefresh = {},
                                onDismissUnlockResult = {},
                                onReport = { _, _, _ -> },
                                onBlockHost = {},
                                mapsAvailable = false,
                                initialBrowseMode = R6BrowseMode.LIST
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText("Map setup is pending. Use List to browse drops.")
            .assertExists()
        composeRule.onNodeWithText(
            "You're offline. Showing saved drops. Reconnect, then Refresh."
        ).assertExists()
        composeRule.onNodeWithText("Distances are off because location is off.")
            .assertExists()
        composeRule.onNode(hasScrollAction()).performScrollToIndex(3)
        composeRule.onNodeWithText("Hidden note")
            .assertIsDisplayed()
    }

    private fun discovery() = R6DropDiscovery(
        id = "drop-r10",
        experienceCode = "DEMO2026",
        ownerId = "host-r10",
        hostLabel = "Local demo host with a longer accessible label",
        lat = 19.7,
        lng = -155.1,
        radiusM = 30,
        contentKind = R6ContentKind.TEXT,
        dropKind = R6DropKind.STANDARD,
        payloadVersion = 1,
        trailId = null,
        trailStepIndex = null,
        trailTotalSteps = null,
        likeCount = 0,
        publishedAtMillis = 1_700_000_000_000,
        editedAtMillis = null,
        expiryMode = R6ExpiryMode.NONE,
        expiresAtMillis = null
    )
}
