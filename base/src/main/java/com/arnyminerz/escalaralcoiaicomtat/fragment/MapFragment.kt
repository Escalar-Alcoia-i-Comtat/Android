package com.arnyminerz.escalaralcoiaicomtat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.AREAS
import com.arnyminerz.escalaralcoiaicomtat.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.data.map.DEFAULT_LATITUDE
import com.arnyminerz.escalaralcoiaicomtat.data.map.DEFAULT_LONGITUDE
import com.arnyminerz.escalaralcoiaicomtat.data.map.DEFAULT_ZOOM
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

private const val ICON_SIZE_MULTIPLIER = .2f

class MapFragment : NetworkChangeListenerFragment() {
    private lateinit var mapHelper: MapHelper
    var mapLoaded = false
    var mapLoading = false

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

    private var map: MapboxMap? = null
    private var markerWindow: MarkerWindow? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Timber.d("onActivityCreated()")

        Timber.v("Preparing MapHelper...")
        mapHelper = MapHelper(binding.pageMapView)
        mapHelper.onCreate(savedInstanceState)
        mapHelper
            .withIconSizeMultiplier(ICON_SIZE_MULTIPLIER)
            .withStartingPosition(LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE), DEFAULT_ZOOM)
            .loadMap(requireContext()) { _, map, _ ->
                this.map = map

                if (context != null)
                    try {
                        if (PermissionsManager.areLocationPermissionsGranted(requireContext()))
                            mapHelper.enableLocationComponent(requireContext())
                        else
                            Timber.w("User hasn't granted the location permission. Marker won't be enabled.")
                    } catch (ex: IllegalStateException) {
                        Timber.w("Tried to check location permission without being attached to a context.")
                    }

                mapHelper.addSymbolClickListener {
                    if (SETTINGS_CENTER_MARKER_PREF.get(requireContext().sharedPreferences))
                        map.animateCamera(CameraUpdateFactory.newLatLng(latLng))

                    markerWindow?.hide()
                    activity?.let {
                        markerWindow = mapHelper.infoCard(it, this, binding.root)
                    }

                    true
                }

                map.addOnMapClickListener {
                    markerWindow?.hide()
                    markerWindow = null
                    true
                }

                Timber.v("Finished loading map. Calling loadMap...")
                loadMap()
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

    override fun onResume() {
        super.onResume()
        mapHelper.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        Timber.v("onStateChange($state)")
        val hasInternet = state.hasInternet

        if (isResumed) {
            visibility(binding.pageMapView, hasInternet)
            visibility(binding.mapsNoInternetCardView.noInternetCardView, !hasInternet)
            loadMap()
        }
    }

    private fun loadMap() {
        if (mapLoaded || mapLoading || !mapHelper.isLoaded) {
            Timber.v("Skipping map load ($mapLoaded, $mapLoading, ${mapHelper.isLoaded}).")
            return
        }
        if (!appNetworkState.hasInternet) {
            Timber.v("Skipping map load: No Internet connection")
            return
        }
        mapLoading = true
        runAsync {
            Timber.v("Loading map...")
            for (area in AREAS.values)
                try {
                    val kml = area.kmlAddress
                    Timber.v("Loading KML ($kml) for ${area.displayName}...")
                    mapHelper.loadKML(requireActivity(), kml, false).apply {
                        Timber.d("Adding features to map...")
                        mapHelper.addMarkers(markers)
                        mapHelper.addGeometries(polygons)
                        mapHelper.addGeometries(polylines)
                    }
                } catch (e: FileNotFoundException) {
                    Timber.e(e, "Could not load KML")
                    runOnUiThread { toast(R.string.toast_error_internal) }
                } catch (e: NoInternetAccessException) {
                    Timber.e(e, "Could not load KML")
                    runOnUiThread { toast(R.string.toast_error_internal) }
                } catch (e: MapNotInitializedException) {
                    Timber.e(e, "Could not load KML")
                    runOnUiThread { toast(R.string.toast_error_internal) }
                }
            runOnUiThread {
                Timber.d("Centering map...")
                mapHelper.display(requireContext())
                mapHelper.center()
            }
            mapLoading = false
            mapLoaded = true
        }
    }
}
