package com.arnyminerz.escalaralcoiaicomtat.core.maps

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.viewbinding.ViewBinding
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.NearbyZonesError
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.getChildren
import com.arnyminerz.escalaralcoiaicomtat.core.data.map.*
import com.arnyminerz.escalaralcoiaicomtat.core.databinding.NearbyZonesCardBinding
import com.arnyminerz.escalaralcoiaicomtat.core.shared.*
import com.arnyminerz.escalaralcoiaicomtat.core.utils.*
import com.arnyminerz.escalaralcoiaicomtat.core.utils.maps.MapAnyDataToLoadException
import com.arnyminerz.escalaralcoiaicomtat.core.utils.maps.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.core.view.hide
import com.arnyminerz.escalaralcoiaicomtat.core.view.show
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * The Nearby Zones module, for showing the user the nearby climbing zones so they can be accessed
 * easily.
 * @author Arnau Mora
 * @since 20210617
 * @param fragment The fragment that is holding the Nearby Zones map.
 * @param mapsActivity The activity where the maps can be loaded.
 * @param binding The [ViewBinding] for the nearby zones card.
 */
class NearbyZonesModule(
    private val fragment: Fragment,
    private val mapsActivity: Class<*>,
    private val binding: NearbyZonesCardBinding,
) {
    /**
     * Fetches whether or not the nearby zones are enabled in preferences.
     * @author Arnau Mora
     * @since 20210617
     */
    private val nearbyEnabled: Boolean
        get() = !PREF_DISABLE_NEARBY.get()

    /**
     * A [MapHelper] instance for doing all the map logic.
     * @author Arnau Mora
     * @since 20210617
     */
    var mapHelper: MapHelper = MapHelper()

    /**
     * An alias for getting the [fragment]'s [Context].
     * @author Arnau Mora
     * @since 20210617
     */
    private val context: Context?
        get() = fragment.context

    /**
     * An alias for getting the [fragment]'s [Activity].
     * @author Arnau Mora
     * @since 20210617
     */
    private val activity: Activity?
        get() = fragment.activity

    /**
     * The [App] module received from the [fragment]'s [Activity].
     * @author Arnau Mora
     * @since 20210817
     */
    private val app: App
        get() = fragment.requireActivity().application as App

    /**
     * Stores if the map is being loaded.
     * @author Arnau Mora
     * @since 20211120
     */
    private var loadingMap = false

    /**
     * Checks if nearby zones is available to be shown.
     * @author Arnau Mora
     * @since 20210617
     * @return A [List] with some [NearbyZonesError]. If empty, there is no error, and nearby zones
     * can be launched.
     */
    @WorkerThread
    @SuppressLint("MissingPermission")
    suspend fun nearbyZonesReady(): List<NearbyZonesError> {
        val errors = arrayListOf<NearbyZonesError>()

        if (!mapHelper.isLoaded)
            errors.add(NearbyZonesError.NOT_LOADED)
        else {
            if (context == null)
                errors.add(NearbyZonesError.CONTEXT)
            else if (!nearbyEnabled)
                errors.add(NearbyZonesError.NOT_ENABLED)
            else if (context.isLocationPermissionGranted()) {
                try {
                    mapHelper.locationComponent?.enable(context!!)
                } catch (_: IllegalStateException) {
                    errors.add(NearbyZonesError.GPS_DISABLED)
                }
            } else errors.add(NearbyZonesError.PERMISSION)

            if (!fragment.isResumed)
                errors.add(NearbyZonesError.RESUMED)
            val areas = app.getAreas()
            if (areas.isEmpty())
                errors.add(NearbyZonesError.EMPTY)
        }

        if (errors.isNotEmpty())
            for (msg in errors)
                Timber.w("Could not update Nearby Zones ($msg): ${msg.message}")

        return errors
    }

    /**
     * When the Nearby Zones card is clicked, this will get called.
     * @author Arnau Mora
     * @since 20210617
     */
    private fun nearbyZonesClick(): Boolean =
        try {
            val intent = mapHelper
                .mapsActivityIntent(activity!!, mapsActivity)
                .putExtra(EXTRA_CENTER_CURRENT_LOCATION, true)
            Timber.v("Starting MapsActivity...")
            activity!!.startActivity(intent)
            true
        } catch (_: MapAnyDataToLoadException) {
            Timber.w("Clicked on nearby zones map and any data has been loaded")
            false
        }

    /**
     * Updates the nearby zones card.
     * @author Arnau Mora
     * @since 20210321
     * @param location The current location, if null, no markers will be added.
     */
    @UiThread
    fun updateNearbyZones(location: Location? = null) {
        val nearbyZonesErrors = runBlocking { nearbyZonesReady() }

        visibility(binding.nearbyZonesCardView, nearbyZonesErrors.isEmpty())

        if (nearbyZonesErrors.isNotEmpty()) {
            if (nearbyZonesErrors.contains(NearbyZonesError.NOT_LOADED))
                (activity as? LifecycleOwner?)?.lifecycle?.coroutineScope?.launchWhenCreated {
                    initializeMap()
                    updateNearbyZones(location)
                    return@launchWhenCreated
                } ?: Timber.w("Could not get lifecycle owner")

            Timber.i("Nearby Zones errors: $nearbyZonesErrors")

            // The location permission is not granted. Show permissions message.
            // Having PERMISSION also implies that Nearby Zones is enabled.
            if (nearbyZonesErrors.contains(NearbyZonesError.PERMISSION)) {
                Timber.v("The Location permission is not granted")
                binding.map.hide()
                binding.nearbyZonesPermissionMessage.show()
                binding.nearbyZonesCardView.show()

                binding.nearbyZonesCardView.isClickable = true
                binding.nearbyZonesCardView.setOnClickListener {
                    ActivityCompat.requestPermissions(
                        activity!!,
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
                binding.nearbyZonesCardView.setOnLongClickListener {
                    PREF_DISABLE_NEARBY.put(true)
                    updateNearbyZones()
                    true
                }
                binding.nearbyZonesIcon.setImageResource(R.drawable.ic_round_explore_off_24)
            } else if (nearbyZonesErrors.contains(NearbyZonesError.GPS_DISABLED))
                MaterialAlertDialogBuilder(
                    context!!,
                    R.style.ThemeOverlay_App_MaterialAlertDialog
                )
                    .setTitle(R.string.dialog_gps_disabled_title)
                    .setMessage(R.string.dialog_gps_disabled_message)
                    .setPositiveButton(R.string.action_enable_location) { _, _ ->
                        context!!.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                    .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            return
        }

        doAsync {
            if (location == null)
                return@doAsync

            Timber.v("Updating nearby zones...")
            val position = location.toLatLng()

            uiContext {
                binding.nearbyZonesIcon.setImageResource(R.drawable.rotating_explore)

                visibility(binding.map, true)
                visibility(binding.nearbyZonesPermissionMessage, false)

                binding.nearbyZonesIcon.setImageResource(R.drawable.rotating_explore)
                binding.nearbyZonesCardView.isClickable = false

                mapHelper.clearSymbols()
            }

            val requiredDistance = SETTINGS_NEARBY_DISTANCE_PREF.get()
            val zones = app.getAreas().getChildren(app.searchSession)
            Timber.v("Iterating through ${zones.size} zones.")
            Timber.v("Current Location: [${location.latitude},${location.longitude}]")
            for (zone in zones) {
                val zoneLocation = zone.position
                if (zoneLocation.distanceTo(position) <= requiredDistance) {
                    Timber.d("Adding zone #${zone.objectId}. Creating marker...")
                    val marker = GeoMarker(
                        zoneLocation,
                        windowData = MapObjectWindowData(zone.displayName, null),
                        icon = ICON_WAYPOINT_ESCALADOR_BLANC.toGeoIcon(context!!)!!
                    ).apply {
                        val icon = ICON_WAYPOINT_ESCALADOR_BLANC.toGeoIcon(context!!)
                        if (icon != null)
                            withImage(icon)
                    }
                    Timber.d("Adding marker to map")
                    mapHelper.add(marker)
                }
            }

            Timber.d("Finished adding markers.")
            uiContext {
                mapHelper.display()
                mapHelper.center(includeCurrentLocation = true)

                binding.nearbyZonesIcon.setImageResource(R.drawable.round_explore_24)
            }
        }
    }

    /**
     * Initializes the map through the [MapHelper].
     * @author Arnau Mora
     * @since 20210617
     * @return true if the map was initialized correctly, or has already been initialized before.
     * false otherwise.
     */
    @UiThread
    private suspend fun initializeMap() {
        if (loadingMap) {
            Timber.d("The map is already loading, awaiting...")
            while (loadingMap) {
                delay(10)
            }
        }

        // If already initialized, return
        if (mapHelper.isLoaded)
            return

        loadingMap = true

        Timber.d("Loading map...")
        mapHelper = mapHelper
            .withMapFragment(fragment, R.id.map)
            .withControllable(false)
            .withStartingPosition(LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE))
            .loadMap()

        Timber.i("Map is ready.")

        mapHelper.locationComponent?.addLocationUpdateCallback { location ->
            Timber.v("Got new location: [${location.latitude}, ${location.longitude}]")

            updateNearbyZones(location)

            binding.nearbyZonesTitle.setOnClickListener {
                updateNearbyZones(location)
            }
        }

        mapHelper.setOnMapClickListener { nearbyZonesClick() }
        mapHelper.setOnMarkerClickListener {
            nearbyZonesClick()
            true
        }

        loadingMap = false
        updateNearbyZones()
    }

    fun onCreate(savedInstanceBundle: Bundle?) {
        mapHelper.onCreate(savedInstanceBundle)
    }

    fun onStart() {
        mapHelper.onStart()
    }

    fun onResume() {
        mapHelper.onResume()
    }

    fun onPause() {
        mapHelper.onPause()
    }

    fun onStop() {
        mapHelper.onStop()
    }

    fun onDestroy() {
        mapHelper.onDestroy()
    }

    fun onLowMemory() {
        mapHelper.onLowMemory()
    }
}
