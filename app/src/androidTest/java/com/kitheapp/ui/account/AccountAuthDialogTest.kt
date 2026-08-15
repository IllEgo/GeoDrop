package com.kitheapp.ui.account

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kitheapp.ui.theme.GeoDropTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountAuthDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun guestSignInUsesTheUnifiedAccountDesign() {
        composeRule.setContent {
            GeoDropTheme {
                var register by remember { mutableStateOf(false) }
                AccountAuthDialog(
                    unlockGate = false,
                    isRegister = register,
                    onRegisterChanged = { register = it },
                    isGuestUpgrade = true,
                    showOrganizerGuidance = false,
                    email = TextFieldValue(),
                    onEmailChange = {},
                    password = TextFieldValue(),
                    onPasswordChange = {},
                    confirmPassword = TextFieldValue(),
                    onConfirmPasswordChange = {},
                    username = TextFieldValue(),
                    onUsernameChange = {},
                    isSubmitting = false,
                    isGoogleSigningIn = false,
                    error = null,
                    status = null,
                    onSubmit = {},
                    onDismiss = {},
                    onForgotPassword = {},
                    onGoogleSignIn = {}
                )
            }
        }

        composeRule.onAllNodesWithText("Sign in").assertCountEquals(2)
        composeRule.onNodeWithText("Keep your finds with you").assertIsDisplayed()
        composeRule.onNodeWithText("Email address").assertIsDisplayed()
        composeRule.onNodeWithText("Password").assertIsDisplayed()
        composeRule.onNodeWithText("Continue with Google").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Keep browsing as a guest").assertIsDisplayed()
        composeRule.onAllNodesWithText("Participant").assertCountEquals(0)
        composeRule.onAllNodesWithText("Organizer").assertCountEquals(0)
        composeRule.onAllNodesWithText("Cancel").assertCountEquals(0)

        composeRule.onNodeWithText("Create account").performScrollTo().performClick()
        composeRule.onNodeWithText("Confirm password").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Username").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun primaryGuestActionsRemainVisibleAtTwoHundredPercentText() {
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = currentDensity.density,
                    fontScale = 2f
                )
            ) {
                GeoDropTheme {
                    AccountAuthDialog(
                        unlockGate = false,
                        isRegister = false,
                        onRegisterChanged = {},
                        isGuestUpgrade = true,
                        showOrganizerGuidance = false,
                        email = TextFieldValue(),
                        onEmailChange = {},
                        password = TextFieldValue(),
                        onPasswordChange = {},
                        confirmPassword = TextFieldValue(),
                        onConfirmPasswordChange = {},
                        username = TextFieldValue(),
                        onUsernameChange = {},
                        isSubmitting = false,
                        isGoogleSigningIn = false,
                        error = null,
                        status = null,
                        onSubmit = {},
                        onDismiss = {},
                        onForgotPassword = {},
                        onGoogleSignIn = {}
                    )
                }
            }
        }

        composeRule.onAllNodesWithText("Sign in")[1].assertIsDisplayed()
        composeRule.onNodeWithText("Keep browsing as a guest").assertIsDisplayed()
        composeRule.onNodeWithText("Email address").performScrollTo().assertIsDisplayed()
    }
}
