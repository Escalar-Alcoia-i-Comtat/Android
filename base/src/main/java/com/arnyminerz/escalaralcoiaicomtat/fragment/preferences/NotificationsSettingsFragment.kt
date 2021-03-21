package com.arnyminerz.escalaralcoiaicomtat.fragment.preferences

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.EXTRA_APP_PACKAGE
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.shared.sharedPreferences

class NotificationsSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_noti, rootKey)

        val alertEnableSwitch: SwitchPreference? = findPreference("pref_alert_enable")
        alertEnableSwitch?.isChecked =
            SETTINGS_ALERT_PREF.get(sharedPreferences)
        alertEnableSwitch?.setOnPreferenceClickListener { p ->
            val pref = p as SwitchPreference
            SETTINGS_ALERT_PREF.put(sharedPreferences, pref.isChecked)

            true
        }

        val deviceNotificationsSettings: Preference? = findPreference("pref_notif_settings")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            deviceNotificationsSettings?.setOnPreferenceClickListener {
                startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(
                        EXTRA_APP_PACKAGE,
                        context?.packageName
                    )
                )

                true
            }
        else
            deviceNotificationsSettings?.isVisible = false
    }
}
