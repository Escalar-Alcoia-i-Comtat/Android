package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.exception.AlreadyLoadingException
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.generic.doAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.getExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.launch
import com.arnyminerz.escalaralcoiaicomtat.generic.put
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.uiContext
import com.arnyminerz.escalaralcoiaicomtat.list.model.dwdataclass.DwDataClassAdapter
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_POSITION
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_SECTOR_COUNT
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_SECTOR_INDEX
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_SECTOR_TRANSITION_NAME
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_STATIC
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_ZONE_TRANSITION_NAME
import com.arnyminerz.escalaralcoiaicomtat.shared.appNetworkState
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class ZoneActivity : DataClassListActivity<Zone>() {
    companion object {
        var errorNotStored: Boolean = false
    }

    private var justAttached = false
    private var loaded = false
    private var dataClassInitialized = false

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

        val areaIdExtra = intent.getExtra(EXTRA_AREA) ?: savedInstanceState?.getExtra(EXTRA_AREA)
        val zoneIdExtra = intent.getExtra(EXTRA_ZONE) ?: savedInstanceState?.getExtra(EXTRA_ZONE)
        if (areaIdExtra == null || zoneIdExtra == null) {
            Timber.e("Area or Zone index wasn't specified")
            onBackPressed()
            return
        }
        areaId = areaIdExtra
        zoneId = zoneIdExtra
        doAsync {
            val area = AREAS[areaId] ?: kotlin.run {
                Timber.w("Could not find area \"$areaId\" in AREAS.")
                return@doAsync
            }
            val zones = area.getChildren(firestore)
            dataClass = zones[zoneId] ?: kotlin.run {
                Timber.w("Could not find zone \"$zoneId\" in \"$areaId\".")
                return@doAsync
            }

            val transitionName = intent.getExtra(EXTRA_ZONE_TRANSITION_NAME)
            position = intent.getExtra(EXTRA_POSITION, 0)

            uiContext {
                binding.titleTextView.text = dataClass.displayName
                binding.titleTextView.transitionName = transitionName

                binding.backImageButton.setOnClickListener { onBackPressed() }

                dataClassInitialized = true
            }
            onStateChangeAsync(appNetworkState)
        }
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.put(EXTRA_AREA, areaId)
        outState.put(EXTRA_ZONE, zoneId)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (getExtra(EXTRA_STATIC, false))
            launch(AreaActivity::class.java) {
                putExtra(EXTRA_AREA, areaId)
            }
        else super.onBackPressed()
    }

    override suspend fun onStateChangeAsync(state: ConnectivityProvider.NetworkState) {
        super.onStateChangeAsync(state)

        if (!loaded && dataClassInitialized)
            try {
                val sectors = arrayListOf<Sector>()
                dataClass.getChildren(firestore).toCollection(sectors)

                Timber.v("Got ${sectors.size} sectors.")

                uiContext {
                    val r = binding.recyclerView
                    r.layoutManager = LinearLayoutManager(this)
                    if (justAttached)
                        r.layoutAnimation =
                            AnimationUtils.loadLayoutAnimation(
                                this,
                                R.anim.item_enter_left_animator
                            )
                    r.adapter = DwDataClassAdapter(
                        this,
                        sectors,
                        2,
                        resources.getDimension(R.dimen.zone_item_height).toInt()
                    ) { _, viewHolder, index ->
                        binding.loadingIndicator.show()

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
