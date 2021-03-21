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
import com.arnyminerz.escalaralcoiaicomtat.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.shared.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid
import io.sentry.android.timber.SentryTimberIntegration
import timber.log.Timber
import java.util.concurrent.CompletableFuture.runAsync

class LoadingActivity : NetworkChangeListenerActivity() {
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

        val showIntro = IntroActivity.shouldShow(this)
        if (showIntro != IntroShowReason.OK) {
            Timber.w("  Showing intro! Reason: ${showIntro.msg}")
            finish()
            startActivity(Intent(this, IntroActivity::class.java))
            return
        } else
            Timber.v("  Won't show intro.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannels()

        Timber.v("Finished preparing App...")
        load()
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        load()
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
