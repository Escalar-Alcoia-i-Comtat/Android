package com.arnyminerz.escalaralcoiaicomtat.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.collection.arrayMapOf
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.AreaActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerFragmentActivity
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
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.MainPagerAdapter
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.notification.createNotificationChannels
import com.arnyminerz.escalaralcoiaicomtat.storage.filesDir
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.show
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.parse.ParseConfig
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid
import io.sentry.android.timber.SentryTimberIntegration
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File

val EXTRA_AREA = IntentExtra<String>("area")
val EXTRA_ZONE = IntentExtra<String>("zone")
val EXTRA_SECTOR = IntentExtra<String>("sector")

val EXTRA_ZONE_TRANSITION_NAME = IntentExtra<String>("zone_transition")
val EXTRA_AREA_TRANSITION_NAME = IntentExtra<String>("area_transition")
val EXTRA_SECTOR_TRANSITION_NAME = IntentExtra<String>("sector_transition")

const val TAB_ITEM_HOME = 0
const val TAB_ITEM_MAP = 1
const val TAB_ITEM_DOWNLOADS = 2
const val TAB_ITEM_SETTINGS = 3

const val UPDATE_CHECKER_WORK_NAME = "update_checker"
const val UPDATE_CHECKER_TAG = "update"
const val UPDATE_CHECKER_FLEX_MINUTES: Long = 15

val AREAS = arrayMapOf<String, Area>()

var serverAvailable = false
    private set

class MainActivity : NetworkChangeListenerFragmentActivity() {

    private fun prepareApp(): Boolean {
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
            return false
        } else Timber.v("  Won't show intro.")

        if (AREAS.size <= 0) {
            startActivity(Intent(this, LoadingActivity::class.java))
            return false
        }

        Timber.v("Data folder path: %s", filesDir(this).path)

        File(cacheDir, "update.apk").deleteIfExists()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannels()

        Timber.v("Getting config...")
        ParseConfig.getInBackground { cnf, e ->
            var config = cnf
            if (e != null)
                config = ParseConfig.getCurrentConfig()

            val test = config.getString("testing_param")
            toast(this, test)
        }

        Timber.v("Finished preparing App...")
        return true
    }

    private lateinit var areasViewFragment: AreasViewFragment
    private lateinit var mapFragment: MapFragment
    lateinit var downloadsFragment: DownloadsFragment
        private set
    private lateinit var settingsFragment: SettingsFragmentManager

    var adapter: MainPagerAdapter? = null
    private lateinit var binding: ActivityMainBinding

    private var skipLoad = false

    private var menu: Menu? = null
    private val menuMapIcon: MenuItem?
        get() = menu?.getItem(0)
    private val menuDownloadsIcon: MenuItem?
        get() = menu?.getItem(1)

    private fun updateBottomAppBar() {
        Timber.d("Updating bottom app bar...")
        val position = binding.mainViewPager.currentItem
        binding.bottomAppBar.navigationIcon =
            ContextCompat.getDrawable(
                this,
                if (position == TAB_ITEM_HOME) R.drawable.round_explore_24 else R.drawable.ic_outline_explore_24
            )
        menuMapIcon?.setIcon(
            ContextCompat.getDrawable(
                this,
                if (position == TAB_ITEM_MAP) R.drawable.ic_round_map_24 else R.drawable.ic_outline_map_24
            )
        ) ?: Timber.w("menuMapIcon is null")
        menuDownloadsIcon?.setIcon(
            ContextCompat.getDrawable(
                this,
                if (position == TAB_ITEM_DOWNLOADS)
                    R.drawable.ic_round_cloud_download_24
                else R.drawable.ic_outline_cloud_download_24
            )
        ) ?: Timber.w("menuDownloadsIcon is null")
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
        binding.mainViewPager.isUserInputEnabled = false

        binding.bottomAppBar.setNavigationOnClickListener {
            if (!visibility(binding.mainViewPager)) {
                visibility(binding.mainViewPager, true)
                visibility(binding.mainFrameLayout, false)
            }

            binding.mainViewPager.currentItem = TAB_ITEM_HOME

            updateBottomAppBar()
        }

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
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        this.menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (!visibility(binding.mainViewPager)) {
            visibility(binding.mainViewPager, true)
            visibility(binding.mainFrameLayout, false)
        }

        val result = when (item.itemId) {
            R.id.action_1 -> {
                binding.mainViewPager.currentItem = TAB_ITEM_MAP
                true
            }
            R.id.action_2 -> {
                binding.mainViewPager.currentItem = TAB_ITEM_DOWNLOADS
                true
            }
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
        updateBottomAppBar()
        return result
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Timber.v("Got permissions result. Code: %s", requestCode)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST -> areasViewFragment.requestLocationUpdates()
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
