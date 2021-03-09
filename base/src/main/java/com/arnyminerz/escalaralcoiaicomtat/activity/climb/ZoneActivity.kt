package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.*
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Zone
import com.arnyminerz.escalaralcoiaicomtat.exception.AlreadyLoadingException
import com.arnyminerz.escalaralcoiaicomtat.generic.getExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.SectorsAdapter
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.show
import timber.log.Timber

@ExperimentalUnsignedTypes
class ZoneActivity : DataClassListActivity<Zone>() {

    private var justAttached = false
    private var loaded = false

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
        dataClass = AREAS[areaIndex][zoneIndex]

        val transitionName = intent.getExtra(EXTRA_ZONE_TRANSITION_NAME)

        binding.titleTextView.text = dataClass.displayName
        binding.titleTextView.transitionName = transitionName

        binding.backImageButton.setOnClickListener { onBackPressed() }
    }

    override fun onResume() {
        super.onResume()
        loaded = false
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)

        if (!loaded)
            try {
                val sectors = dataClass.children
                Timber.v("Got ${sectors.size} sectors.")

                binding.recyclerView.layoutManager = LinearLayoutManager(this@ZoneActivity)
                if (justAttached)
                    binding.recyclerView.layoutAnimation =
                        AnimationUtils.loadLayoutAnimation(
                            this@ZoneActivity,
                            R.anim.item_enter_left_animator
                        )
                binding.recyclerView.adapter =
                    SectorsAdapter(
                        this@ZoneActivity,
                        sectors
                    ) { _, viewHolder, index ->
                        binding.loadingLayout.show()
                        Handler(Looper.getMainLooper()).post {
                            Timber.v("Clicked item $index")
                            val trn =
                                ViewCompat.getTransitionName(viewHolder.titleTextView)
                                    .toString()
                            Timber.v("Transition name: $trn")
                            val intent =
                                Intent(
                                    this@ZoneActivity,
                                    SectorActivity()::class.java
                                )
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
                    }

                loaded = true
            } catch (_: AlreadyLoadingException) {
                // Ignore an already loading exception. The content will be loaded from somewhere else
                Timber.v(
                    "An AlreadyLoadingException has been thrown while loading the zones in ZoneActivity."
                ) // Let's just warn the debugger this is controlled
            } else
            Timber.d("Already loaded!")
    }
}
