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
import com.e3hi.geodrop.BuildConfig
import com.e3hi.geodrop.data.FirestoreRepo
import com.e3hi.geodrop.ui.DropHereScreen
import com.e3hi.geodrop.ui.GhostSplashScreen
import com.e3hi.geodrop.ui.theme.GeoDropTheme
import com.e3hi.geodrop.util.GoogleVisionSafeSearchEvaluator
import com.e3hi.geodrop.util.GroupPreferences
import com.e3hi.geodrop.util.MessagingTokenStore
import com.e3hi.geodrop.util.NotificationPreferences
import com.e3hi.geodrop.util.PilotFeatureFlags
import com.e3hi.geodrop.util.createNotificationChannelIfNeeded
import com.e3hi.geodrop.geo.NearbyDropRegistrar
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var authListener: AuthStateListener? = null
    private val registrar = NearbyDropRegistrar()
    private val firestoreRepo by lazy { FirestoreRepo() }
    private val messagingTokenStore by lazy { MessagingTokenStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PilotFeatureFlags.start()
        createNotificationChannelIfNeeded(this)

        val groupPrefs = GroupPreferences(this)
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
                registrar.unregisterNearby(this)
            } else if (notificationPrefs.areNearbyAlertsEnabled() && hasNearbyAlertPermissions()) {
                enableNearbyAlerts(
                    currentUser.uid,
                    notificationPrefs.getNotificationRadiusMeters(),
                    groupPrefs.getMemberships().map { it.code }.toSet()
                )
            }
        }
        authListener?.let { auth.addAuthStateListener(it) }

        setContent {
            val safeSearchCallable = remember {
                val functions = FirebaseFunctions.getInstance(BuildConfig.FIREBASE_FUNCTIONS_REGION)
                GoogleVisionSafeSearchEvaluator.SafeSearchCallable { payload ->
                    val result = functions
                        .getHttpsCallable("safeSearch")
                        .call(mapOf("base64" to payload))
                        .await()
                    @Suppress("UNCHECKED_CAST")
                    result.data as? Map<String, Any?>
                }
            }
            val dropSafetyEvaluator = remember {
                GoogleVisionSafeSearchEvaluator(
                    safeSearchCallable = safeSearchCallable
                )
            }
            GeoDropTheme {
                var splashDone by remember { mutableStateOf(false) }
                Crossfade(
                    targetState = splashDone,
                    animationSpec = tween(300),
                    label = "SplashToMain"
                ) { done ->
                    if (done) {
                        DropHereScreen(
                            dropSafetyEvaluator = dropSafetyEvaluator,
                            onNearbyAlertsEnabled = { radius, groups ->
                                auth.currentUser?.uid?.let { userId ->
                                    enableNearbyAlerts(userId, radius, groups)
                                }
                            },
                            onNearbyAlertsDisabled = {
                                registrar.unregisterNearby(this)
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

    private fun hasNearbyAlertPermissions(): Boolean {
        val foregroundGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        val backgroundGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        return foregroundGranted && notificationsGranted && backgroundGranted
    }

    private fun enableNearbyAlerts(userId: String, radius: Double, groups: Set<String>) {
        if (!PilotFeatureFlags.notificationsEnabled) {
            registrar.unregisterNearby(this)
            return
        }
        if (!hasNearbyAlertPermissions()) return
        lifecycleScope.launch {
            ensureMessagingTokenRegistered(userId)
        }
        registrar.registerNearby(
            this,
            maxMeters = radius,
            groupCodes = groups
        )
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
