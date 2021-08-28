package com.arnyminerz.escalaralcoiaicomtat.fragment.preferences

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.shared.LOCATION_PERMISSION_REQUEST_CODE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PREF_DISABLE_NEARBY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PREVIEW_SCALE_PREFERENCE_MULTIPLIER
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_CENTER_MARKER_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_ERROR_REPORTING_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_LANGUAGE_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_NEARBY_DISTANCE_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_PREVIEW_SCALE_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import timber.log.Timber

private const val PREVIEW_SCALE_REDUCER = 10f

class GeneralSettingsFragment : PreferenceFragmentCompat() {
    private var errorReportingPreference: SwitchPreference? = null
    private var previewScalePreference: SeekBarPreference? = null
    private var languagePreference: ListPreference? = null
    private var enableNearby: SwitchPreference? = null
    private var centerMarkerPreference: SwitchPreference? = null
    private var nearbyDistance: EditTextPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_general, rootKey)

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

        previewScalePreference = findPreference("pref_preview_scale")
        previewScalePreference?.setOnPreferenceChangeListener { _, value ->
            SETTINGS_PREVIEW_SCALE_PREF.put(
                (value as Int).toFloat() / PREVIEW_SCALE_REDUCER
            )
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

        previewScalePreference?.value =
            (SETTINGS_PREVIEW_SCALE_PREF.get() * PREVIEW_SCALE_PREFERENCE_MULTIPLIER).toInt()

        errorReportingPreference?.isChecked = SETTINGS_ERROR_REPORTING_PREF.get()
    }
}
