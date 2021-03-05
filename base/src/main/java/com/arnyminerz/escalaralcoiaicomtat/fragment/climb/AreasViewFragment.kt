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
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.getZones
import com.arnyminerz.escalaralcoiaicomtat.data.map.GeoMarker
import com.arnyminerz.escalaralcoiaicomtat.data.map.MapObjectWindowData
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentViewAreasBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_NEARBY_DISTANCE_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.ICON_WAYPOINT_ESCALADOR_BLANC
import com.arnyminerz.escalaralcoiaicomtat.generic.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toLatLng
import com.arnyminerz.escalaralcoiaicomtat.generic.runAsync
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.AreaAdapter
import com.arnyminerz.escalaralcoiaicomtat.list.holder.AreaViewHolder
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.gms.location.*
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import timber.log.Timber


const val LOCATION_PERMISSION_REQUEST = 0

@ExperimentalUnsignedTypes
class AreasViewFragment : NetworkChangeListenerFragment() {
    private var justAttached = false

    private lateinit var mapHelper: MapHelper

    private var areaClickListener: ((viewHolder: AreaViewHolder, position: Int) -> Unit)? = null

    private var newLocationProvider: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationListenerAdded = false

    private var counter = 0

    private var _binding: FragmentViewAreasBinding? = null
    private val binding get() = _binding!!

    fun updateNearbyZones(currentLocation: Location) {
        var error = false

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

        visibility(binding.nearbyZonesCardView, !error)
        if (error)
            return
        Timber.v("Updating nearby zones...")
        val position = currentLocation.toLatLng()

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
        } else {
            binding.nearbyZonesIcon.setImageResource(R.drawable.rotating_explore)
            binding.nearbyZonesCardView.isClickable = false

            counter = 0

            val requiredDistance =
                SETTINGS_NEARBY_DISTANCE_PREF.get(requireContext().sharedPreferences)

            if (context != null) {
                mapHelper.clearSymbols()

                runAsync {
                    val zones = AREAS.getZones()
                    Timber.v("Iterating through ${zones.size} zones.")
                    Timber.v("Current Location: [${currentLocation.latitude},${currentLocation.longitude}]")
                    for (zone in zones){
                        val zoneLocation = zone.position ?: continue
                        if (zoneLocation.distanceTo(position) <= requiredDistance) {
                            Timber.d("Adding zone #${zone.id}. Creating marker...")
                            var marker = GeoMarker(
                                zoneLocation,
                                null,
                                MapObjectWindowData(
                                    zone.displayName,
                                    null
                                )
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
            } else Timber.w("Could not show nearby zones")
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

        mapHelper = MapHelper(binding.mapView)
        mapHelper.onCreate(savedInstanceState)

        return binding.root
    }

    @SuppressLint("MissingPermission")
    fun requestLocationUpdates() {
        if (locationListenerAdded)
            return

        if (newLocationProvider == null)
            newLocationProvider = LocationServices.getFusedLocationProviderClient(requireContext())
        if (locationRequest == null)
            locationRequest = LocationRequest.create()

        if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {
            Timber.v("Adding location provider listener...")
            Timber.d("Requesting location updates...")
            newLocationProvider!!.requestLocationUpdates(
                locationRequest!!,
                locationCallback,
                Looper.getMainLooper()
            )
            Timber.d("Adding on success listener...")
            newLocationProvider!!.lastLocation.addOnSuccessListener {
                Timber.d("Location provider got location!")
                updateNearbyZones(it)
            }
            locationListenerAdded = true
        } else Timber.w("Location listener not added since permission is not granted")
    }

    fun refreshAreas() {
        if (context != null && isResumed) {
            Timber.v("Refreshing areas...")
            binding.nearbyZonesCardView.hide()
            binding.nearbyZonesIcon.setImageResource(R.drawable.rotating_explore)

            Timber.d("Loading map...")
            mapHelper
                .withControllable(false)
                .loadMap(requireContext()) { _, map, _ ->
                Timber.d("Map is ready.")

                map.addOnMapClickListener {
                    Timber.v("Starting MapActivity...")
                    startActivity(mapHelper.mapsActivityIntent(requireContext()))
                    true
                }
                mapHelper.addSymbolClickListener {
                    Timber.v("Starting MapActivity...")
                    startActivity(mapHelper.mapsActivityIntent(requireContext()))
                    true
                }

                requestLocationUpdates()
            }

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