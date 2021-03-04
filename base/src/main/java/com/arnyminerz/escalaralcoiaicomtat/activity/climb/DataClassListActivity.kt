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
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import timber.log.Timber
import java.io.FileNotFoundException

@ExperimentalUnsignedTypes
abstract class DataClassListActivity<T : DataClass<*, *>>
    : NetworkChangeListenerActivity(), OnMapReadyCallback, Style.OnStyleLoaded {
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

        if (!mapLoaded) {
            Timber.v("Loading map...")
            mapHelper
                .withStartingPosition(LatLng(38.7216704, -0.4799751), 12.5)
                .loadMap(this)
        }
    }

    lateinit var mapboxMap: MapboxMap
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapHelper.loadMapStyles(mapboxMap, this)
    }

    override fun onStyleLoaded(style: Style) {
        mapLoaded = true
        binding.loadingLayout.hide()

        runAsync {
            try {
                mapHelper.loadKML(this@DataClassListActivity, dataClass.kmlAddress, networkState)
            } catch (e: NoInternetAccessException) {
                Timber.w("Could not load KML since internet connection is not available")
                visibility(binding.map, false)
            } catch (e: FileNotFoundException) {
                Timber.w("KMZ file not found")
            }
        }

        mapboxMap.addOnMapClickListener {
            mapHelper.showMapsActivity(this@DataClassListActivity)
            true
        }
    }
}