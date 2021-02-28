package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.os.Bundle
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.async.ResultListener
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.map.MapFeatures
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.databinding.LayoutListBinding
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_SMALL_MAP_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.generic.onUiThread
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.libraries.maps.model.LatLng
import timber.log.Timber

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
                    .withStartingPosition(LatLng(38.7216704, -0.4799751), 12.5f)
                    .loadMap(
                        supportFragmentManager.findFragmentById(R.id.map)!!,
                        { _, googleMap ->
                            googleMap.uiSettings.apply {
                                isCompassEnabled = false
                                setAllGesturesEnabled(false)
                            }

                            loadKML(this@DataClassListActivity, dataClass.kmlAddress, state)
                                .listen(object : ResultListener<MapFeatures> {
                                    override fun onCompleted(result: MapFeatures) {
                                        googleMap.setOnMapClickListener {
                                            showMapsActivity(this@DataClassListActivity)
                                        }
                                        googleMap.setOnMarkerClickListener {
                                            showMapsActivity(this@DataClassListActivity)
                                            return@setOnMarkerClickListener true
                                        }
                                    }

                                    override fun onFailure(error: Exception?) {
                                        if (error is NoInternetAccessException)
                                            visibility(binding.map, false)
                                        else
                                            error?.let { throw it }
                                    }
                                })
                        }, {
                            Timber.e(it, "Could not load map:")
                        })
            }
    }
}