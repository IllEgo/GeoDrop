package com.e3hi.geodrop.geo

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

private const val RADIUS_METERS = 10f

class GeofenceManager {
    fun addGeofence(context: Context, id: String, lat: Double, lng: Double) {
        val geofence = Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(lat, lng, RADIUS_METERS) // hard 10 m
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val pi = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, GeofenceReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val client: GeofencingClient = LocationServices.getGeofencingClient(context)
        client.addGeofences(request, pi)
            .addOnSuccessListener { Log.d("GeoDrop", "Geofence added: $id") }
            .addOnFailureListener { e -> Log.e("GeoDrop", "Geofence add failed $id", e) }
    }
}
