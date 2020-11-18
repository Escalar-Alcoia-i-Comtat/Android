package com.arnyminerz.escalaralcoiaicomtat.activity

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.AreaActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerFragmentActivity
import com.arnyminerz.escalaralcoiaicomtat.async.EXTENDED_API_URL
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.fragment.AuthFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.DownloadsFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.MapFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.SettingsFragmentManager
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.AreasViewFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.LOCATION_PERMISSION_REQUEST
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.MainSettingsFragment.Companion.SettingsPage
import com.arnyminerz.escalaralcoiaicomtat.generic.IntentExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.deleteIfExists
import com.arnyminerz.escalaralcoiaicomtat.generic.loadLocale
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import com.arnyminerz.escalaralcoiaicomtat.image.dpToPx
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.MainPagerAdapter
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.network.ping
import com.arnyminerz.escalaralcoiaicomtat.notification.createNotificationChannels
import com.arnyminerz.escalaralcoiaicomtat.service.FirebaseMessagingService
import com.arnyminerz.escalaralcoiaicomtat.storage.filesDir
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import io.sentry.android.core.SentryAndroid
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_list.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File
import java.net.URL

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

const val ANALYTICS_EVENT_NAME_ERRORS = "errors"
const val ANALYTICS_EVENT_KEY_SECTORS_NO_CONTEXT = "no_context"

@ExperimentalUnsignedTypes
val AREAS = arrayListOf<Area>()

var serverAvailable = false
    private set

@ExperimentalUnsignedTypes
class MainActivity : NetworkChangeListenerFragmentActivity() {
    companion object {
        val auth
            get() = FirebaseAuth.getInstance()
        var analytics = Firebase.analytics

        fun user(): FirebaseUser? = auth.currentUser
        fun loggedIn(): Boolean = user() != null

        var sharedPreferences: SharedPreferences? = null

        val betaUser: Boolean
            get() = BuildConfig.VERSION_NAME.contains("pre", true) || BuildConfig.DEBUG
    }

    private fun prepareApp(): Boolean {
        Timber.v("Preparing App...")
        Timber.v("Instantiating Sentry")
        SentryAndroid.init(this)

        if (IntroActivity.shouldShow(this)) {
            Timber.w("  Showing intro!")
            startActivity(Intent(this, IntroActivity::class.java))
            return false
        } else Timber.v("  Won't show intro.")

        Timber.v("Data folder path: %s", filesDir(this).path)

        File(cacheDir, "update.apk").deleteIfExists()

        loadLocale()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannels()

        Timber.v("Instantiating Firebase Messaging")
        FirebaseMessagingService()

        Timber.v("Finished preparing App...")
        return true
    }

    private val areasViewFragment = AreasViewFragment()

    private val mapFragment = MapFragment()
    private val authFragment = AuthFragment()
    val downloadsFragment = DownloadsFragment()
    private val settingsFragment = SettingsFragmentManager()

    var adapter: MainPagerAdapter? = null

    private var loaded = false
    private var loading = false

    private fun updateBottomAppBar() {
        val position = main_viewPager.currentItem
        val showingViewPager = visibility(main_viewPager)
        bottom_app_bar.navigationIcon =
            ContextCompat.getDrawable(
                this,
                if (position == TAB_ITEM_HOME && showingViewPager) R.drawable.round_explore_24 else R.drawable.ic_outline_explore_24
            )
        menuMapIcon?.icon =
            ContextCompat.getDrawable(
                this,
                if (position == TAB_ITEM_MAP && showingViewPager) R.drawable.ic_round_map_24 else R.drawable.ic_outline_map_24
            )
        menuDownloadsIcon?.icon =
            ContextCompat.getDrawable(
                this,
                if (position == TAB_ITEM_DOWNLOADS && showingViewPager) R.drawable.ic_round_cloud_download_24 else R.drawable.ic_outline_cloud_download_24
            )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(DebugTree())
        Timber.v("Planted Timber.")

        setContentView(R.layout.activity_main)
        setSupportActionBar(bottom_app_bar)

        if (!prepareApp()) return

        main_viewPager.adapter = MainPagerAdapter(
            this,
            hashMapOf(
                TAB_ITEM_HOME to areasViewFragment,
                TAB_ITEM_MAP to mapFragment,
                TAB_ITEM_DOWNLOADS to downloadsFragment,
                TAB_ITEM_SETTINGS to settingsFragment
            )
        )
        main_viewPager.isUserInputEnabled = false

        main_fab.setOnClickListener {
            Timber.v("Main FAB clicked")
            if (visibility(main_viewPager)) {
                visibility(main_viewPager, false)
                visibility(main_frameLayout, true)

                Timber.v("Changing fragment for main_frameLayout to authFragment")
                supportFragmentManager.beginTransaction().apply {
                    replace(R.id.main_frameLayout, authFragment)
                    commit()
                }
                updateBottomAppBar()
            } else onBackPressed()
        }
        bottom_app_bar.setNavigationOnClickListener {
            if (!visibility(main_viewPager)) {
                visibility(main_viewPager, true)
                visibility(main_frameLayout, false)
            }

            main_viewPager.currentItem = TAB_ITEM_HOME

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
        if (!visibility(main_viewPager)) {
            visibility(main_viewPager, true)
            visibility(main_frameLayout, false)
        }

        return when (item.itemId) {
            R.id.action_1 -> {
                main_viewPager.currentItem = TAB_ITEM_MAP
                true
            }
            R.id.action_2 -> {
                main_viewPager.currentItem = TAB_ITEM_DOWNLOADS
                true
            }
            R.id.settings -> {
                main_viewPager.currentItem = TAB_ITEM_SETTINGS
                true
            }
            R.id.share -> {
                startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text)),
                        getString(R.string.action_share_with)
                    )
                )
                true
            }
            else -> false
        }
    }

    override fun onResume() {
        super.onResume()
        if (loggedIn())
            Glide.with(this)
                .load(user()!!.photoUrl.toString())
                .apply(
                    RequestOptions
                        .circleCropTransform()
                        .placeholder(R.drawable.ic_outline_person_24)
                )
                .into(object : CustomTarget<Drawable>(24.dpToPx(), 24.dpToPx()) {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        Timber.v("Putting user's image on main fab")
                        main_fab.apply {
                            imageTintList = null
                            imageTintMode = null
                            scaleType = ImageView.ScaleType.FIT_XY
                            setImageDrawable(resource)
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}

                })
    }

    override fun onBackPressed() {
        Timber.v("Going back!")
        if (visibility(main_frameLayout))
            if (authFragment.profileFragment?.changedUser == true)
                authFragment.profileFragment!!.restore()
            else {
                visibility(main_viewPager, true)
                visibility(main_frameLayout, false)

                main_viewPager.currentItem = TAB_ITEM_HOME
            }
        else
            if (main_viewPager.currentItem == TAB_ITEM_SETTINGS) {
                val settingsFragmentManager =
                    (main_viewPager.adapter as? MainPagerAdapter)?.items?.get(TAB_ITEM_SETTINGS) as? SettingsFragmentManager
                if (settingsFragmentManager != null && settingsFragmentManager.height > 0)
                    settingsFragmentManager.loadPage(SettingsPage.MAIN, true)
                else main_viewPager.currentItem = TAB_ITEM_HOME
            } else if (main_viewPager.currentItem == TAB_ITEM_DOWNLOADS)
                main_viewPager.currentItem = TAB_ITEM_HOME
            else if (main_viewPager.currentItem == TAB_ITEM_MAP)
                main_viewPager.currentItem = TAB_ITEM_HOME
            else {
                super.onBackPressed()
                finish()
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
                    areasViewFragment.view,
                    null,
                    areasViewFragment.googleMap!!
                )
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        val hasInternet = state.hasInternet
        Timber.v("Connectivity status Updated! Has Internet: %s", hasInternet)

        if (state.hasInternet && !serverAvailable) {
            doAsync {
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

        if (!loaded && !loading) {
            loading = true
            visibility(main_loading_progressBar, true)

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
                toast(R.string.toast_loading)
                Timber.v("Clicked item %s", position)
                val intent = Intent(this, AreaActivity()::class.java)
                    .putExtra(EXTRA_AREA, position)

                val optionsBundle =
                    ViewCompat.getTransitionName(holder.titleTextView)?.let { transitionName ->
                        intent.putExtra(EXTRA_AREA_TRANSITION_NAME, transitionName)

                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                            this,
                            title_textView,
                            transitionName
                        ).toBundle()
                    } ?: Bundle.EMPTY

                startActivity(intent, optionsBundle)
            }

            Timber.v("Finished loading areas, hiding progress bar and showing frameLayout.")
            visibility(main_loading_progressBar, false)
            visibility(main_frameLayout, true)

            loaded = true
            loading = false
        }
    }
}