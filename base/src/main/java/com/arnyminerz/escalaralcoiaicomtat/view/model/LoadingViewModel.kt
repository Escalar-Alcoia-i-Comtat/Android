package com.arnyminerz.escalaralcoiaicomtat.view.model

import android.app.Application
import android.content.Context
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.VolleyError
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.WarningActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.DataClassActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.shared.APP_UPDATE_MAX_TIME_DAYS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.APP_UPDATE_MAX_TIME_DAYS_KEY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENABLE_AUTHENTICATION
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENABLE_AUTHENTICATION_KEY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_WARNING_INTENT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_WARNING_PLAY_SERVICES
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_WARNING_PREFERENCE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PROFILE_IMAGE_SIZE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PROFILE_IMAGE_SIZE_KEY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REMOTE_CONFIG_DEFAULTS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REMOTE_CONFIG_MIN_FETCH_INTERVAL
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REST_API_DATA_LIST
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SHOW_NON_DOWNLOADED
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SHOW_NON_DOWNLOADED_KEY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.context
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getJson
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.md5Compatible
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.perf.ktx.performance
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

class LoadingViewModel(application: Application) : AndroidViewModel(application) {
    var progressMessageResource = mutableStateOf(R.string.status_loading)
    var progressMessageAttributes = mutableStateOf(listOf<Any?>())

    @get:StringRes
    var errorMessage = mutableStateOf<Int?>(null)

    var migratedFromSharedPreferences = false
    var hasGooglePlayServices = true

    /**
     * Requests the view model to start loading
     * @author Arnau Mora
     * @since 20211225
     */
    fun startLoading(
        deepLinkPath: String?,
        remoteConfig: FirebaseRemoteConfig,
        messaging: FirebaseMessaging,
        analytics: FirebaseAnalytics
    ) {
        viewModelScope.launch {
            loadingRoutine(deepLinkPath, remoteConfig, messaging, analytics)
        }
    }

    @UiThread
    private suspend fun loadingRoutine(
        deepLinkPath: String?,
        remoteConfig: FirebaseRemoteConfig,
        messaging: FirebaseMessaging,
        analytics: FirebaseAnalytics
    ) {
        val app = getApplication<App>()

        progressMessageResource.value = R.string.status_loading_config
        initializeRemoteConfig(remoteConfig)

        progressMessageResource.value = R.string.status_loading_noti_list
        messagingSubscribeTest(messaging)
        withContext(Dispatchers.IO) {
            messagingTokenGet(messaging)
        }

        progressMessageResource.value = R.string.status_loading_checks
        hasGooglePlayServices = checkGooglePlayServices(app)
        withContext(Dispatchers.IO) {
            checkMD5Support(analytics, app)
        }

        progressMessageResource.value = R.string.status_loading_data_collection
        withContext(Dispatchers.IO) {
            dataCollectionSetUp()
        }

        progressMessageResource.value = R.string.status_loading_data
        withContext(Dispatchers.IO) {
            load(app, deepLinkPath) { stringResource, errorResource ->
                stringResource?.let { progressMessageResource.value = it }
                errorResource?.let { errorMessage.value = it }
            }
        }
    }

    /**
     * Initializes the Firebase Remote Config, and fetches the data from the server.
     * @author Arnau Mora
     * @since 20211225
     */
    private suspend fun initializeRemoteConfig(remoteConfig: FirebaseRemoteConfig) {
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
     * Subscribes to the testing channel if the app version is compiled in debug mode.
     * @author Arnau Mora
     * @since 20210919
     */
    private fun messagingSubscribeTest(messaging: FirebaseMessaging) {
        if (BuildConfig.DEBUG) {
            Timber.v("Subscribing to \"testing\" topic...")
            messaging
                .subscribeToTopic("testing")
                .addOnSuccessListener {
                    Timber.i("Subscribed to topic \"testing\".")
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Could not subscribe to testing topic.")
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
    private fun checkGooglePlayServices(context: Context): Boolean {
        Timber.i("Checking if Google Play Services are available...")
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val servicesAvailable = googleApiAvailability.isGooglePlayServicesAvailable(context)
        if (servicesAvailable != ConnectionResult.SUCCESS) {
            Timber.e("Google Play Services not available.")
            return false
        }
        return true
    }

    /**
     * Checks if the device supports MD5 hashing. If it isn't supported, it shows a warning to the
     * user.
     * @author Arnau Mora
     * @since 20210929
     */
    @WorkerThread
    private suspend fun checkMD5Support(analytics: FirebaseAnalytics, context: Context) {
        val md5Supported = md5Compatible()
        Timber.i("Is MD5 hashing supported: $md5Supported")
        if (!md5Supported) {
            Timber.w("MD5 hashing is not compatible")
            analytics.logEvent("MD5NotSupported") { }
            val systemPrefRepo = PreferencesModule.systemPreferencesRepository
            val getWarnedMd5 = PreferencesModule.shownMd5Warning
            val shownMd5Warning = getWarnedMd5().first()
            if (!shownMd5Warning)
                uiContext {
                    Timber.i("Showing MD5 hashing warning dialog")
                    MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialog_App)
                        .setTitle(R.string.dialog_md5_incompatible_title)
                        .setMessage(R.string.dialog_md5_incompatible_message)
                        .setPositiveButton(R.string.action_ok) { dialog, _ ->
                            doAsync {
                                systemPrefRepo.markMd5WarningShown()
                            }
                            dialog.dismiss()
                        }
                        .show()
                }
        }
    }

    /**
     * Initializes the user-set data collection policy.
     * If debugging, data collection will always be disabled.
     * @author Arnau Mora
     * @since 20210617
     */
    @WorkerThread
    private suspend fun dataCollectionSetUp() {
        val getDataCollection = PreferencesModule.getDataCollection
        val getErrorCollection = PreferencesModule.getErrorCollection
        val dataCollection = getDataCollection().first()
        val errorCollection = getErrorCollection().first()

        Firebase.crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG && errorCollection)
        Timber.v("Set Crashlytics collection enabled to $errorCollection")

        Firebase.analytics.setAnalyticsCollectionEnabled(!BuildConfig.DEBUG && dataCollection)
        Timber.v("Set Analytics collection enabled to $dataCollection")

        Firebase.performance.isPerformanceCollectionEnabled = dataCollection
        Timber.v("Set Performance collection enabled to $dataCollection")
    }

    /**
     * Fetches the current Firebase Messaging token and logs it.
     * @author Arnau Mora
     * @since 20210919
     */
    @WorkerThread
    private fun messagingTokenGet(messaging: FirebaseMessaging) {
        Timber.v("Getting Firebase Messaging token...")
        messaging.token
            .addOnSuccessListener { token ->
                Timber.i("Firebase messaging token: $token")
            }
            .addOnFailureListener { e ->
                Timber.e(e, "Could not get messaging token.")
            }
    }

    /**
     * Loads and processes all the data from the data module.
     * Note: The data module needs to have been installed.
     * @author Arnau Mora
     * @since 20211225
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @WorkerThread
    private suspend fun load(
        app: App,
        deepLinkPath: String?,
        @UiThread progressUpdater: (textResource: Int?, errorResource: Int?) -> Unit
    ) {
        try {
            Timber.v("Fetching areas data...")
            val jsonData = context.getJson("$REST_API_DATA_LIST/*")
            Timber.i("Data fetched from data module!")
            val areas = loadAreas(app, jsonData.getJSONObject("result"))

            Timber.v("Finished loading areas.")
            if (areas.isNotEmpty()) {
                val intent = deepLinkPath?.let { path ->
                    uiContext {
                        progressUpdater(R.string.status_loading_deep_link, null)
                    }

                    DataClass.getIntent(
                        app,
                        DataClassActivity::class.java,
                        path
                    )
                }

                val systemPreferencesRepository = PreferencesModule.systemPreferencesRepository
                val shownPSWarning = systemPreferencesRepository.shownPlayServicesWarning.first()
                val shownPrefWarning = systemPreferencesRepository.shownPreferencesWarning.first()

                uiContext {
                    if ((migratedFromSharedPreferences && !shownPrefWarning) || (!hasGooglePlayServices && !shownPSWarning))
                        app.launch(WarningActivity::class.java) {
                            if (intent != null)
                                putExtra(EXTRA_WARNING_INTENT, intent)
                            putExtra(EXTRA_WARNING_PREFERENCE, migratedFromSharedPreferences)
                            putExtra(EXTRA_WARNING_PLAY_SERVICES, !hasGooglePlayServices)
                        }
                    if (intent != null) {
                        app.launch(intent) {
                            addFlags(FLAG_ACTIVITY_NEW_TASK)
                        }
                    } else {
                        Timber.v("Launching MainActivity...")
                        app.launch(MainActivity::class.java) {
                            addFlags(FLAG_ACTIVITY_NEW_TASK)
                        }
                    }
                }
            } else Timber.v("Areas is empty, but no handle was called.")
        } catch (e: VolleyError) {
            Timber.e(e, "An error occurred while loading areas from the server.")
            uiContext {
                progressUpdater(null, R.string.status_loading_error_server)
            }
        }
    }
}