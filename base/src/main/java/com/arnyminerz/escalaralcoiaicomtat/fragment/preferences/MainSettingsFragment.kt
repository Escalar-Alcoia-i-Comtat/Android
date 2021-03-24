package com.arnyminerz.escalaralcoiaicomtat.fragment.preferences

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.isolated.FeedbackActivity
import com.arnyminerz.escalaralcoiaicomtat.data.preference.PreferenceData
import com.arnyminerz.escalaralcoiaicomtat.fragment.SETTINGS_HEIGHT_MAIN
import com.arnyminerz.escalaralcoiaicomtat.fragment.SETTINGS_HEIGHT_UPPER
import com.arnyminerz.escalaralcoiaicomtat.shared.DOWNLOAD_QUALITY_DEFAULT

private const val GESTURE_SENSIBILITY_DEFAULT = 3
private const val NEARBY_DISTANCE_DEFAULT = 1000
private const val MARKER_SIZE_DEFAULT = 3
private const val PREVIEW_SCALE_DEFAULT = .5f

val SETTINGS_ALERT_PREF = PreferenceData("alert_pref", true)
val SETTINGS_GESTURE_SENSIBILITY_PREF =
    PreferenceData("gest_sens_pref", GESTURE_SENSIBILITY_DEFAULT)
val SETTINGS_LANGUAGE_PREF = PreferenceData("lang_pref", "en")
val SETTINGS_NEARBY_DISTANCE_PREF = PreferenceData("nearby_distance", NEARBY_DISTANCE_DEFAULT)
val SETTINGS_MARKER_SIZE_PREF = PreferenceData("marker_size", MARKER_SIZE_DEFAULT)
val SETTINGS_CENTER_MARKER_PREF = PreferenceData("center_marker", true)
val SETTINGS_PREVIEW_SCALE_PREF = PreferenceData("preview_scale", PREVIEW_SCALE_DEFAULT)
val SETTINGS_MOBILE_DOWNLOAD_PREF = PreferenceData("mobile_download", true)
val SETTINGS_ROAMING_DOWNLOAD_PREF = PreferenceData("roaming_download", false)
val AUTOMATIC_DOWNLOADS_UPDATE_PREF = PreferenceData("automatic_downloads_update", false)
val DOWNLOADS_QUALITY_PREF = PreferenceData("downloads_quality", DOWNLOAD_QUALITY_DEFAULT)
val PREF_DISABLE_NEARBY = PreferenceData("NearbyZonesDisable", false)
val PREF_SHOWN_INTRO = PreferenceData("ShownIntro", false)

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
