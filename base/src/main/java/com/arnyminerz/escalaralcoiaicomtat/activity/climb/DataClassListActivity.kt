package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.os.Build
import android.os.Bundle
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.map.DEFAULT_LATITUDE
import com.arnyminerz.escalaralcoiaicomtat.data.map.DEFAULT_LONGITUDE
import com.arnyminerz.escalaralcoiaicomtat.data.map.DEFAULT_ZOOM
import com.arnyminerz.escalaralcoiaicomtat.data.map.ICON_SIZE_MULTIPLIER
import com.arnyminerz.escalaralcoiaicomtat.databinding.LayoutListBinding
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.generic.MapAnyDataToLoadException
import com.arnyminerz.escalaralcoiaicomtat.generic.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.shared.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.parse.ParseAnalytics
import timber.log.Timber
import java.io.FileNotFoundException
import java.util.concurrent.CompletableFuture.runAsync

abstract class DataClassListActivity<T : DataClass<*, *>>(
    private val iconSizeMultiplier: Float = ICON_SIZE_MULTIPLIER,
    private val overrideLoadedMapData: Boolean = false,
) : NetworkChangeListenerActivity() {
    protected lateinit var binding: LayoutListBinding
    protected lateinit var dataClass: T
    private lateinit var mapHelper: MapHelper
    private var mapLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        binding = LayoutListBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        ParseAnalytics.trackAppOpenedInBackground(intent)

        mapHelper = MapHelper(binding.map)
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
        if (mapLoaded)
            binding.loadingLayout.visibility(false)
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

        if (!mapLoaded && hasInternet) {
            Timber.v("Loading map...")
            mapHelper
                .show()
                .withStartingPosition(LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE), DEFAULT_ZOOM)
                .withControllable(false)
                .loadMap(this) { _, map, _ ->
                    mapLoaded = true
                    val kmlAddress = dataClass.kmlAddress

                    if (kmlAddress != null)
                        runAsync {
                            try {
                                mapHelper.loadKML(this@DataClassListActivity, kmlAddress)
                            } catch (e: NoInternetAccessException) {
                                Timber.w("Could not load KML since internet connection is not available")
                                visibility(binding.map, false)
                            } catch (e: FileNotFoundException) {
                                Timber.w("KMZ file not found")
                            } finally {
                                binding.loadingLayout.visibility(false)
                            }
                        }
                    else {
                        Timber.w("KML was not found")
                        binding.loadingLayout.visibility(false)
                    }

                    map.addOnMapClickListener {
                        try {
                            val intent = mapHelper.mapsActivityIntent(this, overrideLoadedMapData)
                            Timber.v("Starting MapsActivity...")
                            startActivity(intent)
                            true
                        } catch (e: MapAnyDataToLoadException) {
                            Timber.w("Clicked on map and any data has been loaded")
                            false
                        }
                    }
                }
        } else if (!hasInternet) {
            binding.loadingLayout.visibility(false)
            mapHelper.hide()
        }
    }

    private fun updateIcon() {
        binding.statusImageView.let { i ->
            if (this::dataClass.isInitialized && dataClass.downloadStatus(this).isDownloaded()) {
                i.setImageResource(R.drawable.cloud_check)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    i.tooltipText = getString(R.string.status_downloaded)
                i.visibility(true)
            } else if (!appNetworkState.hasInternet) {
                i.setImageResource(R.drawable.ic_round_signal_cellular_off_24)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    i.tooltipText = getString(R.string.status_no_internet)
                i.visibility(true)
            } else
                i.visibility(false, setGone = false)
        }
    }
}
