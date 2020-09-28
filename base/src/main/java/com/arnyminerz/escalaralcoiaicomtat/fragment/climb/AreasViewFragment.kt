package com.arnyminerz.escalaralcoiaicomtat.fragment.climb

import android.Manifest
import android.content.Context
import android.content.Intent
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
import com.arnyminerz.escalaralcoiaicomtat.activity.IntroActivity.Companion.hasLocationPermission
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity.Companion.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.activity.MapsActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.MapsActivity.Companion.MAP_DATA_BUNDLE_EXTRA
import com.arnyminerz.escalaralcoiaicomtat.data.map.GeoMarker
import com.arnyminerz.escalaralcoiaicomtat.data.map.MapObjectWindowData
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_NEARBY_DISTANCE_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.distanceTo
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toLatLng
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.AreaAdapter
import com.arnyminerz.escalaralcoiaicomtat.list.holder.AreaViewHolder
import com.arnyminerz.escalaralcoiaicomtat.location.LocationProvideError
import com.arnyminerz.escalaralcoiaicomtat.location.LocationProvider
import com.arnyminerz.escalaralcoiaicomtat.location.serializable
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.LatLngBounds
import kotlinx.android.synthetic.main.fragment_view_areas.*
import kotlinx.android.synthetic.main.fragment_view_areas.view.*
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

    private var locationProvider: LocationProvider? = null

    fun updateNearbyZones(view: View?, currentLocation: Location?, googleMap: GoogleMap) {
        if (view == null)
            return Timber.e("Could not update Nearby Zones: View is null")

        if (context == null)
            return Timber.e("Could not update Nearby Zones: Not showing fragment (context is null)")

        if (isResumed)
            return Timber.e("Could not update Nearby Zones: Not showing fragment (not resumed)")

        view.nearby_zones_icon.setImageResource(R.drawable.rotating_explore)

        val hasLocationPermission =
            if (context != null) hasLocationPermission(requireContext()) else false
        visibility(view.mapView, hasLocationPermission)
        visibility(view.nearby_zones_permission_message, !hasLocationPermission)

        Timber.v("Has location permission? $hasLocationPermission")

        if (!hasLocationPermission) {
            view.nearby_zones_cardView?.isClickable = true
            view.nearby_zones_cardView?.setOnClickListener {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST
                )
            }
            view.nearby_zones_icon.setImageResource(R.drawable.round_explore_24)
        } else {
            view.nearby_zones_icon.setImageResource(R.drawable.rotating_explore)
            view.nearby_zones_cardView?.isClickable = false

            val boundsBuilder = LatLngBounds.Builder()
            var addedAnyPoints = false
            var counter = 0

            showingMarkers.clear()

            if (locationProvider != null && !locationProvider!!.isStarted) {
                Timber.d("Starting location provider.")
                val startResult = locationProvider!!.start()
                if (startResult == LocationProvideError.GPS_DISABLED)
                    Timber.e("GPS is disabled")
                else if (startResult == LocationProvideError.NO_PERMISSION)
                    Timber.e("No GPS Permission")
            }

            if (currentLocation != null && context != null)
                for (area in AREAS) GlobalScope.launch {
                    Timber.v("Current Location: [${currentLocation.latitude},${currentLocation.longitude}]")
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
                                        10
                                    )
                                )
                            view.nearby_zones_icon.setImageResource(R.drawable.round_explore_24)
                        }
                    }
                }
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
    ): View? {
        val view = inflater.inflate(R.layout.fragment_view_areas, container, false)

        view.nearby_zones_icon.setImageResource(R.drawable.rotating_explore)

        view.mapView.onCreate(savedInstanceState)
        view.mapView.getMapAsync { googleMap ->
            this.googleMap = googleMap
            updateNearbyZones(view, null, googleMap)

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

            if (locationProvider == null)
                locationProvider = LocationProvider(requireContext())

            Timber.d("Adding location provider listener")
            locationProvider!!.addLocationChangedListener { location -> onLocationChanged(location) }
            updateNearbyZones(view, null, googleMap)
        }

        return view
    }

    private fun refreshAreas() {
        if (context != null && isResumed)
            if (areas_recyclerView != null) {
                Timber.v("Initializing area adapter for AreasViewFragment...")
                val adapter = AreaAdapter(requireContext(), areaClickListener)

                areas_recyclerView.layoutManager = LinearLayoutManager(requireContext())
                if (justAttached)
                    areas_recyclerView.layoutAnimation =
                        AnimationUtils.loadLayoutAnimation(
                            requireContext(),
                            R.anim.item_fall_animator
                        )
                areas_recyclerView.adapter = adapter
            } else
                Timber.e("Areas recycler view is null!")
        else Timber.e("Context is null or AreasViewFragment isn't resumed")
    }

    fun updateAreas(listener: ((viewHolder: AreaViewHolder, position: Int) -> Unit)?) {
        this.areaClickListener = listener
        refreshAreas()
    }

    override fun onResume() {
        super.onResume()

        if (isResumed)
            refreshAreas()

        justAttached = false
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)
        if (!isResumed) return

        val hasInternet = state.hasInternet

        visibility(nearby_zones_cardView, hasInternet)
        visibility(areas_no_internet_cardView, !hasInternet)

        if (isResumed)
            refreshAreas()
    }

    private fun onLocationChanged(location: Location) {
        if (googleMap == null) return

        Timber.v("Got new location: [${location.latitude}, ${location.longitude}]")

        updateNearbyZones(view, location, googleMap!!)

        view?.nearby_zones_title?.setOnClickListener {
            updateNearbyZones(view, location, googleMap!!)
        }
    }
}