package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.os.Build
import android.os.Bundle
import androidx.annotation.UiThread
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.map.DEFAULT_LATITUDE
import com.arnyminerz.escalaralcoiaicomtat.data.map.DEFAULT_LONGITUDE
import com.arnyminerz.escalaralcoiaicomtat.data.map.DEFAULT_ZOOM
import com.arnyminerz.escalaralcoiaicomtat.data.map.ICON_SIZE_MULTIPLIER
import com.arnyminerz.escalaralcoiaicomtat.databinding.LayoutListBinding
import com.arnyminerz.escalaralcoiaicomtat.generic.MapAnyDataToLoadException
import com.arnyminerz.escalaralcoiaicomtat.generic.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.generic.doAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.generic.uiContext
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.shared.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.show
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.Style
import timber.log.Timber
import java.io.FileNotFoundException

abstract class DataClassListActivity<T : DataClass<*, *>>(
    private val iconSizeMultiplier: Float = ICON_SIZE_MULTIPLIER,
    private val overrideLoadedMapData: Boolean = false,
) : NetworkChangeListenerActivity() {
    protected lateinit var binding: LayoutListBinding
    protected lateinit var dataClass: T
    private lateinit var mapHelper: MapHelper

    lateinit var firestore: FirebaseFirestore
    lateinit var storage: FirebaseStorage

    val mapStyle: Style?
        get() = if (this::mapHelper.isInitialized)
            mapHelper.style
        else null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firestore = Firebase.firestore
        storage = Firebase.storage

        binding = LayoutListBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        mapHelper = MapHelper(this)
            .withMapView(binding.map)
            .withIconSizeMultiplier(iconSizeMultiplier)
        mapHelper.onCreate(savedInstanceState)

        binding.statusImageView.setOnClickListener { it.performLongClick() }
        updateIcon()
    }

    override fun onStart() {
        super.onStart()
        mapHelper.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapHelper.onResume()
        if (this::mapHelper.isInitialized && mapHelper.isLoaded)
            binding.loadingIndicator.hide()
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

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        val hasInternet = state.hasInternet
        visibility(binding.noInternetCard.noInternetCardView, !hasInternet)

        updateIcon()

        if (this::mapHelper.isInitialized && !mapHelper.isLoaded && hasInternet) {
            Timber.v("Loading map...")
            mapHelper
                .show()
                .withStartingPosition(LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE), DEFAULT_ZOOM)
                .withControllable(false)
                .loadMap(this) { _, map, _ ->
                    try {
                        doAsync {
                            Timber.v("Getting KMZ file...")
                            val kmzFile = dataClass.kmzFile(this@DataClassListActivity, storage, false)
                            Timber.v("Getting map features...")
                            mapHelper.loadKMZ(this@DataClassListActivity, kmzFile)
                            uiContext {
                                binding.map.show()

                                map.addOnMapClickListener {
                                    try {
                                        val intent = mapHelper.mapsActivityIntent(
                                            this@DataClassListActivity,
                                            overrideLoadedMapData
                                        )
                                        Timber.v("Starting MapsActivity...")
                                        startActivity(intent)
                                        true
                                    } catch (_: MapAnyDataToLoadException) {
                                        Timber.w("Clicked on map and any data has been loaded")
                                        false
                                    }
                                }
                            }
                        }
                    } catch (_: FileNotFoundException) {
                        Timber.w("KMZ file not found")
                        binding.map.hide()
                    } catch (e: IllegalStateException) {
                        Firebase.crashlytics.recordException(e)
                        Timber.w("The DataClass ($dataClass) does not contain a KMZ address")
                        toast(R.string.toast_error_no_kmz)
                        binding.map.hide()
                    } finally {
                        binding.loadingIndicator.hide()
                    }
                }
        } else if (!hasInternet) {
            binding.loadingIndicator.hide()
            if (this::mapHelper.isInitialized)
                mapHelper.hide()
        }
    }

    @UiThread
    private fun updateIcon() {
        val i = binding.statusImageView
        binding.statusImageView.hide(setGone = false)
        val activity = this
        val dataClassInitialized = this::dataClass.isInitialized
        doAsync {
            if (!appNetworkState.hasInternet)
                uiContext {
                    i.setImageResource(R.drawable.ic_round_signal_cellular_off_24)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        i.tooltipText = getString(R.string.status_no_internet)
                    i.show()
                }

            Timber.v("Updating icon, getting download status...")
            val downloadStatus = if (dataClassInitialized)
                dataClass.downloadStatus(activity, firestore)
            else null
            Timber.v("Got download status: $downloadStatus")

            uiContext {
                if (downloadStatus?.isDownloaded() == true) {
                    i.setImageResource(R.drawable.cloud_check)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        i.tooltipText = getString(R.string.status_downloaded)
                    i.show()
                } else if (!appNetworkState.hasInternet) {
                    i.setImageResource(R.drawable.ic_round_signal_cellular_off_24)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        i.tooltipText = getString(R.string.status_no_internet)
                    i.show()
                } else
                    i.hide(setGone = false)
            }
        }
    }
}
