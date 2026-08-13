package com.e3hi.geodrop.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.e3hi.geodrop.ui.theme.GeoDropTheme
import org.junit.Assert.assertNotEquals
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
class GeoDropComponentsAccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dropStatesUseTextAndIconSemanticsInsteadOfColorAlone() {
        composeRule.setContent {
            GeoDropTheme(darkTheme = false) {
                Column {
                    DropVisualState.entries.forEach { state ->
                        DropPin(label = "Kīlauea", visualState = state, onClick = {})
                    }
                }
            }
        }

        composeRule.onNode(hasStateDescription("Locked")).assertIsDisplayed()
        composeRule.onNode(hasStateDescription("Nearby — ready to unlock")).assertIsDisplayed()
        composeRule.onNode(hasStateDescription("Found")).assertIsDisplayed()
    }

    @Test
    fun unlockTargetIsAtLeastFortyEightDpAndDisabledReasonIsExposed() {
        composeRule.setContent {
            GeoDropTheme {
                UnlockButton(
                    state = UnlockButtonState.Disabled("Location is needed."),
                    onUnlock = {}
                )
            }
        }

        composeRule.onNodeWithText("Unlock")
            .assertIsNotEnabled()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
        composeRule.onNodeWithText("Location is needed.").assertIsDisplayed()
    }

    @Test
    fun resultChangesAreAnnouncedAsLiveRegions() {
        composeRule.setContent {
            GeoDropTheme {
                ResultSheet(
                    state = ResultSheetState.Found(
                        title = "Found",
                        body = "A note from the host."
                    ),
                    onRetry = {}
                )
            }
        }

        composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        ).assertIsDisplayed()
    }

    @Test
    fun compactWidthAtTwoHundredPercentFontScaleKeepsActionsReachable() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                GeoDropTheme {
                    Surface(Modifier.fillMaxSize()) {
                        PermissionPrimer(
                            title = "Use your location for nearby drops",
                            explanation = "GeoDrop checks whether you are close enough only when you choose Unlock.",
                            privacyPromise = "We use your exact location for a second, then forget it.",
                            variant = PermissionPrimerVariant.FULL_SCREEN,
                            onAllow = {},
                            onNotNow = {}
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("Allow while using the app")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        composeRule.onNodeWithText("Not now").assertIsDisplayed()
    }

    @Test
    fun fortyPercentLongerCopyRemainsPresentOnCompactComponent() {
        val title = "Kīlauea overlook note with a longer place name"
        val host = "Hosted by the Hawaiʻi Island Museum and local partners"
        val distance = "Approximately 40 metres from this location"
        val description = "$title, Locked, $distance"
        composeRule.setContent {
            GeoDropTheme {
                Surface(Modifier.width(320.dp)) {
                    DropCard(
                        title = title,
                        hostLabel = host,
                        distanceLabel = distance,
                        visualState = DropVisualState.LOCKED,
                        onClick = {}
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription(description).assertIsDisplayed()
    }

    @Test
    fun lightAndDarkCatalogConfigurationsRenderDifferentThemeSurfaces() {
        var lightSurface = androidx.compose.ui.graphics.Color.Unspecified
        var darkSurface = androidx.compose.ui.graphics.Color.Unspecified
        composeRule.setContent {
            Column {
                GeoDropTheme(darkTheme = false) {
                    val surface = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    SideEffect { lightSurface = surface }
                    StatCard(label = "Found", value = "128", supportingText = null)
                }
                GeoDropTheme(darkTheme = true) {
                    val surface = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    SideEffect { darkSurface = surface }
                    StatCard(label = "Found", value = "128", supportingText = null)
                }
            }
        }

        composeRule.runOnIdle { assertNotEquals(lightSurface, darkSurface) }
    }

    @Test
    fun reducedMotionThemeStillPresentsUnlockAction() {
        composeRule.setContent {
            GeoDropTheme(reducedMotion = true) {
                UnlockButton(UnlockButtonState.Idle, onUnlock = {})
            }
        }

        composeRule.onNodeWithText("Unlock").assertIsDisplayed().assertIsEnabled()
    }
}
