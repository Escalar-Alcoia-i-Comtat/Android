package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.area.Area
import com.arnyminerz.escalaralcoiaicomtat.data.map.ICON_SIZE_MULTIPLIER
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.generic.getExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.ZoneAdapter
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_AREA_TRANSITION_NAME
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_POSITION
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_ZONE_TRANSITION_NAME
import com.arnyminerz.escalaralcoiaicomtat.view.show
import timber.log.Timber

class AreaActivity : DataClassListActivity<Area>(ICON_SIZE_MULTIPLIER, true) {

    private var justAttached = false
    private var loaded = false
    private var loading = false

    private lateinit var areaId: String
    private var position = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        justAttached = true

        val extras = intent.extras
        if (extras == null) {
            Timber.e("Extras is null")
            onBackPressed()
            return
        }

        intent.getExtra(EXTRA_AREA)?.let {
            areaId = it
            Timber.d("Area id: $areaId")
        }
        if (!this::areaId.isInitialized) {
            Timber.e("Area extra is null")
            onBackPressed()
            return
        }
        if (!AREAS.containsKey(areaId)) {
            Timber.e("Area is not loaded in AREAS")
            onBackPressed()
            return
        }
        dataClass = AREAS[areaId]!!
        Timber.d("DataClass id: ${dataClass.objectId}")

        position = intent.getExtra(
            EXTRA_POSITION,
            savedInstanceState?.getInt(EXTRA_POSITION.key, 0) ?: 0
        )
        Timber.d("Current position: $position")

        binding.titleTextView.text = dataClass.displayName
        intent.getExtra(EXTRA_AREA_TRANSITION_NAME)?.let {
            binding.titleTextView.transitionName = it
        }

        binding.backImageButton.setOnClickListener { onBackPressed() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(EXTRA_POSITION.key, position)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()

        loaded = false
        justAttached = false
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)

        if (!loaded && !isDestroyed && !loading) {
            loading = true
            try {
                val zones = dataClass.children

                binding.recyclerView.apply {
                    layoutManager = GridLayoutManager(this@AreaActivity, 2)
                    if (justAttached)
                        binding.recyclerView.layoutAnimation =
                            AnimationUtils.loadLayoutAnimation(
                                this@AreaActivity,
                                R.anim.item_enter_left_animator
                            )
                    adapter =
                        ZoneAdapter(zones, this@AreaActivity) { _, holder, position ->
                            binding.loadingLayout.show()
                            runOnUiThread {
                                Timber.v("Clicked item $position")
                                val intent =
                                    Intent(this@AreaActivity, ZoneActivity()::class.java)
                                        .putExtra(EXTRA_AREA, areaId)
                                        .putExtra(EXTRA_ZONE, AREAS[areaId]!![position].objectId)

                                val optionsBundle =
                                    ViewCompat.getTransitionName(holder.titleTextView)
                                        ?.let { transitionName ->
                                            intent.putExtra(
                                                EXTRA_ZONE_TRANSITION_NAME,
                                                transitionName
                                            )

                                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                                this@AreaActivity,
                                                holder.titleTextView,
                                                transitionName
                                            ).toBundle()
                                        } ?: Bundle.EMPTY

                                startActivity(intent, optionsBundle)
                            }
                        }
                    scrollToPosition(position)
                }
                loaded = true
            } catch (_: NoInternetAccessException) {
            } finally {
                loading = false
            }
        }
    }
}
