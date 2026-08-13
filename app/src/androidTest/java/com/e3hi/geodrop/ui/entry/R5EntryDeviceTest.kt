package com.e3hi.geodrop.ui.entry

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.e3hi.geodrop.data.R5EntryChannel
import com.e3hi.geodrop.data.R5EntryGateway
import com.e3hi.geodrop.data.R5EntryRequest
import com.e3hi.geodrop.data.R5ExperienceAvailability
import com.e3hi.geodrop.data.R5ExperienceMembership
import com.e3hi.geodrop.data.R5ExperiencePreview
import com.e3hi.geodrop.ui.theme.GeoDropTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class R5EntryDeviceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun installedQrShowsPreviewBeforeAccountAndPermissions() {
        composeRule.setContent {
            GeoDropTheme {
                R5EntryFlow(
                    initialRequest = R5EntryRequest(
                        code = "ABCDEFGH",
                        entrySessionId = "0123456789abcdef",
                        channel = R5EntryChannel.QR
                    ),
                    gateway = DeviceEntryGateway,
                    onRequestResolved = {},
                    onClearRequest = {},
                    onEntered = { _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Hilo Garden Walk").assertIsDisplayed()
        composeRule.onNodeWithText("Start exploring").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithText("Sign in").assertDoesNotExist()
        composeRule.onNodeWithText("Allow while using the app").assertDoesNotExist()
    }
}

private object DeviceEntryGateway : R5EntryGateway {
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

    override suspend fun ensureGuestSession(entrySessionId: String) = Unit
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
