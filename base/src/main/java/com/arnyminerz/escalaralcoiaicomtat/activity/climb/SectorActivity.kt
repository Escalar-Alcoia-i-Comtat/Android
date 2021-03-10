package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.*
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerFragmentActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Sector
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivitySectorBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.SectorFragment
import com.arnyminerz.escalaralcoiaicomtat.generic.getExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import timber.log.Timber

@ExperimentalUnsignedTypes
class SectorActivity : NetworkChangeListenerFragmentActivity() {
    private var transitionName: String? = null

    private var areaIndex = -1
    private var zoneIndex = -1
    private var sector: Int = 0
    val sectors = arrayListOf<Sector>()

    private val fragments = arrayListOf<Fragment>()

    private lateinit var binding: ActivitySectorBinding

    private fun updateTitle() {
        binding.titleTextView.text = sectors[sector].displayName
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

        areaIndex = intent.getExtra(EXTRA_AREA, -1)
        zoneIndex = intent.getExtra(EXTRA_ZONE, -1)
        sector = intent.getExtra(EXTRA_SECTOR, 0)
        if (savedInstanceState != null)
            sector = savedInstanceState.getInt(EXTRA_SECTOR.key, sector)
        if (areaIndex < 0 || zoneIndex < 0) {
            Timber.e("Area or Zone index wasn't specified")
            onBackPressed()
            return
        } else
            Timber.d("Loading sectors from area #$areaIndex, zone #$zoneIndex...")
        sectors.clear()
        sectors.addAll(AREAS[areaIndex][zoneIndex].children)

        transitionName = intent.getExtra(EXTRA_SECTOR_TRANSITION_NAME)
        if (transitionName == null)
            Timber.w("Transition name is null")

        binding.backImageButton.bringToFront()
        updateTitle()

        binding.backImageButton.setOnClickListener { onBackPressed() }
        binding.noInternetImageView.setOnClickListener { toast(R.string.toast_error_no_internet) }

        fragments.clear()
        for (sector in sectors)
            fragments.add(SectorFragment(sector, binding.sectorViewPager))

        Timber.d("Sector index: $sector")
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
                sector = position

                for (fragment in fragments)
                    (fragment as SectorFragment).minimize()

                updateTitle()
            }
        })
        binding.sectorViewPager.setCurrentItem(sector, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(EXTRA_SECTOR.key, sector)
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
