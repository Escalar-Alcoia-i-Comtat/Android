package com.arnyminerz.escalaralcoiaicomtat.fragment.preferences

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.*
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.LOCATION_PERMISSION_REQUEST
import timber.log.Timber

private const val PREVIEW_SCALE_REDUCER = 10f

@ExperimentalUnsignedTypes
class GeneralSettingsFragment(private val activity: Activity) : PreferenceFragmentCompat() {
    private var sensibilityPreference: SeekBarPreference? = null
    private var markerSizePreference: SeekBarPreference? = null
    private var previewScalePreference: SeekBarPreference? = null
    private var languagePreference: ListPreference? = null
    private var enableNearby: SwitchPreference? = null
    private var centerMarkerPreference: SwitchPreference? = null
    private var nearbyDistance: EditTextPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_general, rootKey)

        onResume()

        sensibilityPreference = findPreference("pref_swipe_sensibility")
        sensibilityPreference?.setOnPreferenceChangeListener { _, value ->
            SETTINGS_GESTURE_SENSIBILITY_PREF.put(requireContext().sharedPreferences, value as Int)
            true
        }

        languagePreference = findPreference("pref_language")
        languagePreference?.setOnPreferenceChangeListener { _, newValue ->
            Timber.v("New Language: $newValue")
            val languages = context?.resources?.getStringArray(R.array.app_languages_values)
            if (languages != null) {
                val langIndex = languages.indexOf(newValue)
                Timber.d("Language index: $langIndex")
                SETTINGS_LANGUAGE_PREF.put(requireContext().sharedPreferences, langIndex)
                when (SETTINGS_LANGUAGE_PREF.get(requireContext().sharedPreferences)) {
                    0 -> // English
                        Timber.d("Set English")
                    1 -> // Catalan
                        Timber.d("Set Catalan")
                    2 -> // Spanish
                        Timber.d("Set Spanish")
                    else -> Timber.d("Option not handled!")
                }

                with(requireActivity()) {
                    finish()
                    overridePendingTransition(0, 0)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
            } else Timber.d("Language not found!")
            true
        }

        enableNearby = findPreference("pref_enable_nearby")
        enableNearby?.setOnPreferenceChangeListener { _, value ->
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                PREF_DISABLE_NEARBY.put(requireContext().sharedPreferences, !(value as Boolean))
                true
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST
                )

                false
            }
        }

        nearbyDistance = findPreference("pref_nearby_distance")
        nearbyDistance?.setOnPreferenceChangeListener { _, value ->
            SETTINGS_NEARBY_DISTANCE_PREF.put(requireContext().sharedPreferences, (value as String).toInt())
            true
        }

        markerSizePreference = findPreference("pref_marker_size")
        markerSizePreference?.setOnPreferenceChangeListener { _, value ->
            SETTINGS_MARKER_SIZE_PREF.put(requireContext().sharedPreferences, value as Int)
            true
        }

        centerMarkerPreference = findPreference("pref_move_marker")
        centerMarkerPreference?.setOnPreferenceChangeListener { _, value ->
            SETTINGS_CENTER_MARKER_PREF.put(requireContext().sharedPreferences, value as Boolean)
            true
        }

        previewScalePreference = findPreference("pref_preview_scale")
        previewScalePreference?.setOnPreferenceChangeListener { _, value ->
            SETTINGS_PREVIEW_SCALE_PREF.put(
                requireContext().sharedPreferences,
                (value as Int).toFloat() / PREVIEW_SCALE_REDUCER
            )
            true
        }
    }

    override fun onResume() {
        super.onResume()

        sensibilityPreference?.value = SETTINGS_GESTURE_SENSIBILITY_PREF.get(requireContext().sharedPreferences)

        languagePreference?.setValueIndex(
            SETTINGS_LANGUAGE_PREF.get(requireContext().sharedPreferences)
        )

        enableNearby?.isChecked = !(PREF_DISABLE_NEARBY.get(requireContext().sharedPreferences)) &&
                (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)

        val nearbyDistancePref = SETTINGS_NEARBY_DISTANCE_PREF.get(requireContext().sharedPreferences).toString()
        Timber.d("Nearby distance: $nearbyDistancePref")
        nearbyDistance?.text = nearbyDistancePref

        markerSizePreference?.value = SETTINGS_MARKER_SIZE_PREF.get(requireContext().sharedPreferences)
        previewScalePreference?.value =
            (SETTINGS_PREVIEW_SCALE_PREF.get(requireContext().sharedPreferences) * 10).toInt()
    }
}
