package com.arnyminerz.escalaralcoiaicomtat.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.work.*
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.AreaActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerFragmentActivity
import com.arnyminerz.escalaralcoiaicomtat.async.EXTENDED_API_URL
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityMainBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.DownloadsFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.MapFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.SettingsFragmentManager
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.AreasViewFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.LOCATION_PERMISSION_REQUEST
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.MainSettingsFragment.Companion.SettingsPage
import com.arnyminerz.escalaralcoiaicomtat.generic.*
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.MainPagerAdapter
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.network.ping
import com.arnyminerz.escalaralcoiaicomtat.notification.createNotificationChannels
import com.arnyminerz.escalaralcoiaicomtat.storage.filesDir
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.show
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.worker.UpdateWorker
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid
import io.sentry.android.timber.SentryTimberIntegration
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

val EXTRA_AREA = IntentExtra<Int>("area")
val EXTRA_ZONE = IntentExtra<Int>("zone")
val EXTRA_SECTOR = IntentExtra<Int>("sector")

val EXTRA_ZONE_TRANSITION_NAME = IntentExtra<String>("zone_transition")
val EXTRA_AREA_TRANSITION_NAME = IntentExtra<String>("area_transition")
val EXTRA_SECTOR_TRANSITION_NAME = IntentExtra<String>("sector_transition")

const val TAB_ITEM_HOME = 0

const val TAB_ITEM_MAP = 1
const val TAB_ITEM_DOWNLOADS = 2
const val TAB_ITEM_SETTINGS = 3

const val UPDATE_CHECKER_WORK_NAME = "update_checker"
const val UPDATE_CHECKER_TAG = "update"

@ExperimentalUnsignedTypes
val AREAS = arrayListOf<Area>()

var serverAvailable = false
    private set


@ExperimentalUnsignedTypes
class MainActivity : NetworkChangeListenerFragmentActivity() {

    private fun prepareApp(): Boolean {
        Timber.v("Preparing App...")
        Timber.v("Instantiating Sentry")
        SentryAndroid.init(this) { options ->
            options.addIntegration(
                SentryTimberIntegration(SentryLevel.ERROR, SentryLevel.INFO)
            )
        }

        if (IntroActivity.shouldShow(this)) {
            Timber.w("  Showing intro!")
            startActivity(Intent(this, IntroActivity::class.java))
            return false
        } else Timber.v("  Won't show intro.")

        Timber.v("Data folder path: %s", filesDir(this).path)

        File(cacheDir, "update.apk").deleteIfExists()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannels()

        Timber.v("Initializing update checker...")
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            UPDATE_CHECKER_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UpdateWorker>(
                1, TimeUnit.HOURS,
                15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .addTag(UPDATE_CHECKER_TAG)
                .build()
        )

        Timber.v("Finished preparing App...")
        return true
    }

    private val areasViewFragment = AreasViewFragment()

    private val mapFragment = MapFragment()
    val downloadsFragment = DownloadsFragment()
    private val settingsFragment = SettingsFragmentManager()

    var adapter: MainPagerAdapter? = null
    private lateinit var binding: ActivityMainBinding

    private var loaded = false
    private var loading = false

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
                if (position == TAB_ITEM_DOWNLOADS) R.drawable.ic_round_cloud_download_24 else R.drawable.ic_outline_cloud_download_24
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
    }

    private var menu: Menu? = null

    private val menuMapIcon: MenuItem?
        get() = menu?.getItem(0)
    private val menuDownloadsIcon: MenuItem?
        get() = menu?.getItem(1)

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
        val areasViewFragment = areasViewFragment
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST -> if (areasViewFragment.googleMap != null)
                areasViewFragment.updateNearbyZones(
                    null,
                    areasViewFragment.googleMap!!
                )
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        val hasInternet = state.hasInternet
        Timber.v("Connectivity status Updated! Has Internet: %s", hasInternet)
        binding.loadingLayout.hide()

        if (state.hasInternet && !serverAvailable) {
            runAsync {
                val canReachServer = URL(EXTENDED_API_URL).ping()
                if (canReachServer) {
                    Timber.v("Reached arnyminerz.com")
                    serverAvailable = true
                } else {
                    Timber.e("Could not ping $EXTENDED_API_URL")
                    startActivity(Intent(this@MainActivity, ServerDownActivity::class.java))
                }
            }
        } else
            Timber.v("Didn't check for server connection since Internet is not available")

        if (!loaded && !loading)
            runAsync {
                loading = true
                visibility(binding.mainLoadingProgressBar, true)

                Timber.v("Loading areas...")
                val areasFlow = loadAreas(this)
                Timber.v("  Clearing AREAS collection...")
                AREAS.clear()
                Timber.v("  Importing to collection...")
                AREAS.addAll(areasFlow)
                Timber.v("  --- Found ${AREAS.size} areas ---")

                Timber.v("Got areas, setting in map fragment")
                mapFragment.setAreas(AREAS)

                areasViewFragment.updateAreas { holder, position ->
                    binding.loadingLayout.show()
                    Handler(Looper.getMainLooper()).post {
                        Timber.v("Clicked item %s", position)
                        val intent = Intent(this, AreaActivity()::class.java)
                            .putExtra(EXTRA_AREA, position)

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

                runOnUiThread {
                    Timber.v("Finished loading areas, hiding progress bar and showing frameLayout.")
                    visibility(binding.mainLoadingProgressBar, false)
                    visibility(binding.mainFrameLayout, true)
                }

                loaded = true
                loading = false
            }
    }
}