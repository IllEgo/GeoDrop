package com.e3hi.geodrop.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.e3hi.geodrop.data.R7OrganizerAccessState
import com.e3hi.geodrop.data.R7OrganizerAccessStatus
import com.e3hi.geodrop.data.R9ExperienceAvailability
import com.e3hi.geodrop.data.R9JoinedExperience
import com.e3hi.geodrop.data.R9ReportState
import com.e3hi.geodrop.data.R9ReportStatus
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
class R4NavigationAccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun participantNavigationExposesOnlyTheThreeApprovedTabs() {
        var selected by mutableStateOf(ParticipantDestination.NEARBY)
        composeRule.setContent {
            GeoDropTheme {
                GeoDropParticipantNavigationBar(
                    selected = selected,
                    onSelect = { selected = it }
                )
            }
        }

        composeRule.onNodeWithText("Nearby").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Collection").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(ParticipantDestination.COLLECTION, selected) }
        composeRule.onNodeWithText("Account").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(ParticipantDestination.ACCOUNT, selected) }
        composeRule.onNodeWithText("Drop something").assertDoesNotExist()
        composeRule.onNodeWithText("Manage groups").assertDoesNotExist()
        composeRule.onNodeWithText("Organizer").assertDoesNotExist()
    }

    @Test
    fun experienceSwitcherAndJoinActionRemainReachable() {
        var selectedCode by mutableStateOf("ALPHA")
        var joinRequests = 0
        composeRule.setContent {
            GeoDropTheme {
                GeoDropExperienceTopBar(
                    experiences = listOf(
                        ExperienceNavigationItem(code = "ALPHA", isOwned = false),
                        ExperienceNavigationItem(code = "BETA", isOwned = true)
                    ),
                    activeCode = selectedCode,
                    onSelectExperience = { selectedCode = it },
                    onJoinExperience = { joinRequests += 1 }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Switch Experience").performClick()
        composeRule.onNodeWithText("Experience BETA").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals("BETA", selectedCode) }

        composeRule.onNodeWithContentDescription("Switch Experience").performClick()
        composeRule.onNodeWithText("Join another Experience").performClick()
        composeRule.runOnIdle { assertEquals(1, joinRequests) }
    }

    @Test
    fun noExperienceStateProvidesTheRequiredJoinPath() {
        var joinRequests = 0
        composeRule.setContent {
            GeoDropTheme {
                Surface(Modifier.fillMaxSize()) {
                    GeoDropNoExperienceState(onJoinExperience = { joinRequests += 1 })
                }
            }
        }

        composeRule.onNodeWithText("Join your first Experience").assertIsDisplayed()
        composeRule.onNodeWithText("Join an Experience")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(1, joinRequests) }
    }

    @Test
    fun participantJoinSurfaceCannotCreateOrRemoveExperiences() {
        var joinedCode: String? = null
        composeRule.setContent {
            GeoDropTheme {
                GeoDropJoinExperienceDialog(
                    snackbarHostState = SnackbarHostState(),
                    onDismiss = {},
                    onJoin = { joinedCode = it }
                )
            }
        }

        composeRule.onNodeWithText("Join an Experience").assertIsDisplayed()
        composeRule.onNodeWithText("Create group").assertDoesNotExist()
        composeRule.onNodeWithText("Remove").assertDoesNotExist()
        composeRule.onNodeWithText("Experience code").performTextInput("alpha")
        composeRule.onNodeWithText("Join")
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals("ALPHA", joinedCode) }
    }

    @Test
    fun organizerActionIsAbsentUntilApprovedAndThenLivesInsideAccount() {
        var approved by mutableStateOf(false)
        var organizerOpenRequests = 0
        composeRule.setContent {
            GeoDropTheme {
                Surface(Modifier.fillMaxSize()) {
                    GeoDropAccountDestination(
                        identityLabel = "Kai",
                        identitySupportingText = "kai@example.com",
                        isGuest = false,
                        locationGranted = true,
                        notificationsGranted = false,
                        joinedExperiences = listOf(
                            ExperienceNavigationItem(code = "ALPHA", isOwned = false)
                        ),
                        experienceHistory = emptyList(),
                        reportStatuses = emptyList(),
                        blockedHostCount = 0,
                        accountDetailsLoading = false,
                        accountDetailsError = null,
                        organizerAccessState = R7OrganizerAccessState(
                            status = if (approved) {
                                R7OrganizerAccessStatus.APPROVED
                            } else {
                                R7OrganizerAccessStatus.NOT_APPLIED
                            }
                        ),
                        signingOut = false,
                        onSignIn = {},
                        onEditProfile = {},
                        onOpenLocationSettings = {},
                        onOpenNotificationSettings = {},
                        onOpenOrganizerAccess = {},
                        onOpenOrganizerTools = { organizerOpenRequests += 1 },
                        onOpenBlockedCreators = {},
                        onOpenData = {},
                        onRetryAccountDetails = {},
                        onSignOut = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("Organizer access").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Open Organizer tools").assertDoesNotExist()

        composeRule.runOnIdle { approved = true }
        composeRule.onNodeWithText("Open Organizer tools")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(1, organizerOpenRequests) }
        composeRule.onNodeWithText("Experience ALPHA").assertExists()
    }

    @Test
    fun accountShowsDetailedExperienceAndSafetyHistoryWithoutRetiredIdentityCopy() {
        composeRule.setContent {
            GeoDropTheme {
                Surface(Modifier.fillMaxSize()) {
                    GeoDropAccountDestination(
                        identityLabel = "Kai",
                        identitySupportingText = "kai@example.com",
                        isGuest = false,
                        locationGranted = true,
                        notificationsGranted = true,
                        joinedExperiences = emptyList(),
                        experienceHistory = listOf(
                            R9JoinedExperience(
                                code = "ALPHA",
                                name = "Hilo garden walk",
                                hostLabel = "Island host",
                                startsAtMillis = 1_786_425_600_000L,
                                endsAtMillis = 1_786_512_000_000L,
                                timeZone = "Pacific/Honolulu",
                                availability = R9ExperienceAvailability.ACTIVE,
                                isOwned = false
                            )
                        ),
                        reportStatuses = listOf(
                            R9ReportStatus(
                                reportId = "report-1",
                                dropId = "drop-1",
                                state = R9ReportState.RECEIVED,
                                updatedAtMillis = 1_786_425_600_000L
                            )
                        ),
                        blockedHostCount = 1,
                        accountDetailsLoading = false,
                        accountDetailsError = null,
                        organizerAccessState = R7OrganizerAccessState(),
                        signingOut = false,
                        onSignIn = {},
                        onEditProfile = {},
                        onOpenLocationSettings = {},
                        onOpenNotificationSettings = {},
                        onOpenOrganizerAccess = {},
                        onOpenOrganizerTools = {},
                        onOpenBlockedCreators = {},
                        onOpenData = {},
                        onRetryAccountDetails = {},
                        onSignOut = {}
                    )
                }
            }
        }

        composeRule.onNode(hasScrollAction()).performScrollToIndex(3)
        composeRule.onNodeWithText("Joined Experiences").assertIsDisplayed()
        composeRule.onNodeWithText("Hilo garden walk").assertIsDisplayed()
        composeRule.onNodeWithText("Active").assertExists()
        composeRule.onNode(hasScrollAction()).performScrollToIndex(4)
        composeRule.onNodeWithText("Safety").assertIsDisplayed()
        composeRule.onNodeWithText("Received").assertIsDisplayed()
        composeRule.onNodeWithText("Blocked hosts (1)").assertIsDisplayed()
        composeRule.onNodeWithText("Explorer account").assertDoesNotExist()
        composeRule.onNodeWithText("Business account").assertDoesNotExist()
    }

    @Test
    fun saveableStateSurvivesTabAndExperienceSwitches() {
        var destination by mutableStateOf(ParticipantDestination.NEARBY)
        var activeCode by mutableStateOf("ALPHA")
        composeRule.setContent {
            GeoDropTheme {
                Column {
                    Button(onClick = { destination = ParticipantDestination.NEARBY }) {
                        Text("Show Nearby")
                    }
                    Button(onClick = { destination = ParticipantDestination.COLLECTION }) {
                        Text("Show Collection")
                    }
                    Button(onClick = { activeCode = "ALPHA" }) { Text("Use ALPHA") }
                    Button(onClick = { activeCode = "BETA" }) { Text("Use BETA") }
                    GeoDropParticipantStateHost(destination, activeCode) {
                        var count by rememberSaveable { mutableStateOf(0) }
                        Text("${R4NavigationPolicy.stateKey(destination, activeCode)}:$count")
                        Button(onClick = { count += 1 }) { Text("Increment current") }
                    }
                }
            }
        }

        composeRule.onNodeWithText("Increment current").performClick()
        composeRule.onNodeWithText("nearby:ALPHA:1").assertIsDisplayed()

        composeRule.onNodeWithText("Show Collection").performClick()
        composeRule.onNodeWithText("collection:0").assertIsDisplayed()
        composeRule.onNodeWithText("Increment current").performClick()
        composeRule.onNodeWithText("collection:1").assertIsDisplayed()

        composeRule.onNodeWithText("Show Nearby").performClick()
        composeRule.onNodeWithText("nearby:ALPHA:1").assertIsDisplayed()
        composeRule.onNodeWithText("Use BETA").performClick()
        composeRule.onNodeWithText("nearby:BETA:0").assertIsDisplayed()
        composeRule.onNodeWithText("Use ALPHA").performClick()
        composeRule.onNodeWithText("nearby:ALPHA:1").assertIsDisplayed()
    }
}
