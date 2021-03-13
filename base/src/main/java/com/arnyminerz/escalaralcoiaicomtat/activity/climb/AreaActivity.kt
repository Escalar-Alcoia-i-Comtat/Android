package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.*
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Area
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.generic.getExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.ZoneAdapter
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.show
import timber.log.Timber

private const val ICON_SIZE_MULTIPLIER = .2f

class AreaActivity : DataClassListActivity<Area>(ICON_SIZE_MULTIPLIER, true) {

    private var justAttached = false
    private var loaded = false
    private var loading = false

    private lateinit var areaId: String

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
        } ?: run {
            Timber.e("Area is null")
            onBackPressed()
            return
        }
        dataClass = AREAS[areaId]!!

        val transitionName = intent.getExtra(EXTRA_AREA_TRANSITION_NAME)

        binding.titleTextView.text = dataClass.displayName
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

        if (!loaded && !isDestroyed && !loading) {
            loading = true
            try {
                val zones = dataClass.children

                binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
                if (justAttached)
                    binding.recyclerView.layoutAnimation =
                        AnimationUtils.loadLayoutAnimation(
                            this@AreaActivity,
                            R.anim.item_enter_left_animator
                        )
                binding.recyclerView.adapter =
                    ZoneAdapter(zones, this) { _, holder, position ->
                        binding.loadingLayout.show()
                        Handler(Looper.getMainLooper()).post {
                            Timber.v("Clicked item $position")
                            val intent =
                                Intent(this@AreaActivity, ZoneActivity()::class.java)
                                    .putExtra(EXTRA_AREA, areaId)
                                    .putExtra(EXTRA_ZONE, AREAS[areaId]!![position].objectId)

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
                    }
                loaded = true
            } catch (_: NoInternetAccessException) {
            } finally {
                loading = false
            }
        }
    }
}
