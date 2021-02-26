package com.arnyminerz.escalaralcoiaicomtat.fragment.preferences

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.isolated.FeedbackActivity
import com.arnyminerz.escalaralcoiaicomtat.data.preference.PreferenceData
import com.arnyminerz.escalaralcoiaicomtat.fragment.SETTINGS_HEIGHT_MAIN
import com.arnyminerz.escalaralcoiaicomtat.fragment.SETTINGS_HEIGHT_UPPER
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerPreferenceFragment

val SETTINGS_ALERT_PREF = PreferenceData("alert_pref", true)
val SETTINGS_GESTURE_SENSIBILITY_PREF = PreferenceData("gest_sens_pref", 3)
val SETTINGS_LANGUAGE_PREF = PreferenceData("language_pref", 0)
val SETTINGS_NEARBY_DISTANCE_PREF = PreferenceData("nearby_distance", 1000)
val SETTINGS_MARKER_SIZE_PREF = PreferenceData("marker_size", 3)
val SETTINGS_CENTER_MARKER_PREF = PreferenceData("center_marker", true)
val SETTINGS_SMALL_MAP_PREF = PreferenceData("small_map", true)
val SETTINGS_PREVIEW_SCALE_PREF = PreferenceData("preview_scale", .5f)
val SETTINGS_MOBILE_DOWNLOAD_PREF = PreferenceData("mobile_download", true)
val SETTINGS_ROAMING_DOWNLOAD_PREF = PreferenceData("roaming_download", false)
val AUTOMATIC_DOWNLOADS_UPDATE_PREF = PreferenceData("automatic_downloads_update", false)
val AUTOMATIC_DATA_UPDATE_PREF = PreferenceData("automatic_data_update", true)
val PREF_DISABLE_NEARBY = PreferenceData("NearbyZonesDisable", false)
val PREF_SHOWN_INTRO = PreferenceData("ShownIntro", false)

@ExperimentalUnsignedTypes
class MainSettingsFragment : NetworkChangeListenerPreferenceFragment() {
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