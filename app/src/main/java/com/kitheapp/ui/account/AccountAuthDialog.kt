package com.kitheapp.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kitheapp.R
import com.kitheapp.ui.theme.GeoDropSize
import com.kitheapp.ui.theme.GeoDropSpacing

/**
 * Full-screen account entry shared by the voluntary guest upgrade and first-Unlock gate.
 * Authentication behavior remains owned by the caller so guest LINK/MERGE continuity is
 * unchanged; this composable owns only presentation and input ergonomics.
 */
@Composable
fun AccountAuthDialog(
    unlockGate: Boolean,
    isRegister: Boolean,
    onRegisterChanged: (Boolean) -> Unit,
    isGuestUpgrade: Boolean,
    showOrganizerGuidance: Boolean,
    email: TextFieldValue,
    onEmailChange: (TextFieldValue) -> Unit,
    password: TextFieldValue,
    onPasswordChange: (TextFieldValue) -> Unit,
    confirmPassword: TextFieldValue,
    onConfirmPasswordChange: (TextFieldValue) -> Unit,
    username: TextFieldValue,
    onUsernameChange: (TextFieldValue) -> Unit,
    isSubmitting: Boolean,
    isGoogleSigningIn: Boolean,
    error: String?,
    status: String?,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    val isBusy = isSubmitting || isGoogleSigningIn
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    fun clearInputFocus() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }

    Dialog(
        onDismissRequest = {
            if (!isBusy) {
                clearInputFocus()
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = !isBusy,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding()
            ) {
                AccountAuthHeader(
                    title = stringResource(
                        when {
                            unlockGate -> R.string.account_auth_unlock_title
                            isRegister -> R.string.account_auth_create_title
                            else -> R.string.account_auth_sign_in_title
                        }
                    ),
                    isBusy = isBusy,
                    onClose = {
                        clearInputFocus()
                        onDismiss()
                    }
                )

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = GeoDropSpacing.screenGutter,
                            vertical = GeoDropSpacing.lg
                        ),
                    verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.lg)
                ) {
                    AccountAuthIntroduction(
                        unlockGate = unlockGate,
                        isRegister = isRegister
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.md)) {
                        Text(
                            text = stringResource(R.string.account_auth_details_heading),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() }
                        )

                        if (isRegister && !unlockGate) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = onUsernameChange,
                                label = {
                                    Text(stringResource(R.string.explorer_profile_username_label))
                                },
                                supportingText = {
                                    Text(stringResource(R.string.explorer_profile_hint))
                                },
                                singleLine = true,
                                enabled = !isBusy,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.None,
                                    autoCorrect = false,
                                    keyboardType = KeyboardType.Ascii,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = onEmailChange,
                            label = { Text(stringResource(R.string.account_auth_email_label)) },
                            singleLine = true,
                            enabled = !isBusy,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Next) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = onPasswordChange,
                            label = { Text(stringResource(R.string.account_auth_password_label)) },
                            singleLine = true,
                            enabled = !isBusy,
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                PasswordVisibilityButton(
                                    visible = passwordVisible,
                                    enabled = !isBusy,
                                    onClick = { passwordVisible = !passwordVisible }
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (isRegister) ImeAction.Next else ImeAction.Done
                            ),
                            keyboardActions = if (isRegister) {
                                KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                                )
                            } else {
                                KeyboardActions(
                                    onDone = {
                                        clearInputFocus()
                                        onSubmit()
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isRegister) {
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = onConfirmPasswordChange,
                                label = {
                                    Text(stringResource(R.string.account_auth_confirm_password_label))
                                },
                                singleLine = true,
                                enabled = !isBusy,
                                visualTransformation = if (confirmPasswordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    PasswordVisibilityButton(
                                        visible = confirmPasswordVisible,
                                        enabled = !isBusy,
                                        onClick = {
                                            confirmPasswordVisible = !confirmPasswordVisible
                                        }
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        clearInputFocus()
                                        onSubmit()
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (!isRegister) {
                            TextButton(
                                onClick = {
                                    clearInputFocus()
                                    onForgotPassword()
                                },
                                enabled = !isBusy,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(stringResource(R.string.account_auth_forgot_password))
                            }
                        }
                    }

                    AccountAuthMessage(error = error, status = status)

                    OutlinedButton(
                        onClick = {
                            clearInputFocus()
                            onGoogleSignIn()
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = GeoDropSize.minimumTouchTarget)
                    ) {
                        if (isGoogleSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.size(GeoDropSpacing.xs))
                            Text(stringResource(R.string.account_auth_connecting_google))
                        } else {
                            Text(stringResource(R.string.account_auth_google_action))
                        }
                    }

                    AccountAuthModeSwitch(
                        isRegister = isRegister,
                        isBusy = isBusy,
                        onRegisterChanged = {
                            passwordVisible = false
                            confirmPasswordVisible = false
                            onRegisterChanged(it)
                        }
                    )

                    if (showOrganizerGuidance && !unlockGate) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(GeoDropSpacing.md),
                                horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Rounded.AccountCircle, contentDescription = null)
                                Text(
                                    text = stringResource(R.string.account_auth_organizer_guidance),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            horizontal = GeoDropSpacing.screenGutter,
                            vertical = GeoDropSpacing.md
                        ),
                    verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs)
                ) {
                    Button(
                        onClick = {
                            clearInputFocus()
                            onSubmit()
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = GeoDropSize.minimumTouchTarget)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.size(GeoDropSpacing.xs))
                            Text(
                                stringResource(
                                    if (isRegister) R.string.account_auth_creating
                                    else R.string.account_auth_signing_in
                                )
                            )
                        } else {
                            Text(
                                stringResource(
                                    if (isRegister) R.string.account_auth_create_action
                                    else R.string.account_auth_sign_in_action
                                )
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            clearInputFocus()
                            onDismiss()
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = GeoDropSize.minimumTouchTarget)
                    ) {
                        Text(
                            stringResource(
                                if (isGuestUpgrade) R.string.account_auth_keep_browsing
                                else R.string.account_auth_back
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountAuthHeader(
    title: String,
    isBusy: Boolean,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = GeoDropSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onClose,
            enabled = !isBusy,
            modifier = Modifier.sizeIn(
                minWidth = GeoDropSize.minimumTouchTarget,
                minHeight = GeoDropSize.minimumTouchTarget
            )
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.account_auth_close)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        Spacer(Modifier.size(GeoDropSize.minimumTouchTarget))
    }
}

@Composable
private fun AccountAuthIntroduction(unlockGate: Boolean, isRegister: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(GeoDropSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.md),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (unlockGate) Icons.Rounded.Lock else Icons.Rounded.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xs),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(
                        when {
                            unlockGate -> R.string.r5_unlock_account_title
                            isRegister -> R.string.account_auth_create_heading
                            else -> R.string.account_auth_sign_in_heading
                        }
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = stringResource(
                        when {
                            unlockGate -> R.string.r5_unlock_account_body
                            isRegister -> R.string.account_auth_create_body
                            else -> R.string.account_auth_sign_in_body
                        }
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun PasswordVisibilityButton(
    visible: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.sizeIn(
            minWidth = GeoDropSize.minimumTouchTarget,
            minHeight = GeoDropSize.minimumTouchTarget
        )
    ) {
        Icon(
            imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            contentDescription = stringResource(
                if (visible) R.string.account_auth_hide_password
                else R.string.account_auth_show_password
            )
        )
    }
}

@Composable
private fun AccountAuthMessage(error: String?, status: String?) {
    val message = error ?: status ?: return
    val isError = error != null
    Surface(
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (isError) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = if (isError) LiveRegionMode.Assertive else LiveRegionMode.Polite
            }
    ) {
        Row(
            modifier = Modifier.padding(GeoDropSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(GeoDropSpacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
                contentDescription = null
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AccountAuthModeSwitch(
    isRegister: Boolean,
    isBusy: Boolean,
    onRegisterChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GeoDropSpacing.xxs)
    ) {
        Text(
            text = stringResource(
                if (isRegister) R.string.account_auth_existing_prompt
                else R.string.account_auth_new_prompt
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            onClick = { onRegisterChanged(!isRegister) },
            enabled = !isBusy,
            modifier = Modifier.heightIn(min = GeoDropSize.minimumTouchTarget)
        ) {
            Text(
                stringResource(
                    if (isRegister) R.string.account_auth_sign_in_action
                    else R.string.account_auth_create_action
                )
            )
        }
    }
}
