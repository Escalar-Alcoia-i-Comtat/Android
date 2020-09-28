package com.arnyminerz.escalaralcoiaicomtat.fragment.preferences

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.generic.shareString
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.service.FIREBASE_MESSAGING_TOKEN
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth


class InfoSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_info, rootKey)

        val versionPreference: Preference? = findPreference("pref_version")
        val buildPreference: Preference? = findPreference("pref_build")
        val messagingTokenPreference: Preference? = findPreference("pref_mess_token")
        val messagingTopicPreference: Preference? = findPreference("pref_mess_topic")

        val versionCode = BuildConfig.VERSION_CODE
        val versionName = BuildConfig.VERSION_NAME

        versionPreference?.summary = versionName
        buildPreference?.summary = versionCode.toString()

        messagingTokenPreference?.setOnPreferenceClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pref_info_messaging_token_title)
                .setMessage(FIREBASE_MESSAGING_TOKEN)
                .setNeutralButton(R.string.action_share) { dialog, _ ->
                    FIREBASE_MESSAGING_TOKEN?.let {
                        activity?.shareString(it)
                    }
                    dialog.dismiss()
                }
                .setPositiveButton(R.string.action_copy) { dialog, _ ->
                    val clipboard =
                        requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val data =
                        ClipData.newPlainText("firebase messaging token", FIREBASE_MESSAGING_TOKEN)
                    clipboard.setPrimaryClip(data)
                    toast(context, R.string.toast_copied)

                    dialog.dismiss()
                }
                .show()

            true
        }
        messagingTopicPreference?.setOnPreferenceClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null)
                toast(context, R.string.toast_error_not_logged_in)
            else
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.pref_info_messaging_topic_title)
                    .setMessage(user.uid)
                    .setNeutralButton(R.string.action_share) { dialog, _ ->
                        activity?.shareString(user.uid)
                        dialog.dismiss()
                    }
                    .setPositiveButton(R.string.action_copy) { dialog, _ ->
                        val clipboard =
                            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val data =
                            ClipData.newPlainText(
                                "firebase messaging topic",
                                user.uid
                            )
                        clipboard.setPrimaryClip(data)
                        toast(context, R.string.toast_copied)

                        dialog.dismiss()
                    }
                    .show()

            true
        }
    }
}