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
import com.arnyminerz.escalaralcoiaicomtat.activity.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.data.map.GeoMarker
import com.arnyminerz.escalaralcoiaicomtat.data.map.MapObjectWindowData
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentViewAreasBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_NEARBY_DISTANCE_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.distanceTo
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toLatLng
import com.arnyminerz.escalaralcoiaicomtat.generic.isNull
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.AreaAdapter
import com.arnyminerz.escalaralcoiaicomtat.list.holder.AreaViewHolder
import com.arnyminerz.escalaralcoiaicomtat.location.serializable
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.show
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.gms.location.*
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.LatLngBounds
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.runOnUiThread
import timber.log.Timber
import java.io.Serializable


const val LOCATION_PERMISSION_REQUEST = 0

@ExperimentalUnsignedTypes
class AreasViewFragment : NetworkChangeListenerFragment() {
    private var justAttached = false

    var googleMap: GoogleMap? = null

    private var areaClickListener: ((viewHolder: AreaViewHolder, position: Int) -> Unit)? = null

    private val showingMarkers = arrayListOf<GeoMarker>()

    private var newLocationProvider: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null

    private var addedAnyPoints = false
    private var counter = 0

    private var _binding: FragmentViewAreasBinding? = null
    private val binding get() = _binding!!

    fun updateNearbyZones(currentLocation: Location?, googleMap: GoogleMap) {
        var error = false
        binding.nearbyZonesCardView.hide()

        if (context == null) {
            error = true
            Timber.e("Could not update Nearby Zones: Not showing fragment (context is null)")
        }

        if (!isResumed) {
            error = true
            Timber.e("Could not update Nearby Zones: Not showing fragment (not resumed)")
        }

        if (newLocationProvider == null) {
            error = true
            Timber.e("Could not update Nearby Zones: Location provider is null")
        }

        if (AREAS.isEmpty()) {
            error = true
            Timber.e("Could not update Nearby Zones: AREAS is empty")
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

            if (currentLocation != null && context != null)
                GlobalScope.launch {
                    Timber.v("Iterating through ${AREAS.size} areas.")
                    Timber.v("Current Location: [${currentLocation.latitude},${currentLocation.longitude}]")
                    boundsBuilder.include(currentLocation.toLatLng())
                    for (area in AREAS) {
                        area.children.forEach { zone ->
                            val zoneLocation = zone.position
                            if (zoneLocation != null) {
                                val requiredDistance =
                                    SETTINGS_NEARBY_DISTANCE_PREF.get(sharedPreferences)
                                if (zoneLocation.distanceTo(currentLocation.toLatLng()) <= requiredDistance) {
                                    boundsBuilder.include(zoneLocation)
                                    addedAnyPoints = true
                                    showingMarkers.add(
                                        GeoMarker(
                                            zoneLocation.serializable(),
                                            2f,
                                            MapObjectWindowData(
                                                zone.displayName,
                                                null,
                                                null
                                            )
                                        ).withImage(
                                            requireContext(),
                                            R.drawable.ic_waypoint_escalador_blanc
                                        )
                                    )
                                }
                                //Log.v(TAG, "  Zone Location (${zoneLocation.distanceTo(currentLocation.toLatLng())}): [${zoneLocation.latitude},${zoneLocation.longitude}]")
                            }
                        }
                        counter++

                        if (counter >= AREAS.size) {
                            requireContext().runOnUiThread {
                                for (marker in showingMarkers)
                                    marker.addToMap(googleMap)

                                if (addedAnyPoints)
                                    googleMap.animateCamera(
                                        CameraUpdateFactory.newLatLngBounds(
                                            boundsBuilder.build(),
                                            11
                                        )
                                    )
                                binding.nearbyZonesIcon.setImageResource(R.drawable.round_explore_24)
                            }
                        }
                    }
                }
            else Timber.e("Could not show nearby zones. currentLocation null? ${currentLocation.isNull()}")
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
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
        binding.mapView.getMapAsync { googleMap ->
            this.googleMap = googleMap
            updateNearbyZones(null, googleMap)

            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE

            googleMap.uiSettings.apply {
                setAllGesturesEnabled(false)
            }

            fun onMapClickListener() {
                Timber.v("Starting MapActivity...")
                val intent = Intent(requireContext(), MapsActivity::class.java)
                val mapData = arrayListOf<Serializable>()
                for (zm in showingMarkers)
                    zm.let { zoneMarker ->
                        Timber.v("  Adding position [${zoneMarker.position.latitude}, ${zoneMarker.position.longitude}]")
                        mapData.add(zoneMarker as Serializable)
                    }
                intent.putExtra(MAP_DATA_BUNDLE_EXTRA, mapData)
                startActivity(intent)
            }
            googleMap.setOnMapClickListener {
                onMapClickListener()
            }
            googleMap.setOnMarkerClickListener {
                onMapClickListener()
                true
            }

            if (newLocationProvider == null)
                newLocationProvider =
                    LocationServices.getFusedLocationProviderClient(requireContext())
            if (locationRequest == null)
                locationRequest = LocationRequest()

            Timber.d("Adding location provider listener")
            if (hasLocationPermission(requireContext())) {
                newLocationProvider!!.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
                newLocationProvider!!.lastLocation.addOnSuccessListener {
                    updateNearbyZones(it, googleMap)
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
        } else Timber.e("Context is null or AreasViewFragment isn't resumed")
    }

    fun updateAreas(listener: ((viewHolder: AreaViewHolder, position: Int) -> Unit)?) {
        this.areaClickListener = listener
        refreshAreas()
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()

        if (isResumed)
            refreshAreas()

        if (hasLocationPermission(requireContext()))
            newLocationProvider?.requestLocationUpdates(
                locationRequest,
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
            googleMap ?: return
            locationResult ?: return

            val location = locationResult.lastLocation

            Timber.v("Got new location: [${location.latitude}, ${location.longitude}]")

            updateNearbyZones(location, googleMap!!)

            binding.nearbyZonesTitle.setOnClickListener {
                updateNearbyZones(location, googleMap!!)
            }
        }
    }
}