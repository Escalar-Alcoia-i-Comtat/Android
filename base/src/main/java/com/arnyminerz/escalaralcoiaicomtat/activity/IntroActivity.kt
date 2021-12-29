package com.arnyminerz.escalaralcoiaicomtat.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.arnyminerz.escalaralcoiaicomtat.core.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.ui.intro.IntroPageData
import com.arnyminerz.escalaralcoiaicomtat.core.ui.intro.IntroWindow
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.IntroViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.introViewModelFactory
import com.google.accompanist.pager.ExperimentalPagerApi
import kotlinx.coroutines.flow.single
import timber.log.Timber

@ExperimentalMaterial3Api
class IntroActivity : ComponentActivity() {
    companion object {
        /**
         * Tells whether or not the Intro page should be shown.
         * @author Arnau Mora
         * @since 20210811
         */
        suspend fun shouldShow(): Boolean =
            PreferencesModule
                .systemPreferencesRepository
                .systemPreferences
                .single()
                .shownIntro
    }

    /**
     * The view model for updating the preference.
     * @author Arnau Mora
     * @since 20211229
     */
    private val viewModel by viewModels<IntroViewModel>(factoryProducer = { PreferencesModule.introViewModelFactory })

    @OptIn(ExperimentalPagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.v("Launching")

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
                val context = LocalContext.current

                IntroWindow(
                    introPages
                ) {
                    Timber.v("Finished showing intro pages. Loading LoadingActivity")
                    viewModel.markIntroAsShown()
                    context.startActivity(Intent(this, LoadingActivity::class.java))
                }
            }
        }
    }
}
