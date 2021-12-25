package com.arnyminerz.escalaralcoiaicomtat.activity

import android.os.Bundle
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.compose.material3.ExperimentalMaterial3Api
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.DataLoaderInterface
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.isolated.EmailConfirmationActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass.Companion.getIntent
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.shared.*
import com.arnyminerz.escalaralcoiaicomtat.core.utils.*
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.core.worker.BlockStatusWorker
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityLoadingBinding
import com.google.android.gms.common.ConnectionResult.SUCCESS
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.google.firebase.FirebaseException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import com.google.firebase.perf.ktx.performance
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@ExperimentalMaterial3Api
class OldLoadingActivity : NetworkChangeListenerActivity() {
    private lateinit var binding: ActivityLoadingBinding
    private var loading = false

    private var deepLinkPath: String? = null

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var messaging: FirebaseMessaging
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var auth: FirebaseAuth
    private lateinit var remoteConfig: FirebaseRemoteConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Timber.v("Getting Firestore instance...")
        firestore = Firebase.firestore
        Timber.v("Getting Firebase Storage instance...")
        storage = Firebase.storage
        Timber.v("Getting Firebase Messaging instance...")
        messaging = Firebase.messaging
        Timber.v("Getting Firebase Analytics instance...")
        analytics = Firebase.analytics
        Timber.v("Getting Firebase Auth instance...")
        auth = Firebase.auth
        Timber.v("Getting Firebase Remote Config instance...")
        remoteConfig = Firebase.remoteConfig

        doAsync {
            initializeRemoteConfig()
            messagingSubscribeTest()
        }

        checkGooglePlayServices()
        checkMD5Support()
        dataCollectionSetUp()
        messagingTokenGet()
        authSetup()
    }

    override fun onStart() {
        super.onStart()

        // Check takes around 5ms
        val showIntro = IntroActivity.shouldShow()
        if (showIntro) {
            Timber.w("  Showing intro!")
            finish()
            launch(IntroActivity::class.java)
            return
        } else
            Timber.v("  Won't show intro.")

        deepLinkPath = getExtra(EXTRA_LINK_PATH)

        Timber.v("Installing data module...")
        val splitInstallManager = SplitInstallManagerFactory.create(this)
        val request = SplitInstallRequest.newBuilder()
            .addModule("data")
            .build()
        Timber.v("Requesting installation of the data module...")
        splitInstallManager.apply {
            val listener = SplitInstallStateUpdatedListener { state ->
                Timber.v("Got split install update. State: $state")
                when (val status = state.status()) {
                    SplitInstallSessionStatus.FAILED ->
                        Timber.e("Data module download status: Failed")
                    SplitInstallSessionStatus.INSTALLING ->
                        Timber.i("Data module download status: Installing")
                    SplitInstallSessionStatus.INSTALLED -> {
                        Timber.i("Data module download status: Installed")
                        doAsync { load() }
                    }
                    else -> Timber.v("Data module install status: $status")
                }
            }
            Timber.v("Registering listener for data module installation..")
            registerListener(listener)
            Timber.v("Starting data module installation...")
            startInstall(request)
                .addOnSuccessListener { sessionId ->
                    Timber.i("Started install of the data module (sessionId=$sessionId)")
                }
                .addOnFailureListener { exception ->
                    Timber.e(exception, "Could not install data module!")
                    unregisterListener(listener)
                }
        }

        doAsync { initializeBlockStatusWorker() }
    }

    override suspend fun onStateChangeAsync(state: ConnectivityProvider.NetworkState) {
        super.onStateChangeAsync(state)
        //load()
    }

    /**
     * Checks if the device supports MD5 hashing. If it isn't supported, it shows a warning to the
     * user.
     * @author Arnau Mora
     * @since 20210929
     */
    private fun checkMD5Support() {
        val md5Supported = md5Compatible()
        Timber.i("Is MD5 hashing supported: $md5Supported")
        if (!md5Supported) {
            Timber.w("MD5 hashing is not compatible")
            analytics.logEvent("MD5NotSupported") { }
            if (!PREF_SHOWN_MD5_WARNING.get()) {
                Timber.i("Showing MD5 hashing warning dialog")
                MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_App)
                    .setTitle(R.string.dialog_md5_incompatible_title)
                    .setMessage(R.string.dialog_md5_incompatible_message)
                    .setPositiveButton(R.string.action_ok) { dialog, _ ->
                        PREF_SHOWN_MD5_WARNING.put(true)
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    /**
     * Checks if the device supports Google Play services, if not, tell the user that this might
     * affect the experience on the app.
     * @author Arnau Mora
     * @since 20210919
     */
    @UiThread
    private fun checkGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val servicesAvailable = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (servicesAvailable != SUCCESS)
            try {
                googleApiAvailability
                    .getErrorDialog(this, servicesAvailable, 0)
                    ?.show()
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Google Play Services required.")
                toast(R.string.toast_error_google_play)
            }
    }

    /**
     * Initializes the user-set data collection policy.
     * If debugging, data collection will always be disabled.
     * @author Arnau Mora
     * @since 20210617
     * @see SETTINGS_ERROR_REPORTING_PREF
     */
    @UiThread
    private fun dataCollectionSetUp() {
        val enableErrorReporting = SETTINGS_ERROR_REPORTING_PREF.get()

        Firebase.crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG && enableErrorReporting)
        Timber.v("Set Crashlytics collection enabled to $enableErrorReporting")

        Firebase.analytics.setAnalyticsCollectionEnabled(!BuildConfig.DEBUG && enableErrorReporting)
        Timber.v("Set Analytics collection enabled to $enableErrorReporting")

        Firebase.performance.isPerformanceCollectionEnabled = enableErrorReporting
        Timber.v("Set Performance collection enabled to $enableErrorReporting")
    }

    /**
     * Fetches the current Firebase Messaging token and logs it.
     * @author Arnau Mora
     * @since 20210919
     */
    private fun messagingTokenGet() {
        Timber.v("Getting Firebase Messaging token...")
        messaging.token
            .addOnSuccessListener { token ->
                Timber.i("Firebase messaging token: $token")
            }
            .addOnFailureListener { error ->
                Timber.e(error, "Could not get Firebase Messaging token.")
            }
    }

    /**
     * Subscribes to the testing channel if the app version is compiled in debug mode.
     * @author Arnau Mora
     * @since 20210919
     */
    private suspend fun messagingSubscribeTest() {
        if (BuildConfig.DEBUG)
            try {
                Timber.v("Subscribing to \"testing\" topic...")
                messaging.subscribeToTopic("testing")
                    .await()

                Timber.i("Subscribed to topic \"testing\".")
            } catch (e: FirebaseException) {
                Timber.e(e, "Could not subscribe to testing topic.")
            }
    }

    private suspend fun initializeRemoteConfig() {
        Timber.v("Getting remote configuration...")
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = REMOTE_CONFIG_MIN_FETCH_INTERVAL
        }

        try {
            Timber.d("Setting remote config settings...")
            remoteConfig
                .setConfigSettingsAsync(configSettings)
                .await()

            Timber.d("Setting remote config defaults...")
            remoteConfig
                .setDefaultsAsync(REMOTE_CONFIG_DEFAULTS)
                .await()

            Timber.d("Fetching remote config values...")
            val remoteConfigFetched = remoteConfig
                .fetchAndActivate()
                .await()

            if (remoteConfigFetched) {
                Timber.i("Got remote config values!")
                APP_UPDATE_MAX_TIME_DAYS =
                    remoteConfig.getLong(APP_UPDATE_MAX_TIME_DAYS_KEY)
                SHOW_NON_DOWNLOADED =
                    remoteConfig.getBoolean(SHOW_NON_DOWNLOADED_KEY)
                ENABLE_AUTHENTICATION =
                    remoteConfig.getBoolean(ENABLE_AUTHENTICATION_KEY)
                PROFILE_IMAGE_SIZE = remoteConfig.getLong(PROFILE_IMAGE_SIZE_KEY)

                Timber.v("APP_UPDATE_MAX_TIME_DAYS: $APP_UPDATE_MAX_TIME_DAYS")
                Timber.v("SHOW_NON_DOWNLOADED: $SHOW_NON_DOWNLOADED")
                Timber.v("ENABLE_AUTHENTICATION: $ENABLE_AUTHENTICATION")
                Timber.v("PROFILE_IMAGE_SIZE: $PROFILE_IMAGE_SIZE")
            } else
                Timber.w("Could not fetch default remote config.")
        } catch (e: FirebaseException) {
            Timber.e(e, "Could not get remote config.")
        } catch (e: NullPointerException) {
            Timber.e(e, "Could not get remote config.")
        }
    }

    /**
     * Initializes and starts the [BlockStatusWorker] if not already running.
     * @author Arnau Mora
     * @since 20210824
     */
    private suspend fun initializeBlockStatusWorker() {
        // First check if already scheduled.
        val isScheduled = BlockStatusWorker.isScheduled(this)
        if (isScheduled) {
            // It's scheduled, check if we should refresh the schedule
            val incorrectSchedule = BlockStatusWorker.shouldUpdateSchedule(this)
            if (incorrectSchedule) {
                // Cancel the worker for rescheduling it
                Timber.v("The worker's schedule is incorrect. Cancelling...")
                BlockStatusWorker.cancel(this)
            } else {
                Timber.v("Won't start BlockStatusWorker since already running.")
                return
            }
        }
        Timber.v("Scheduling BlockStatusWorker...")
        BlockStatusWorker.schedule(this)
    }

    /**
     * This updates the UI accordingly when there's no Internet connection.
     * @author Arnau Mora
     * @since 20210617
     */
    @UiThread
    private fun noInternetAccess() {
        Timber.w("There's no Internet connection to download new data")
        binding.progressTextView.setText(R.string.status_no_internet)
        binding.progressBar.hide()
        loading = false
    }

    /**
     * Removes auth state listeners if the authentication is not enabled.
     * @author Arnau Mora
     * @since 20210811
     */
    private fun authSetup() {
        if (!ENABLE_AUTHENTICATION) {
            Timber.v("Removing auth state listener...")
            auth.removeAuthStateListener((application as App).authStateListener)
        }
        if (auth.currentUser == null) {
            Timber.d("Signing in anonymously...")
            auth.signInAnonymously()
                .addOnSuccessListener { Timber.i("Logged in anonymously.") }
                .addOnFailureListener { Timber.e(it, "Could not login anonymously:") }
        } else
            Timber.d("Anonymous login not performed.")
    }

    @WorkerThread
    private suspend fun load() {
        val waitingForEmailConfirmation = PREF_WAITING_EMAIL_CONFIRMATION.get()
        if (waitingForEmailConfirmation) {
            uiContext {
                launch(EmailConfirmationActivity::class.java)
            }
            return
        }

        if (loading) {
            Timber.v("Skipped load since already loading")
            return
        }
        Timber.i("Started download from server.")
        loading = true
        uiContext {
            binding.progressTextView.setText(R.string.status_downloading)
        }

        Timber.v("Getting DataLoader instance...")
        val dataLoader = Class
            .forName("com.arnyminerz.escalaralcoiaicomtat.data.DataLoader")
            .kotlin.objectInstance as DataLoaderInterface
        Timber.v("Fetching data...")
        val data = dataLoader.fetchData(this)
        Timber.i("Data fetched from data module!")
        val areas = loadAreas(application as App, data)
        Timber.v("Finished loading areas.")
        if (areas.isNotEmpty()) {
            if (deepLinkPath != null) {
                uiContext {
                    binding.progressTextView.setText(R.string.status_loading_deep_link)
                    binding.progressBar.visibility(false)
                    binding.progressBar.isIndeterminate = true
                    binding.progressBar.visibility(true)
                }

                val intent = getIntent(app, app.searchSession, deepLinkPath!!)
                uiContext {
                    if (intent != null)
                        startActivity(intent)
                    else
                        launch(MainActivity::class.java)
                }
            } else
                uiContext {
                    Timber.v("Launching MainActivity...")
                    launch(MainActivity::class.java)
                }
        } else if (!appNetworkState.hasInternet)
            uiContext {
                noInternetAccess()
            }
        else Timber.v("Areas is empty, but no handle was called.")
    }
}
