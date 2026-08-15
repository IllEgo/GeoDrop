package com.kitheapp.util

import android.util.Log
import com.kitheapp.BuildConfig
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Fail-closed production controls. A feature is enabled only when both the
 * signed build and the current Remote Config value allow it.
 */
object PilotFeatureFlags {
    const val CREATION_KEY = "pilot_creation_enabled"
    const val NOTIFICATIONS_KEY = "pilot_notifications_enabled"
    const val COUPONS_KEY = "pilot_coupons_enabled"
    const val MEDIA_KEY = "pilot_media_enabled"
    const val HUNTS_KEY = "pilot_hunts_enabled"
    const val REDESIGN_BACKEND_KEY = "pilot_redesign_backend_enabled"
    const val REDESIGN_MIN_CONTRACT_KEY = "pilot_redesign_min_contract_version"

    private const val TAG = "PilotFeatureFlags"
    private val started = AtomicBoolean(false)
    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            setDefaultsAsync(
                mapOf(
                    CREATION_KEY to false,
                    NOTIFICATIONS_KEY to false,
                    COUPONS_KEY to false,
                    MEDIA_KEY to false,
                    HUNTS_KEY to false,
                    REDESIGN_BACKEND_KEY to false,
                    REDESIGN_MIN_CONTRACT_KEY to Int.MAX_VALUE.toLong()
                )
            )
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(3_600)
                    .build()
            )
        }
    }

    val creationEnabled: Boolean
        get() = BuildConfig.FEATURE_CREATION_ENABLED && remoteConfig.getBoolean(CREATION_KEY)
    val notificationsEnabled: Boolean
        get() = BuildConfig.FEATURE_NOTIFICATIONS_ENABLED && remoteConfig.getBoolean(NOTIFICATIONS_KEY)
    val couponsEnabled: Boolean
        get() = BuildConfig.FEATURE_COUPONS_ENABLED && remoteConfig.getBoolean(COUPONS_KEY)
    val mediaEnabled: Boolean
        get() = BuildConfig.FEATURE_MEDIA_ENABLED && remoteConfig.getBoolean(MEDIA_KEY)
    val huntsEnabled: Boolean
        get() = BuildConfig.FEATURE_HUNTS_ENABLED && remoteConfig.getBoolean(HUNTS_KEY)
    val redesignBackendEnabled: Boolean
        get() = R6RolloutPolicy.isTargetBackendUsable(
            enabled = remoteConfig.getBoolean(REDESIGN_BACKEND_KEY),
            minimumContractVersion = remoteConfig.getLong(REDESIGN_MIN_CONTRACT_KEY)
        )

    fun start() {
        if (!started.compareAndSet(false, true)) return
        remoteConfig.fetchAndActivate().addOnFailureListener { error ->
            Log.w(TAG, "Remote Config refresh failed; retaining fail-closed cached values.", error)
        }
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                remoteConfig.activate().addOnFailureListener { error ->
                    Log.w(TAG, "Remote Config activation failed; retaining previous values.", error)
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.w(TAG, "Remote Config realtime listener failed.", error)
            }
        })
    }
}
