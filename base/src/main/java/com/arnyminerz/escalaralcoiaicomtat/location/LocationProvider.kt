package com.arnyminerz.escalaralcoiaicomtat.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.arnyminerz.escalaralcoiaicomtat.generic.isNotNull
import timber.log.Timber

@Suppress("unused")
class LocationProvider(private val context: Context): LocationListener {
    companion object {
        const val TWO_MINUTES = 1000 * 60 * 2
    }

    private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var currentBestLocation: Location? = null

    private val locationChangedListeners = arrayListOf<(location: Location) -> Unit>()

    private var minUpdateTime: Long = 5000
    private var minUpdateDistance: Float = 10f

    var isStarted = false
        private set

    fun start(): LocationProvideError {
        isStarted = false
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return LocationProvideError.NO_PERMISSION
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            return LocationProvideError.GPS_DISABLED
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minUpdateTime, minUpdateDistance, this)

        Timber.v("Requested location updates")

        isStarted = true
        return LocationProvideError.OK
    }

    fun setMinUpdateTime(minUpdateTime: Long): LocationProvider {
        this.minUpdateTime = minUpdateTime
        return this
    }

    fun setMinUpdateDistance(minUpdateDistance: Float): LocationProvider {
        this.minUpdateDistance = minUpdateDistance
        return this
    }

    fun clearLocationChangedListener(): LocationProvider {
        locationChangedListeners.clear()
        return this
    }

    fun removeLocationChangedListener(index: Int): LocationProvider {
        locationChangedListeners.removeAt(index)
        return this
    }

    fun addLocationChangedListener(listener: ((location: Location) -> Unit)?): LocationProvider {
        if(listener != null)
            locationChangedListeners.add(listener)
        return this
    }

    override fun onLocationChanged(location: Location) {
        makeUseOfNewLocation(location)

        if (currentBestLocation == null)
            currentBestLocation = location

        locationChangedListeners.forEach { listener ->
            listener(currentBestLocation!!)
        }
    }

    override fun onProviderDisabled(provider: String) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    /**
     * @return the last know best location
     */
    fun getLastBestLocation(): Location? {
        val locationGPS = if (
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) null else locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val locationNet = if (
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) null else locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        var gpsLocationTime: Long = 0
        if (locationGPS.isNotNull())
            gpsLocationTime = locationGPS!!.time

        var netLocationTime: Long = 0
        if (locationNet.isNotNull())
            netLocationTime = locationNet!!.time

        return if (gpsLocationTime - netLocationTime > 0)
            locationGPS
        else
            locationNet
    }

    /**
     * This method modify the last know good location according to the arguments.
     *
     * @param location The possible new location.
     */
    private fun makeUseOfNewLocation(location: Location) {
        if (isBetterLocation(location, currentBestLocation)) {
            currentBestLocation = location
        }
    }

    /** Determines whether one location reading is better than the current location fix
     * @param location  The new location that you want to evaluate
     * @param currentBestLocation  The current location fix, to which you want to compare the new one.
     */
    private fun isBetterLocation(
        location: Location,
        currentBestLocation: Location?
    ): Boolean {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true
        }

        // Check whether the new location fix is newer or older
        val timeDelta = location.time - currentBestLocation.time
        val isSignificantlyNewer = timeDelta > TWO_MINUTES
        val isSignificantlyOlder = timeDelta < -TWO_MINUTES
        val isNewer = timeDelta > 0

        // If it's been more than two minutes since the current location, use the new location,
        // because the user has likely moved.
        if (isSignificantlyNewer) {
            return true
            // If the new location is more than two minutes older, it must be worse.
        } else if (isSignificantlyOlder) {
            return false
        }

        // Check whether the new location fix is more or less accurate
        val accuracyDelta = (location.accuracy - currentBestLocation.accuracy).toInt()
        val isLessAccurate = accuracyDelta > 0
        val isMoreAccurate = accuracyDelta < 0
        val isSignificantlyLessAccurate = accuracyDelta > 200

        // Check if the old and new location are from the same provider
        val isFromSameProvider = isSameProvider(
            location.provider,
            currentBestLocation.provider
        )

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true
        } else if (isNewer && !isLessAccurate) {
            return true
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true
        }
        return false
    }

    // Checks whether two providers are the same
    private fun isSameProvider(
        provider1: String?,
        provider2: String?
    ): Boolean {
        return if (provider1 == null) {
            provider2 == null
        } else provider1 == provider2
    }
}