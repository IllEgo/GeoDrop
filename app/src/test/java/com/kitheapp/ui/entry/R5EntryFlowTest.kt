package com.kitheapp.ui.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kitheapp.data.R5EntryGateway
import com.kitheapp.data.R5EntryFailureReason
import com.kitheapp.data.R5EntryRequest
import com.kitheapp.data.R5ExperienceAvailability
import com.kitheapp.data.R5ExperienceMembership
import com.kitheapp.data.R5ExperiencePreview
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
class R5EntryFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `link resolves to preview before account permission or map surfaces`() {
        val gateway = FakeEntryGateway()
        composeRule.setContent {
            GeoDropTheme {
                R5EntryFlow(
                    initialRequest = R5EntryRequest(
                        code = "ABCDEFGH",
                        entrySessionId = "0123456789abcdef",
                        channel = com.kitheapp.data.R5EntryChannel.QR
                    ),
                    gateway = gateway,
                    onRequestResolved = {},
                    onClearRequest = {},
                    onEntered = { _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Hilo Garden Walk").assertIsDisplayed()
        composeRule.onNodeWithText("Start exploring")
            .assertHeightIsAtLeast(56.dp)
            .assertIsEnabled()
        composeRule.onNodeWithText("Sign in").assertDoesNotExist()
        composeRule.onNodeWithText("Allow while using the app").assertDoesNotExist()
        composeRule.onNodeWithText("Browse map").assertDoesNotExist()
        assertEquals(1, gateway.guestSessionCalls)
    }

    @Test
    fun `manual presentation code is compacted for resolve`() {
        val gateway = FakeEntryGateway()
        var resolved: R5EntryRequest? = null
        composeRule.setContent {
            GeoDropTheme {
                R5EntryFlow(
                    initialRequest = null,
                    gateway = gateway,
                    onRequestResolved = { resolved = it },
                    onClearRequest = {},
                    onEntered = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText("Experience code").performTextInput("abcd-efgh")
        composeRule.onNodeWithText("Preview Experience").performClick()
        composeRule.waitUntil(5_000) { resolved != null }

        assertEquals("ABCDEFGH", resolved?.code)
        composeRule.onNodeWithText("Hilo Garden Walk").assertIsDisplayed()
    }

    @Test
    fun `compact preview keeps both actions reachable at two hundred percent font scale`() {
        val gateway = FakeEntryGateway()
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                GeoDropTheme {
                    Box(Modifier.width(320.dp)) {
                        R5EntryFlow(
                            initialRequest = R5EntryRequest(
                                code = "ABCDEFGH",
                                entrySessionId = "0123456789abcdef",
                                channel = com.kitheapp.data.R5EntryChannel.QR
                            ),
                            gateway = gateway,
                            onRequestResolved = {},
                            onClearRequest = {},
                            onEntered = { _, _ -> }
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Start exploring")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(56.dp)
        composeRule.onNodeWithText("Enter a different code")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `entry failures are assertive live regions with recovery`() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                GeoDropTheme {
                    Box(Modifier.width(320.dp)) {
                        R5EntryErrorContent(
                            reason = R5EntryFailureReason.OFFLINE,
                            retryable = true,
                            onRetry = {},
                            onDifferentCode = {}
                        )
                    }
                }
            }
        }

        composeRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Assertive
            )
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Try again")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Enter a different code")
            .performScrollTo()
            .assertIsDisplayed()
    }
}

private class FakeEntryGateway : R5EntryGateway {
    var guestSessionCalls = 0
    private val preview = R5ExperiencePreview(
        code = "ABCDEFGH",
        name = "Hilo Garden Walk",
        description = "Follow the garden path.",
        hostLabel = "Hawaiʻi Island Museum",
        startsAt = null,
        endsAt = null,
        timeZone = "Pacific/Honolulu",
        availability = R5ExperienceAvailability.ACTIVE,
        availableDropCount = 4,
        membership = R5ExperienceMembership.MEMBER
    )

    override suspend fun ensureGuestSession(entrySessionId: String) {
        guestSessionCalls += 1
    }

    override suspend fun resolve(request: R5EntryRequest): R5ExperiencePreview = preview

    override suspend fun join(request: R5EntryRequest): R5ExperiencePreview = preview

    override suspend fun recordAuthCompletion(
        entrySessionId: String,
        upgradePath: String?,
        pendingUnlockResumed: Boolean
    ) = Unit

    override suspend fun recordClientEvent(
        eventName: String,
        entrySessionId: String?,
        experienceCode: String?,
        dropId: String?,
        installKey: String?,
        params: Map<String, Any>
    ) = Unit
}
