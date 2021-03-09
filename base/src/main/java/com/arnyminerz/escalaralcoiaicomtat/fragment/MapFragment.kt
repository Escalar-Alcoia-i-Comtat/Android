package com.arnyminerz.escalaralcoiaicomtat.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Area
import com.arnyminerz.escalaralcoiaicomtat.data.map.GeoGeometry
import com.arnyminerz.escalaralcoiaicomtat.data.map.GeoMarker
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentMapBinding
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_CENTER_MARKER_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.*
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import timber.log.Timber
import java.io.FileNotFoundException

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
            .loadMap(requireContext()) { _, map, _ ->
                this@MapFragment.map = map

                if (context != null)
                    try {
                        if (PermissionsManager.areLocationPermissionsGranted(requireContext()))
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
                                mapHelper.addMarkers(markers)
                                mapHelper.addGeometries(polygons.toList())
                                mapHelper.addGeometries(polylines.toList())

                                mapHelper.display(requireContext())
                                mapHelper.center()
                            }
                        } catch (e: FileNotFoundException) {
                            Timber.e(e, "Could not load KML")
                            requireContext().toast(R.string.toast_error_internal)
                        } catch (e: NoInternetAccessException) {
                            Timber.e(e, "Could not load KML")
                            requireContext().toast(R.string.toast_error_internal)
                        } catch (e: MapNotInitializedException) {
                            Timber.e(e, "Could not load KML")
                            requireContext().toast(R.string.toast_error_internal)
                        }
                    }
                }

                mapHelper.addSymbolClickListener {
                    if (SETTINGS_CENTER_MARKER_PREF.get(requireContext().sharedPreferences))
                        map.animateCamera(CameraUpdateFactory.newLatLng(latLng))

                    context?.let {
                        markerWindow = mapHelper.infoCard(it, this, binding.dialogMapMarker)
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
