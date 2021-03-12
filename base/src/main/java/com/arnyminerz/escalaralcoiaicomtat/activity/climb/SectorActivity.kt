package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.os.Bundle
import androidx.collection.arrayMapOf
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.arnyminerz.escalaralcoiaicomtat.activity.*
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerFragmentActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Sector
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivitySectorBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.ARGUMENT_SECTOR
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.SectorFragment
import com.arnyminerz.escalaralcoiaicomtat.generic.getExtra
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import timber.log.Timber

@ExperimentalUnsignedTypes
class SectorActivity : NetworkChangeListenerFragmentActivity() {
    private var transitionName: String? = null

    private lateinit var areaId: String
    private lateinit var zoneId: String
    private lateinit var sectorId: String
    private val sectorIndex: Int
        get() {
            for ((s, sector) in AREAS[areaId]!![zoneId].withIndex())
                if (sector.objectId == sectorId)
                    return s
            return -1
        }
    val sectors = arrayMapOf<String, Sector>()

    private val fragments = arrayListOf<Fragment>()

    lateinit var binding: ActivitySectorBinding

    private fun updateTitle() {
        binding.titleTextView.text = sectors[sectorId]?.displayName
        binding.titleTextView.transitionName = transitionName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySectorBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val extras = intent.extras
        if (extras == null) {
            Timber.e("Extras is null")
            onBackPressed()
            return
        }

        val areaIdExtra = intent.getExtra(EXTRA_AREA)
        val zoneIdExtra = intent.getExtra(EXTRA_ZONE)
        val sectorIdExtra = intent.getExtra(EXTRA_SECTOR)
        if (areaIdExtra == null || zoneIdExtra == null) {
            Timber.e("Area or Zone index wasn't specified")
            onBackPressed()
            return
        } else {
            areaId = areaIdExtra
            zoneId = zoneIdExtra
            Timber.d("Loading sectors from area $areaId, zone $zoneId...")
        }
        if (savedInstanceState != null)
            sectorId = savedInstanceState.getString(EXTRA_SECTOR.key, sectorId)
        sectorId = if (sectorIdExtra == null) {
            Timber.v("Sector Id not passed. Loading the first one...")
            AREAS[areaId]!![zoneId].children[0].objectId
        } else sectorIdExtra
        sectors.clear()
        for (sector in AREAS[areaId]!![zoneId])
            sectors[sector.objectId] = sector

        transitionName = intent.getExtra(EXTRA_SECTOR_TRANSITION_NAME)
        if (transitionName == null)
            Timber.w("Transition name is null")

        binding.backImageButton.bringToFront()
        updateTitle()

        binding.backImageButton.setOnClickListener { onBackPressed() }
        binding.noInternetImageView.setOnClickListener { it.performLongClick() }

        fragments.clear()
        for (sector in sectors.values)
            fragments.add(
                SectorFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(ARGUMENT_SECTOR, sector)
                    }
                }
            )

        Timber.d("Sector ID: $sectorId")
        binding.sectorViewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = sectors.size
            override fun createFragment(position: Int): Fragment {
                Timber.d("Creating fragment #$position")
                return fragments[position]
            }
        }
        binding.sectorViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                Timber.d("Selected page #$position")
                sectorId = sectors.values.toList()[position].objectId

                for (fragment in fragments)
                    (fragment as SectorFragment).minimize()

                updateTitle()
            }
        })
        binding.sectorViewPager.setCurrentItem(sectorIndex, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(EXTRA_SECTOR.key, sectorId)
        super.onSaveInstanceState(outState)
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        if (!this::binding.isInitialized)
            return Timber.e("Binding not initialized")

        val hasInternet = state.hasInternet
        Timber.v("Has internet? $hasInternet")
        visibility(binding.noInternetImageView, !hasInternet)
    }
}
