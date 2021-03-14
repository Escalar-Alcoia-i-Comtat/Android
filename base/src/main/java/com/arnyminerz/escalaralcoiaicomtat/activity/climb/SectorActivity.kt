package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.arnyminerz.escalaralcoiaicomtat.activity.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.activity.EXTRA_POSITION
import com.arnyminerz.escalaralcoiaicomtat.activity.EXTRA_SECTOR_TRANSITION_NAME
import com.arnyminerz.escalaralcoiaicomtat.activity.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Zone
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.count
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivitySectorBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.ARGUMENT_AREA_ID
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.ARGUMENT_SECTOR_INDEX
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.ARGUMENT_ZONE_ID
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.SectorFragment
import com.arnyminerz.escalaralcoiaicomtat.generic.getExtra
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.show
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.parse.ParseObject
import com.parse.ParseQuery
import timber.log.Timber

class SectorActivity : NetworkChangeListenerActivity() {
    private var transitionName: String? = null

    private lateinit var areaId: String
    private lateinit var zoneId: String

    private val fragments = arrayListOf<Fragment>()

    private lateinit var binding: ActivitySectorBinding

    /**
     * Updates the Activity's title
     * @author Arnau Mora
     * @since 20210314
     * @param newTitle The new title to set
     */
    fun updateTitle(newTitle: String? = null) {
        if (newTitle == null)
            binding.titleTextView.hide()
        else {
            binding.titleTextView.text = newTitle
            binding.titleTextView.transitionName = transitionName
            binding.titleTextView.show()
        }
    }

    /**
     * Sets whether or not the slider can be controlled
     * @author Arnau Mora
     * @since 20210314
     * @param enabled If true, the view pager would be able to slide
     */
    fun userInputEnabled(enabled: Boolean) {
        binding.sectorViewPager.isUserInputEnabled = enabled
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
        if (areaIdExtra == null || zoneIdExtra == null) {
            return goBack()
        } else {
            areaId = areaIdExtra
            zoneId = zoneIdExtra
            Timber.d("Loading sectors from area $areaId, zone $zoneId...")
        }

        transitionName = intent.getExtra(EXTRA_SECTOR_TRANSITION_NAME)
        if (transitionName == null)
            Timber.w("Transition name is null")

        binding.backImageButton.bringToFront()
        updateTitle()

        binding.backImageButton.setOnClickListener { onBackPressed() }
        binding.noInternetImageView.setOnClickListener { it.performLongClick() }

        val parentQuery = ParseQuery.getQuery<ParseObject>(Zone.NAMESPACE)
        parentQuery.whereEqualTo("objectId", zoneId)

        val query = ParseQuery.getQuery<ParseObject>(Sector.NAMESPACE)
        query.addAscendingOrder("displayName")
        query.whereMatchesQuery("zone", parentQuery)
        Timber.v("Counting sectors in zone $zoneId")
        query.count { count, error ->
            if (error != null)
                throw error

            Timber.v("There are $count sectors.")

            fragments.clear()
            for (sector in 0 until count)
                fragments.add(
                    SectorFragment().apply {
                        arguments = Bundle().apply {
                            putString(ARGUMENT_AREA_ID, areaId)
                            putString(ARGUMENT_ZONE_ID, zoneId)
                            putInt(ARGUMENT_SECTOR_INDEX, sector)
                        }
                    }
                )
            binding.sectorViewPager.adapter = object : FragmentStateAdapter(this) {
                override fun getItemCount(): Int = count
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

                    for (fragment in fragments)
                        (fragment as? SectorFragment)?.minimize()

                    updateTitle()
                }
            })
            // If there's an stored positon, load it
            if (savedInstanceState != null)
                binding.sectorViewPager.setCurrentItem(
                    savedInstanceState.getInt(EXTRA_POSITION.key),
                    false
                )
            binding.loadingLayout.hide()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(EXTRA_AREA.key, areaId)
        outState.putString(EXTRA_ZONE.key, zoneId)
        outState.putInt(EXTRA_POSITION.key, binding.sectorViewPager.currentItem)
        super.onSaveInstanceState(outState)
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        if (!this::binding.isInitialized)
            return Timber.e("Binding not initialized")

        val hasInternet = state.hasInternet
        Timber.v("Has internet? $hasInternet")
        visibility(binding.noInternetImageView, !hasInternet)
    }

    private fun goBack() {
        Timber.e("No loaded data for activity")
        errorNotStored = true
        onBackPressed()
    }
}
