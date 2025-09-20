package com.e3hi.geodrop.geo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.e3hi.geodrop.data.Drop
import com.e3hi.geodrop.data.NoteInventory
import com.e3hi.geodrop.util.GroupPreferences
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@SuppressLint("MissingPermission") // we check at runtime in hasPreciseLocation()
class NearbyDropRegistrar {

    sealed interface NearbySyncStatus {
        data class Success(val count: Int) : NearbySyncStatus
        object MissingPermission : NearbySyncStatus
        object NoLocation : NearbySyncStatus
        data class Error(val message: String) : NearbySyncStatus
    }

    /** Entry point: try to detect current location, then register geofences around you. */
    fun registerNearby(
        context: Context,
        maxMeters: Double = 300.0,
        groupCodes: Set<String> = emptySet(),
        onStatus: (NearbySyncStatus) -> Unit = {}
    ) {
        if (!hasPreciseLocation(context)) {
            Log.e(TAG, "No precise location permission (ACCESS_FINE_LOCATION=false). Skipping geofence registration.")
            logPerms(context)
            notifyStatus(onStatus, NearbySyncStatus.MissingPermission)
            return
        }

        getLocation(
            context,
            onLocation = { lat, lng ->
                Log.d(TAG, "Registrar using location lat=$lat lng=$lng")
                registerNearbyAt(context, lat, lng, maxMeters, groupCodes, onStatus)
            },
            onNoLocation = {
                Log.w(TAG, "No location; will retry once in 5s (set emulator/device location).")
                Handler(Looper.getMainLooper()).postDelayed({
                    getLocation(
                        context,
                        onLocation = { lat, lng ->
                            Log.d(TAG, "Retry found location lat=$lat lng=$lng")
                            registerNearbyAt(context, lat, lng, maxMeters, groupCodes, onStatus)
                        },
                        onNoLocation = {
                            Log.e(TAG, "Retry also failed to get location. Make sure Location is ON and a mock/fresh fix is set.")
                            notifyStatus(onStatus, NearbySyncStatus.NoLocation)
                        }
                    )
                }, 5000)
            }
        )
    }

    /**
     * Testing helper: force a specific origin (e.g., emulator Extended controls → Location).
     * This bypasses FusedLocation so you can validate geofences at known coords.
     */
    fun registerNearbyAt(
        context: Context,
        originLat: Double,
        originLng: Double,
        maxMeters: Double = 300.0,
        groupCodes: Set<String> = emptySet(),
        onStatus: (NearbySyncStatus) -> Unit = {}
    ) {
        if (!hasPreciseLocation(context)) {
            Log.e(TAG, "No precise location permission; cannot add geofences.")
            logPerms(context)
            notifyStatus(onStatus, NearbySyncStatus.MissingPermission)
            return
        }

        val geos: GeofencingClient = LocationServices.getGeofencingClient(context)
        val db = Firebase.firestore
        val inventory = NoteInventory(context)
        val me = FirebaseAuth.getInstance().currentUser?.uid
        val allowedGroups = groupCodes.mapNotNull { GroupPreferences.normalizeGroupCode(it) }.toSet()

        db.collection("drops")
            .get()
            .addOnSuccessListener { snap ->
                val pendingIntent = GeofencePendingIntent.get(context)
                val toAdd = mutableListOf<Geofence>()

                for (doc in snap.documents) {
                    val drop = doc.toObject(Drop::class.java) ?: continue
                    if (drop.isDeleted) continue
                    val id = doc.id

                    if (inventory.isCollected(id) || inventory.isIgnored(id)) continue

                    // ignore my own drops — only notify for "other users"
                    if (drop.createdBy == me) continue

                    val dropGroup = GroupPreferences.normalizeGroupCode(drop.groupCode)
                    if (dropGroup != null && dropGroup !in allowedGroups) continue

                    val d = distanceMeters(originLat, originLng, drop.lat, drop.lng)
                    if (d <= maxMeters) {
                        toAdd += Geofence.Builder()
                            .setRequestId(id)
                            .setCircularRegion(drop.lat, drop.lng, /* 10 m radius */ 10f)
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                            .setExpirationDuration(Geofence.NEVER_EXPIRE)
                            .build()
                    }
                }

                if (toAdd.isEmpty()) {
                    Log.d(TAG, "No nearby foreign drops within ${maxMeters.toInt()} m to register.")
                    notifyStatus(onStatus, NearbySyncStatus.Success(0))
                    return@addOnSuccessListener
                }

                val req = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofences(toAdd)
                    .build()

                geos.addGeofences(req, pendingIntent)
                    .addOnSuccessListener {
                        Log.d(TAG, "Geofences added: ${toAdd.size}")
                        notifyStatus(onStatus, NearbySyncStatus.Success(toAdd.size))
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "addGeofences FAILED", e)
                        logPerms(context)
                        notifyStatus(
                            onStatus,
                            NearbySyncStatus.Error(
                                "Couldn't register nearby drops: ${e.localizedMessage ?: "unknown error"}"
                            )
                        )
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Query drops FAILED", e)
                notifyStatus(
                    onStatus,
                    NearbySyncStatus.Error(
                        "Couldn't check for nearby drops: ${e.localizedMessage ?: "unknown error"}"
                    )
                )
            }
    }

    // ---------- helpers ----------

    private fun notifyStatus(callback: (NearbySyncStatus) -> Unit, status: NearbySyncStatus) {
        Handler(Looper.getMainLooper()).post { callback(status) }
    }

    private fun hasPreciseLocation(ctx: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine
    }

    private fun hasBackground(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    /** Print what the OS thinks about your permissions (very helpful for 1004). */
    private fun logPerms(ctx: Context) {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val bg = hasBackground(ctx)
        Log.d(TAG, "Perms => FINE=$fine, COARSE=$coarse, BG=$bg (BG needed only if you want background triggers)")
    }

    private fun distanceMeters(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(bLat - aLat)
        val dLng = Math.toRadians(bLng - aLng)
        val la1 = Math.toRadians(aLat)
        val la2 = Math.toRadians(bLat)
        val h = sin(dLat / 2).pow(2.0) + cos(la1) * cos(la2) * sin(dLng / 2).pow(2.0)
        return 2 * R * asin(min(1.0, sqrt(h)))
    }

    /**
     * Try HIGH_ACCURACY fresh fix → last known location; all via listeners (no blocking on main).
     */
    private fun getLocation(
        context: Context,
        onLocation: (Double, Double) -> Unit,
        onNoLocation: () -> Unit
    ) {
        val fused = LocationServices.getFusedLocationProviderClient(context)

        // 1) Prefer fresh, high-accuracy fix (avoids stale Googleplex default)
        val cts = CancellationTokenSource()
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { fresh ->
                if (fresh != null) {
                    onLocation(fresh.latitude, fresh.longitude)
                } else {
                    // 2) Fallback to last known
                    fused.lastLocation
                        .addOnSuccessListener { last ->
                            if (last != null) {
                                onLocation(last.latitude, last.longitude)
                            } else {
                                onNoLocation()
                            }
                        }
                        .addOnFailureListener { _ -> onNoLocation() }
                }
            }
            .addOnFailureListener { _ ->
                // Failure on fresh; try last known
                fused.lastLocation
                    .addOnSuccessListener { last ->
                        if (last != null) onLocation(last.latitude, last.longitude) else onNoLocation()
                    }
                    .addOnFailureListener { _ -> onNoLocation() }
            }
    }

    companion object {
        private const val TAG = "GeoDrop"
    }
}
