package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.os.Bundle
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.databinding.LayoutListBinding
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_SMALL_MAP_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.generic.onUiThread
import com.arnyminerz.escalaralcoiaicomtat.generic.runAsync
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.mapbox.mapboxsdk.geometry.LatLng

@ExperimentalUnsignedTypes
abstract class DataClassListActivity<T : DataClass<*, *>> : NetworkChangeListenerActivity() {
    protected lateinit var binding: LayoutListBinding
    protected lateinit var dataClass: T

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutListBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        val hasInternet = state.hasInternet
        visibility(binding.noInternetCard.noInternetCardView, !hasInternet)

        val smallMapEnabled = SETTINGS_SMALL_MAP_PREF.get(sharedPreferences)

        visibility(binding.map, state.hasInternet && smallMapEnabled)
        binding.loadingLayout.hide()

        if (smallMapEnabled)
            onUiThread {
                MapHelper()
                    .withStartingPosition(LatLng(38.7216704, -0.4799751), 12.5)
                    .loadMap(
                        binding.map
                    ) { mapView, map, _ ->
                        map.uiSettings.apply {
                            isCompassEnabled = false
                            setAllGesturesEnabled(false)
                        }

                        runAsync {
                            try{
                                loadKML(this@DataClassListActivity, dataClass.kmlAddress, state)
                            }catch (e: NoInternetAccessException){
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
}