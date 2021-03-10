package com.arnyminerz.escalaralcoiaicomtat.fragment.climb

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.getZones
import com.arnyminerz.escalaralcoiaicomtat.data.map.*
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentViewAreasBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.PREF_DISABLE_NEARBY
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_NEARBY_DISTANCE_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toLatLng
import com.arnyminerz.escalaralcoiaicomtat.generic.runAsync
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.AreaAdapter
import com.arnyminerz.escalaralcoiaicomtat.list.holder.AreaViewHolder
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import timber.log.Timber

const val LOCATION_PERMISSION_REQUEST = 0

@ExperimentalUnsignedTypes
class AreasViewFragment : NetworkChangeListenerFragment() {
    private var justAttached = false
    private val mapInitialized: Boolean
        get() = this::mapHelper.isInitialized && mapHelper.isLoaded
    private val nearbyEnabled: Boolean
        get() = !PREF_DISABLE_NEARBY.get(requireContext().sharedPreferences)

    private lateinit var mapHelper: MapHelper

    private var areaClickListener: ((viewHolder: AreaViewHolder, position: Int) -> Unit)? = null

    private var locationManager: LocationManager? = null
    private var locationListenerAdded = false

    private var _binding: FragmentViewAreasBinding? = null
    private val binding get() = _binding!!

    private fun nearbyZonesReady(): Boolean {
        var error = false

        if (context == null) {
            error = true
            Timber.w("Could not update Nearby Zones: Not showing fragment (context is null)")
        } else if (!nearbyEnabled) {
            error = true
            Timber.w("Could not update Nearby Zones: Nearby Zones not enabled")
        }

        if (!isResumed) {
            error = true
            Timber.w("Could not update Nearby Zones: Not showing fragment (not resumed)")
        }

        if (locationManager == null) {
            error = true
            Timber.w("Could not update Nearby Zones: Location manager is null")
        }

        if (AREAS.isEmpty()) {
            error = true
            Timber.w("Could not update Nearby Zones: AREAS is empty")
        }

        visibility(binding.nearbyZonesCardView, !error)
        return !error
    }

    private fun updateNearbyZones(location: Location) {
        if (!nearbyZonesReady())
            return

        Timber.v("Updating nearby zones...")
        val position = location.toLatLng()

        binding.nearbyZonesIcon.setImageResource(R.drawable.rotating_explore)

        val hasLocationPermission = if (context != null)
            PermissionsManager.areLocationPermissionsGranted(requireContext())
        else false
        visibility(binding.mapView, hasLocationPermission)
        visibility(binding.nearbyZonesPermissionMessage, !hasLocationPermission)

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
        } else if (mapHelper.isLoaded) {
            binding.nearbyZonesIcon.setImageResource(R.drawable.rotating_explore)
            binding.nearbyZonesCardView.isClickable = false

            val requiredDistance =
                SETTINGS_NEARBY_DISTANCE_PREF.get(requireContext().sharedPreferences)

            mapHelper.clearSymbols()

            runAsync {
                val zones = AREAS.getZones()
                Timber.v("Iterating through ${zones.size} zones.")
                Timber.v("Current Location: [${location.latitude},${location.longitude}]")
                for (zone in zones) {
                    val zoneLocation = zone.position ?: continue
                    if (zoneLocation.distanceTo(position) <= requiredDistance) {
                        Timber.d("Adding zone #${zone.id}. Creating marker...")
                        var marker = GeoMarker(
                            zoneLocation,
                            windowData = MapObjectWindowData(zone.displayName, null)
                        )
                        Timber.d("Setting image...")
                        marker = marker.withImage(ICON_WAYPOINT_ESCALADOR_BLANC)
                        Timber.d("Adding marker to map")
                        mapHelper.add(marker)
                    }
                }

                Timber.d("Finished adding markers.")
                requireActivity().runOnUiThread {
                    mapHelper.display(requireContext())
                    mapHelper.center()

                    binding.nearbyZonesIcon.setImageResource(R.drawable.round_explore_24)
                }
            }
        } else
            Timber.w("Could not update Nearby Zones: MapHelper not loaded")
    }

    private fun initializeMap(savedInstanceState: Bundle? = null) {
        if (mapInitialized)
            return

        Timber.d("Initializing MapHelper...")
        mapHelper = MapHelper(binding.mapView)
        mapHelper.onCreate(savedInstanceState)

        Timber.d("Getting nearby zones disable pref...")
        val nearbyEnabled = !PREF_DISABLE_NEARBY.get(requireContext().sharedPreferences)

        Timber.d("Loading map...")
        mapHelper
            .withControllable(false)
            .withStartingPosition(LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE))
            .loadMap(requireContext()) { _, map, _ ->
                Timber.d("Map is ready.")

                map.addOnMapClickListener {
                    Timber.v("Starting MapActivity...")
                    mapHelper.showMapsActivity(requireContext())
                    true
                }
                mapHelper.addSymbolClickListener {
                    Timber.v("Starting MapActivity...")
                    mapHelper.showMapsActivity(requireContext())
                    true
                }

                if (nearbyEnabled)
                    requestLocationUpdates()
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

        initializeMap(savedInstanceState)

        return binding.root
    }

    @SuppressLint("MissingPermission")
    fun requestLocationUpdates() {
        if (locationListenerAdded)
            return

        if (locationManager == null)
            locationManager =
                requireContext().applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager

        if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {
            Timber.d("Requesting location updates...")
            binding.nearbyZonesIcon.setImageResource(R.drawable.rotating_explore)
            locationManager!!.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                LOCATION_UPDATE_MIN_TIME,
                LOCATION_UPDATE_MIN_DIST,
                locationCallback,
                Looper.getMainLooper()
            )
            locationListenerAdded = true
        } else Timber.w("Location listener not added since permission is not granted")
    }

    fun refreshAreas() {
        if (context != null && isResumed) {
            Timber.v("Refreshing areas...")
            nearbyZonesReady()

            Timber.d("Initializing area adapter for AreasViewFragment...")
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

    fun setItemClickListener(areaClickListener: ((viewHolder: AreaViewHolder, position: Int) -> Unit)?) {
        this.areaClickListener = areaClickListener
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()

        justAttached = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)
        if (!isResumed) return

        visibility(binding.areasNoInternetCardView.noInternetCardView, !state.hasInternet)
    }

    private val locationCallback = LocationListener { location ->
        Timber.v("Got new location: [${location.latitude}, ${location.longitude}]")

        updateNearbyZones(location)

        binding.nearbyZonesTitle.setOnClickListener {
            updateNearbyZones(location)
        }
    }
}
