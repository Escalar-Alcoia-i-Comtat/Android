package com.arnyminerz.escalaralcoiaicomtat.core.maps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.annotation.UiThread
import com.google.android.gms.maps.LocationSource

class NearbyZonesModule
@UiThread constructor(
    context: Context,
    private val locationListener: (location: Location) -> Unit
) : LocationSource, LocationListener {
    private lateinit var listener: LocationSource.OnLocationChangedListener

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    override fun activate(listener: LocationSource.OnLocationChangedListener) {
        this.listener = listener

        // Get updates every 10 seconds or 10 meters
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10f, this)
    }

    override fun deactivate() {
        locationManager.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location) {
        listener.onLocationChanged(location)
        locationListener(location)
    }
}