package com.arnyminerz.escalaralcoiaicomtat.fragment.preferences

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.*
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.data.preference.store
import com.arnyminerz.escalaralcoiaicomtat.fragment.climb.LOCATION_PERMISSION_REQUEST
import com.arnyminerz.escalaralcoiaicomtat.generic.loadLocale
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import timber.log.Timber

@ExperimentalUnsignedTypes
class GeneralSettingsFragment(private val activity: Activity) : PreferenceFragmentCompat() {
    companion object {
        fun getNearbyZonesDistance(): Int = SETTINGS_NEARBY_DISTANCE_PREF.get(sharedPreferences!!)
    }

    private var sensibilityPreference: SeekBarPreference? = null
    private var markerSizePreference: SeekBarPreference? = null
    private var previewScalePreference: SeekBarPreference? = null
    private var languagePreference: ListPreference? = null
    private var enableNearby: SwitchPreference? = null
    private var centerMarkerPreference: SwitchPreference? = null
    private var smallMapPreference: SwitchPreference? = null
    private var nearbyDistance: EditTextPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_general, rootKey)

        onResume()

        sensibilityPreference = findPreference("pref_swipe_sensibility")
        sensibilityPreference?.setOnPreferenceChangeListener { _, value ->
            if (sharedPreferences == null)
                false
            else {
                SETTINGS_GESTURE_SENSIBILITY_PREF.put(sharedPreferences!!, value as Int)
                true
            }
        }

        languagePreference = findPreference("pref_language")
        languagePreference?.setOnPreferenceChangeListener { _, newValue ->
            Timber.v("New Language: $newValue")
            val languages = context?.resources?.getStringArray(R.array.app_languages_values)
            if (languages != null) {
                val langIndex = languages.indexOf(newValue)
                Timber.d("Language index: $langIndex")
                SETTINGS_LANGUAGE_PREF.put(sharedPreferences!!, langIndex)
                when (langIndex) {
                    0 -> { // English
                        Timber.d("Set English")
                        requireActivity().loadLocale()
                    }
                    1 -> { // Catalan
                        Timber.d("Set Catalan")
                        requireActivity().loadLocale()
                    }
                    2 -> { // Castellano
                        Timber.d("Set Spanish")
                        requireActivity().loadLocale()
                    }
                    else -> Timber.d("Option not handled!")
                }
                activity.finish()
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
                val enabled = value as Boolean
                sharedPreferences?.let {
                    (!enabled).store(sharedPreferences!!, PREF_DISABLE_NEARBY)
                    true
                } ?: false
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
            if (sharedPreferences == null)
                false
            else
                sharedPreferences?.let {
                    SETTINGS_NEARBY_DISTANCE_PREF.put(it, (value as String).toInt())
                    true
                } ?: false
        }

        markerSizePreference = findPreference("pref_marker_size")
        markerSizePreference?.setOnPreferenceChangeListener { _, value ->
            if (sharedPreferences == null)
                false
            else
                if (sharedPreferences != null && context != null) {
                    SETTINGS_MARKER_SIZE_PREF.put(sharedPreferences!!, value as Int)
                    true
                } else {
                    context?.toast(R.string.toast_error_internal)
                    Timber.e("Context nor sharedPreferences are null")
                    false
                }
        }

        centerMarkerPreference = findPreference("pref_move_marker")
        centerMarkerPreference?.setOnPreferenceChangeListener { _, value ->
            if (sharedPreferences == null)
                false
            else
                if (sharedPreferences != null && context != null) {
                    SETTINGS_CENTER_MARKER_PREF.put(sharedPreferences!!, value as Boolean)
                    true
                } else {
                    context?.toast(R.string.toast_error_internal)
                    Timber.e("Context nor sharedPreferences are null")
                    false
                }
        }

        smallMapPreference = findPreference("pref_small_map_enable")
        smallMapPreference?.setOnPreferenceChangeListener { _, value ->
            if (sharedPreferences == null)
                false
            else
                if (sharedPreferences != null && context != null) {
                    SETTINGS_SMALL_MAP_PREF.put(sharedPreferences!!, value as Boolean)
                    true
                } else {
                    context?.toast(R.string.toast_error_internal)
                    Timber.e("Context nor sharedPreferences are null")
                    false
                }
        }

        previewScalePreference = findPreference("pref_preview_scale")
        previewScalePreference?.setOnPreferenceChangeListener { _, value ->
            if (sharedPreferences == null)
                false
            else
                if (sharedPreferences != null && context != null) {
                    SETTINGS_PREVIEW_SCALE_PREF.put(
                        sharedPreferences!!,
                        (value as Int).toFloat() / 10f
                    )
                    true
                } else {
                    context?.toast(R.string.toast_error_internal)
                    Timber.d("Context nor sharedPreferences are null")
                    false
                }
        }
    }

    override fun onResume() {
        super.onResume()

        sensibilityPreference?.value = SETTINGS_GESTURE_SENSIBILITY_PREF.get(sharedPreferences)

        languagePreference?.setValueIndex(
            SETTINGS_LANGUAGE_PREF.get(sharedPreferences)
        ) ?: Timber.d("Could not set languagePreference default value")

        enableNearby?.isChecked = !(PREF_DISABLE_NEARBY.get(sharedPreferences)) &&
                (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)

        val nearbyDistancePref = getNearbyZonesDistance().toString()
        Timber.d("Nearby distance: $nearbyDistancePref")
        nearbyDistance?.setText(nearbyDistancePref)
            ?: Timber.e("Could not set nearbyDistance default value")

        markerSizePreference?.value = SETTINGS_MARKER_SIZE_PREF.get(sharedPreferences)
        previewScalePreference?.value =
            (SETTINGS_PREVIEW_SCALE_PREF.get(sharedPreferences) * 10).toInt()
        smallMapPreference?.isChecked = SETTINGS_SMALL_MAP_PREF.get(sharedPreferences)
    }
}