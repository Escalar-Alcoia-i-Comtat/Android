package com.arnyminerz.escalaralcoiaicomtat.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.UiThread
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.data.IntroShowReason
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.loadAreasFromCache
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityLoadingBinding
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.notification.createNotificationChannels
import com.arnyminerz.escalaralcoiaicomtat.shared.APP_UPDATE_MAX_TIME_DAYS
import com.arnyminerz.escalaralcoiaicomtat.shared.APP_UPDATE_MAX_TIME_DAYS_DEFAULT
import com.arnyminerz.escalaralcoiaicomtat.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.shared.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
import com.google.android.play.core.tasks.Tasks
import com.parse.ParseConfig
import com.parse.ParseException
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid
import io.sentry.android.timber.SentryTimberIntegration
import timber.log.Timber
import java.util.concurrent.CompletableFuture.runAsync

class LoadingActivity : NetworkChangeListenerActivity() {
    companion object {
        private const val APP_UPDATE_REQUEST_CODE = 8 // This number was chosen by Eva
    }

    private lateinit var binding: ActivityLoadingBinding
    private var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Timber.plant(Timber.DebugTree())
        Timber.v("Planted Timber.")

        Timber.v("Instantiating Sentry")
        SentryAndroid.init(this) { options ->
            options.addIntegration(
                SentryTimberIntegration(SentryLevel.ERROR, SentryLevel.INFO)
            )
        }

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

        runAsync {
            Timber.v("Getting Parse configuration...")
            try {
                val config = ParseConfig.get()
                Timber.d("Got Parse configuration.")
                APP_UPDATE_MAX_TIME_DAYS =
                    config.getInt("APP_UPDATE_MAX_TIME_DAYS", APP_UPDATE_MAX_TIME_DAYS_DEFAULT)
                Timber.d("  APP_UPDATE_MAX_TIME_DAYS=$APP_UPDATE_MAX_TIME_DAYS")
            } catch (e: ParseException) {
                Timber.w("Could not get configuration. Loading defaults...")
                APP_UPDATE_MAX_TIME_DAYS = APP_UPDATE_MAX_TIME_DAYS_DEFAULT
            }

            Timber.v("Searching for updates...")
            val appUpdateManager = AppUpdateManagerFactory.create(this)
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo
            val appUpdateInfo = Tasks.await(appUpdateInfoTask)
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
                        return@runAsync
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
                    return@runAsync
                } else Timber.w("Flexible update is not allowed")
            } else Timber.d("There's no update available. ($updateAvailability)")

            Timber.v("Finished preparing App...")
            load()
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

    private fun load() {
        if (loading)
            return
        loading = true
        binding.progressTextView.setText(R.string.status_downloading)
        try {
            runAsync {
                loadAreasFromCache({ progress, max ->
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
                    if (AREAS.size > 0)
                        startActivity(Intent(this, MainActivity::class.java))
                    else if (!appNetworkState.hasInternet)
                        noInternetAccess()
                }
            }
        } catch (e: NoInternetAccessException) {
            noInternetAccess()
        }
    }
}
