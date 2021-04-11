package com.arnyminerz.escalaralcoiaicomtat.fragment.preferences

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R

class InfoSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_info, rootKey)

        val versionPreference: Preference? = findPreference("pref_version")
        val buildPreference: Preference? = findPreference("pref_build")
        val githubPreference: Preference? = findPreference("pref_github")

        val versionCode = BuildConfig.VERSION_CODE
        val versionName = BuildConfig.VERSION_NAME

        versionPreference?.summary = versionName
        buildPreference?.summary = versionCode.toString()

        githubPreference?.setOnPreferenceClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://github.com/ArnyminerZ/EscalarAlcoiaIComtat-Android")
            })
            true
        }
    }
}
