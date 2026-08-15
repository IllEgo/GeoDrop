package com.kitheapp.ui.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kitheapp.ui.theme.GeoDropTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditProfileDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun editorKeepsIdentityFieldsAndOmitsLegacyExplorerPreferences() {
        var submitted = false
        composeRule.setContent {
            GeoDropTheme {
                var displayName by remember { mutableStateOf(TextFieldValue("Kai")) }
                var username by remember { mutableStateOf(TextFieldValue("kai.hawaii")) }
                EditProfileDialog(
                    displayNameField = displayName,
                    onDisplayNameChange = { displayName = it },
                    username = username,
                    onUsernameChange = { username = it },
                    isSubmitting = false,
                    error = null,
                    onSubmit = { submitted = true },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Edit profile").assertIsDisplayed()
        composeRule.onNodeWithText("Display name").assertIsDisplayed()
        composeRule.onNodeWithText("Username").assertIsDisplayed()
        composeRule.onAllNodesWithText("Explorer profile").assertCountEquals(0)
        composeRule.onAllNodesWithText("Default explorer destination").assertCountEquals(0)
        composeRule.onAllNodesWithText("Nearby alerts").assertCountEquals(0)
        composeRule.onAllNodesWithText("Upload avatar").assertCountEquals(0)

        composeRule.onNodeWithText("Save changes").performClick()
        composeRule.runOnIdle { assertTrue(submitted) }
    }

    @Test
    fun saveErrorIsShownInTheEditor() {
        composeRule.setContent {
            GeoDropTheme {
                EditProfileDialog(
                    displayNameField = TextFieldValue(),
                    onDisplayNameChange = {},
                    username = TextFieldValue("taken"),
                    onUsernameChange = {},
                    isSubmitting = false,
                    error = "That username is already taken. Try another one.",
                    onSubmit = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("That username is already taken. Try another one.")
            .assertIsDisplayed()
    }
}
