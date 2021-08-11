package com.arnyminerz.escalaralcoiaicomtat.activity

import android.os.Bundle
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.isolated.EmailConfirmationActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENABLE_AUTHENTICATION
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_LINK_PATH
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PREF_WAITING_EMAIL_CONFIRMATION
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_ERROR_REPORTING_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.shared.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityLoadingBinding
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import timber.log.Timber

class LoadingActivity : NetworkChangeListenerActivity() {
    private lateinit var binding: ActivityLoadingBinding
    private var loading = false

    private var deepLinkPath: String? = null

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Timber.v("Getting Firestore instance...")
        firestore = Firebase.firestore
        Timber.v("Getting Firebase Storage instance...")
        storage = Firebase.storage
    }

    override fun onStart() {
        super.onStart()

        dataCollectionSetUp()

        val time = System.currentTimeMillis()
        // Check takes around 5ms
        val showIntro = IntroActivity.shouldShow()
        if (showIntro) {
            Timber.w("  Showing intro!")
            finish()
            launch(IntroActivity::class.java)
            return
        } else
            Timber.v("  Won't show intro.")
        Timber.v("Intro check time: ${System.currentTimeMillis() - time}")

        deepLinkPath = getExtra(EXTRA_LINK_PATH)

        doAsync { preLoad() }
    }

    override suspend fun onStateChangeAsync(state: ConnectivityProvider.NetworkState) {
        super.onStateChangeAsync(state)
        load()
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
     * This should be ran before [load]. It loads the data from RemoteConfig, and adds some listeners.
     */
    @WorkerThread
    private suspend fun preLoad() {
        if (!ENABLE_AUTHENTICATION) {
            Timber.v("Removing auth state listener...")
            Firebase.auth.removeAuthStateListener((application as App).authStateListener)
        }

        Timber.v("Finished preparing App...")
        load()
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
        loading = true
        binding.progressTextView.setText(R.string.status_downloading)

        firestore.loadAreas(application) { progress, max ->
            Timber.i("Download progress: $progress / $max")
            if (max >= 0 && progress < max) {
                binding.progressBar.max = max
                binding.progressBar.setProgressCompat(progress, true)
                binding.progressTextView.text =
                    getString(R.string.status_loading_progress, progress, max)
            } else {
                if (!binding.progressBar.isIndeterminate) {
                    visibility(binding.progressBar, false)
                    binding.progressBar.isIndeterminate = true
                    visibility(binding.progressBar, true)
                }
                binding.progressTextView.setText(R.string.status_storing)
            }
        }
        if (AREAS.size > 0) {
            if (deepLinkPath != null) {
                uiContext {
                    binding.progressTextView.setText(R.string.status_loading_deep_link)
                    binding.progressBar.visibility(false)
                    binding.progressBar.isIndeterminate = true
                    binding.progressBar.visibility(true)
                }

                val intent =
                    DataClass.getIntent(this@LoadingActivity, deepLinkPath!!, storage)
                uiContext {
                    if (intent != null)
                        startActivity(intent)
                    /*else if (BuildConfig.DEBUG)
                        launch(SectorActivity::class.java) {
                            putExtra(EXTRA_AREA, "WWQME983XhriXVhtVxFu")
                            putExtra(EXTRA_ZONE, "LtYZWlzTPwqHsWbYIDTt")
                            putExtra(EXTRA_SECTOR_COUNT, 15)
                            putExtra(EXTRA_SECTOR_INDEX, 11)
                        }*/
                    else
                        launch(MainActivity::class.java)
                }
            }/* else if (BuildConfig.DEBUG)
                        launch(SectorActivity::class.java) {
                            putExtra(EXTRA_AREA, "WWQME983XhriXVhtVxFu")
                            putExtra(EXTRA_ZONE, "LtYZWlzTPwqHsWbYIDTt")
                            putExtra(EXTRA_SECTOR_COUNT, 9)
                            putExtra(EXTRA_SECTOR_INDEX, 6)
                        }*/
            else uiContext {
                launch(MainActivity::class.java)
            }
        } else if (!appNetworkState.hasInternet)
            uiContext {
                noInternetAccess()
            }
    }
}
