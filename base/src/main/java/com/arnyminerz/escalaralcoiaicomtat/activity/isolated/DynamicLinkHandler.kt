package com.arnyminerz.escalaralcoiaicomtat.activity.isolated

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.LoadingActivity
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.PREF_WAITING_EMAIL_CONFIRMATION
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.google.firebase.auth.ktx.auth
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import timber.log.Timber

class DynamicLinkHandler : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Firebase.dynamicLinks
            .getDynamicLink(intent)
            .addOnSuccessListener(this) { pendingDynamicLinkData ->
                // Get deep link from result (may be null if no link is found)
                var deepLink: Uri? = null
                if (pendingDynamicLinkData != null)
                    deepLink = pendingDynamicLinkData.link

                if (deepLink == null) {
                    finish()
                    return@addOnSuccessListener
                }

                Timber.v("Got deep link: $deepLink. Path: ${deepLink.path}")

                val mode = deepLink.getQueryParameter("mode")
                val actionCode = deepLink.getQueryParameter("oobCode")
                if (mode.equals("verifyEmail", true) && actionCode != null)
                    Firebase.auth.applyActionCode(actionCode)
                        .addOnSuccessListener {
                            PREF_WAITING_EMAIL_CONFIRMATION.put(false)
                            startActivity(Intent(this, LoadingActivity::class.java))
                        }
                        .addOnFailureListener {
                            Timber.e(it, "Could not verify account.")
                            toast(R.string.toast_error_confirmation)
                            finish()
                        }
            }
            .addOnFailureListener(this) { e ->
                Timber.w(e, "getDynamicLink:onFailure")
                finish()
            }
    }
}
