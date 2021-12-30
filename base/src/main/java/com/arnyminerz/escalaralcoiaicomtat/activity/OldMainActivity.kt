package com.arnyminerz.escalaralcoiaicomtat.activity

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.PopupMenu
import androidx.annotation.UiThread
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.ImageViewCompat
import androidx.viewpager2.widget.ViewPager2
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.isolated.EmailConfirmationActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageAppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.profile.ProfileActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.exception.MissingPermissionException
import com.arnyminerz.escalaralcoiaicomtat.core.shared.APP_UPDATE_MAX_TIME_DAYS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENABLE_AUTHENTICATION
import com.arnyminerz.escalaralcoiaicomtat.core.shared.LOCATION_PERMISSION_REQUEST_CODE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PREF_DISABLE_NEARBY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PREF_WAITING_EMAIL_CONFIRMATION
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PREF_WARN_BATTERY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PROFILE_IMAGE_MAX_SIZE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_ALERT_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.shared.TAB_ITEM_DOWNLOADS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.TAB_ITEM_EXTRA
import com.arnyminerz.escalaralcoiaicomtat.core.shared.TAB_ITEM_HOME
import com.arnyminerz.escalaralcoiaicomtat.core.shared.TAB_ITEM_MAP
import com.arnyminerz.escalaralcoiaicomtat.core.shared.TAB_ITEM_SETTINGS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.isLocationPermissionGranted
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.view.getColorFromAttribute
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityMainBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.DownloadsFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.MapFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.SettingsFragmentManager
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.AreasViewFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.MainSettingsFragment.Companion.SettingsPage
import com.arnyminerz.escalaralcoiaicomtat.intent.LoginRequestContract
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.MainPagerAdapter
import com.arnyminerz.escalaralcoiaicomtat.shared.RESULT_CODE_LOGGED_IN
import com.arnyminerz.escalaralcoiaicomtat.shared.RESULT_CODE_WAITING_EMAIL_CONFIRMATION
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import timber.log.Timber

private const val APP_UPDATE_REQUEST_CODE = 8 // This number was chosen by Eva

/**
 * The Main Activity, shows a view pager with the areas view, a map, the downloads view, and the
 * settings page.
 * @author Arnau Mora
 * @since 20210617
 */
@ExperimentalMaterial3Api
class OldMainActivity : LanguageAppCompatActivity() {

    /**
     * The fragment that shows the Areas View.
     * @author Arnau Mora
     * @since 20210617
     */
    private lateinit var areasViewFragment: AreasViewFragment

    /**
     * The fragment that shows the map with all the zones loaded.
     * @author Arnau Mora
     * @since 20210617
     */
    lateinit var mapFragment: MapFragment
        private set

    /**
     * The fragment that shows the downloaded features.
     * @author Arnau Mora
     * @since 20210617
     */
    lateinit var downloadsFragment: DownloadsFragment
        private set

    /**
     * The fragment that shows the settings screen for configuring the app.
     * @author Arnau Mora
     * @since 20210617
     */
    private lateinit var settingsFragment: SettingsFragmentManager

    /**
     * The adapter for the main scroll view, and showing the correct fragment.
     * @author Arnau Mora
     * @since 20210617
     */
    var adapter: MainPagerAdapter? = null

    /**
     * The view binding for the MainActivity.v
     */
    lateinit var binding: ActivityMainBinding

    /**
     * A reference for the Firebase Firestore instance.
     * @author Arnau Mora
     * @since 20210617
     */
    internal lateinit var firestore: FirebaseFirestore

    /**
     * A reference for the Firebase Storage instance.
     * @author Arnau Mora
     * @since 20210617
     */
    internal lateinit var storage: FirebaseStorage

    /**
     * The Request contract for making login requests.
     * @author Arnau Mora
     * @since 20210617
     */
    private val loginRequest = registerForActivityResult(LoginRequestContract()) { resultCode ->
        Timber.i("Got login result: $resultCode")
        when (resultCode) {
            RESULT_CODE_LOGGED_IN -> refreshLoginStatus()
            RESULT_CODE_WAITING_EMAIL_CONFIRMATION -> launch(EmailConfirmationActivity::class.java)
        }
    }

    /**
     * A cache for storing the [Area]s list from [App.getAreas].
     * @author Arnau Mora
     * @since 20210817
     */
    private var areas: List<Area> = listOf()

    /**
     * The watcher for preference changes. This is useful for detecting when the Nearby Zones are
     * enabled or disabled.
     * @author Arnau Mora
     * @since 20211120
     */
    @SuppressLint("MissingPermission")
    private val sharedPreferencesWatcher =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PREF_DISABLE_NEARBY.key) {
                val nearbyDisabled = PREF_DISABLE_NEARBY.get()
                Timber.i("Changed Nearby Zones disable state to: $nearbyDisabled")

                if (nearbyDisabled) {
                    Timber.i("Disabling location component...")
                    areasViewFragment.nearbyZones?.mapHelper?.locationComponent?.disable()
                } else try {
                    Timber.i("Enabling location component")
                    areasViewFragment.nearbyZones?.mapHelper?.locationComponent?.enable(this)
                } catch (e: MissingPermissionException) {
                    Timber.w("The location permission is not granted, will ask when updating Nearby Zones.")
                }

                areasViewFragment.nearbyZones?.updateNearbyZones()
            }
        }

    /**
     * Updates the bottom app bar icons according to the position of the [ActivityMainBinding.mainViewPager].
     * @author Arnau Mora
     * @since 20210617
     */
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
     * Checks if there is any update available for the app at the Play Store.
     * @author Arnau Mora
     * @since 20210617
     */
    @UiThread
    private fun updatesCheck() {
        Timber.v("Searching for updates...")
        val appUpdateManager = AppUpdateManagerFactory.create(this)

        val listener = InstallStateUpdatedListener { state ->
            val status = state.installStatus()
            if (status == InstallStatus.DOWNLOADING) {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytesToDownload = state.totalBytesToDownload()
                val progress = (bytesDownloaded / totalBytesToDownload).toInt() * 100

                // Show update progress bar.
                Timber.d("Downloading update $progress%: $bytesDownloaded / $totalBytesToDownload")
            } else if (status == InstallStatus.DOWNLOADED) {
                Timber.v("Finished downloading update, showing restart request.")
                Snackbar.make(
                    binding.mainLayout,
                    R.string.toast_update_downloaded,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.action_restart) { appUpdateManager.completeUpdate() }
                    .show()
            }
        }

        appUpdateManager.registerListener(listener)

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            val updateAvailability = appUpdateInfo.updateAvailability()
            try {
                if (updateAvailability == UpdateAvailability.UPDATE_AVAILABLE) {
                    Timber.v("There's an update available")

                    val updateStaleness = appUpdateInfo.clientVersionStalenessDays()
                    if (updateStaleness != null && updateStaleness >= APP_UPDATE_MAX_TIME_DAYS) {
                        if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                            // Request immediate update
                            Timber.v("Requesting immediate update.")
                            appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                this,
                                APP_UPDATE_REQUEST_CODE
                            )
                        } else Timber.w("Immediate update is not allowed")
                    } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        // Request flexible update
                        Timber.v("Requesting flexible update.")
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.FLEXIBLE,
                            this,
                            APP_UPDATE_REQUEST_CODE
                        )
                    } else Timber.w("Flexible update is not allowed")
                } else Timber.d("There's no update available. ($updateAvailability)")
            } catch (e: IntentSender.SendIntentException) {
                Timber.w(e, "Could not request update.")
            }
        }
    }

    /**
     * Moves the pager to the specified position.
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

        val app = application as App
        doAsync {
            areas = app.getAreas()
            Timber.v("  --- Found ${areas.size} areas ---")
        }

        firestore = Firebase.firestore
        storage = Firebase.storage

        addListeners()

        // Check if battery optimization is disabled, if so, alerts can't be enabled, since
        //   Firebase's notifications can't be received.
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && PREF_WARN_BATTERY.get())
            if (am.isBackgroundRestricted)
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_background_restricted_title)
                    .setMessage(R.string.dialog_background_restricted_message)
                    .setPositiveButton(R.string.action_open_settings) { _, _ ->
                        startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
                    }
                    .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                        SETTINGS_ALERT_PREF.put(false)
                        dialog.dismiss()
                    }
                    .setNeutralButton(R.string.action_do_not_remind) { dialog, _ ->
                        PREF_WARN_BATTERY.put(false)
                        dialog.dismiss()
                    }
                    .show()

        updateBottomAppBar()
        refreshLoginStatus()
        updatesCheck()
    }

    override fun onStart() {
        super.onStart()
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesWatcher)
    }

    override fun onStop() {
        super.onStop()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferencesWatcher)
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
                if (this.isLocationPermissionGranted())
                    try {
                        areasViewFragment.mapHelper?.locationComponent?.enable(this)
                    } catch (_: IllegalStateException) {
                        Timber.w("The GPS is disabled.")
                        toast(R.string.toast_error_gps_disabled)
                    }
                areasViewFragment.nearbyZones?.updateNearbyZones()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == APP_UPDATE_REQUEST_CODE)
            when (resultCode) {
                RESULT_OK -> Timber.v("App update complete")
                RESULT_CANCELED -> Timber.w("App update cancelled. We might need to force the update.")
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> Timber.w("In app update failed.")
            }
        else
            super.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Updates the UI according to the login status.
     * @author Arnau Mora
     * @since 20210424
     */
    @UiThread
    private fun refreshLoginStatus() {
        binding.spaceAuth.visibility(ENABLE_AUTHENTICATION)
        binding.authFab.visibility(ENABLE_AUTHENTICATION)

        if (ENABLE_AUTHENTICATION) {
            val auth = Firebase.auth
            val currentUser = auth.currentUser
            val user = if (currentUser?.isAnonymous == true) null else currentUser

            binding.profileCardView.visibility(user != null)
            binding.profileImageView.visibility(user != null)

            user?.reload()?.addOnSuccessListener {
                if (!user.isEmailVerified) {
                    PREF_WAITING_EMAIL_CONFIRMATION.put(true)
                    launch(EmailConfirmationActivity::class.java)
                }

                user.photoUrl?.also { photoUrl ->
                    storage.getReferenceFromUrl(photoUrl.toString())
                        .getBytes(PROFILE_IMAGE_MAX_SIZE)
                        .addOnSuccessListener { bytes ->
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            binding.profileImageView.setImageResource(0)
                            binding.profileImageView.background = bitmap.toDrawable(resources)
                        }
                        .addOnFailureListener { e ->
                            Timber.e(e, "Could not load profile image.")
                            toast(R.string.toast_error_profile_image_load)
                        }
                }
            }?.addOnFailureListener {
                if (it is FirebaseAuthInvalidUserException) {
                    Firebase.auth.signOut()
                    refreshLoginStatus()
                }
            }
        } else {
            binding.profileCardView.visibility(false)
            binding.profileImageView.visibility(false)
        }
    }

    /**
     * Adds the corresponding listeners to the views.
     * @author Arnau Mora
     * @since 20210617
     */
    @UiThread
    private fun addListeners() {
        binding.actionExplore.setOnClickListener { navigate(TAB_ITEM_HOME) }
        binding.actionMap.setOnClickListener { navigate(TAB_ITEM_MAP) }
        binding.actionDownloads.setOnClickListener { navigate(TAB_ITEM_DOWNLOADS) }
        binding.actionExtra.setOnClickListener { navigate(TAB_ITEM_EXTRA) }

        binding.authFab.setOnClickListener {
            loginRequest.launch(null)
        }
        binding.profileImageView.setOnClickListener {
            launch(ProfileActivity::class.java)
        }
        binding.profileImageView.setOnLongClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_message)
                .setPositiveButton(R.string.action_logout) { _, _ ->
                    Firebase.auth.signOut()
                    refreshLoginStatus()
                }
                .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()

            true
        }
    }
}
