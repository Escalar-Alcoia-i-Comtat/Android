package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.*
import com.arnyminerz.escalaralcoiaicomtat.async.ResultListener
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Area
import com.arnyminerz.escalaralcoiaicomtat.data.map.MapFeatures
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_SMALL_MAP_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.generic.getExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.ZoneAdapter
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.libraries.maps.model.LatLng
import timber.log.Timber

@ExperimentalUnsignedTypes
class AreaActivity : DataClassListActivity() {

    private var justAttached = false
    private var loaded = false
    private var loading = false

    private lateinit var area: Area
    private var areaIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        justAttached = true

        val extras = intent.extras
        if (extras == null) {
            Timber.e("Extras is null")
            onBackPressed()
            return
        }

        areaIndex = intent.getExtra(EXTRA_AREA, -1)
        if (areaIndex < 0) {
            Timber.e("Area is null")
            onBackPressed()
            return
        }
        area = AREAS[areaIndex]

        val transitionName = intent.getExtra(EXTRA_AREA_TRANSITION_NAME)

        binding.titleTextView.text = area.displayName
        binding.titleTextView.transitionName = transitionName

        binding.backImageButton.setOnClickListener { onBackPressed() }
    }

    override fun onResume() {
        super.onResume()

        loaded = false
        justAttached = false
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)
        val smallMapEnabled = SETTINGS_SMALL_MAP_PREF.get(sharedPreferences)

        visibility(binding.map, state.hasInternet && smallMapEnabled)

        if (!loaded && !isDestroyed && !loading)
            try {
                loading = true
                val zones = area.children

                binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
                if (justAttached)
                    binding.recyclerView.layoutAnimation =
                        AnimationUtils.loadLayoutAnimation(
                            this@AreaActivity,
                            R.anim.item_enter_left_animator
                        )
                binding.recyclerView.adapter =
                    ZoneAdapter(zones, this) { _, holder, position ->
                        toast(R.string.toast_loading)
                        Timber.v("Clicked item $position")
                        val intent =
                            Intent(this@AreaActivity, ZoneActivity()::class.java)
                                .putExtra(EXTRA_AREA, areaIndex)
                                .putExtra(EXTRA_ZONE, position)

                        val optionsBundle =
                            ViewCompat.getTransitionName(holder.titleTextView)
                                ?.let { transitionName ->
                                    intent.putExtra(EXTRA_ZONE_TRANSITION_NAME, transitionName)

                                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                                        this,
                                        holder.titleTextView,
                                        transitionName
                                    ).toBundle()
                                } ?: Bundle.EMPTY

                        startActivity(intent, optionsBundle)
                    }

                if (smallMapEnabled) {
                    val mapHelper = MapHelper()
                    mapHelper
                        .withStartingPosition(LatLng(38.7216704, -0.4799751), 12.5f)
                        .loadMap(
                            supportFragmentManager.findFragmentById(R.id.map)!!,
                            { _, googleMap ->
                                googleMap.uiSettings.apply {
                                    isCompassEnabled = false
                                    setAllGesturesEnabled(false)
                                }

                                mapHelper
                                    .loadKML(this, area.kmlAddress, state)
                                    .listen(object : ResultListener<MapFeatures> {
                                        override fun onCompleted(result: MapFeatures) {
                                            googleMap.setOnMapClickListener {
                                                mapHelper.showMapsActivity(
                                                    this@AreaActivity
                                                )
                                            }
                                            googleMap.setOnMarkerClickListener {
                                                mapHelper.showMapsActivity(
                                                    this@AreaActivity
                                                ); return@setOnMarkerClickListener true
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

                loaded = true
            } catch (error: Exception) {
                Timber.e(error, "Could not load area")
            } finally {
                loading = false
            }
    }
}