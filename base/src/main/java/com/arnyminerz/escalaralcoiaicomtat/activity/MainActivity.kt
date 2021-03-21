package com.arnyminerz.escalaralcoiaicomtat.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.PopupMenu
import androidx.collection.arrayMapOf
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.viewpager2.widget.ViewPager2
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.AreaActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.data.IntroShowReason
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Area
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityMainBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.DownloadsFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.MapFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.SettingsFragmentManager
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.AreasViewFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.LOCATION_PERMISSION_REQUEST
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.MainSettingsFragment.Companion.SettingsPage
import com.arnyminerz.escalaralcoiaicomtat.generic.IntentExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.deleteIfExists
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.MainPagerAdapter
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.notification.createNotificationChannels
import com.arnyminerz.escalaralcoiaicomtat.storage.filesDir
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.show
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.parse.ParseAnalytics
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid
import io.sentry.android.timber.SentryTimberIntegration
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File

val EXTRA_AREA = IntentExtra<String>("area")
val EXTRA_ZONE = IntentExtra<String>("zone")
val EXTRA_SECTOR_INDEX = IntentExtra<Int>("sector_index")

val EXTRA_POSITION = IntentExtra<Int>("position")

val EXTRA_ZONE_TRANSITION_NAME = IntentExtra<String>("zone_transition")
val EXTRA_AREA_TRANSITION_NAME = IntentExtra<String>("area_transition")
val EXTRA_SECTOR_TRANSITION_NAME = IntentExtra<String>("sector_transition")

const val TAB_ITEM_HOME = 0
const val TAB_ITEM_MAP = 1
const val TAB_ITEM_DOWNLOADS = 2
const val TAB_ITEM_SETTINGS = 3
const val TAB_ITEM_EXTRA = -1

const val UPDATE_CHECKER_WORK_NAME = "update_checker"
const val UPDATE_CHECKER_TAG = "update"
const val UPDATE_CHECKER_FLEX_MINUTES: Long = 15

val AREAS = arrayMapOf<String, Area>()

var serverAvailable = false
    private set

class MainActivity : NetworkChangeListenerActivity() {

    /**
     * Prepares the app and runs some initial tests to see if the content should be loaded or other
     * actions should be executed.
     * @author Arnau Mora
     * @since 20210321
     * @return If the content should be started loading.
     */
    private fun prepareApp(): Boolean {
        var error = false
        Timber.v("Preparing App...")
        Timber.v("Instantiating Sentry")
        SentryAndroid.init(this) { options ->
            options.addIntegration(
                SentryTimberIntegration(SentryLevel.ERROR, SentryLevel.INFO)
            )
        }

        val showIntro = IntroActivity.shouldShow(this)
        if (showIntro != IntroShowReason.OK) {
            Timber.w("  Showing intro! Reason: ${showIntro.msg}")
            skipLoad = true
            startActivity(Intent(this, IntroActivity::class.java))
            error = true
        } else
            Timber.v("  Won't show intro.")

        if (AREAS.size <= 0) {
            startActivity(Intent(this, LoadingActivity::class.java))
            error = true
        }

        Timber.v("Data folder path: %s", filesDir(this).path)

        File(cacheDir, "update.apk").deleteIfExists()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannels()

        Timber.v("Finished preparing App...")
        return !error
    }

    private lateinit var areasViewFragment: AreasViewFragment
    private lateinit var mapFragment: MapFragment
    lateinit var downloadsFragment: DownloadsFragment
        private set
    private lateinit var settingsFragment: SettingsFragmentManager

    var adapter: MainPagerAdapter? = null
    private lateinit var binding: ActivityMainBinding

    private var skipLoad = false

    private fun updateBottomAppBar() {
        Timber.d("Updating bottom app bar...")
        val position = binding.mainViewPager.currentItem
        binding.actionExploreImage.setImageResource(
            if (position == TAB_ITEM_HOME) R.drawable.round_explore_24
            else R.drawable.ic_outline_explore_24
        )
        visibility(binding.actionExploreText, position == TAB_ITEM_HOME)
        binding.actionMapImage.setImageResource(
            if (position == TAB_ITEM_MAP) R.drawable.ic_round_map_24
            else R.drawable.ic_outline_map_24
        )
        visibility(binding.actionMapText, position == TAB_ITEM_MAP)
        binding.actionDownloadsImage.setImageResource(
            if (position == TAB_ITEM_DOWNLOADS) R.drawable.ic_round_cloud_download_24
            else R.drawable.ic_outline_cloud_download_24
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
        Timber.plant(DebugTree())
        Timber.v("Planted Timber.")

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setSupportActionBar(binding.bottomAppBar)

        if (!prepareApp()) return

        ParseAnalytics.trackAppOpenedInBackground(intent)

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

        areasViewFragment.setItemClickListener { holder, position ->
            binding.loadingLayout.show()
            Timber.v("Clicked item %s", position)
            val intent = Intent(this, AreaActivity()::class.java)
                .putExtra(EXTRA_AREA, AREAS.valueAt(position)!!.objectId)

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
            LOCATION_PERMISSION_REQUEST -> areasViewFragment.mapHelper.enableLocationComponent(this)
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        if (skipLoad) {
            Timber.w("Skipped onStateChange since skipLoad is true.")
            return
        }

        val hasInternet = state.hasInternet
        Timber.v("Connectivity status Updated! Has Internet: %s", hasInternet)
        binding.loadingLayout.hide()
    }
}
