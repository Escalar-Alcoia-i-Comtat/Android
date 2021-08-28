package com.arnyminerz.escalaralcoiaicomtat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.DownloadsSettingsFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.GeneralSettingsFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.InfoSettingsFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.MainSettingsFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.MainSettingsFragment.Companion.SettingsPage
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.NotificationsSettingsFragment
import timber.log.Timber

const val SETTINGS_HEIGHT_MAIN = 0
const val SETTINGS_HEIGHT_UPPER = 1

/**
 * The Fragment that manages the settings screen.
 * @author Arnau Mora
 * @since 20210828
 */
class SettingsFragmentManager : Fragment() {
    /**
     * The current page height.
     * @author Arnau Mora
     * @since 20210828
     */
    var height = 0
        private set

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_settings_manager, container, false)

    /**
     * Selects a page in the settings screen.
     * @author Arnau Mora
     * @since 20210828
     * @param page The page to select
     * @param backPressed If the change is requested through a back press.
     */
    fun loadPage(page: SettingsPage, backPressed: Boolean) {
        if (view == null) {
            Timber.w("View not showing, shouldn't load any pages.")
            return
        }

        activity?.supportFragmentManager?.beginTransaction()?.apply {
            if (backPressed)
                if (height > page.height) // Going Back
                    setCustomAnimations(R.anim.enter_left, R.anim.exit_right)
                else
                    setCustomAnimations(R.anim.enter_right, R.anim.exit_left)
            replace(
                R.id.settings_manager_frameLayout,
                when (page) {
                    SettingsPage.MAIN -> MainSettingsFragment().listen {
                        loadPage(it, false)
                    }
                    SettingsPage.GENERAL -> GeneralSettingsFragment()
                    SettingsPage.NOTIFICATIONS -> NotificationsSettingsFragment()
                    SettingsPage.DOWNLOADS -> DownloadsSettingsFragment()
                    SettingsPage.INFO -> InfoSettingsFragment()
                }
            )
            commit()
        } ?: Timber.e("No Activity Found!")

        height = page.height
        Timber.d("Loaded settings page $page. Height: $height")
    }

    override fun onResume() {
        super.onResume()

        if (isResumed)
            loadPage(SettingsPage.MAIN, false)
    }
}
