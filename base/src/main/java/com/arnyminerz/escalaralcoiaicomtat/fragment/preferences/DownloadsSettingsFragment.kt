package com.arnyminerz.escalaralcoiaicomtat.fragment.preferences

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.UPDATE_CACHE
import com.arnyminerz.escalaralcoiaicomtat.activity.UPDATE_IMAGES
import com.arnyminerz.escalaralcoiaicomtat.activity.UpdatingActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra

@ExperimentalUnsignedTypes
class DownloadsSettingsFragment : PreferenceFragmentCompat() {

    private var mobileDataDownloadPref: SwitchPreference? = null
    private var roamingDownloadPref: SwitchPreference? = null
    private var downloadDataPref: Preference? = null
    private var downloadDownloadsPref: Preference? = null

    private var downloadsAutoUpdatePref: SwitchPreference? = null
    private var dataAutoUpdatePref: SwitchPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_downloads, rootKey)

        mobileDataDownloadPref = findPreference("pref_mobile_download")
        mobileDataDownloadPref?.isChecked = SETTINGS_MOBILE_DOWNLOAD_PREF.get(sharedPreferences!!)
        mobileDataDownloadPref?.setOnPreferenceClickListener { p ->
            val pref = p as SwitchPreference
            SETTINGS_MOBILE_DOWNLOAD_PREF.put(sharedPreferences!!, pref.isChecked)

            true
        }

        roamingDownloadPref = findPreference("pref_roaming_download")
        roamingDownloadPref?.isChecked =
            SETTINGS_ROAMING_DOWNLOAD_PREF.get(sharedPreferences!!)
        roamingDownloadPref?.setOnPreferenceClickListener { p ->
            val pref = p as SwitchPreference
            SETTINGS_ROAMING_DOWNLOAD_PREF.put(sharedPreferences!!, pref.isChecked)

            true
        }

        downloadDataPref = findPreference("pref_download_data")
        downloadDataPref?.setOnPreferenceClickListener {
            startActivity(
                Intent(requireContext(), UpdatingActivity::class.java).apply {
                    putExtra(UPDATE_CACHE, true)
                }
            )

            true
        }

        downloadDownloadsPref = findPreference("pref_download_downloads")
        downloadDownloadsPref?.setOnPreferenceClickListener {
            startActivity(
                Intent(requireContext(), UpdatingActivity::class.java).apply {
                    putExtra(UPDATE_IMAGES, true)
                }
            )

            true
        }

        downloadsAutoUpdatePref = findPreference("pref_download_auto_update")
        downloadsAutoUpdatePref?.isChecked =
            AUTOMATIC_DOWNLOADS_UPDATE_PREF.get(sharedPreferences!!)
        downloadsAutoUpdatePref?.setOnPreferenceClickListener { p ->
            val pref = p as SwitchPreference
            AUTOMATIC_DOWNLOADS_UPDATE_PREF.put(sharedPreferences!!, pref.isChecked)

            true
        }

        dataAutoUpdatePref = findPreference("pref_data_auto_update")
        dataAutoUpdatePref?.isChecked =
            AUTOMATIC_DATA_UPDATE_PREF.get(sharedPreferences!!)
        dataAutoUpdatePref?.setOnPreferenceClickListener { p ->
            val pref = p as SwitchPreference
            AUTOMATIC_DATA_UPDATE_PREF.put(sharedPreferences!!, pref.isChecked)

            true
        }
    }

    override fun onResume() {
        super.onResume()

        mobileDataDownloadPref = mobileDataDownloadPref ?: findPreference("pref_mobile_download")
        mobileDataDownloadPref?.isChecked = SETTINGS_MOBILE_DOWNLOAD_PREF.get(sharedPreferences!!)

        roamingDownloadPref = roamingDownloadPref ?: findPreference("pref_roaming_download")
        roamingDownloadPref?.isChecked = SETTINGS_ROAMING_DOWNLOAD_PREF.get(sharedPreferences!!)

        downloadsAutoUpdatePref =
            downloadsAutoUpdatePref ?: findPreference("pref_download_auto_update")
        downloadsAutoUpdatePref?.isChecked =
            AUTOMATIC_DOWNLOADS_UPDATE_PREF.get(sharedPreferences!!)

        dataAutoUpdatePref = dataAutoUpdatePref ?: findPreference("pref_data_auto_update")
        dataAutoUpdatePref?.isChecked = AUTOMATIC_DATA_UPDATE_PREF.get(sharedPreferences!!)
    }
}