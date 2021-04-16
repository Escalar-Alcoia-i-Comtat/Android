package com.arnyminerz.escalaralcoiaicomtat.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.data.map.DEFAULT_LATITUDE
import com.arnyminerz.escalaralcoiaicomtat.data.map.DEFAULT_LONGITUDE
import com.arnyminerz.escalaralcoiaicomtat.data.map.DEFAULT_ZOOM
import com.arnyminerz.escalaralcoiaicomtat.data.map.ICON_SIZE_MULTIPLIER
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentMapBinding
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_CENTER_MARKER_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.generic.MapNotInitializedException
import com.arnyminerz.escalaralcoiaicomtat.generic.doAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.generic.uiContext
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.shared.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import timber.log.Timber
import java.io.FileNotFoundException

class MapFragment : NetworkChangeListenerFragment() {
    private lateinit var mapHelper: MapHelper
    var mapLoaded = false
    var mapLoading = false
    val mapStyle: Style?
        get() = if (this::mapHelper.isInitialized)
            mapHelper.style
        else null

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
    private var markerWindow: MapHelper.MarkerWindow? = null

    @SuppressLint("MissingPermission")
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
                        val permissionGranted = try {
                            PermissionsManager.areLocationPermissionsGranted(requireContext())
                        } catch (_: IllegalStateException) {
                            Timber.w("Tried to check location permission without being attached to a context.")
                            false
                        }
                        if (permissionGranted)
                            mapHelper.enableLocationComponent(requireContext())
                        else
                            Timber.w("User hasn't granted the location permission. Marker won't be enabled.")
                    } catch (_: IllegalStateException) {
                        Timber.d("GPS not enabled.")
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
                    }

                mapHelper.addSymbolClickListener {
                    if (SETTINGS_CENTER_MARKER_PREF.get())
                        map.animateCamera(CameraUpdateFactory.newLatLng(latLng))

                    markerWindow?.hide()
                    activity?.let {
                        markerWindow = mapHelper.infoCard(
                            it,
                            (requireActivity() as MainActivity).firestore,
                            this,
                            binding.root
                        )
                    }

                    true
                }

                map.addOnMapClickListener {
                    markerWindow?.hide()
                    markerWindow = null
                    true
                }

                Timber.v("Finished loading map. Calling loadMap...")
                doAsync {
                    loadMap()
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
            doAsync {
                loadMap()
            }
        }
    }

    private suspend fun loadMap() {
        if (mapLoaded || mapLoading || !mapHelper.isLoaded) {
            Timber.v("Skipping map load ($mapLoaded, $mapLoading, ${mapHelper.isLoaded}).")
            return
        }
        if (!appNetworkState.hasInternet) {
            Timber.v("Skipping map load: No Internet connection")
            return
        }
        mapLoading = true

        Timber.v("Loading map...")
        for (area in AREAS)
            try {
                val kml = area.kmlAddress
                Timber.v("Loading KML ($kml) for ${area.displayName}...")
                val features = mapHelper.loadKML(requireActivity(), kml)
                uiContext {
                    mapHelper.add(features)
                }
            } catch (e: FileNotFoundException) {
                Timber.e(e, "Could not load KML")
                uiContext { toast(requireContext(), R.string.toast_error_internal) }
            } catch (e: NoInternetAccessException) {
                Timber.e(e, "Could not load KML")
                uiContext { toast(requireContext(), R.string.toast_error_internal) }
            } catch (e: MapNotInitializedException) {
                Timber.e(e, "Could not load KML")
                uiContext { toast(requireContext(), R.string.toast_error_internal) }
            }
        uiContext {
            Timber.d("Centering map...")
            mapHelper.display()
            mapHelper.center()
        }
        mapLoading = false
        mapLoaded = true
    }
}
