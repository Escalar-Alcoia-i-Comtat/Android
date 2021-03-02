package com.arnyminerz.escalaralcoiaicomtat.fragment.preferences

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.UPDATE_CHECKER_TAG


class InfoSettingsFragment : PreferenceFragmentCompat() {
    private var serviceUpdateCheckerPreference: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_info, rootKey)

        val versionPreference: Preference? = findPreference("pref_version")
        val buildPreference: Preference? = findPreference("pref_build")
        val githubPreference: Preference? = findPreference("pref_github")
        serviceUpdateCheckerPreference = findPreference("pref_update_checker")

        val versionCode = BuildConfig.VERSION_CODE
        val versionName = BuildConfig.VERSION_NAME

        versionPreference?.summary = versionName
        buildPreference?.summary = versionCode.toString()

        githubPreference?.setOnPreferenceClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://github.com/Escalar-Alcoia-i-Comtat/Android")
            })
            true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        serviceUpdateCheckerPreference?.summary = getString(R.string.status_loading)
        val workManager = WorkManager.getInstance(requireContext())
        workManager.getWorkInfosByTagLiveData(UPDATE_CHECKER_TAG)
            .observe(viewLifecycleOwner) { workInfos ->
                // There should be just one or any
                if (workInfos.isEmpty()){
                    serviceUpdateCheckerPreference?.summary =
                        getString(R.string.pref_info_service_updates_sum, getString(R.string.status_not_running))
                    return@observe
                }
                val workInfo = workInfos.first()
                val workStateString =
                    when(workInfo.state){
                        WorkInfo.State.RUNNING -> R.string.status_running
                        WorkInfo.State.ENQUEUED -> R.string.status_enqueued
                        WorkInfo.State.SUCCEEDED -> R.string.status_succeeded
                        WorkInfo.State.FAILED -> R.string.status_failed
                        WorkInfo.State.BLOCKED -> R.string.status_blocked
                        WorkInfo.State.CANCELLED -> R.string.status_cancelled
                    }
                serviceUpdateCheckerPreference?.summary =
                    getString(R.string.pref_info_service_updates_sum, getString(workStateString))
            }
        return super.onCreateView(inflater, container, savedInstanceState)
    }
}