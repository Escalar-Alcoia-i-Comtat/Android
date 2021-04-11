package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.os.Build
import android.os.Bundle
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.ZoneActivity.Companion.errorNotStored
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageAppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivitySectorBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.SectorFragment
import com.arnyminerz.escalaralcoiaicomtat.generic.getExtra
import com.arnyminerz.escalaralcoiaicomtat.shared.ARGUMENT_AREA_ID
import com.arnyminerz.escalaralcoiaicomtat.shared.ARGUMENT_SECTOR_INDEX
import com.arnyminerz.escalaralcoiaicomtat.shared.ARGUMENT_ZONE_ID
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_POSITION
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_SECTOR_COUNT
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_SECTOR_INDEX
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_SECTOR_TRANSITION_NAME
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.shared.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.show
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import timber.log.Timber
import java.util.concurrent.CompletableFuture.runAsync

class SectorActivity : LanguageAppCompatActivity() {
    private var transitionName: String? = null

    private lateinit var areaId: String
    private lateinit var zoneId: String
    private var sectorCount: Int = -1

    private val fragments = arrayListOf<SectorFragment>()

    private lateinit var binding: ActivitySectorBinding

    /**
     * Tells if the content has been loaded correctly
     * @author Arnau Mora
     * @since 20210324
     */
    private var loadComplete = false

    /**
     * Updates the Activity's title
     * @author Arnau Mora
     * @since 20210314
     * @param newTitle The new title to set
     */
    @UiThread
    fun updateTitle(newTitle: String? = null, isDownloaded: Boolean = false) {
        if (newTitle == null)
            binding.titleTextView.hide()
        else {
            binding.titleTextView.text = newTitle
            binding.titleTextView.transitionName = transitionName
            binding.titleTextView.show()
        }
        binding.statusImageView.apply {
            if (isDownloaded) {
                setImageResource(R.drawable.cloud_check)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    tooltipText = getString(R.string.status_downloaded)
                show()
            } else if (!appNetworkState.hasInternet) {
                setImageResource(R.drawable.ic_round_signal_cellular_off_24)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    tooltipText = getString(R.string.status_no_internet)
                show()
            } else
                hide()
        }
    }

    /**
     * Sets whether or not the slider can be controlled
     * @author Arnau Mora
     * @since 20210314
     * @param enabled If true, the view pager would be able to slide
     */
    @UiThread
    fun userInputEnabled(enabled: Boolean) {
        binding.sectorViewPager.isUserInputEnabled = enabled
    }

    /**
     * Sets the loading status of the activity
     * @author Arnau Mora
     * @since 20210314
     * @param loading If the activity should be loading
     */
    @UiThread
    fun setLoading(loading: Boolean) {
        visibility(binding.loadingLayout, loading)
        visibility(binding.titleTextView, !loading)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySectorBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val extras = intent.extras
        if (extras == null && savedInstanceState == null) {
            Timber.e("Extras is null and there's no savedInstanceState")
            onBackPressed()
            return
        }

        val areaIdExtra = intent.getExtra(EXTRA_AREA)
        val zoneIdExtra = intent.getExtra(EXTRA_ZONE)
        val sectorCountExtra = intent.getExtra(EXTRA_SECTOR_COUNT, -1)
        val areaIdInstanceState = savedInstanceState?.getString(EXTRA_AREA.key, null)
        val zoneIdInstanceState = savedInstanceState?.getString(EXTRA_ZONE.key, null)
        val sectorCountInstanceState = savedInstanceState?.getInt(EXTRA_SECTOR_COUNT.key, -1)
        val areaIdBothInvalid = areaIdInstanceState == null && areaIdExtra == null
        val zoneIdBothInvalid = zoneIdInstanceState == null && zoneIdExtra == null
        val sectorCountBothInvalid =
            (sectorCountInstanceState == null || sectorCountInstanceState < 0) && sectorCountExtra < 0
        if (areaIdBothInvalid || zoneIdBothInvalid || sectorCountBothInvalid) {
            Timber.e("No loaded data for activity")
            errorNotStored = true
            onBackPressed()
            return
        }

        areaId = areaIdInstanceState ?: areaIdExtra!!
        zoneId = zoneIdInstanceState ?: zoneIdExtra!!
        sectorCount = sectorCountInstanceState ?: sectorCountExtra
        Timber.d("Loading sectors from area $areaId, zone $zoneId...")

        transitionName = intent.getExtra(EXTRA_SECTOR_TRANSITION_NAME)
        if (transitionName == null)
            Timber.w("Transition name is null")

        binding.backImageButton.bringToFront()
        updateTitle()

        binding.backImageButton.setOnClickListener { onBackPressed() }
        binding.statusImageView.setOnClickListener { it.performLongClick() }

        runAsync {
            Timber.v("There are $sectorCount sectors.")
            Timber.d("Initializing fragments...")
            fragments.clear()
            for (i in 0 until sectorCount)
                fragments.add(
                    SectorFragment().apply {
                        arguments = Bundle().apply {
                            putString(ARGUMENT_AREA_ID, areaId)
                            putString(ARGUMENT_ZONE_ID, zoneId)
                            putInt(ARGUMENT_SECTOR_INDEX, i)
                        }
                    }
                )
            runOnUiThread {
                Timber.v("Initializing view pager...")
                Timber.d("  Initializing adapter for ${fragments.size} pages...")
                val adapter = object : FragmentStateAdapter(this) {
                    override fun getItemCount(): Int = fragments.size
                    override fun createFragment(position: Int): Fragment {
                        Timber.d("Creating fragment #$position")
                        return fragments[position]
                    }
                }
                Timber.d("  Initializing page change callback...")
                val callback = object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        Timber.d("Selected page #$position")

                        // Minimize all fragments
                        for (fragment in fragments)
                            fragment.minimize()

                        fragments[position].load()
                    }
                }
                Timber.d("  Setting adapter...")
                binding.sectorViewPager.adapter = adapter
                Timber.d("  Registering callback...")
                binding.sectorViewPager.registerOnPageChangeCallback(callback)
                // If there's an stored positon, load it
                Timber.d("  Setting position")
                val defaultPosition = savedInstanceState?.getInt(EXTRA_POSITION.key)
                    ?: intent.getExtra(EXTRA_SECTOR_INDEX, 0)
                binding.sectorViewPager.setCurrentItem(defaultPosition, false)
                fragments[defaultPosition].load()

                loadComplete = true
                Timber.d("Load completed, hiding loading layout")
                setLoading(false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (loadComplete && this::binding.isInitialized)
            setLoading(false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (this::areaId.isInitialized && this::zoneId.isInitialized) {
            outState.putString(EXTRA_AREA.key, areaId)
            outState.putString(EXTRA_ZONE.key, zoneId)
            outState.putInt(EXTRA_POSITION.key, binding.sectorViewPager.currentItem)
        }
        super.onSaveInstanceState(outState)
    }
}
