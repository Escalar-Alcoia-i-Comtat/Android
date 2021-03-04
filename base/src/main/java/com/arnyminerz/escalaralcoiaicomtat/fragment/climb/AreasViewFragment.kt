package com.arnyminerz.escalaralcoiaicomtat.fragment.climb

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import com.arnyminerz.escalaralcoiaicomtat.activity.MapsActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.MapsActivity.Companion.MAP_DATA_BUNDLE_EXTRA
import com.arnyminerz.escalaralcoiaicomtat.data.map.GeoMarker
import com.arnyminerz.escalaralcoiaicomtat.data.map.MapObjectWindowData
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentViewAreasBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_MARKER_SIZE_PREF
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_NEARBY_DISTANCE_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toLatLng
import com.arnyminerz.escalaralcoiaicomtat.generic.isNull
import com.arnyminerz.escalaralcoiaicomtat.generic.mapDouble
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
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import timber.log.Timber
import java.io.Serializable


const val LOCATION_PERMISSION_REQUEST = 0

@ExperimentalUnsignedTypes
class AreasViewFragment : NetworkChangeListenerFragment() {
    private var justAttached = false

    var map: MapboxMap? = null
    var style: Style? = null
    private var symbolManager: SymbolManager? = null

    private var areaClickListener: ((viewHolder: AreaViewHolder, position: Int) -> Unit)? = null

    private val showingMarkers = arrayListOf<GeoMarker?>()

    private var newLocationProvider: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null

    private var addedAnyPoints = false
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
            addedAnyPoints = false
            counter = 0

            showingMarkers.clear()

            if (currentLocation != null && context != null && style != null)
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
                                    addedAnyPoints = true
                                    showingMarkers.add(
                                        GeoMarker(
                                            zoneLocation.serializable(),
                                            mapDouble(
                                                SETTINGS_MARKER_SIZE_PREF.get(requireContext().sharedPreferences)
                                                    .toDouble(), 1.0, 5.0, 0.5, 3.0
                                            ).toInt(),
                                            MapObjectWindowData(
                                                zone.displayName,
                                                null
                                            )
                                        ).withImage(
                                            requireContext(),
                                            style!!,
                                            R.drawable.ic_waypoint_escalador_blanc
                                        )
                                    )
                                }
                                //Log.v(TAG, "  Zone Location (${zoneLocation.distanceTo(currentLocation.toLatLng())}): [${zoneLocation.latitude},${zoneLocation.longitude}]")
                            }
                        }
                        counter++

                        if (counter >= AREAS.size && map != null && symbolManager != null)
                            requireContext().onUiThread {
                                for (marker in showingMarkers)
                                    marker?.addToMap(symbolManager!!)

                                if (addedAnyPoints)
                                    map!!.animateCamera(
                                        CameraUpdateFactory.newLatLngBounds(
                                            boundsBuilder.build(),
                                            11
                                        )
                                    )
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

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync { map ->
            map.setStyle(Style.SATELLITE) { style ->
                this.map = map
                this.style = style
                this.symbolManager = SymbolManager(binding.mapView, map, style)
                updateNearbyZones(null)

                map.uiSettings.apply {
                    setAllGesturesEnabled(false)
                }

                fun onMapClickListener() {
                    Timber.v("Starting MapActivity...")
                    val intent = Intent(requireContext(), MapsActivity::class.java)
                    val mapData = arrayListOf<Serializable>()
                    for (zm in showingMarkers)
                        zm.let { zoneMarker ->
                            Timber.v("  Adding position [${zoneMarker?.position?.latitude}, ${zoneMarker?.position?.longitude}]")
                            mapData.add(zoneMarker as Serializable)
                        }
                    intent.putExtra(MAP_DATA_BUNDLE_EXTRA, mapData)
                    startActivity(intent)
                }
                map.addOnMapClickListener {
                    onMapClickListener()
                    true
                }
                symbolManager?.addClickListener {
                    onMapClickListener()
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