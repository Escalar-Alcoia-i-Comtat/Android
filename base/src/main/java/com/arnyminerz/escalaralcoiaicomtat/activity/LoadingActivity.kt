package com.arnyminerz.escalaralcoiaicomtat.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.UiThread
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.isolated.EmailConfirmationActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.data.IntroShowReason
import com.arnyminerz.escalaralcoiaicomtat.data.climb.area.loadAreasFromCache
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityLoadingBinding
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.PREF_WAITING_EMAIL_CONFIRMATION
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_ERROR_REPORTING_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.doAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.getExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.launch
import com.arnyminerz.escalaralcoiaicomtat.generic.uiContext
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.notification.createNotificationChannels
import com.arnyminerz.escalaralcoiaicomtat.shared.APP_UPDATE_MAX_TIME_DAYS
import com.arnyminerz.escalaralcoiaicomtat.shared.APP_UPDATE_MAX_TIME_DAYS_KEY
import com.arnyminerz.escalaralcoiaicomtat.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.shared.App
import com.arnyminerz.escalaralcoiaicomtat.shared.ENABLE_AUTHENTICATION
import com.arnyminerz.escalaralcoiaicomtat.shared.ENABLE_AUTHENTICATION_KEY
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_LINK_PATH
import com.arnyminerz.escalaralcoiaicomtat.shared.PROFILE_IMAGE_SIZE
import com.arnyminerz.escalaralcoiaicomtat.shared.PROFILE_IMAGE_SIZE_KEY
import com.arnyminerz.escalaralcoiaicomtat.shared.REMOTE_CONFIG_DEFAULTS
import com.arnyminerz.escalaralcoiaicomtat.shared.REMOTE_CONFIG_MIN_FETCH_INTERVAL
import com.arnyminerz.escalaralcoiaicomtat.shared.SHOW_NON_DOWNLOADED
import com.arnyminerz.escalaralcoiaicomtat.shared.SHOW_NON_DOWNLOADED_KEY
import com.arnyminerz.escalaralcoiaicomtat.shared.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfigClientException
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class LoadingActivity : NetworkChangeListenerActivity() {
    companion object {
        private const val APP_UPDATE_REQUEST_CODE = 8 // This number was chosen by Eva
    }

    private lateinit var binding: ActivityLoadingBinding
    private var loading = false

    private var deepLinkPath: String? = null

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val enableErrorReporting = SETTINGS_ERROR_REPORTING_PREF.get()
        Firebase.crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG && enableErrorReporting)
        Timber.v("Set Crashlytics collection enabled to $enableErrorReporting")
        Firebase.analytics.setAnalyticsCollectionEnabled(!BuildConfig.DEBUG && enableErrorReporting)
        Timber.v("Set Analytics collection enabled to $enableErrorReporting")

        Timber.v("Getting Firestore instance...")
        firestore = Firebase.firestore

        val showIntro = IntroActivity.shouldShow()
        if (showIntro != IntroShowReason.OK) {
            Timber.w("  Showing intro! Reason: ${showIntro.msg}")
            finish()
            startActivity(Intent(this, IntroActivity::class.java))
            return
        } else
            Timber.v("  Won't show intro.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannels()

        deepLinkPath = getExtra(EXTRA_LINK_PATH)

        Timber.v("Searching for updates...")
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            val updateAvailability = appUpdateInfo.updateAvailability()
            if (updateAvailability == UPDATE_AVAILABLE) {
                Timber.v("There's an update available")
                val updateSteless = appUpdateInfo.clientVersionStalenessDays()
                if (updateSteless != null && updateSteless >= APP_UPDATE_MAX_TIME_DAYS) {
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        // Request immediate update
                        Timber.v("Requesting immediate update.")
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this,
                            APP_UPDATE_REQUEST_CODE
                        )
                        loading = true
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
                    loading = true
                } else Timber.w("Flexible update is not allowed")
            } else Timber.d("There's no update available. ($updateAvailability)")
        }

        doAsync {
            Timber.v("Getting remote configuration...")
            val remoteConfig = Firebase.remoteConfig
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = REMOTE_CONFIG_MIN_FETCH_INTERVAL
            }
            remoteConfig.setConfigSettingsAsync(configSettings).await()
            remoteConfig.setDefaultsAsync(REMOTE_CONFIG_DEFAULTS).await()
            try {
                remoteConfig.fetchAndActivate().await()
            } catch (e: FirebaseRemoteConfigClientException) {
                Timber.e(e, "Could not get remote config.")
            }
            APP_UPDATE_MAX_TIME_DAYS = remoteConfig.getLong(APP_UPDATE_MAX_TIME_DAYS_KEY)
            SHOW_NON_DOWNLOADED = remoteConfig.getBoolean(SHOW_NON_DOWNLOADED_KEY)
            ENABLE_AUTHENTICATION = remoteConfig.getBoolean(ENABLE_AUTHENTICATION_KEY)
            PROFILE_IMAGE_SIZE = remoteConfig.getLong(PROFILE_IMAGE_SIZE_KEY)

            Timber.v("APP_UPDATE_MAX_TIME_DAYS: $APP_UPDATE_MAX_TIME_DAYS")
            Timber.v("SHOW_NON_DOWNLOADED: $SHOW_NON_DOWNLOADED")
            Timber.v("ENABLE_AUTHENTICATION: $ENABLE_AUTHENTICATION")

            if (!ENABLE_AUTHENTICATION) {
                Timber.v("Removing auth state listener...")
                Firebase.auth.removeAuthStateListener((application as App).authStateListener)
            }

            uiContext {
                Timber.v("Finished preparing App...")
                load()
            }
        }
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        load()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == APP_UPDATE_REQUEST_CODE)
            when (resultCode) {
                RESULT_OK -> Timber.v("App update complete")
                RESULT_CANCELED -> Timber.w("App update cancelled. We might need to force the update.")
                RESULT_IN_APP_UPDATE_FAILED -> Timber.w("In app update failed.")
            }
        else
            super.onActivityResult(requestCode, resultCode, data)
    }

    @UiThread
    private fun noInternetAccess() {
        Timber.w("There's no Internet connection to download new data")
        binding.progressTextView.setText(R.string.status_no_internet)
        binding.progressBar.hide()
        loading = false
    }

    @UiThread
    private fun load() {
        val waitingForEmailConfirmation = PREF_WAITING_EMAIL_CONFIRMATION.get()
        if (waitingForEmailConfirmation) {
            startActivity(Intent(this, EmailConfirmationActivity::class.java))
            return
        }

        if (loading) {
            Timber.v("Skipped load since already loading")
            return
        }
        loading = true
        binding.progressTextView.setText(R.string.status_downloading)
        try {
            loadAreasFromCache(firestore, { progress, max ->
                Timber.i("Download progress: $progress / $max")
                runOnUiThread {
                    if (max >= 0) {
                        binding.progressBar.max = max
                        binding.progressBar.setProgressCompat(progress, true)
                        binding.progressTextView.text =
                            getString(R.string.status_loading_progress, progress, max)
                    } else {
                        visibility(binding.progressBar, false)
                        binding.progressBar.isIndeterminate = true
                        visibility(binding.progressBar, true)
                        binding.progressTextView.setText(R.string.status_storing)
                    }
                }
            }) {
                if (AREAS.size > 0) {
                    if (deepLinkPath != null) {
                        binding.progressTextView.setText(R.string.status_loading_deep_link)
                        binding.progressBar.visibility(false)
                        binding.progressBar.isIndeterminate = true
                        binding.progressBar.visibility(true)

                        doAsync {
                            val intent =
                                DataClass.getIntent(this@LoadingActivity, deepLinkPath!!, firestore)
                            uiContext {
                                if (intent != null)
                                    startActivity(intent)
                                else
                                    launch(MainActivity::class.java)
                            }
                        }
                    } else
                        launch(MainActivity::class.java)
                } else if (!appNetworkState.hasInternet)
                    noInternetAccess()
            }
        } catch (_: NoInternetAccessException) {
            noInternetAccess()
        }
    }
}
