package com.e3hi.geodrop

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.lifecycleScope
import android.util.Log
import com.e3hi.geodrop.BuildConfig
import com.e3hi.geodrop.data.FirestoreRepo
import com.e3hi.geodrop.ui.DropHereScreen
import com.e3hi.geodrop.ui.theme.GeoDropTheme
import com.e3hi.geodrop.util.GoogleVisionSafeSearchEvaluator
import com.e3hi.geodrop.util.GroupPreferences
import com.e3hi.geodrop.util.MessagingTokenStore
import com.e3hi.geodrop.util.NotificationPreferences
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

    private val requiredPermissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        // If you want true background geofence triggers, request this separately with rationale:
        // add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handle if needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannelIfNeeded(this)
        ensureRuntimePermissions()

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
            lifecycleScope.launch {
                ensureMessagingTokenRegistered(currentUser.uid)
            }

            registrar.registerNearby(
                this,
                maxMeters = notificationPrefs.getNotificationRadiusMeters(),
                groupCodes = groupPrefs.getMemberships().map { it.code }.toSet()
            )
        }
        authListener?.let { auth.addAuthStateListener(it) }

        setContent {
            val apiKey = remember { fetchVisionApiKey() }
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
            val dropSafetyEvaluator = remember(apiKey) {
                GoogleVisionSafeSearchEvaluator(
                    apiKey = apiKey,
                    safeSearchCallable = safeSearchCallable
                )
            }
            GeoDropTheme {
                DropHereScreen(dropSafetyEvaluator = dropSafetyEvaluator)
            }
        }

        // 🔎 Debug log to confirm Firebase is connected
        val opts = FirebaseApp.getInstance().options
        Log.d("GeoDrop", "Firebase projectId=${opts.projectId}, appId=${opts.applicationId}")
    }

    private fun ensureRuntimePermissions() {
        val need = requiredPermissions.any {
            ContextCompat.checkSelfPermission(this, it) != PermissionChecker.PERMISSION_GRANTED
        }
        if (need) permissionLauncher.launch(requiredPermissions)
    }

    private suspend fun ensureMessagingTokenRegistered(userId: String) {
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

    private fun fetchVisionApiKey(): String {
        return try {
            val field = BuildConfig::class.java.getField("GOOGLE_VISION_API_KEY")
            (field.get(null) as? String).orEmpty()
        } catch (noField: NoSuchFieldException) {
            Log.w(
                "GeoDrop",
                "GOOGLE_VISION_API_KEY missing from BuildConfig; NSFW detection will be disabled.",
                noField
            )
            ""
        } catch (illegal: IllegalAccessException) {
            Log.w(
                "GeoDrop",
                "Unable to access GOOGLE_VISION_API_KEY from BuildConfig; NSFW detection will be disabled.",
                illegal
            )
            ""
        }
    }

    override fun onDestroy() {
        authListener?.let { auth.removeAuthStateListener(it) }
        authListener = null
        super.onDestroy()
    }
}
