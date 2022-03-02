package com.arnyminerz.escalaralcoiaicomtat.core.maps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.annotation.UiThread
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider

class NearbyZonesModule
@UiThread constructor(
    context: Context,
    private val locationListener: (location: Location) -> Unit
) : IMyLocationProvider, LocationListener {
    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    fun activate() {
        // Get updates every 10 seconds or 10 meters
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10f, this)
    }

    fun deactivate() {
        locationManager.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location) {
        locationListener(location)
    }

    override fun startLocationProvider(myLocationConsumer: IMyLocationConsumer?): Boolean {
        activate()
        return true
    }

    override fun stopLocationProvider() {
        deactivate()
    }

    @SuppressLint("MissingPermission")
    override fun getLastKnownLocation(): Location? =
        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

    override fun destroy() {}
}