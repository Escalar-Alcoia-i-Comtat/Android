package com.arnyminerz.escalaralcoiaicomtat.core.utils.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.annotation.RequiresPermission
import com.arnyminerz.escalaralcoiaicomtat.core.data.map.LOCATION_UPDATE_MIN_DIST
import com.arnyminerz.escalaralcoiaicomtat.core.data.map.LOCATION_UPDATE_MIN_TIME
import com.arnyminerz.escalaralcoiaicomtat.core.exception.MissingPermissionException
import com.arnyminerz.escalaralcoiaicomtat.core.utils.isLocationPermissionGranted
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toLatLng
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber

class LocationComponent(private val mapHelper: MapHelper) {
    /**
     * Checks if the location manager is ready to be used.
     * @author Arnau Mora
     * @since 20210602
     */
    val isReady: Boolean
        get() = this::locationManager.isInitialized

    /**
     * Stores whether or not the location listeners are enabled.
     * @author Arnau Mora
     * @since 20211121
     */
    var listeningForUpdates: Boolean = false
        private set

    private lateinit var locationManager: LocationManager

    var lastKnownLocation: LatLng? = null
        private set

    /**
     * The tracking mode for the camera.
     * @author Arnau Mora
     * @since 20210602
     */
    var trackMode: TrackMode = TrackMode.NONE

    private val locationUpdateCallbacks = arrayListOf<(location: Location) -> Unit>()

    private val locationUpdateCallback = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Timber.v("Got new location: $location")
            lastKnownLocation = location.toLatLng()

            if (trackMode != TrackMode.NONE)
                mapHelper.move(location.toLatLng())

            for (callback in locationUpdateCallbacks)
                callback(location)
        }

        override fun onProviderEnabled(provider: String) {
            Timber.d("The location provider has been enabled")
        }

        override fun onProviderDisabled(provider: String) {
            Timber.d("The location provider has been disabled")
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Timber.d("The status of provider $provider has been changed to $status.")
        }
    }

    /**
     * Enables the current location pointer. Requires the location permission to be granted
     * @param context The context to initialize the location component from.
     * @param provider The location provider from [LocationManager] for location updates.
     * @author Arnau Mora
     * @see LocationManager.GPS_PROVIDER
     * @see LocationManager.NETWORK_PROVIDER
     * @see LocationManager.PASSIVE_PROVIDER
     * @throws MissingPermissionException If the location permission is not granted
     * @throws MapNotInitializedException If the map has not been initialized
     * @throws IllegalStateException If the [provider] is not enabled
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(
        anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION]
    )
    @Throws(
        MissingPermissionException::class,
        MapNotInitializedException::class,
        IllegalStateException::class
    )
    fun enable(
        context: Context,
        provider: String = LocationManager.GPS_PROVIDER
    ) {
        if (!mapHelper.isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        if (!context.isLocationPermissionGranted())
            throw MissingPermissionException("Location permission not granted")

        if (this::locationManager.isInitialized) {
            Timber.v("Location component already enabled")
        } else
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!locationManager.isProviderEnabled(provider))
            throw IllegalStateException("The specified provider ($provider) is not enabled.")

        if (!listeningForUpdates) {
            locationManager.requestLocationUpdates(
                provider,
                LOCATION_UPDATE_MIN_TIME,
                LOCATION_UPDATE_MIN_DIST,
                locationUpdateCallback
            )
            listeningForUpdates = true
        }

        mapHelper.map!!.isMyLocationEnabled = true
        mapHelper.map!!.uiSettings.isMyLocationButtonEnabled = false
        Timber.i("Enabled location component for MapHelper")
    }

    /**
     * Disables the location updates. Can be enabled again calling [enable].
     * @author Arnau Mora
     * @since 20211121
     */
    @SuppressLint("MissingPermission")
    fun disable() {
        if (!isReady)
            return

        locationManager.removeUpdates(locationUpdateCallback)
        listeningForUpdates = false

        mapHelper.map!!.isMyLocationEnabled = false
    }

    /**
     * Adds a new location update callback
     * @author Arnau Mora
     * @since 20210319
     * @param callback What to run when location is updated
     */
    fun addLocationUpdateCallback(callback: (location: Location) -> Unit) =
        locationUpdateCallbacks.add(callback)

    /**
     * Gets the last location the location engine got.
     * @author Arnau Mora
     * @since 20210322
     * @param provider The location provider from [LocationManager]
     * @throws IllegalStateException When the location engine is not initialized. This may be because
     * the location is not enabled.
     * @see LocationManager.GPS_PROVIDER
     * @see LocationManager.NETWORK_PROVIDER
     * @see LocationManager.PASSIVE_PROVIDER
     */
    @Throws(IllegalStateException::class)
    @SuppressLint("MissingPermission")
    fun getLocation(provider: String = LocationManager.GPS_PROVIDER): Location? =
        if (this::locationManager.isInitialized)
            locationManager.getLastKnownLocation(provider)
        else throw IllegalStateException("Location Engine is not initialized.")

    /**
     * Removes location updates from the location manager.
     * @author Arnau Mora
     * @since 20210602
     */
    fun destroy() {
        if (isReady)
            locationManager.removeUpdates(locationUpdateCallback)
    }
}
