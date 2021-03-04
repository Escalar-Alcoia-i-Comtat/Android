package com.arnyminerz.escalaralcoiaicomtat.fragment.climb

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.AREAS
import com.arnyminerz.escalaralcoiaicomtat.activity.IntroActivity.Companion.hasLocationPermission
import com.arnyminerz.escalaralcoiaicomtat.data.map.GeoMarker
import com.arnyminerz.escalaralcoiaicomtat.data.map.MapObjectWindowData
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentViewAreasBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_NEARBY_DISTANCE_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toLatLng
import com.arnyminerz.escalaralcoiaicomtat.generic.isNull
import com.arnyminerz.escalaralcoiaicomtat.generic.onUiThread
import com.arnyminerz.escalaralcoiaicomtat.generic.runAsync
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.AreaAdapter
import com.arnyminerz.escalaralcoiaicomtat.list.holder.AreaViewHolder
import com.arnyminerz.escalaralcoiaicomtat.location.serializable
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.show
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.gms.location.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import timber.log.Timber


const val LOCATION_PERMISSION_REQUEST = 0

@ExperimentalUnsignedTypes
class AreasViewFragment : NetworkChangeListenerFragment() {
    private var justAttached = false

    var mapHelper: MapHelper? = null

    private var areaClickListener: ((viewHolder: AreaViewHolder, position: Int) -> Unit)? = null

    private var newLocationProvider: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null

    private var counter = 0

    private var _binding: FragmentViewAreasBinding? = null
    private val binding get() = _binding!!

    fun updateNearbyZones(currentLocation: Location?) {
        var error = false
        binding.nearbyZonesCardView.hide()

        if (context == null) {
            error = true
            Timber.w("Could not update Nearby Zones: Not showing fragment (context is null)")
        }

        if (!isResumed) {
            error = true
            Timber.w("Could not update Nearby Zones: Not showing fragment (not resumed)")
        }

        if (newLocationProvider == null) {
            error = true
            Timber.w("Could not update Nearby Zones: Location provider is null")
        }

        if (AREAS.isEmpty()) {
            error = true
            Timber.w("Could not update Nearby Zones: AREAS is empty")
        }

        if (error)
            return

        binding.nearbyZonesCardView.show()
        binding.nearbyZonesIcon.setImageResource(R.drawable.rotating_explore)

        val hasLocationPermission =
            if (context != null) hasLocationPermission(requireContext()) else false
        visibility(binding.mapView, hasLocationPermission)
        visibility(binding.nearbyZonesPermissionMessage, !hasLocationPermission)

        Timber.v("Has location permission? $hasLocationPermission")

        if (!hasLocationPermission) {
            binding.nearbyZonesCardView.isClickable = true
            binding.nearbyZonesCardView.setOnClickListener {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST
                )
            }
            binding.nearbyZonesIcon.setImageResource(R.drawable.round_explore_24)
        } else {
            binding.nearbyZonesIcon.setImageResource(R.drawable.rotating_explore)
            binding.nearbyZonesCardView.isClickable = false

            val boundsBuilder = LatLngBounds.Builder()
            counter = 0

            if (currentLocation != null && context != null && mapHelper != null)
                runAsync {
                    Timber.v("Iterating through ${AREAS.size} areas.")
                    Timber.v("Current Location: [${currentLocation.latitude},${currentLocation.longitude}]")
                    boundsBuilder.include(currentLocation.toLatLng())
                    for (area in AREAS) {
                        area.children.forEach { zone ->
                            val zoneLocation = zone.position
                            if (zoneLocation != null) {
                                val requiredDistance =
                                    SETTINGS_NEARBY_DISTANCE_PREF.get(requireContext().sharedPreferences)
                                if (zoneLocation.distanceTo(currentLocation.toLatLng()) <= requiredDistance) {
                                    boundsBuilder.include(zoneLocation)
                                    mapHelper!!.add(
                                        GeoMarker(
                                            zoneLocation.serializable(),
                                            null,
                                            MapObjectWindowData(
                                                zone.displayName,
                                                null
                                            )
                                        ).withImage(
                                            requireContext(),
                                            mapHelper!!,
                                            R.drawable.ic_waypoint_escalador_blanc
                                        )
                                    )
                                }
                                //Log.v(TAG, "  Zone Location (${zoneLocation.distanceTo(currentLocation.toLatLng())}): [${zoneLocation.latitude},${zoneLocation.longitude}]")
                            }
                        }
                        counter++

                        if (counter >= AREAS.size)
                            requireContext().onUiThread {
                                mapHelper!!.display(it)
                                mapHelper!!.center()

                                binding.nearbyZonesIcon.setImageResource(R.drawable.round_explore_24)
                            }
                    }
                }
            else Timber.w("Could not show nearby zones. currentLocation null? ${currentLocation.isNull()}")
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Mapbox.getInstance(context, getString(R.string.mapbox_access_token))
        justAttached = true
    }

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewAreasBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.nearbyZonesIcon.setImageResource(R.drawable.rotating_explore)

        mapHelper = MapHelper(binding.mapView)
        mapHelper!!.onCreate(savedInstanceState)
        mapHelper!!.loadMap { _, map, _ ->
            updateNearbyZones(null)

            map.uiSettings.apply {
                setAllGesturesEnabled(false)
            }

            map.addOnMapClickListener {
                Timber.v("Starting MapActivity...")
                startActivity(mapHelper!!.mapsActivityIntent(requireContext()))
                true
            }
            mapHelper!!.addSymbolClickListener {
                Timber.v("Starting MapActivity...")
                startActivity(mapHelper!!.mapsActivityIntent(requireContext()))
                true
            }

            if (newLocationProvider == null)
                newLocationProvider =
                    LocationServices.getFusedLocationProviderClient(requireContext())
            if (locationRequest == null)
                locationRequest = LocationRequest.create()

            Timber.d("Adding location provider listener")
            if (hasLocationPermission(requireContext())) {
                newLocationProvider!!.requestLocationUpdates(
                    locationRequest!!,
                    locationCallback,
                    Looper.getMainLooper()
                )
                newLocationProvider!!.lastLocation.addOnSuccessListener {
                    updateNearbyZones(it)
                }
            }
        }

        return view
    }

    private fun refreshAreas() {
        if (context != null && isResumed) {
            Timber.v("Initializing area adapter for AreasViewFragment...")
            val adapter = AreaAdapter(requireContext(), areaClickListener)

            binding.areasRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            if (justAttached)
                binding.areasRecyclerView.layoutAnimation =
                    AnimationUtils.loadLayoutAnimation(
                        requireContext(),
                        R.anim.item_fall_animator
                    )
            binding.areasRecyclerView.adapter = adapter
        } else Timber.w("Context is null or AreasViewFragment isn't resumed")
    }

    fun updateAreas(listener: ((viewHolder: AreaViewHolder, position: Int) -> Unit)?) {
        this.areaClickListener = listener
        onUiThread { refreshAreas() }
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()

        if (isResumed)
            refreshAreas()

        if (hasLocationPermission(requireContext()))
            newLocationProvider?.requestLocationUpdates(
                locationRequest!!,
                locationCallback,
                Looper.getMainLooper()
            )

        justAttached = false
    }

    override fun onPause() {
        super.onPause()

        newLocationProvider?.removeLocationUpdates(locationCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)
        if (!isResumed) return

        visibility(binding.areasNoInternetCardView.noInternetCardView, !state.hasInternet)

        if (isResumed)
            refreshAreas()
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return

            val location = locationResult.lastLocation

            Timber.v("Got new location: [${location.latitude}, ${location.longitude}]")

            updateNearbyZones(location)

            binding.nearbyZonesTitle.setOnClickListener {
                updateNearbyZones(location)
            }
        }
    }
}