package com.e3hi.geodrop.ui.participant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.e3hi.geodrop.data.R6CollectionReceipt
import com.e3hi.geodrop.data.R6ContentKind
import com.e3hi.geodrop.data.R6DiscoveryState
import com.e3hi.geodrop.data.R6DropDiscovery
import com.e3hi.geodrop.data.R6DropKind
import com.e3hi.geodrop.data.R6ExpiryMode
import com.e3hi.geodrop.data.R6PayloadSnapshot
import com.e3hi.geodrop.data.R6RewardReceipt
import com.e3hi.geodrop.ui.theme.GeoDropTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class R6ParticipantDeviceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listPathExposesLockedContentStateAndUnlockAction() {
        var unlockRequested = false
        val drop = discovery()
        composeRule.setContent {
            GeoDropTheme {
                var selectedId by remember { mutableStateOf<String?>(null) }
                R6NearbyContent(
                    loading = false,
                    refreshing = false,
                    error = null,
                    items = listOf(
                        R6DiscoveryPresentation(drop, R6DiscoveryState.NEAR, "Nearby")
                    ),
                    selectedDropId = selectedId,
                    unlockingDropId = null,
                    trailProgress = null,
                    currentLocation = null,
                    approximateLocationEnabled = false,
                    topPadding = 0.dp,
                    unlockResult = null,
                    unlockError = null,
                    onSelect = { selectedId = it?.id },
                    onUnlock = { unlockRequested = true },
                    onRequestLocation = {},
                    onRefresh = {},
                    onDismissUnlockResult = {},
                    onReport = { _, _, _ -> },
                    onBlockHost = {},
                    initialBrowseMode = R6BrowseMode.LIST
                )
            }
        }

        composeRule.onNodeWithText("Hidden note").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("The content stays private until the server confirms this unlock.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Unlock").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertTrue(unlockRequested) }
    }

    @Test
    fun collectionKeepsExperienceRewardAndReportReachable() {
        var reportedReason: String? = null
        composeRule.setContent {
            GeoDropTheme {
                R6CollectionContent(
                    loading = false,
                    error = null,
                    receipts = listOf(receipt()),
                    topPadding = 0.dp,
                    onRefresh = {},
                    onReport = { _, reason, _ -> reportedReason = reason }
                )
            }
        }

        composeRule.onNodeWithText("Garden clue").assertIsDisplayed()
        composeRule.onNodeWithText("SECRET-7").assertIsDisplayed()
        composeRule.onNodeWithText("Ready to use").assertIsDisplayed()
        composeRule.onNodeWithText("Report").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Spam or misleading").performClick()
        composeRule.onNodeWithText("Send report").performClick()
        composeRule.runOnIdle { assertEquals("SPAM", reportedReason) }
    }

    private fun discovery() = R6DropDiscovery(
        id = "drop-1",
        experienceCode = "ABCD1234",
        ownerId = "host-1",
        hostLabel = "Garden host",
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

    private fun receipt() = R6CollectionReceipt(
        receiptId = "receipt-1",
        dropId = "drop-1",
        experienceCode = "ABCD1234",
        unlockedAtMillis = 1_700_000_000_000,
        payloadVersion = 1,
        snapshot = R6PayloadSnapshot(
            title = "Garden clue",
            body = "Look beneath the palms.",
            contentKind = R6ContentKind.TEXT,
            hostLabel = "Garden host",
            mediaAssetId = null,
            mediaMimeType = null,
            mediaAltText = null,
            rewardPresentation = mapOf("instructions" to "Show this code at the desk."),
            editedAtMillis = null
        ),
        trail = null,
        hasRewardReceipt = true,
        reward = R6RewardReceipt(
            receiptId = "reward-1",
            dropId = "drop-1",
            experienceCode = "ABCD1234",
            code = "SECRET-7",
            state = "ISSUED",
            issuedAtMillis = 1_700_000_000_000,
            usedAtMillis = null
        )
    )
}
