package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.ensureGet
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.has
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_AREA_TRANSITION_NAME
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_POSITION
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_STATIC
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_ZONE_TRANSITION_NAME
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.put
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.list.model.dwdataclass.DwDataClassAdapter
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import timber.log.Timber

/**
 * A [DataClassListActivity] for exploring the interior of an [Area].
 * The [Area] to explore must be passed through [EXTRA_AREA].
 * @author Arnau Mora
 * @since 20210719
 * @see Area
 * @see DataClassListActivity
 */
class AreaActivity : DataClassListActivity<Area>(true) {
    /**
     * If the fragment has just been attached. This will tell whether or not to animate the items
     * in the recycler view.
     * @author Arnau Mora
     * @since 20210719
     */
    private var justAttached = false

    /**
     * If the contents of the [Area] have been loaded.
     * @author Arnau Mora
     * @since 20210719
     */
    private var loaded = false

    /**
     * If the contents of the [Area] are being loaded.
     * @author Arnau Mora
     * @since 20210719
     */
    private var loading = false

    /**
     * The id of the loaded [Area].
     * @author Arnau Mora
     * @since 20210719
     */
    private lateinit var areaId: String

    /**
     * The current position of the recycler view.
     * @author Arnau Mora
     * @since 20210719
     */
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

        val areaId = intent.getExtra(EXTRA_AREA) ?: savedInstanceState?.getExtra(EXTRA_AREA)

        if (areaId == null) {
            Timber.e("Area extra is null")
            onBackPressed()
            return
        }

        this.areaId = areaId
        if (!AREAS.has(areaId)) {
            Timber.e("Area is not loaded in AREAS")
            onBackPressed()
            return
        }
        dataClass = AREAS.ensureGet(areaId)
        Timber.d("DataClass id: ${dataClass.objectId}")

        position =
            intent.getExtra(EXTRA_POSITION) ?: savedInstanceState?.getInt(EXTRA_POSITION.key, 0)
                    ?: 0
        Timber.d("Current position: $position")

        binding.titleTextView.text = dataClass.displayName
        binding.titleTextView.transitionName = intent.getExtra(EXTRA_AREA_TRANSITION_NAME)

        binding.backImageButton.setOnClickListener { onBackPressed() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(EXTRA_POSITION.key, position)
        outState.put(EXTRA_AREA, areaId)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (getExtra(EXTRA_STATIC, false))
            launch(MainActivity::class.java)
        else super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()

        loaded = false
        justAttached = false
    }

    override suspend fun onStateChangeAsync(state: ConnectivityProvider.NetworkState) {
        super.onStateChangeAsync(state)

        if (!loaded && !isDestroyed && !loading) {
            loading = true
            try {
                Timber.v("Getting children zones...")
                val zones = arrayListOf<Zone>()
                dataClass.getChildren(firestore).toCollection(zones)
                Timber.v("Got zones.")

                uiContext {
                    Timber.v("Preparing recycler view...")
                    binding.recyclerView.apply {
                        layoutManager = GridLayoutManager(this@AreaActivity, 2)
                        if (justAttached)
                            binding.recyclerView.layoutAnimation =
                                AnimationUtils.loadLayoutAnimation(
                                    this@AreaActivity,
                                    R.anim.item_enter_left_animator
                                )
                        adapter =
                            DwDataClassAdapter(
                                this@AreaActivity,
                                zones,
                                1,
                                resources.getDimension(R.dimen.area_item_height).toInt()
                            ) { _, holder, position ->
                                binding.loadingIndicator.show()

                                Timber.v("Clicked item $position")
                                val intent =
                                    Intent(this@AreaActivity, ZoneActivity()::class.java)
                                        .putExtra(EXTRA_AREA, areaId)
                                        .putExtra(
                                            EXTRA_ZONE,
                                            zones[position].objectId
                                        )

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
                        scrollToPosition(position)
                    }
                }
                loaded = true
            } catch (_: NoInternetAccessException) {
            } finally {
                loading = false
            }
        }
    }
}
