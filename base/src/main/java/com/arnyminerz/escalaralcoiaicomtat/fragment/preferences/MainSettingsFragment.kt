package com.arnyminerz.escalaralcoiaicomtat.fragment.preferences

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.isolated.FeedbackActivity
import com.arnyminerz.escalaralcoiaicomtat.fragment.SETTINGS_HEIGHT_MAIN
import com.arnyminerz.escalaralcoiaicomtat.fragment.SETTINGS_HEIGHT_UPPER

class MainSettingsFragment : PreferenceFragmentCompat() {
    companion object {
        enum class SettingsPage(val height: Int) {
            MAIN(SETTINGS_HEIGHT_MAIN),
            GENERAL(SETTINGS_HEIGHT_UPPER),
            NOTIFICATIONS(SETTINGS_HEIGHT_UPPER),
            INFO(SETTINGS_HEIGHT_UPPER),
            DOWNLOADS(SETTINGS_HEIGHT_UPPER)
        }
    }

    private var settingsListener: ((page: SettingsPage) -> Unit)? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_main, rootKey)

        val generalPreference: Preference? = findPreference("pref_general")
        val notificationsPreference: Preference? = findPreference("pref_notifications")
        val downloadsPreference: Preference? = findPreference("pref_downloads")
        val infoPreference: Preference? = findPreference("pref_info")
        val feedbackPreference: Preference? = findPreference("pref_feedback")

        generalPreference?.setOnPreferenceClickListener {
            settingsListener?.invoke(SettingsPage.GENERAL)
            true
        }
        notificationsPreference?.setOnPreferenceClickListener {
            settingsListener?.invoke(SettingsPage.NOTIFICATIONS)
            true
        }
        downloadsPreference?.setOnPreferenceClickListener {
            settingsListener?.invoke(SettingsPage.DOWNLOADS)
            true
        }
        infoPreference?.setOnPreferenceClickListener {
            settingsListener?.invoke(SettingsPage.INFO)
            true
        }
        feedbackPreference?.setOnPreferenceClickListener {
            startActivity(Intent(context, FeedbackActivity::class.java))
            true
        }
    }

    fun listen(listener: (page: SettingsPage) -> Unit): MainSettingsFragment {
        this.settingsListener = listener
        return this
    }
}
