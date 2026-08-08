package com.e3hi.geodrop.geo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.e3hi.geodrop.data.FirestoreRepo
import com.e3hi.geodrop.data.NoteInventory
import com.e3hi.geodrop.util.ExplorerAccountStore
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/** Outcome of the one-shot precise check performed at the moment of pickup. */
enum class ProximityVerdict {
    IN_RANGE,
    OUT_OF_RANGE,

    /** No fix, a stale fix, or one too coarse to decide on. */
    UNAVAILABLE
}

/**
 * The single place a drop becomes a collected note.
 *
 * Every pickup entry point — the map, the drop detail screen, and the
 * notification action — routes through here, so none of them can report success
 * for a note that was never saved. Proximity is re-verified with this class's own
 * one-shot precise fix rather than trusting the caller's coordinates, and the fix
 * is not retained afterwards.
 */
object DropCollector {
    private const val TAG = "DropCollector"
    private val LOCATION_STALE_THRESHOLD_MILLIS = TimeUnit.MINUTES.toMillis(2)

    suspend fun collect(
        context: Context,
        request: DropCollectionRequest,
        userId: String?,
        repo: FirestoreRepo = FirestoreRepo(),
        now: Long = System.currentTimeMillis(),
        verifyProximity: suspend (Double, Double) -> ProximityVerdict = { lat, lng ->
            verifyPickupProximity(context, lat, lng)
        }
    ): DropCollectionResult {
        if (request.dropId.isBlank()) {
            return DropCollectionResult.Failed(IllegalArgumentException("Missing drop id"))
        }
        if (request.isExpired(now)) {
            Log.d(TAG, "Drop ${request.dropId} has expired; ignoring pick up action.")
            return DropCollectionResult.Expired
        }

        val lat = request.lat
        val lng = request.lng
        if (lat == null || lng == null) {
            Log.w(TAG, "Drop ${request.dropId} has no coordinates; rejecting pickup.")
            return DropCollectionResult.LocationUnavailable
        }

        when (verifyProximity(lat, lng)) {
            ProximityVerdict.OUT_OF_RANGE -> {
                Log.d(TAG, "Rejecting pickup for ${request.dropId}: outside the pickup radius.")
                return DropCollectionResult.OutOfRange
            }
            ProximityVerdict.UNAVAILABLE -> {
                Log.d(TAG, "Rejecting pickup for ${request.dropId}: location could not be confirmed.")
                return DropCollectionResult.LocationUnavailable
            }
            ProximityVerdict.IN_RANGE -> Unit
        }

        val appContext = context.applicationContext
        return try {
            val username = request.dropperUsername?.takeIf { it.isNotBlank() }
                ?: runCatching { repo.fetchDropperUsername(request.dropId) }.getOrNull()

            val inventory = NoteInventory(appContext)
            inventory.setActiveUser(userId)
            inventory.saveCollected(request.toCollectedNote(username, now))

            if (!userId.isNullOrBlank()) {
                ExplorerAccountStore(appContext).setLastExplorerUid(userId)

                runCatching { repo.markDropCollected(request.dropId, userId) }.onFailure { error ->
                    // The note is already saved locally and syncs on its own; a failure
                    // here costs the server-side collected marker, not the user's drop.
                    Log.w(TAG, "Failed to mark ${request.dropId} collected for $userId", error)
                }

                val huntId = request.huntId
                val stepIndex = request.huntStepIndex
                val totalSteps = request.huntTotalSteps
                if (!huntId.isNullOrBlank() && stepIndex != null && totalSteps != null) {
                    runCatching {
                        repo.advanceHuntProgress(
                            userId = userId,
                            huntId = huntId,
                            collectedDropId = request.dropId,
                            nextStepIndex = stepIndex + 1,
                            totalSteps = totalSteps
                        )
                    }.onFailure { error ->
                        Log.w(TAG, "Failed to advance hunt $huntId for $userId", error)
                    }
                }
            }

            Log.d(TAG, "Collected drop ${request.dropId} and added it to the inventory")
            DropCollectionResult.Collected
        } catch (error: Exception) {
            Log.w(TAG, "Failed to collect drop ${request.dropId}", error)
            DropCollectionResult.Failed(error)
        }
    }

    /**
     * Requests one precise fix, answers the proximity question, and keeps nothing.
     * Anything that leaves the answer in doubt returns [ProximityVerdict.UNAVAILABLE].
     */
    suspend fun verifyPickupProximity(
        context: Context,
        dropLat: Double,
        dropLng: Double
    ): ProximityVerdict {
        val finePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarsePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val hasFineLocation = finePermission == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = coarsePermission == PackageManager.PERMISSION_GRANTED
        if (!hasFineLocation && !hasCoarseLocation) {
            Log.w(TAG, "Location permission unavailable for pickup validation; rejecting pickup.")
            return ProximityVerdict.UNAVAILABLE
        }

        val fused = LocationServices.getFusedLocationProviderClient(context)
        val currentLocation = runCatching {
            val token = CancellationTokenSource()
            fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
        }.getOrNull() ?: runCatching {
            fused.lastLocation.await()
        }.getOrNull()

        val location = currentLocation ?: run {
            Log.w(TAG, "Couldn't obtain current location to validate drop proximity; rejecting pickup.")
            return ProximityVerdict.UNAVAILABLE
        }

        if (isLocationStale(location)) {
            Log.w(TAG, "Location reading for pickup check is stale; rejecting pickup.")
            return ProximityVerdict.UNAVAILABLE
        }

        val accuracy = location.accuracy.takeIf { location.hasAccuracy() && it > 0f }
        if (accuracy == null || accuracy > PICKUP_RADIUS_METERS) {
            Log.w(TAG, "Location accuracy is insufficient (${accuracy ?: Float.NaN}m); rejecting pickup.")
            return ProximityVerdict.UNAVAILABLE
        }

        val results = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, dropLat, dropLng, results)
        return if (isWithinPickupRadius(results[0], accuracy)) {
            ProximityVerdict.IN_RANGE
        } else {
            ProximityVerdict.OUT_OF_RANGE
        }
    }

    private fun isLocationStale(location: Location): Boolean {
        val ageMillis = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val elapsedNanos = location.elapsedRealtimeNanos
            if (elapsedNanos > 0L) {
                (SystemClock.elapsedRealtimeNanos() - elapsedNanos) / 1_000_000
            } else {
                Long.MAX_VALUE
            }
        } else {
            System.currentTimeMillis() - location.time
        }

        return ageMillis > LOCATION_STALE_THRESHOLD_MILLIS
    }
}
