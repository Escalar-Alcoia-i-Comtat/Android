package com.arnyminerz.escalaralcoiaicomtat.activity

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.res.stringResource
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.Keys
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.observe
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.set
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_WARNING_INTENT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_WARNING_MD5
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_WARNING_PREFERENCE
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.lib.app_intro.IntroPageData
import com.arnyminerz.lib.app_intro.IntroWindow
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import timber.log.Timber

class WarningActivity : AppCompatActivity() {
    @OptIn(
        ExperimentalPagerApi::class,
        ExperimentalMaterial3Api::class,
        ExperimentalPermissionsApi::class,
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Listen for changes on introShown
        /*observe(Keys.shownIntro, false) { shownIntro ->
            if (shownIntro) {
                Timber.i("Marked shownIntro as true, launching LoadingActivity")
                uiContext { launch(LoadingActivity::class.java) }
            }
        }*/

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
                    doAsync {
                        set(Keys.shownPlayServicesWarning, true)
                        set(Keys.shownPreferencesWarning, true)
                        set(Keys.shownMd5Warning, true)

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
