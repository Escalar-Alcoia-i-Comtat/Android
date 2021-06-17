package com.arnyminerz.escalaralcoiaicomtat.fragment.climb

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.MapsActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.NearbyZonesError
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.getZones
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.data.map.DEFAULT_LATITUDE
import com.arnyminerz.escalaralcoiaicomtat.core.data.map.DEFAULT_LONGITUDE
import com.arnyminerz.escalaralcoiaicomtat.core.data.map.GeoMarker
import com.arnyminerz.escalaralcoiaicomtat.core.data.map.ICON_WAYPOINT_ESCALADOR_BLANC
import com.arnyminerz.escalaralcoiaicomtat.core.data.map.MapObjectWindowData
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_CENTER_CURRENT_LOCATION
import com.arnyminerz.escalaralcoiaicomtat.core.shared.LOCATION_PERMISSION_REQUEST_CODE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PREF_DISABLE_NEARBY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_NEARBY_DISTANCE_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.utils.distanceTo
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.isLocationPermissionGranted
import com.arnyminerz.escalaralcoiaicomtat.core.utils.maps.MapAnyDataToLoadException
import com.arnyminerz.escalaralcoiaicomtat.core.utils.maps.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toLatLng
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentViewAreasBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.AreaAdapter
import com.arnyminerz.escalaralcoiaicomtat.list.holder.AreaViewHolder
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import timber.log.Timber

class AreasViewFragment : NetworkChangeListenerFragment() {
    private var justAttached = false
    private val mapInitialized: Boolean
        get() = this::mapHelper.isInitialized && mapHelper.isLoaded
    private val nearbyEnabled: Boolean
        get() = !PREF_DISABLE_NEARBY.get()

    private lateinit var mapHelper: MapHelper

    val mapHelperInstance: MapHelper?
        get() = if (this::mapHelper.isInitialized) mapHelper else null

    private lateinit var firestore: FirebaseFirestore

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
            else if (requireContext().isLocationPermissionGranted()) {
                try {
                    mapHelper.locationComponent?.enable(requireContext())

                    if (!isResumed)
                        errors.add(NearbyZonesError.NEARBY_ZONES_RESUMED)

                    if (AREAS.isEmpty())
                        errors.add(NearbyZonesError.NEARBY_ZONES_EMPTY)
                } catch (_: IllegalStateException) {
                    errors.add(NearbyZonesError.NEARBY_ZONES_GPS_DISABLED)
                }
            } else errors.add(NearbyZonesError.NEARBY_ZONES_PERMISSION)
        }

        if (errors.isNotEmpty())
            for (msg in errors)
                Timber.w("Could not update Nearby Zones ($msg): ${msg.message}")

        visibility(binding.nearbyZonesCardView, errors.isEmpty())
        return errors
    }

    /**
     * Updates the nearby zones card.
     * @author Arnau Mora
     * @since 20210321
     * @param location The current location, if null, no markers will be added.
     */
    fun updateNearbyZones(location: Location? = null) {
        val nearbyZonesErrors = nearbyZonesReady()
        if (nearbyZonesErrors.isNotEmpty()) {
            Timber.i("Nearby Zones errors: $nearbyZonesErrors")
            // The location permission is not granted. Show permissions message.
            // Having NEARBY_ZONES_PERMISSION also implies that Nearby Zones is enabled.
            if (nearbyZonesErrors.contains(NearbyZonesError.NEARBY_ZONES_PERMISSION)) {
                Timber.v("The Location permission is not granted")
                visibility(binding.mapView, false)
                visibility(binding.nearbyZonesPermissionMessage, true)
                visibility(binding.nearbyZonesCardView, true)

                binding.nearbyZonesCardView.isClickable = true
                binding.nearbyZonesCardView.setOnClickListener {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
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
            } else if (nearbyZonesErrors.contains(NearbyZonesError.NEARBY_ZONES_GPS_DISABLED))
                MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.ThemeOverlay_App_MaterialAlertDialog
                )
                    .setTitle(R.string.dialog_gps_disabled_title)
                    .setMessage(R.string.dialog_gps_disabled_message)
                    .setPositiveButton(R.string.action_enable_location) { _, _ ->
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                    .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            return
        }

        if (location != null) {
            Timber.v("Updating nearby zones...")
            val position = location.toLatLng()

            binding.nearbyZonesIcon.setImageResource(R.drawable.rotating_explore)

            visibility(binding.mapView, true)
            visibility(binding.nearbyZonesPermissionMessage, false)

            binding.nearbyZonesIcon.setImageResource(R.drawable.rotating_explore)
            binding.nearbyZonesCardView.isClickable = false

            val requiredDistance = SETTINGS_NEARBY_DISTANCE_PREF.get()

            mapHelper.clearSymbols()

            doAsync {
                val zones = arrayListOf<Zone>()
                AREAS.getZones(firestore).toCollection(zones)
                Timber.v("Iterating through ${zones.size} zones.")
                Timber.v("Current Location: [${location.latitude},${location.longitude}]")
                for (zone in zones) {
                    val zoneLocation = zone.position
                    if (zoneLocation.distanceTo(position) <= requiredDistance) {
                        Timber.d("Adding zone #${zone.objectId}. Creating marker...")
                        val marker = GeoMarker(
                            zoneLocation,
                            windowData = MapObjectWindowData(zone.displayName, null),
                            icon = ICON_WAYPOINT_ESCALADOR_BLANC.toGeoIcon(requireContext())!!
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
                uiContext {
                    mapHelper.display()
                    mapHelper.center(includeCurrentLocation = true)

                    binding.nearbyZonesIcon.setImageResource(R.drawable.round_explore_24)
                }
            }
        }
    }

    private fun initializeMap() {
        if (mapInitialized)
            return

        Timber.d("Loading map...")
        mapHelper = mapHelper
            .withMapView(binding.mapView)
            .withControllable(false)
            .withStartingPosition(LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE))
            .loadMap { _, map ->
                Timber.d("Map is ready.")

                mapHelper.locationComponent?.addLocationUpdateCallback { location ->
                    Timber.v("Got new location: [${location.latitude}, ${location.longitude}]")

                    updateNearbyZones(location)

                    binding.nearbyZonesTitle.setOnClickListener {
                        updateNearbyZones(location)
                    }
                }

                map.setOnMapClickListener {
                    nearbyZonesClick()
                }
                map.setOnMarkerClickListener {
                    nearbyZonesClick()
                    true
                }

                updateNearbyZones()
            }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        justAttached = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewAreasBinding.inflate(inflater, container, false)

        firestore = Firebase.firestore

        mapHelper = MapHelper()
            .withMapView(binding.mapView)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.v("Initializing MapHelper")
        mapHelper.onCreate(savedInstanceState ?: Bundle.EMPTY)

        initializeMap()

        Timber.v("Refreshing areas...")
        Timber.d("Initializing area adapter for AreasViewFragment...")
        binding.areasRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        if (justAttached)
            binding.areasRecyclerView.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(
                    requireContext(),
                    R.anim.item_fall_animator
                )
        binding.areasRecyclerView.adapter =
            AreaAdapter(requireActivity() as MainActivity, areaClickListener)
    }

    private fun nearbyZonesClick(): Boolean =
        try {
            val intent = mapHelper.mapsActivityIntent(requireContext(), MapsActivity::class.java)
                .putExtra(EXTRA_CENTER_CURRENT_LOCATION, true)
            Timber.v("Starting MapsActivity...")
            startActivity(intent)
            true
        } catch (_: MapAnyDataToLoadException) {
            Timber.w("Clicked on nearby zones map and any data has been loaded")
            false
        }

    override fun onStart() {
        super.onStart()
        if (this::mapHelper.isInitialized)
            mapHelper.onStart()
    }

    override fun onResume() {
        super.onResume()

        initializeMap()
        justAttached = false
        if (this::mapHelper.isInitialized)
            mapHelper.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (this::mapHelper.isInitialized)
            mapHelper.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (this::mapHelper.isInitialized)
            mapHelper.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (this::mapHelper.isInitialized)
            mapHelper.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::mapHelper.isInitialized)
            mapHelper.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        if (!isResumed) return

        visibility(binding.areasNoInternetCardView.noInternetCardView, !state.hasInternet)
    }

    fun setItemClickListener(areaClickListener: ((viewHolder: AreaViewHolder, position: Int) -> Unit)?) {
        this.areaClickListener = areaClickListener
    }
}
