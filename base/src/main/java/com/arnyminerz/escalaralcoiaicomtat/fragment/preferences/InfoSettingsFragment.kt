package com.arnyminerz.escalaralcoiaicomtat.fragment.preferences

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.worker.BlockStatusWorker

class InfoSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_info, rootKey)

        val blockStatusServicePreference: Preference? = findPreference("pref_service_block_status")
        val versionPreference: Preference? = findPreference("pref_version")
        val buildPreference: Preference? = findPreference("pref_build")
        val githubPreference: Preference? = findPreference("pref_github")

        val versionCode = BuildConfig.VERSION_CODE
        val versionName = BuildConfig.VERSION_NAME

        blockStatusServicePreference?.summary =
            getString(
                R.string.pref_info_service_block_status_summary,
                getString(R.string.status_loading)
            )

        versionPreference?.summary = versionName
        buildPreference?.summary = versionCode.toString()

        githubPreference?.setOnPreferenceClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://github.com/ArnyminerZ/EscalarAlcoiaIComtat-Android")
            })
            true
        }

        // Fetch the BlockStatusWorker status
        doAsync {
            val workerInfo = BlockStatusWorker.info(requireContext())
            uiContext {
                val status = workerInfo?.let {
                    if (it.state.isFinished)
                        R.string.status_finished
                    else
                        R.string.status_running
                } ?: R.string.status_not_running

                blockStatusServicePreference?.summary =
                    getString(R.string.pref_info_service_block_status_summary, getString(status))
            }
        }
    }
}
