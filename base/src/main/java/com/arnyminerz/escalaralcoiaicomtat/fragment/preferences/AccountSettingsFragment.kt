package com.arnyminerz.escalaralcoiaicomtat.fragment.preferences

import android.os.Bundle
import androidx.preference.SwitchPreference
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.data.user.UserData
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerPreferenceFragment
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.disable
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.enable
import com.arnyminerz.escalaralcoiaicomtat.generic.runOnUiThread
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import timber.log.Timber

@ExperimentalUnsignedTypes
class AccountSettingsFragment : NetworkChangeListenerPreferenceFragment() {

    private var profileImagePublicPreference: SwitchPreference? = null
    private var completedPathsPublicPreference: SwitchPreference? = null
    private var friendsPublicPreference: SwitchPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_account, rootKey)

        profileImagePublicPreference = findPreference("profile_image_public")
        completedPathsPublicPreference = findPreference("completed_paths_public")
        friendsPublicPreference = findPreference("friends_public")

        disable(
            profileImagePublicPreference,
            completedPathsPublicPreference,
            friendsPublicPreference
        )

        onResume()
    }

    override fun onResume() {
        super.onResume()

        if (MainActivity.loggedIn())
            GlobalScope.launch {
                val user = UserData.fromUID(networkState, MainActivity.user()!!.uid)
                val preferences = user.preferences(networkState)

                runOnUiThread {
                    enable(
                        profileImagePublicPreference,
                        completedPathsPublicPreference,
                        friendsPublicPreference
                    )

                    profileImagePublicPreference?.isChecked = preferences.profileImagePublic
                    completedPathsPublicPreference?.isChecked = preferences.completedPathsPublic
                    friendsPublicPreference?.isChecked = preferences.friendsPublic

                    profileImagePublicPreference?.setOnPreferenceChangeListener { pref, v ->
                        val value = v as Boolean
                        disable(profileImagePublicPreference)

                        GlobalScope.launch {
                            try {
                                preferences.updateCompletedPublic(networkState, value)
                                runOnUiThread {
                                    (pref as SwitchPreference).isChecked = value
                                    enable(profileImagePublicPreference)
                                }
                            } catch (error: Exception) {
                                enable(profileImagePublicPreference)
                                Timber.e(error, "Could not change.")
                                if (error is NoInternetAccessException)
                                    context?.toast(R.string.toast_error_no_internet)
                                else
                                    context?.toast(R.string.toast_error_internal)
                            }
                        }

                        false
                    }

                    completedPathsPublicPreference?.setOnPreferenceChangeListener { pref, v ->
                        val value = v as Boolean
                        disable(completedPathsPublicPreference)

                        GlobalScope.launch {
                            try {
                                preferences.updateCompletedPublic(networkState, value)
                                runOnUiThread {
                                    (pref as SwitchPreference).isChecked = value
                                    enable(completedPathsPublicPreference)
                                }
                            } catch (error: Exception) {
                                enable(completedPathsPublicPreference)
                                Timber.e(error, "Could not change.")
                                if (error is NoInternetAccessException)
                                    context?.toast(R.string.toast_error_no_internet)
                                else
                                    context?.toast(R.string.toast_error_internal)
                            }
                        }

                        false
                    }

                    friendsPublicPreference?.setOnPreferenceChangeListener { pref, v ->
                        val value = v as Boolean
                        disable(friendsPublicPreference)

                        GlobalScope.launch {
                            try {
                                preferences.updateCompletedPublic(networkState, value)
                                runOnUiThread {
                                    (pref as SwitchPreference).isChecked = value
                                    enable(friendsPublicPreference)
                                }
                            } catch (error: Exception) {
                                enable(friendsPublicPreference)
                                Timber.e(error, "Could not change.")
                                if (error is NoInternetAccessException)
                                    context?.toast(R.string.toast_error_no_internet)
                                else
                                    context?.toast(R.string.toast_error_internal)
                            }
                        }

                        false
                    }
                }
            }
        else
            Timber.e("User not logged in!")
    }
}