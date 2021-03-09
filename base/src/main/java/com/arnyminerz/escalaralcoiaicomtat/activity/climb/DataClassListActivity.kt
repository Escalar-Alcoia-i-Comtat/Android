package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.os.Bundle
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.DataClass
import com.arnyminerz.escalaralcoiaicomtat.databinding.LayoutListBinding
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.generic.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.generic.runAsync
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.mapbox.mapboxsdk.geometry.LatLng
import timber.log.Timber
import java.io.FileNotFoundException

const val DEFAULT_LAT = 38.7216704
const val DEFAULT_LON = -0.4799751
const val DEFAULT_ZOOM = 12.5

@ExperimentalUnsignedTypes
abstract class DataClassListActivity<T : DataClass<*, *>> : NetworkChangeListenerActivity() {
    protected lateinit var binding: LayoutListBinding
    protected lateinit var dataClass: T
    private lateinit var mapHelper: MapHelper
    private var mapLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutListBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        mapHelper = MapHelper(binding.map)
        mapHelper.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        mapHelper.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapHelper.onResume()
        if (mapLoaded)
            binding.loadingLayout.hide()
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

        if (!mapLoaded && hasInternet) {
            Timber.v("Loading map...")
            mapHelper
                .withStartingPosition(LatLng(DEFAULT_LAT, DEFAULT_LON), DEFAULT_ZOOM)
                .withControllable(false)
                .loadMap(this) { _, map, _ ->
                    mapLoaded = true
                    val kmlAddress = dataClass.kmlAddress

                    if (kmlAddress != null)
                        runAsync {
                            try {
                                mapHelper.loadKML(
                                    this@DataClassListActivity,
                                    kmlAddress,
                                    state
                                )
                            } catch (e: NoInternetAccessException) {
                                Timber.w("Could not load KML since internet connection is not available")
                                visibility(binding.map, false)
                            } catch (e: FileNotFoundException) {
                                Timber.w("KMZ file not found")
                            } finally {
                                runOnUiThread { binding.loadingLayout.hide() }
                            }
                        }
                    else {
                        Timber.w("KML was not found")
                        binding.loadingLayout.hide()
                    }

                    map.addOnMapClickListener {
                        mapHelper.showMapsActivity(this@DataClassListActivity)
                        true
                    }
                }
        }
    }
}
