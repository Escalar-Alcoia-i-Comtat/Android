package com.arnyminerz.escalaralcoiaicomtat.activity.isolated

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.compose.material3.ExperimentalMaterial3Api
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.LoadingActivity
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ESCALAR_ALCOIA_I_COMTAT_HOSTNAME
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_LINK_PATH
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.google.firebase.auth.ktx.auth
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import timber.log.Timber

@ExperimentalMaterial3Api
class DynamicLinkHandler : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Firebase.dynamicLinks
            .getDynamicLink(intent)
            .addOnSuccessListener(this) { pendingDynamicLinkData ->
                // Get deep link from result (may be null if no link is found)
                val deepLink: Uri? = pendingDynamicLinkData.link

                if (deepLink == null) {
                    finish()
                    return@addOnSuccessListener
                }

                Timber.v("Got deep link: $deepLink. Path: ${deepLink.path}. Host: ${deepLink.host}")

                val mode = deepLink.getQueryParameter("mode")
                val actionCode = deepLink.getQueryParameter("oobCode")
                if (mode.equals("verifyEmail", true) && actionCode != null) {
                    Timber.i("Applying email verification code...")
                    Firebase.auth.applyActionCode(actionCode)
                        .addOnSuccessListener {
                            doAsync {
                                Timber.i("Verified email successfully.")
                                PreferencesModule
                                    .systemPreferencesRepository
                                    .setWaitingForEmailConfirmation(false)

                                uiContext {
                                    launch(LoadingActivity::class.java)
                                }
                            }
                        }
                        .addOnFailureListener {
                            Timber.e(it, "Could not verify account.")
                            toast(R.string.toast_error_confirmation)
                            finish()
                        }
                }

                if (ESCALAR_ALCOIA_I_COMTAT_HOSTNAME == deepLink.host)
                    launch(LoadingActivity::class.java) {
                        putExtra(EXTRA_LINK_PATH, deepLink.toString())
                    }
            }
            .addOnFailureListener(this) { e ->
                Timber.w(e, "getDynamicLink:onFailure")
                finish()
            }
    }
}
