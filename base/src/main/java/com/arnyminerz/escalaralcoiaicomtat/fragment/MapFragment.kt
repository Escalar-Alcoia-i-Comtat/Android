package com.arnyminerz.escalaralcoiaicomtat.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.IntroActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Area
import com.arnyminerz.escalaralcoiaicomtat.data.map.GeoGeometry
import com.arnyminerz.escalaralcoiaicomtat.data.map.GeoMarker
import com.arnyminerz.escalaralcoiaicomtat.data.map.addToMap
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentMapBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_CENTER_MARKER_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.*
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.bounds
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.annotation.FillManager
import com.mapbox.mapboxsdk.plugins.annotation.LineManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import timber.log.Timber

@ExperimentalUnsignedTypes
class MapFragment : NetworkChangeListenerFragment() {
    private lateinit var mapHelper: MapHelper

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val areas = arrayListOf<Area>()
    private var map: MapboxMap? = null
    private var markerWindow: MarkerWindow? = null

    fun setAreas(areas: ArrayList<Area>) =
        this.areas.addAll(areas)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mapHelper = MapHelper(binding.pageMapView)
        mapHelper.onCreate(savedInstanceState)
        mapHelper
            .withStartingPosition(LatLng(38.7216704, -0.4799751), 12.5)
            .loadMap { mapView, map, style ->
                this@MapFragment.map = map

                Timber.d("Getting map managers...")
                val symbolManager = SymbolManager(mapView, map, style)
                val lineManager = LineManager(mapView, map, style)
                val fillManager = FillManager(mapView, map, style)

                if (context != null)
                    try {
                        if (IntroActivity.hasLocationPermission(requireContext()))
                            mapHelper.enableLocationComponent(requireContext())
                        else
                            Timber.w("User hasn't granted the location permission. Marker won't be enabled.")
                    } catch (ex: IllegalStateException) {
                        Timber.w("Tried to check location permission without being attached to a context.")
                    }

                var counter = 0
                val max = 3
                val markers = arrayListOf<GeoMarker>()
                val polygons = arrayListOf<GeoGeometry>()
                val polylines = arrayListOf<GeoGeometry>()

                runAsync {
                    for (area in areas) {
                        if (context == null || !isResumed) break

                        try {
                            val result =
                                mapHelper.loadKML(requireActivity(), area.kmlAddress, networkState)

                            Timber.d("Adding features to list...")
                            markers.addAll(result.markers)
                            polygons.addAll(result.polygons)
                            polylines.addAll(result.polylines)

                            Timber.d("Adding features to map...")
                            counter++
                            if (counter >= max) {
                                markers.addToMap(requireContext(), symbolManager)
                                polygons.addToMap(fillManager, lineManager)
                                polylines.addToMap(fillManager, lineManager)

                                val positions = arrayListOf<LatLng>()

                                for (marker in markers)
                                    positions.add(marker.position.toLatLng())
                                for (marker in polygons)
                                    positions.addAll(marker.points)
                                for (marker in polylines)
                                    positions.addAll(marker.points)

                                if (positions.size > 1)
                                    map.moveCamera(
                                        CameraUpdateFactory.newLatLngBounds(
                                            positions.bounds(),
                                            30
                                        )
                                    )
                                else if (positions.size > 0)
                                    map.moveCamera(
                                        CameraUpdateFactory.newLatLng(positions.first())
                                    )
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Could not load KML")
                            requireContext().toast(R.string.toast_error_internal)
                        }
                    }
                }

                symbolManager.addClickListener { marker ->
                    if (SETTINGS_CENTER_MARKER_PREF.get(requireContext().sharedPreferences))
                        map.animateCamera(CameraUpdateFactory.newLatLng(marker.latLng))

                    context?.let {
                        markerWindow = MapHelper.infoCard(it, marker, binding.dialogMapMarker)
                    }

                    true
                }

                map.addOnMapClickListener {
                    markerWindow?.hide()
                    markerWindow = null
                    true
                }
            }
    }

    override fun onStart() {
        super.onStart()
        mapHelper.onStart()
    }

    override fun onPause() {
        super.onPause()
        mapHelper.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapHelper.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapHelper.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapHelper.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapHelper.onDestroy()
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        mapHelper.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)
        val hasInternet = state.hasInternet

        if (isResumed) {
            visibility(binding.pageMapView, hasInternet)
            visibility(binding.mapsNoInternetCardView.noInternetCardView, !hasInternet)
        }
    }
}