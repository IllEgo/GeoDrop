package com.e3hi.geodrop

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class GeoDropApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        val appCheck = FirebaseAppCheck.getInstance()
        val usePlayIntegrityAppCheck = resolvePlayIntegrityFlag()
        val shouldUseDebugProvider = BuildConfig.DEBUG || !usePlayIntegrityAppCheck
        val debugProviderInstalled = if (shouldUseDebugProvider) {
            tryInstallDebugProvider(appCheck)
        } else {
            false
        }

        if (!debugProviderInstalled && usePlayIntegrityAppCheck) {
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }

    private fun resolvePlayIntegrityFlag(): Boolean {
        return runCatching {
            BuildConfig::class.java.getField("USE_PLAY_INTEGRITY_APPCHECK").getBoolean(null)
        }.onFailure { error ->
            Log.w(
                TAG,
                "USE_PLAY_INTEGRITY_APPCHECK not present in BuildConfig; defaulting to false",
                error
            )
        }.getOrDefault(false)
    }

    private fun tryInstallDebugProvider(appCheck: FirebaseAppCheck): Boolean {
        val providerFactory = runCatching {
            val factoryClass = Class.forName(
                "com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory"
            )
            val getInstance = factoryClass.getMethod("getInstance")
            @Suppress("UNCHECKED_CAST")
            getInstance.invoke(null) as AppCheckProviderFactory
        }.onFailure { error ->
            Log.w(TAG, "Failed to load DebugAppCheckProviderFactory; falling back", error)
        }.getOrNull()

        return if (providerFactory != null) {
            appCheck.installAppCheckProviderFactory(providerFactory)
            true
        } else {
            Log.w(TAG, "DebugAppCheckProviderFactory not available; skipping App Check")
            false
        }
    }

    companion object {
        private const val TAG = "GeoDropApplication"
    }
}