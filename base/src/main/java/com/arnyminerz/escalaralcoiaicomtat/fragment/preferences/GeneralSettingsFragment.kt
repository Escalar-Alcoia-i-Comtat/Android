package com.arnyminerz.escalaralcoiaicomtat.fragment.preferences

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.preference.*
import androidx.work.WorkInfo
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.shared.*
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.worker.UpdateWorker
import timber.log.Timber

class GeneralSettingsFragment : PreferenceFragmentCompat() {
    private var errorReportingPreference: SwitchPreference? = null
    private var languagePreference: ListPreference? = null
    private var enableNearby: SwitchPreference? = null
    private var centerMarkerPreference: SwitchPreference? = null
    private var nearbyDistance: EditTextPreference? = null
    private var downloadDataPreference: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_general, rootKey)

        downloadDataPreference = findPreference("pref_download_data")
        downloadDataPreference?.setOnPreferenceClickListener {
            Timber.v("Scheduling UpdateWorker...")
            UpdateWorker.schedule(requireContext())
            observeUpdateWorkerState()
            true
        }

        errorReportingPreference = findPreference("pref_error_reporting")
        errorReportingPreference?.setOnPreferenceChangeListener { _, value ->
            val newValue = value as Boolean
            SETTINGS_ERROR_REPORTING_PREF.put(newValue)
            true
        }

        languagePreference = findPreference("pref_language")
        languagePreference?.setOnPreferenceChangeListener { _, value ->
            val language = value as? String?
            Timber.v("New Language: $language")
            if (language != null) {
                SETTINGS_LANGUAGE_PREF.put(language)
                when (SETTINGS_LANGUAGE_PREF.get()) {
                    "en" -> // English
                        Timber.d("Set English")
                    "ca" -> // Catalan
                        Timber.d("Set Catalan")
                    "es" -> // Spanish
                        Timber.d("Set Spanish")
                    else -> {
                        Timber.d("Option not handled!")
                        toast(requireContext(), R.string.toast_error_language)
                        return@setOnPreferenceChangeListener false
                    }
                }

                with(requireActivity()) {
                    finish()
                    overridePendingTransition(0, 0)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
                true
            } else {
                Timber.d("Could not get languages list")
                toast(requireContext(), R.string.toast_error_language)
                false
            }
        }

        enableNearby = findPreference("pref_enable_nearby")
        enableNearby?.setOnPreferenceChangeListener { _, value ->
            if (ContextCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                PREF_DISABLE_NEARBY.put(!(value as Boolean))
                true
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST_CODE
                )

                false
            }
        }

        nearbyDistance = findPreference("pref_nearby_distance")
        nearbyDistance?.setOnPreferenceChangeListener { _, value ->
            SETTINGS_NEARBY_DISTANCE_PREF.put((value as String).toInt())
            true
        }

        centerMarkerPreference = findPreference("pref_move_marker")
        centerMarkerPreference?.setOnPreferenceChangeListener { _, value ->
            SETTINGS_CENTER_MARKER_PREF.put(value as Boolean)
            true
        }

        updateFields()
    }

    override fun onResume() {
        super.onResume()

        updateFields()
    }

    /**
     * Updates all the preferences to match the stored value
     * @author Arnau Mora
     * @since 20210324
     */
    private fun updateFields() {
        languagePreference?.setDefaultValue(
            SETTINGS_LANGUAGE_PREF.get()
        )

        enableNearby?.isChecked = !(PREF_DISABLE_NEARBY.get()) &&
                (ContextCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)

        val nearbyDistancePref = SETTINGS_NEARBY_DISTANCE_PREF.get().toString()
        Timber.d("Nearby distance: $nearbyDistancePref")
        nearbyDistance?.text = nearbyDistancePref

        errorReportingPreference?.isChecked = SETTINGS_ERROR_REPORTING_PREF.get()

        observeUpdateWorkerState()
    }

    /**
     * Starts observing the work info of the [UpdateWorker] for updating the enabled state of
     * [downloadDataPreference].
     * @author Arnau Mora
     * @since 20210926
     */
    private fun observeUpdateWorkerState() {
        if (downloadDataPreference == null) {
            Timber.d("Won't observe UpdateWorker state since pref is null.")
            return
        }

        Timber.v("Observing UpdateWorker state...")
        val lifecycleOwner = requireActivity() as LifecycleOwner
        doAsync {
            UpdateWorker.getWorkInfo(requireContext())?.let { liveData ->
                uiContext {
                    liveData.observe(lifecycleOwner) { workInfo ->
                        handleWorkInfo(workInfo) {
                            Timber.i("Removing UpdateWorker observer since already finished.")
                            liveData.removeObservers(lifecycleOwner)
                        }
                    }
                }
            }
        }
    }

    private fun handleWorkInfo(workInfo: WorkInfo, removeListener: () -> Unit) {
        val state = workInfo.state
        val finished = state.isFinished
        Timber.v("UpdateWorker's state update: $state")

        if (!finished) {
            val progressData = workInfo.progress
            val step = progressData.getString(UpdateWorker.PROGRESS_KEY_STEP)
            val progress = progressData.getInt(UpdateWorker.PROGRESS_KEY_VALUE, -1)
            Timber.v("UpdateWorker step: $step. Progress: $progress.")
        }

        downloadDataPreference?.isEnabled = finished

        if (finished)
            removeListener()
    }
}
