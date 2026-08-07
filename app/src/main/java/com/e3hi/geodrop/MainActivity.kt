package com.e3hi.geodrop

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import android.util.Log
import com.e3hi.geodrop.data.FirestoreRepo
import com.e3hi.geodrop.ui.DropHereScreen
import com.e3hi.geodrop.ui.GhostSplashScreen
import com.e3hi.geodrop.ui.theme.GeoDropTheme
import com.e3hi.geodrop.util.GroupPreferences
import com.e3hi.geodrop.util.MessagingTokenStore
import com.e3hi.geodrop.util.NotificationPreferences
import com.e3hi.geodrop.util.PilotFeatureFlags
import com.e3hi.geodrop.util.createNotificationChannelIfNeeded
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var authListener: AuthStateListener? = null
    private val firestoreRepo by lazy { FirestoreRepo() }
    private val messagingTokenStore by lazy { MessagingTokenStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PilotFeatureFlags.start()
        createNotificationChannelIfNeeded(this)

        val notificationPrefs = NotificationPreferences(this)

        authListener = AuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                notificationPrefs.setActiveUser(null)
                messagingTokenStore.clearSynced()
                return@AuthStateListener
            }

            notificationPrefs.setActiveUser(currentUser.uid)
            if (!PilotFeatureFlags.notificationsEnabled) {
                notificationPrefs.setNearbyAlertsEnabled(false)
                messagingTokenStore.clearSynced()
            } else if (notificationPrefs.areNearbyAlertsEnabled() && hasNearbyAlertPermissions()) {
                enableNearbyAlerts(currentUser.uid)
            }
        }
        authListener?.let { auth.addAuthStateListener(it) }

        setContent {
            GeoDropTheme {
                var splashDone by remember { mutableStateOf(false) }
                Crossfade(
                    targetState = splashDone,
                    animationSpec = tween(300),
                    label = "SplashToMain"
                ) { done ->
                    if (done) {
                        DropHereScreen(
                            onNearbyAlertsEnabled = {
                                auth.currentUser?.uid?.let { userId ->
                                    enableNearbyAlerts(userId)
                                }
                            },
                            onNearbyAlertsDisabled = {
                                messagingTokenStore.clearSynced()
                            }
                        )
                    } else {
                        GhostSplashScreen(onFinished = { splashDone = true })
                    }
                }
            }
        }

        // 🔎 Debug log to confirm Firebase is connected
        val opts = FirebaseApp.getInstance().options
        Log.d("GeoDrop", "Firebase projectId=${opts.projectId}, appId=${opts.applicationId}")
    }

    /**
     * Task 3.4 — alerts are membership-scoped now, sent by the server when a drop is
     * added to an experience the user joined. No location permission of any kind is
     * involved; the device only needs to be able to show a notification.
     */
    private fun hasNearbyAlertPermissions(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun enableNearbyAlerts(userId: String) {
        if (!PilotFeatureFlags.notificationsEnabled) return
        if (!hasNearbyAlertPermissions()) return
        lifecycleScope.launch {
            ensureMessagingTokenRegistered(userId)
        }
    }

    private suspend fun ensureMessagingTokenRegistered(userId: String) {
        if (!PilotFeatureFlags.notificationsEnabled) return
        if (userId.isBlank()) return

        val token = runCatching { Firebase.messaging.token.await() }
            .onFailure { error ->
                Log.w("GeoDrop", "Failed to fetch messaging token", error)
            }
            .getOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return

        messagingTokenStore.saveToken(token)
        if (messagingTokenStore.lastSyncedToken() == token) return

        runCatching {
            firestoreRepo.registerMessagingToken(userId, token)
            messagingTokenStore.markSynced(token)
        }.onFailure { error ->
            Log.e("GeoDrop", "Failed to register messaging token for $userId", error)
        }
    }

    override fun onDestroy() {
        authListener?.let { auth.removeAuthStateListener(it) }
        authListener = null
        super.onDestroy()
    }
}
