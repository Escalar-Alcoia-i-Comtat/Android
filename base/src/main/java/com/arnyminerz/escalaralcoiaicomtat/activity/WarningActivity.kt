package com.arnyminerz.escalaralcoiaicomtat.activity

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_WARNING_INTENT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_WARNING_MD5
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_WARNING_PLAY_SERVICES
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_WARNING_PREFERENCE
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.IntroViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.introViewModelFactory
import com.arnyminerz.lib.app_intro.IntroPageData
import com.arnyminerz.lib.app_intro.IntroWindow
import com.google.accompanist.pager.ExperimentalPagerApi
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
class WarningActivity : ComponentActivity() {
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
                val introPages = mutableListOf<IntroPageData<*>>()
                    .apply {
                        if (intent.getExtra(EXTRA_WARNING_PREFERENCE, false))
                            add(
                                IntroPageData<Any?>(
                                    stringResource(R.string.warning_preferences_title),
                                    stringResource(R.string.warning_preferences_message)
                                )
                            )
                        if (intent.getExtra(EXTRA_WARNING_PLAY_SERVICES, false))
                            add(
                                IntroPageData<Any?>(
                                    stringResource(R.string.warning_play_services_title),
                                    stringResource(R.string.warning_play_services_message)
                                )
                            )
                        if (intent.getExtra(EXTRA_WARNING_MD5, false))
                            add(
                                IntroPageData<Any?>(
                                    stringResource(R.string.warning_md5_title),
                                    stringResource(R.string.warning_md5_message)
                                )
                            )
                    }

                IntroWindow(
                    introPages.toList(),
                ) {
                    PreferencesModule
                        .systemPreferencesRepository
                        .apply {
                            doAsync {
                                shownPlayServicesWarning()
                                shownPreferencesMigrationWarning()
                                markMd5WarningShown()

                                uiContext {
                                    intent.getExtra(EXTRA_WARNING_INTENT)
                                        ?.let { launch(it as Intent) }
                                        ?: launch(MainActivity::class.java) {
                                            addFlags(FLAG_ACTIVITY_NEW_TASK)
                                        }
                                }
                            }
                        }
                }
            }
        }
    }
}
