package com.arnyminerz.escalaralcoiaicomtat.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkAwareComponentActivity
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_LINK_PATH
import com.arnyminerz.escalaralcoiaicomtat.core.shared.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.LoadingActivityViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.loadingActivityViewModel
import com.arnyminerz.escalaralcoiaicomtat.view.model.LoadingViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import timber.log.Timber

/**
 * The Activity that is shown to the user when the app is loading.
 * @author Arnau Mora
 * @since 20211225
 */
class LoadingActivity : NetworkAwareComponentActivity() {
    companion object {
        /**
         * The key used in shared preferences for checking if data has been indexed before v83.
         * @author Arnau Mora
         * @since 20220125
         */
        private const val LegacyIndexedSearchKey = "SearchIndexed"
    }

    /**
     * The Firebase Messaging instance to use.
     * @author Arnau Mora
     * @since 20211225
     */
    private lateinit var messaging: FirebaseMessaging

    /**
     * The Firebase Analytics instance to use.
     * @author Arnau Mora
     * @since 20211225
     */
    private lateinit var analytics: FirebaseAnalytics

    /**
     * The Firebase Remote Config instance to use.
     * @author Arnau Mora
     * @since 20211225
     */
    private lateinit var remoteConfig: FirebaseRemoteConfig

    /**
     * Used for storing the path if launched from a deep link.
     * @author Arnau Mora
     * @since 20211225
     */
    private var deepLinkPath: String? = null

    /**
     * The loading activity view model. Used for fetching preferences.
     * @author Arnau Mora
     * @since 20211229
     */
    private val viewModel by viewModels<LoadingActivityViewModel>(factoryProducer = { PreferencesModule.loadingActivityViewModel })

    private lateinit var loadingViewModel: LoadingViewModel

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Detect if there's a sharedPreference for intro shown set to true. This means the
        // user has already been using the app, and it should be informed that the behaviour has
        // been changed, and that it should download everything again. Or create a migration dialog.
        val shownIntro = viewModel.introShown.value
        if (!shownIntro) {
            Timber.w("Showing intro!")
            finish()
            launch(IntroActivity::class.java)
            return
        } else
            Timber.v("Won't show intro.")

        Timber.i("Getting deep link...")
        deepLinkPath = getExtra(EXTRA_LINK_PATH)

        Timber.i("Initializing Firebase instances...")
        Timber.v("Getting Firebase Messaging instance...")
        messaging = Firebase.messaging
        Timber.v("Getting Firebase Analytics instance...")
        analytics = Firebase.analytics
        Timber.v("Getting Firebase Remote Config instance...")
        remoteConfig = Firebase.remoteConfig

        setContent {
            AppTheme {
                loadingViewModel = viewModel()
                val progressMessageResource by loadingViewModel.progressMessageResource
                val progressMessageAttributes by loadingViewModel.progressMessageAttributes

                val errorMessageResource by loadingViewModel.errorMessage

                Scaffold {
                    LoadingWindow(
                        stringResource(progressMessageResource, progressMessageAttributes),
                        errorMessageResource != null,
                        if (errorMessageResource != null) stringResource(errorMessageResource!!) else ""
                    )
                }

                loadingViewModel.migratedFromSharedPreferences =
                    sharedPreferences.getBoolean(LegacyIndexedSearchKey, false)

                loadingViewModel.startLoading(
                    deepLinkPath,
                    remoteConfig,
                    messaging,
                    analytics
                )
            }
        }
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)

        if (this::loadingViewModel.isInitialized)
            loadingViewModel.tryLoading()
    }
}

@Composable
fun LoadingWindow(
    progressMessage: String,
    shouldShowErrorMessage: Boolean = false,
    errorMessage: String? = null
) {
    val bottomPadding: Int by animateIntAsState(
        targetValue = if (shouldShowErrorMessage && errorMessage != null) 500 else 0,
        animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing)
    )
    val drawable = AppCompatResources.getDrawable(LocalContext.current, R.mipmap.ic_launcher_round)
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = bottomPadding.dp)
        ) {
            Image(
                painter = rememberDrawablePainter(drawable),
                contentDescription = "",
                modifier = Modifier
                    .size(128.dp)
                    .align(Alignment.TopCenter)
            )
        }
        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(.7f),
            visible = shouldShowErrorMessage && errorMessage != null
        ) {
            Text(
                text = errorMessage ?: "",
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Visible
            )
        }

        AnimatedVisibility(
            visible = !shouldShowErrorMessage,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    modifier = Modifier
                        .padding(bottom = 9.dp, end = 4.dp),
                    text = progressMessage,
                    textAlign = TextAlign.End
                )
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
    }
}
