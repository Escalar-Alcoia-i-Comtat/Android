package com.arnyminerz.escalaralcoiaicomtat.fragment.climb

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
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
import com.arnyminerz.escalaralcoiaicomtat.generic.MapAnyDataToLoadException
import com.arnyminerz.escalaralcoiaicomtat.generic.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toLatLng
import com.arnyminerz.escalaralcoiaicomtat.generic.runAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.runOnUiThread
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.AreaAdapter
import com.arnyminerz.escalaralcoiaicomtat.list.holder.AreaViewHolder
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import timber.log.Timber

const val LOCATION_PERMISSION_REQUEST = 0

enum class NearbyZonesError(val message: String) {
    NEARBY_ZONES_NOT_INITIALIZED("MapHelper is not initialized"),
    NEARBY_ZONES_NOT_LOADED("MapHelper is not loaded"),
    NEARBY_ZONES_CONTEXT("Not showing fragment (context is null)"),
    NEARBY_ZONES_NOT_ENABLED("Nearby Zones not enabled"),
    NEARBY_ZONES_PERMISSION("Location permission not granted"),
    NEARBY_ZONES_RESUMED("Not showing fragment (not resumed)"),
    NEARBY_ZONES_EMPTY("AREAS is empty")
}

class AreasViewFragment : NetworkChangeListenerFragment() {
    private var justAttached = false
    private val mapInitialized: Boolean
        get() = this::mapHelper.isInitialized && mapHelper.isLoaded
    private val nearbyEnabled: Boolean
        get() = !PREF_DISABLE_NEARBY.get(requireContext().sharedPreferences)

    internal lateinit var mapHelper: MapHelper

    private var areaClickListener: ((viewHolder: AreaViewHolder, position: Int) -> Unit)? = null

    private var _binding: FragmentViewAreasBinding? = null
    private val binding get() = _binding!!

    @SuppressLint("MissingPermission")
    private fun nearbyZonesReady(): List<NearbyZonesError> {
        val errors = arrayListOf<NearbyZonesError>()

        if (!this::mapHelper.isInitialized)
            errors.add(NearbyZonesError.NEARBY_ZONES_NOT_INITIALIZED)
        else if (!mapHelper.isLoaded)
            errors.add(NearbyZonesError.NEARBY_ZONES_NOT_LOADED)
        else {
            if (context == null)
                errors.add(NearbyZonesError.NEARBY_ZONES_CONTEXT)
            else if (!nearbyEnabled)
                errors.add(NearbyZonesError.NEARBY_ZONES_NOT_ENABLED)
            else if (PermissionsManager.areLocationPermissionsGranted(requireContext()))
                try {
                    mapHelper.enableLocationComponent(requireContext())
                } catch (ex: IllegalStateException) {
                    Timber.w("Tried to enable location component that is already enabled")
                }
            else errors.add(NearbyZonesError.NEARBY_ZONES_PERMISSION)

            if (!isResumed)
                errors.add(NearbyZonesError.NEARBY_ZONES_RESUMED)

            if (AREAS.isEmpty)
                errors.add(NearbyZonesError.NEARBY_ZONES_EMPTY)
        }

        if (errors.isNotEmpty())
            for (msg in errors)
                Timber.w("Could not update Nearby Zones ($msg): ${msg.message}")

        visibility(binding.nearbyZonesCardView, errors.isEmpty())
        return errors
    }

    private fun updateNearbyZones(location: Location) {
        if (nearbyZonesReady().isEmpty())
            return

        Timber.v("Updating nearby zones...")
        val position = location.toLatLng()

        binding.nearbyZonesIcon.setImageResource(R.drawable.rotating_explore)

        val hasLocationPermission =
            context?.let { PermissionsManager.areLocationPermissionsGranted(requireContext()) }
                ?: false
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
                        Timber.d("Adding zone #${zone.objectId}. Creating marker...")
                        val marker = GeoMarker(
                            zoneLocation,
                            windowData = MapObjectWindowData(zone.displayName, null)
                        ).apply {
                            val icon = ICON_WAYPOINT_ESCALADOR_BLANC.toGeoIcon(requireContext())
                            if (icon != null)
                                withImage(icon)
                        }
                        Timber.d("Adding marker to map")
                        mapHelper.add(marker)
                    }
                }

                Timber.d("Finished adding markers.")
                runOnUiThread {
                    mapHelper.display(this)
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

        Timber.d("Loading map...")
        mapHelper
            .withControllable(false)
            .withStartingPosition(LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE))
            .loadMap(requireContext()) { _, map, _ ->
                Timber.d("Map is ready.")

                mapHelper.addLocationUpdateCallback { location ->
                    Timber.v("Got new location: [${location.latitude}, ${location.longitude}]")

                    updateNearbyZones(location)

                    binding.nearbyZonesTitle.setOnClickListener {
                        updateNearbyZones(location)
                    }
                }

                map.addOnMapClickListener {
                    try {
                        val intent = mapHelper.mapsActivityIntent(requireContext())
                        Timber.v("Starting MapsActivity...")
                        startActivity(intent)
                        true
                    } catch (e: MapAnyDataToLoadException) {
                        Timber.w("Clicked on nearby zones map and any data has been loaded")
                        false
                    }
                }
                mapHelper.addSymbolClickListener {
                    val intent = mapHelper.mapsActivityIntent(requireContext())
                    Timber.v("Starting MapsActivity...")
                    startActivity(intent)
                    true
                }

                nearbyZonesReady()
            }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Mapbox.getInstance(context, getString(R.string.mapbox_access_token))
        justAttached = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewAreasBinding.inflate(inflater, container, false)

        initializeMap(savedInstanceState)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        refreshAreas()
    }

    private fun refreshAreas() {
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
    }

    fun setItemClickListener(areaClickListener: ((viewHolder: AreaViewHolder, position: Int) -> Unit)?) {
        this.areaClickListener = areaClickListener
    }

    override fun onResume() {
        super.onResume()

        initializeMap()
        nearbyZonesReady()
        justAttached = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        if (!isResumed) return

        visibility(binding.areasNoInternetCardView.noInternetCardView, !state.hasInternet)
    }
}
