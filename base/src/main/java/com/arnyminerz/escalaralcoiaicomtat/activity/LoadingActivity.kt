package com.arnyminerz.escalaralcoiaicomtat.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.DataClassActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkAwareActivity
import com.arnyminerz.escalaralcoiaicomtat.core.dataClassExploreActivity
import com.arnyminerz.escalaralcoiaicomtat.core.network.ConnectivityStateListener
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.Keys
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.get
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_LINK_PATH
import com.arnyminerz.escalaralcoiaicomtat.core.shared.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.LoadingWindow
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launchStore
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.view.model.LoadingViewModel
import timber.log.Timber

/**
 * The Activity that is shown to the user when the app is loading.
 * @author Arnau Mora
 * @since 20211225
 */
class LoadingActivity : NetworkAwareActivity() {
    companion object {
        /**
         * The key used in shared preferences for checking if data has been indexed before v83.
         * @author Arnau Mora
         * @since 20220125
         */
        private const val LegacyIndexedSearchKey = "SearchIndexed"
    }

    /**
     * Used for storing the path if launched from a deep link.
     * @author Arnau Mora
     * @since 20211225
     */
    private var deepLinkPath: String? = null

    /**
     * Stores whether or not the server is compatible with the current app's version.
     * @author Arnau Mora
     * @since 2020627
     */
    private val isServerIncompatible = mutableStateOf(false)

    private lateinit var loadingViewModel: LoadingViewModel

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataClassExploreActivity = DataClassActivity::class.java

        // TODO: Detect if there's a sharedPreference for intro shown set to true. This means the
        // user has already been using the app, and it should be informed that the behaviour has
        // been changed, and that it should download everything again. Or create a migration dialog.
        doAsync {
            val shownIntro = get(Keys.shownIntro, false)

            if (!shownIntro) uiContext {
                Timber.w("Showing intro!")
                finish()
                launch(IntroActivity::class.java)
            } else
                Timber.v("Won't show intro.")
        }

        Timber.i("Getting deep link...")
        deepLinkPath = getExtra(EXTRA_LINK_PATH)

        setContent {
            AppTheme {
                loadingViewModel = viewModel()
                val progressMessageResource by loadingViewModel.progressMessageResource
                val progressMessageAttributes by loadingViewModel.progressMessageAttributes

                val errorMessageResource by loadingViewModel.errorMessage

                val serverIncompatible by isServerIncompatible

                if (serverIncompatible)
                    AlertDialog(
                        onDismissRequest = { },
                        confirmButton = {
                            Button(onClick = { launchStore() }) {
                                Text(
                                    stringResource(R.string.action_open_store)
                                )
                            }
                        },
                        dismissButton = {
                            Button(onClick = { finishAndRemoveTask() }) {
                                Text(
                                    stringResource(R.string.action_close_app)
                                )
                            }
                        },
                    )

                Scaffold { padding ->
                    LoadingWindow(
                        padding,
                        MainActivity::class.java,
                        stringResource(progressMessageResource, progressMessageAttributes),
                        shouldShowErrorMessage = errorMessageResource != null,
                        if (errorMessageResource != null) stringResource(errorMessageResource!!) else "",
                    )
                }

                loadingViewModel.migratedFromSharedPreferences =
                    sharedPreferences.getBoolean(LegacyIndexedSearchKey, false)

                try {
                    loadingViewModel.startLoading(deepLinkPath)
                } catch (e: SecurityException) {
                    isServerIncompatible.value = true
                }
            }
        }
    }

    override fun onStateChange(state: ConnectivityStateListener.NetworkState) {
        super.onStateChange(state)

        if (this::loadingViewModel.isInitialized && !isServerIncompatible.value)
            try {
                loadingViewModel.tryLoading()
            } catch (e: SecurityException) {
                isServerIncompatible.value = true
            }
    }
}
