package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.*
import com.arnyminerz.escalaralcoiaicomtat.async.ResultListener
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Zone
import com.arnyminerz.escalaralcoiaicomtat.data.map.MapFeatures
import com.arnyminerz.escalaralcoiaicomtat.exception.AlreadyLoadingException
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_SMALL_MAP_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.MapAnyDataToLoadException
import com.arnyminerz.escalaralcoiaicomtat.generic.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.generic.getExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.SectorsAdapter
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import kotlinx.android.synthetic.main.layout_list.*
import timber.log.Timber

@ExperimentalUnsignedTypes
class ZoneActivity : DataClassListActivity() {

    private var justAttached = false
    private var loaded = false

    private lateinit var zone: Zone
    private var zoneIndex = -1
    private var areaIndex = -1
    private var savedInstanceState: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.savedInstanceState = savedInstanceState
        justAttached = true

        val extras = intent.extras
        if (extras == null) {
            Timber.e("Extras is null")
            onBackPressed()
            return
        }

        areaIndex = intent.getExtra(EXTRA_AREA, -1)
        zoneIndex = intent.getExtra(EXTRA_ZONE, -1)
        if (areaIndex < 0 || zoneIndex < 0) {
            Timber.e("Area or Zone index wasn't specified")
            onBackPressed()
            return
        }
        zone = AREAS[areaIndex][zoneIndex]

        val transitionName = intent.getExtra(EXTRA_ZONE_TRANSITION_NAME)

        title_textView.text = zone.displayName
        title_textView.transitionName = transitionName

        back_imageButton.setOnClickListener { onBackPressed() }
    }

    override fun onResume() {
        super.onResume()
        loaded = false
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)
        val smallMapEnabled = SETTINGS_SMALL_MAP_PREF.get(MainActivity.sharedPreferences)

        visibility(map, state.hasInternet && smallMapEnabled)

        if (!loaded)
            try {
                val sectors = zone.children
                Timber.v("Got ${sectors.size} sectors.")

                recyclerView.layoutManager = LinearLayoutManager(this@ZoneActivity)
                if (justAttached)
                    recyclerView.layoutAnimation =
                        AnimationUtils.loadLayoutAnimation(
                            this@ZoneActivity,
                            R.anim.item_enter_left_animator
                        )
                recyclerView.adapter =
                    SectorsAdapter(
                        this@ZoneActivity,
                        sectors
                    ).withItemListener { _, viewHolder, index ->
                        Timber.v("Clicked item $index")
                        val trn =
                            ViewCompat.getTransitionName(viewHolder.titleTextView)
                                .toString()
                        Timber.v("Transition name: $trn")
                        val intent =
                            Intent(this@ZoneActivity, SectorActivity()::class.java)
                                .putExtra(EXTRA_AREA, areaIndex)
                                .putExtra(EXTRA_ZONE, zoneIndex)
                                .putExtra(EXTRA_SECTOR, index)
                                .putExtra(EXTRA_SECTOR_TRANSITION_NAME, trn)
                        val options =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                this@ZoneActivity, viewHolder.titleTextView, trn
                            )

                        startActivity(intent, options.toBundle())
                    }

                if (smallMapEnabled) {
                    val mapHelper = MapHelper(this@ZoneActivity, R.id.map)
                    mapHelper.loadMap { _, _, googleMap ->
                        googleMap.uiSettings.apply {
                            isCompassEnabled = false
                            setAllGesturesEnabled(false)
                        }

                        mapHelper.loadKML(zone.kmlAddress, state)
                            .listen(object : ResultListener<MapFeatures> {
                                override fun onCompleted(result: MapFeatures) {
                                    googleMap.setOnMapClickListener {
                                        try {
                                            mapHelper.showMapsActivity()
                                        } catch (ex: MapAnyDataToLoadException) {
                                            Timber.e(
                                                ex,
                                                "Map doesn't have any data to show."
                                            )
                                        }
                                    }
                                    googleMap.setOnMarkerClickListener { mapHelper.showMapsActivity(); return@setOnMarkerClickListener true }
                                }

                                override fun onFailure(error: Exception?) {
                                    error?.let { throw it }
                                }
                            })
                    }
                }

                loaded = true
            } catch (_: AlreadyLoadingException) { // Ignore an already loading exception. The content will be loaded from somewhere else
                Timber.v("An AlreadyLoadingException has been thrown while loading the zones in ZoneActivity.") // Let's just warn the debugger this is controlled
            }
        else
            Timber.w("Already loaded!")
    }
}