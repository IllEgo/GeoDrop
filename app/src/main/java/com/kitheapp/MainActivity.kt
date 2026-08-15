package com.kitheapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import android.util.Log
import com.kitheapp.data.FirestoreRepo
import com.kitheapp.data.DebugDemoR5EntryGateway
import com.kitheapp.data.DebugDemoLegalConsentGateway
import com.kitheapp.data.DebugDemoR6ParticipantGateway
import com.kitheapp.data.DebugDemoR7OrganizerGateway
import com.kitheapp.data.DebugDemoR9AccountGateway
import com.kitheapp.data.DebugDemoExperienceStore
import com.kitheapp.data.FirebaseR5EntryGateway
import com.kitheapp.data.R5EntryRequest
import com.kitheapp.data.GroupMembership
import com.kitheapp.data.GroupRole
import com.kitheapp.ui.entry.R5AppRoot
import com.kitheapp.ui.theme.GeoDropTheme
import com.kitheapp.util.GroupPreferences
import com.kitheapp.util.MessagingTokenStore
import com.kitheapp.util.NotificationPreferences
import com.kitheapp.util.PilotFeatureFlags
import com.kitheapp.util.PlayInstallReferrerSource
import com.kitheapp.util.R5EntryParser
import com.kitheapp.util.R5EntryStore
import com.kitheapp.util.createNotificationChannelIfNeeded
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
    private val r5EntryStore by lazy { R5EntryStore(this) }
    private val debugExperienceStore by lazy {
        if (BuildConfig.DEBUG) DebugDemoExperienceStore() else null
    }
    private val debugEntryGateway by lazy {
        debugExperienceStore?.let { store ->
            DebugDemoR5EntryGateway(FirebaseR5EntryGateway(), store)
        }
    }
    private val debugParticipantGateway by lazy {
        debugExperienceStore?.let(::DebugDemoR6ParticipantGateway)
    }
    private val debugOrganizerGateway by lazy {
        debugExperienceStore?.let(::DebugDemoR7OrganizerGateway)
    }
    private val debugAccountGateway by lazy {
        debugExperienceStore?.let(::DebugDemoR9AccountGateway)
    }
    private var incomingEntryRequest by mutableStateOf<R5EntryRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PilotFeatureFlags.start()
        createNotificationChannelIfNeeded(this)
        acceptEntryIntent(intent)

        if (
            BuildConfig.DEBUG &&
            r5EntryStore.activeExperienceCode() == DebugDemoR5EntryGateway.DEVICE_DEMO_CODE
        ) {
            GroupPreferences(this).addGroup(
                GroupMembership(
                    code = DebugDemoR5EntryGateway.DEVICE_DEMO_CODE,
                    ownerId = null,
                    role = GroupRole.SUBSCRIBER
                )
            )
        }

        if (
            incomingEntryRequest == null &&
            r5EntryStore.pendingEntry() == null &&
            r5EntryStore.shouldReadInstallReferrer()
        ) {
            lifecycleScope.launch {
                val rawReferrer = PlayInstallReferrerSource(this@MainActivity).readOnce()
                r5EntryStore.markInstallReferrerRead()
                if (incomingEntryRequest == null && r5EntryStore.pendingEntry() == null) {
                    R5EntryParser.fromInstallReferrer(rawReferrer)?.let { request ->
                        r5EntryStore.savePendingEntry(request)
                        incomingEntryRequest = request
                    }
                }
            }
        }

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
                R5AppRoot(
                    incomingRequest = incomingEntryRequest,
                    onIncomingRequestConsumed = { incomingEntryRequest = null },
                    onNearbyAlertsEnabled = {
                        auth.currentUser?.uid?.let { userId ->
                            enableNearbyAlerts(userId)
                        }
                    },
                    onNearbyAlertsDisabled = {
                        auth.currentUser?.uid?.let { userId ->
                            disableNearbyAlerts(userId)
                        } ?: messagingTokenStore.clearSynced()
                    },
                    gatewayOverride = debugEntryGateway,
                    r6GatewayOverride = debugParticipantGateway,
                    r7GatewayOverride = debugOrganizerGateway?.takeIf {
                        r5EntryStore.activeExperienceCode() ==
                            DebugDemoR5EntryGateway.DEVICE_DEMO_CODE
                    },
                    r9GatewayOverride = debugAccountGateway?.takeIf {
                        r5EntryStore.activeExperienceCode() ==
                            DebugDemoR5EntryGateway.DEVICE_DEMO_CODE
                    },
                    legalConsentGatewayOverride = if (BuildConfig.DEBUG) {
                        DebugDemoLegalConsentGateway
                    } else {
                        null
                    },
                    debugDeviceDemoEnabled = BuildConfig.DEBUG
                )
            }
        }

        // 🔎 Debug log to confirm Firebase is connected
        val opts = FirebaseApp.getInstance().options
        Log.d("GeoDrop", "Firebase projectId=${opts.projectId}, appId=${opts.applicationId}")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptEntryIntent(intent)
    }

    private fun acceptEntryIntent(intent: Intent?) {
        R5EntryParser.fromAppLink(intent?.data, BuildConfig.APP_LINK_HOST)?.let { request ->
            r5EntryStore.savePendingEntry(request)
            incomingEntryRequest = request
        }
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
            runCatching { firestoreRepo.setExperienceAlertsEnabled(userId, true) }
                .onFailure { error ->
                    Log.w("GeoDrop", "Failed to record the alert opt-in", error)
                }
        }
    }

    /**
     * Task 4.5 — turning alerts off has to reach the server twice over.
     *
     * The token goes so FCM stops delivering to this device, and the preference is
     * written so the trigger skips this user even when another device still holds
     * a token. Clearing only the local sync marker, as this used to, left both the
     * send and the delivery intact.
     */
    private fun disableNearbyAlerts(userId: String) {
        val token = messagingTokenStore.lastSyncedToken()
        messagingTokenStore.clearSynced()
        lifecycleScope.launch {
            runCatching { firestoreRepo.setExperienceAlertsEnabled(userId, false) }
                .onFailure { error ->
                    Log.w("GeoDrop", "Failed to record the alert opt-out", error)
                }
            if (!token.isNullOrBlank()) {
                runCatching { firestoreRepo.unregisterMessagingToken(userId, token) }
                    .onFailure { error ->
                        Log.w("GeoDrop", "Failed to remove the messaging token", error)
                    }
            }
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
