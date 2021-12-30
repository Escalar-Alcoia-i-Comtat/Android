package com.arnyminerz.escalaralcoiaicomtat.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.arnyminerz.escalaralcoiaicomtat.core.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.ui.intro.IntroPageData
import com.arnyminerz.escalaralcoiaicomtat.core.ui.intro.IntroWindow
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.IntroViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.introViewModelFactory
import com.google.accompanist.pager.ExperimentalPagerApi
import kotlinx.coroutines.launch
import timber.log.Timber

@ExperimentalMaterial3Api
class IntroActivity : ComponentActivity() {
    /**
     * The view model for updating the preference.
     * @author Arnau Mora
     * @since 20211229
     */
    private val viewModel by viewModels<IntroViewModel>(factoryProducer = { PreferencesModule.introViewModelFactory })

    @OptIn(ExperimentalPagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Listen for changes on introShown
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.introShown.collect { shownIntro ->
                    if (shownIntro)
                        launch(LoadingActivity::class.java)
                }
            }
        }

        setContent {
            AppTheme {
                val introPages = mutableListOf(
                    IntroPageData(
                        stringResource(R.string.intro_main_title, "Escalar Alcoià i Comtat"),
                        stringResource(R.string.intro_main_message)
                    ),
                    IntroPageData(
                        stringResource(R.string.intro_warning_title),
                        stringResource(R.string.intro_warning_message)
                    )
                ).apply {
                    if (BuildConfig.DEBUG)
                    // If debug build, warn user
                        add(
                            IntroPageData(
                                stringResource(R.string.intro_beta_title),
                                stringResource(R.string.intro_beta_message)
                            )
                        )
                }

                IntroWindow(
                    introPages
                ) {
                    Timber.v("Finished showing intro pages. Loading LoadingActivity")
                    viewModel.markIntroAsShown()
                }
            }
        }
    }
}
