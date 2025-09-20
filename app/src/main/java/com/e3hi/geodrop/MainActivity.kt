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
import com.e3hi.geodrop.util.GroupPreferences
import com.e3hi.geodrop.util.createNotificationChannelIfNeeded
import com.e3hi.geodrop.geo.NearbyDropRegistrar
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener

class MainActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var authListener: AuthStateListener? = null
    private val registrar = NearbyDropRegistrar()

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

        authListener = AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) return@AuthStateListener
            registrar.registerNearby(
                this,
                maxMeters = 300.0,
                groupCodes = groupPrefs.getJoinedGroups().toSet()
            )
        }
        authListener?.let { auth.addAuthStateListener(it) }

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

    override fun onDestroy() {
        authListener?.let { auth.removeAuthStateListener(it) }
        authListener = null
        super.onDestroy()
    }
}
