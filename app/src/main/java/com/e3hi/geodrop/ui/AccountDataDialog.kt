package com.e3hi.geodrop.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.e3hi.geodrop.R
import com.e3hi.geodrop.data.AccountDeletionReceipt
import com.e3hi.geodrop.data.AccountLifecycleRepo
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private enum class AccountDataAction { EXPORT, DELETE }

@Composable
fun AccountDataDialog(
    scope: CoroutineScope,
    onDismiss: () -> Unit,
    onDeleted: (AccountDeletionReceipt) -> Unit,
    repo: AccountLifecycleRepo = remember { AccountLifecycleRepo() }
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val user = auth.currentUser
    val passwordProvider = user?.providerData?.any {
        it.providerId == EmailAuthProvider.PROVIDER_ID
    } == true
    val googleClient = remember(context) {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, options)
    }

    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var exportUrl by remember { mutableStateOf<String?>(null) }
    var exportExpiry by remember { mutableStateOf<String?>(null) }
    var pendingGoogleAction by remember { mutableStateOf<AccountDataAction?>(null) }
    var googleLaunchNonce by remember { mutableStateOf(0) }

    suspend fun execute(action: AccountDataAction) {
        when (action) {
            AccountDataAction.EXPORT -> {
                val result = repo.requestExport()
                exportUrl = result.downloadUrl
                exportExpiry = result.expiresAt
            }
            AccountDataAction.DELETE -> {
                val receipt = repo.deleteAccount(confirmation)
                context.getSharedPreferences(
                    "geodrop_account_lifecycle",
                    android.content.Context.MODE_PRIVATE
                ).edit()
                    .putString("last_deletion_receipt_id", receipt.receiptId)
                    .putString("last_deletion_completed_at", receipt.completedAt)
                    .putString("last_deletion_policy_version", receipt.policyVersion)
                    .apply()
                onDeleted(receipt)
            }
        }
    }

    fun runReauthenticated(action: AccountDataAction) {
        if (working || user == null) return
        error = null
        if (!passwordProvider) {
            working = true
            pendingGoogleAction = action
            scope.launch {
                runCatching { googleClient.signOut().await() }
                googleLaunchNonce += 1
            }
            return
        }
        scope.launch {
            working = true
            try {
                val email = user.email
                    ?: throw IllegalStateException("Your account has no email address")
                val credential = EmailAuthProvider.getCredential(email, password)
                user.reauthenticate(credential).await()
                execute(action)
            } catch (throwable: Throwable) {
                error = throwable.localizedMessage ?: "Couldn't verify your identity."
            } finally {
                working = false
            }
        }
    }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val action = pendingGoogleAction
        pendingGoogleAction = null
        scope.launch {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).await()
                val idToken = account.idToken
                    ?: throw IllegalStateException("Google returned no ID token")
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val current = auth.currentUser
                    ?: throw IllegalStateException("Sign in to manage account data")
                current.reauthenticate(credential).await()
                if (action != null) execute(action)
            } catch (throwable: Throwable) {
                error = throwable.localizedMessage ?: "Google reauthentication failed."
            } finally {
                working = false
            }
        }
    }

    // Launch only after the Activity Result launcher has been initialized.
    if (pendingGoogleAction != null && working && googleLaunchNonce > 0) {
        androidx.compose.runtime.LaunchedEffect(googleLaunchNonce) {
            googleLauncher.launch(googleClient.signInIntent)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text("Your GeoDrop data") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (passwordProvider) "Enter your password before either action."
                    else "GeoDrop will ask you to sign in with Google again before either action.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (passwordProvider) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text("Export creates a private JSON download link that expires after 15 minutes.")
                Button(
                    onClick = { runReauthenticated(AccountDataAction.EXPORT) },
                    enabled = !working && (!passwordProvider || password.isNotBlank()),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Create data export") }
                exportUrl?.let { url ->
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Download export") }
                    exportExpiry?.let { Text("Link expires: $it", style = MaterialTheme.typography.bodySmall) }
                }
                Text(
                    "Deletion permanently removes your account, memberships, inventory, owned drops, and media.",
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it.uppercase() },
                    label = { Text("Type DELETE") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { runReauthenticated(AccountDataAction.DELETE) },
                    enabled = !working && confirmation == "DELETE" &&
                            (!passwordProvider || password.isNotBlank()),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Permanently delete account") }
                if (working) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                        Text("Working…")
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !working) { Text("Close") }
        }
    )
}
