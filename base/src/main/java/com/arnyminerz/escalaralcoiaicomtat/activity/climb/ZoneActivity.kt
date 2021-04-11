package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.exception.AlreadyLoadingException
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.generic.getExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.SectorsAdapter
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_POSITION
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_SECTOR_COUNT
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_SECTOR_INDEX
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_SECTOR_TRANSITION_NAME
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_ZONE_TRANSITION_NAME
import com.arnyminerz.escalaralcoiaicomtat.view.show
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class ZoneActivity : DataClassListActivity<Zone>() {
    companion object {
        var errorNotStored: Boolean = false
    }

    private var justAttached = false
    private var loaded = false

    private lateinit var areaId: String
    private lateinit var zoneId: String
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

        val areaIdExtra = intent.getExtra(EXTRA_AREA)
        val zoneIdExtra = intent.getExtra(EXTRA_ZONE)
        if (areaIdExtra == null || zoneIdExtra == null) {
            Timber.e("Area or Zone index wasn't specified")
            onBackPressed()
            return
        }
        areaId = areaIdExtra
        zoneId = zoneIdExtra
        dataClass = AREAS[areaId]!![zoneId]

        val transitionName = intent.getExtra(EXTRA_ZONE_TRANSITION_NAME)
        position = intent.getExtra(EXTRA_POSITION, 0)

        binding.titleTextView.text = dataClass.displayName
        binding.titleTextView.transitionName = transitionName

        binding.backImageButton.setOnClickListener { onBackPressed() }
    }

    override fun onResume() {
        super.onResume()
        loaded = false
        justAttached = false

        if (errorNotStored) {
            Timber.w("errorNotStored")
            Snackbar
                .make(binding.root, R.string.toast_error_no_internet, Snackbar.LENGTH_SHORT)
                .show()
            errorNotStored = false
        }
    }

    override fun onStateChangeAsync(state: ConnectivityProvider.NetworkState) {
        super.onStateChangeAsync(state)

        if (!loaded)
            try {
                val sectors = dataClass.getChildren(firestore)
                Timber.v("Got ${sectors.size} sectors.")

                runOnUiThread {
                    binding.recyclerView.let { r ->
                        r.layoutManager = LinearLayoutManager(this)
                        if (justAttached)
                            binding.recyclerView.layoutAnimation =
                                AnimationUtils.loadLayoutAnimation(
                                    this,
                                    R.anim.item_enter_left_animator
                                )
                        r.adapter =
                            SectorsAdapter(
                                this,
                                areaId, zoneId
                            ) { viewHolder, index ->
                                binding.loadingLayout.show()

                                Timber.v("Clicked item $index")
                                val trn =
                                    ViewCompat.getTransitionName(viewHolder.titleTextView)
                                        .toString()
                                Timber.v("Transition name: $trn")
                                val intent =
                                    Intent(this, SectorActivity()::class.java)
                                        .putExtra(EXTRA_AREA, areaId)
                                        .putExtra(EXTRA_ZONE, zoneId)
                                        .putExtra(EXTRA_SECTOR_COUNT, sectors.size)
                                        .putExtra(EXTRA_SECTOR_INDEX, index)
                                        .putExtra(EXTRA_SECTOR_TRANSITION_NAME, trn)
                                val options =
                                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                                        this, viewHolder.titleTextView, trn
                                    )

                                startActivity(intent, options.toBundle())
                            }
                    }
                }

                loaded = true
            } catch (_: AlreadyLoadingException) {
                // Ignore an already loading exception. The content will be loaded from somewhere else
                Timber.v(
                    "An AlreadyLoadingException has been thrown while loading the zones in ZoneActivity."
                ) // Let's just warn the debugger this is controlled
            } catch (_: NoInternetAccessException) {
            }
        else
            Timber.d("Already loaded!")
    }
}
