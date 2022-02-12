package com.arnyminerz.escalaralcoiaicomtat.core.maps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.annotation.UiThread
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.LocationSource

class NearbyZonesModule
@UiThread constructor(
    context: Context,
    private val locationListener: (location: Location) -> Unit
) : LocationSource, LocationCallback() {
    private lateinit var listener: LocationSource.OnLocationChangedListener

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override fun activate(listener: LocationSource.OnLocationChangedListener) {
        this.listener = listener

        val locationRequest = LocationRequest.create()
            .apply {
                interval = 10000
                fastestInterval = 5000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            this,
            Looper.getMainLooper()
        )
    }

    override fun deactivate() {
        fusedLocationClient.removeLocationUpdates(this)
    }

    override fun onLocationResult(result: LocationResult) {
        super.onLocationResult(result)
        result.locations.forEach { location ->
            listener.onLocationChanged(location)
            locationListener(location)
        }
    }
}