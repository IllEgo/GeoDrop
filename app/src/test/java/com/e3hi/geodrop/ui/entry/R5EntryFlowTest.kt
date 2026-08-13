package com.e3hi.geodrop.ui.entry

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.e3hi.geodrop.data.R5EntryGateway
import com.e3hi.geodrop.data.R5EntryRequest
import com.e3hi.geodrop.data.R5ExperienceAvailability
import com.e3hi.geodrop.data.R5ExperienceMembership
import com.e3hi.geodrop.data.R5ExperiencePreview
import com.e3hi.geodrop.ui.components.NoOpFirebaseInitProviderShadow
import com.e3hi.geodrop.ui.components.R3TestApplication
import com.e3hi.geodrop.ui.theme.GeoDropTheme
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
                        channel = com.e3hi.geodrop.data.R5EntryChannel.QR
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
