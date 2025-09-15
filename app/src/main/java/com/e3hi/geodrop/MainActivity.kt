package com.e3hi.geodrop

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.e3hi.geodrop.ui.DropHereScreen
import com.e3hi.geodrop.util.createNotificationChannelIfNeeded
import com.e3hi.geodrop.geo.NearbyDropRegistrar
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

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

        // Anonymous sign-in (PLAIN API, no ktx)
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnCompleteListener {
                    // After we have a UID, try registering nearby geofences
                    NearbyDropRegistrar().registerNearby(this, maxMeters = 300.0)
                }
        } else {
            NearbyDropRegistrar().registerNearby(this, maxMeters = 300.0)
        }

        setContent { DropHereScreen() }

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
}
