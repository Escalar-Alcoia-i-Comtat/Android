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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.arnyminerz.escalaralcoiaicomtat.activity.isolated.EmailConfirmationActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageComponentActivity
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_LINK_PATH
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PREF_WAITING_EMAIL_CONFIRMATION
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.LoadingActivityViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.loadingActivityViewModel
import com.arnyminerz.escalaralcoiaicomtat.view.model.LoadingViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import timber.log.Timber

/**
 * The Activity that is shown to the user when the app is loading.
 * @author Arnau Mora
 * @since 20211225
 */
class LoadingActivity : LanguageComponentActivity() {
    /**
     * The Firestore instance to use.
     * @author Arnau Mora
     * @since 20211225
     */
    private lateinit var firestore: FirebaseFirestore

    /**
     * The Firebase Storage instance to use.
     * @author Arnau Mora
     * @since 20211225
     */
    private lateinit var storage: FirebaseStorage

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
     * The Firebase Auth instance to use.
     * @author Arnau Mora
     * @since 20211225
     */
    private lateinit var auth: FirebaseAuth

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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val shownIntro = viewModel.introShown.value
        if (!shownIntro) {
            Timber.w("Showing intro!")
            finish()
            launch(IntroActivity::class.java)
            return
        } else
            Timber.v("Won't show intro.")

        val waitingForEmailConfirmation = PREF_WAITING_EMAIL_CONFIRMATION.get()
        if (waitingForEmailConfirmation) {
            Timber.i("Launching email confirmation activity.")
            finish()
            launch(EmailConfirmationActivity::class.java)
            return
        }

        Timber.i("Getting deep link...")
        deepLinkPath = getExtra(EXTRA_LINK_PATH)

        Timber.i("Initializing Firebase instances...")
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

        setContent {
            AppTheme {
                val loadingViewModel: LoadingViewModel = viewModel()
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

                loadingViewModel.startLoading(
                    deepLinkPath,
                    remoteConfig,
                    messaging,
                    analytics,
                    auth
                )
            }
        }
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
                .fillMaxWidth(.5f),
            visible = shouldShowErrorMessage && errorMessage != null
        ) {
            Text(
                text = errorMessage ?: "",
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Visible
            )
        }

        Text(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 9.dp, end = 4.dp),
            text = progressMessage,
            textAlign = TextAlign.End
        )
        LinearProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
        )
    }
}
