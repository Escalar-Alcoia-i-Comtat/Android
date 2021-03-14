package com.arnyminerz.escalaralcoiaicomtat.activity

import android.content.Intent
import android.os.Bundle
import androidx.annotation.UiThread
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.loadAreasFromCache
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityLoadingBinding
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.generic.runAsync
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import timber.log.Timber

class LoadingActivity : NetworkChangeListenerActivity() {
    private lateinit var binding: ActivityLoadingBinding
    private var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                            binding.progressBar.hide()
                            binding.progressBar.isIndeterminate = true
                            binding.progressBar.show()
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
