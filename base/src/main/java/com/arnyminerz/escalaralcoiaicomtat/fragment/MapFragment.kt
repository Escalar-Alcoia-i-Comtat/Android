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
import com.arnyminerz.escalaralcoiaicomtat.shared.exception_handler.handleStorageException
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.show
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.ktx.storage
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import timber.log.Timber
import java.io.FileNotFoundException

class MapFragment : NetworkChangeListenerFragment() {
    private lateinit var mapHelper: MapHelper
    private var mapLoaded = false
    private var mapLoading = false
    val mapStyle: Style?
        get() = if (this::mapHelper.isInitialized)
            mapHelper.style
        else null

    private var binding: FragmentMapBinding? = null

    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    private var map: MapboxMap? = null
    private var markerWindow: MapHelper.MarkerWindow? = null

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated()")
        binding!!.loadingMapCardView.show()

        firebaseStorage = Firebase.storage
        firestore = Firebase.firestore

        Timber.v("Preparing MapHelper...")
        mapHelper = MapHelper(requireContext())
            .withMapView(binding!!.pageMapView)
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
                    Timber.v("Tapped on symbol.")
                    if (SETTINGS_CENTER_MARKER_PREF.get())
                        map.animateCamera(CameraUpdateFactory.newLatLng(latLng))

                    markerWindow?.hide()
                    activity?.let {
                        Timber.v("There's an available activity")
                        if (it is MainActivity)
                            it.binding.bottomAppBar.performHide()

                        Timber.v("Creating marker window...")
                        binding?.root?.let { viewRoot ->
                            markerWindow = mapHelper.infoCard(it, firestore, this, viewRoot)
                                .also { markerWindow ->
                                    doAsync { markerWindow.show() }
                                    markerWindow.listenHide {
                                        if (it is MainActivity)
                                            it.binding.bottomAppBar.performShow()
                                    }
                                }
                        }
                    } ?: Timber.w("Could not get activity")

                    true
                }

                map.addOnMapClickListener {
                    try {
                        markerWindow?.hide()
                    } catch (e: IllegalStateException) {
                        Timber.i(e, "The card has already been hidden.")
                    }
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
        binding = null
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        Timber.v("onStateChange($state)")
        val hasInternet = state.hasInternet

        if (isResumed) {
            visibility(binding?.pageMapView, hasInternet)
            visibility(binding?.mapsNoInternetCardView?.noInternetCardView, !hasInternet)
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
                Timber.v("Getting KMZ file of $area...")
                val kmzFile = area.kmzFile(requireContext(), firebaseStorage, false)
                Timber.v("Loading KMZ features...")
                val features = mapHelper.loadKMZ(
                    requireContext(),
                    kmzFile,
                    addToMap = false
                )
                if (features != null) {
                    Timber.v("Adding features to map...")
                    mapHelper.add(features)
                }
            } catch (e: FileNotFoundException) {
                Timber.e(e, "Could not load KML")
                Firebase.crashlytics.recordException(e)
                uiContext { toast(context, R.string.toast_error_internal) }
            } catch (e: NoInternetAccessException) {
                Timber.e(e, "Could not load KML")
                Firebase.crashlytics.recordException(e)
                uiContext { toast(context, R.string.toast_error_no_internet) }
            } catch (e: MapNotInitializedException) {
                Timber.e(e, "Could not load KML")
                Firebase.crashlytics.recordException(e)
                uiContext { toast(context, R.string.toast_error_internal) }
            } catch (e: IllegalStateException) {
                Timber.e("Could not load KML")
                Firebase.crashlytics.recordException(e)
                uiContext { toast(context, R.string.toast_error_no_kmz) }
            } catch (e: StorageException) {
                Firebase.crashlytics.recordException(e)
                val handler = handleStorageException(e)
                if (handler != null) {
                    Timber.e(e, handler.second)
                    toast(context, handler.first)
                }
            }

        try {
            uiContext {
                binding?.loadingMapCardView?.hide()
                Timber.d("Displaying and centering map...")
                mapHelper.display()
                mapHelper.center()
            }

            mapLoading = false
            mapLoaded = true
        } catch (e: MapNotInitializedException) {
            Timber.e(e, "Could not display and center map.")
            mapLoading = false
            mapLoaded = false
        }
    }
}
