package com.arnyminerz.escalaralcoiaicomtat.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.PopupMenu
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.ImageViewCompat
import androidx.viewpager2.widget.ViewPager2
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.AreaActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageAppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityMainBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.DownloadsFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.MapFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.SettingsFragmentManager
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.AreasViewFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.MainSettingsFragment.Companion.SettingsPage
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.MainPagerAdapter
import com.arnyminerz.escalaralcoiaicomtat.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_AREA_TRANSITION_NAME
import com.arnyminerz.escalaralcoiaicomtat.shared.LOCATION_PERMISSION_REQUEST_CODE
import com.arnyminerz.escalaralcoiaicomtat.shared.TAB_ITEM_DOWNLOADS
import com.arnyminerz.escalaralcoiaicomtat.shared.TAB_ITEM_EXTRA
import com.arnyminerz.escalaralcoiaicomtat.shared.TAB_ITEM_HOME
import com.arnyminerz.escalaralcoiaicomtat.shared.TAB_ITEM_MAP
import com.arnyminerz.escalaralcoiaicomtat.shared.TAB_ITEM_SETTINGS
import com.arnyminerz.escalaralcoiaicomtat.view.getColorFromAttribute
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.mapbox.android.core.permissions.PermissionsManager
import timber.log.Timber

class MainActivity : LanguageAppCompatActivity() {

    private lateinit var areasViewFragment: AreasViewFragment
    lateinit var mapFragment: MapFragment
        private set
    lateinit var downloadsFragment: DownloadsFragment
        private set
    private lateinit var settingsFragment: SettingsFragmentManager

    var adapter: MainPagerAdapter? = null
    lateinit var binding: ActivityMainBinding

    internal lateinit var firestore: FirebaseFirestore
    internal lateinit var storage: FirebaseStorage

    private fun updateBottomAppBar() {
        Timber.d("Updating bottom app bar...")
        val position = binding.mainViewPager.currentItem
        binding.actionExploreImage.setImageResource(
            if (position == TAB_ITEM_HOME) R.drawable.round_explore_24
            else R.drawable.ic_outline_explore_24
        )
        ImageViewCompat.setImageTintList(
            binding.actionExploreImage, ColorStateList.valueOf(
                getColorFromAttribute(
                    this,
                    if (position == TAB_ITEM_HOME) android.R.attr.textColorPrimary
                    else R.attr.colorControlNormal
                )
            )
        )

        visibility(binding.actionExploreText, position == TAB_ITEM_HOME)
        binding.actionMapImage.setImageResource(
            if (position == TAB_ITEM_MAP) R.drawable.ic_round_map_24
            else R.drawable.ic_outline_map_24
        )
        ImageViewCompat.setImageTintList(
            binding.actionMapImage, ColorStateList.valueOf(
                getColorFromAttribute(
                    this,
                    if (position == TAB_ITEM_MAP) android.R.attr.textColorPrimary
                    else R.attr.colorControlNormal
                )
            )
        )

        visibility(binding.actionMapText, position == TAB_ITEM_MAP)
        binding.actionDownloadsImage.setImageResource(
            if (position == TAB_ITEM_DOWNLOADS) R.drawable.ic_round_cloud_download_24
            else R.drawable.ic_outline_cloud_download_24
        )
        ImageViewCompat.setImageTintList(
            binding.actionDownloadsImage, ColorStateList.valueOf(
                getColorFromAttribute(
                    this,
                    if (position == TAB_ITEM_DOWNLOADS) android.R.attr.textColorPrimary
                    else R.attr.colorControlNormal
                )
            )
        )

        visibility(binding.actionDownloadsText, position == TAB_ITEM_DOWNLOADS)

        binding.mainViewPager.isUserInputEnabled =
            position == TAB_ITEM_HOME || position == TAB_ITEM_DOWNLOADS
    }

    /**
     * @author Arnau Mora
     * @since 20210321
     * @param position The position to navigate to
     */
    private fun navigate(position: Int) {
        if (position == TAB_ITEM_EXTRA)
            PopupMenu(this, binding.actionExtra).apply {
                menuInflater.inflate(R.menu.menu_main_extra, menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.settings -> {
                            binding.mainViewPager.currentItem = TAB_ITEM_SETTINGS
                            true
                        }
                        R.id.share -> {
                            startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).setType("text/plain")
                                        .putExtra(
                                            Intent.EXTRA_TEXT,
                                            getString(R.string.share_text)
                                        ),
                                    getString(R.string.action_share_with)
                                )
                            )
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        else {
            if (!visibility(binding.mainViewPager)) {
                visibility(binding.mainViewPager, true)
                visibility(binding.mainFrameLayout, false)
            }

            binding.mainViewPager.setCurrentItem(position, true)

            updateBottomAppBar()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setSupportActionBar(binding.bottomAppBar)

        areasViewFragment = AreasViewFragment()
        mapFragment = MapFragment()
        downloadsFragment = DownloadsFragment()
        settingsFragment = SettingsFragmentManager()

        binding.mainViewPager.adapter = MainPagerAdapter(
            this,
            hashMapOf(
                TAB_ITEM_HOME to areasViewFragment,
                TAB_ITEM_MAP to mapFragment,
                TAB_ITEM_DOWNLOADS to downloadsFragment,
                TAB_ITEM_SETTINGS to settingsFragment
            )
        )
        binding.mainViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateBottomAppBar()
            }
        })

        binding.actionExplore.setOnClickListener { navigate(TAB_ITEM_HOME) }
        binding.actionMap.setOnClickListener { navigate(TAB_ITEM_MAP) }
        binding.actionDownloads.setOnClickListener { navigate(TAB_ITEM_DOWNLOADS) }
        binding.actionExtra.setOnClickListener { navigate(TAB_ITEM_EXTRA) }

        Timber.v("  --- Found ${AREAS.size} areas ---")

        firestore = Firebase.firestore
        storage = Firebase.storage

        areasViewFragment.setItemClickListener { holder, position ->
            Timber.v("Clicked item %s", position)
            val intent = Intent(this, AreaActivity()::class.java)
                .putExtra(EXTRA_AREA, AREAS[position].objectId)

            val optionsBundle =
                ViewCompat.getTransitionName(holder.titleTextView)?.let { transitionName ->
                    intent.putExtra(EXTRA_AREA_TRANSITION_NAME, transitionName)

                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this,
                        findViewById(R.id.title_textView),
                        transitionName
                    ).toBundle()
                } ?: Bundle.EMPTY

            startActivity(intent, optionsBundle)
        }

        updateBottomAppBar()
    }

    override fun onBackPressed() {
        Timber.v("Going back!")
        when (binding.mainViewPager.currentItem) {
            TAB_ITEM_HOME -> return finishAndRemoveTask()
            TAB_ITEM_DOWNLOADS, TAB_ITEM_MAP -> {
                visibility(binding.mainViewPager, true)
                visibility(binding.mainFrameLayout, false)

                binding.mainViewPager.currentItem = TAB_ITEM_HOME
            }
            TAB_ITEM_SETTINGS -> {
                val settingsFragmentManager =
                    (binding.mainViewPager.adapter as? MainPagerAdapter)?.items
                        ?.get(TAB_ITEM_SETTINGS) as? SettingsFragmentManager
                Timber.e("settingsFragmentManager is null!")
                if (settingsFragmentManager != null && settingsFragmentManager.height > 0)
                    settingsFragmentManager.loadPage(SettingsPage.MAIN, true)
                else binding.mainViewPager.currentItem = TAB_ITEM_HOME
            }
        }
        updateBottomAppBar()
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Timber.v("Got permissions result. Code: %s", requestCode)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (PermissionsManager.areLocationPermissionsGranted(this))
                    areasViewFragment.mapHelper.enableLocationComponent(this)
                areasViewFragment.updateNearbyZones()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}
