package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.os.Bundle
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.databinding.LayoutListBinding
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_SMALL_MAP_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.generic.runAsync
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.mapbox.mapboxsdk.geometry.LatLng

@ExperimentalUnsignedTypes
abstract class DataClassListActivity<T : DataClass<*, *>> : NetworkChangeListenerActivity() {
    protected lateinit var binding: LayoutListBinding
    protected lateinit var dataClass: T
    private lateinit var mapHelper: MapHelper

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

        val smallMapEnabled = SETTINGS_SMALL_MAP_PREF.get(sharedPreferences)

        visibility(binding.map, state.hasInternet && smallMapEnabled)
        binding.loadingLayout.hide()

        if (smallMapEnabled)
            mapHelper
                .withStartingPosition(LatLng(38.7216704, -0.4799751), 12.5)
                .loadMap { mapView, map, _ ->
                    map.uiSettings.apply {
                        isCompassEnabled = false
                        setAllGesturesEnabled(false)
                    }

                    runAsync {
                        try {
                            loadKML(this@DataClassListActivity, dataClass.kmlAddress, state)
                        } catch (e: NoInternetAccessException) {
                            visibility(mapView, false)
                        }
                    }

                    map.addOnMapClickListener {
                        showMapsActivity(this@DataClassListActivity)
                        true
                    }
                }
    }
}