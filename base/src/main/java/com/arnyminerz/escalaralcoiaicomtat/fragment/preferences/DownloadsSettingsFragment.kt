package com.arnyminerz.escalaralcoiaicomtat.fragment.preferences

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.UpdatingActivity
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOADS_QUALITY_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_QUALITY_MAX
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_QUALITY_MIN
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_MOBILE_DOWNLOAD_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_ROAMING_DOWNLOAD_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.shared.UPDATE_IMAGES
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra

class DownloadsSettingsFragment : PreferenceFragmentCompat() {

    private var mobileDataDownloadPref: SwitchPreference? = null
    private var roamingDownloadPref: SwitchPreference? = null
    private var downloadDownloadsPref: Preference? = null

    private var downloadQualityPref: SeekBarPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_downloads, rootKey)

        mobileDataDownloadPref = findPreference("pref_mobile_download")
        mobileDataDownloadPref?.isChecked = SETTINGS_MOBILE_DOWNLOAD_PREF.get()
        mobileDataDownloadPref?.setOnPreferenceClickListener { p ->
            val pref = p as SwitchPreference
            SETTINGS_MOBILE_DOWNLOAD_PREF.put(pref.isChecked)

            true
        }

        roamingDownloadPref = findPreference("pref_roaming_download")
        roamingDownloadPref?.isChecked = SETTINGS_ROAMING_DOWNLOAD_PREF.get()
        roamingDownloadPref?.setOnPreferenceClickListener { p ->
            val pref = p as SwitchPreference
            SETTINGS_ROAMING_DOWNLOAD_PREF.put(pref.isChecked)

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

        downloadQualityPref = findPreference("pref_download_quality")
        downloadQualityPref?.value = DOWNLOADS_QUALITY_PREF.get()
        downloadQualityPref?.max = DOWNLOAD_QUALITY_MAX
        downloadQualityPref?.min = DOWNLOAD_QUALITY_MIN
        downloadQualityPref?.setOnPreferenceChangeListener { _, value ->
            DOWNLOADS_QUALITY_PREF.put(value as Int)

            true
        }
    }

    override fun onResume() {
        super.onResume()

        mobileDataDownloadPref = mobileDataDownloadPref ?: findPreference("pref_mobile_download")
        mobileDataDownloadPref?.isChecked = SETTINGS_MOBILE_DOWNLOAD_PREF.get()

        roamingDownloadPref = roamingDownloadPref ?: findPreference("pref_roaming_download")
        roamingDownloadPref?.isChecked = SETTINGS_ROAMING_DOWNLOAD_PREF.get()

        downloadQualityPref = downloadQualityPref ?: findPreference("pref_download_quality")
        downloadQualityPref?.value = DOWNLOADS_QUALITY_PREF.get()
        downloadQualityPref?.max = DOWNLOAD_QUALITY_MAX
        downloadQualityPref?.min = DOWNLOAD_QUALITY_MIN
    }
}
