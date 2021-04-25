package com.arnyminerz.escalaralcoiaicomtat.activity.isolated

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.LoadingActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityEmailConfirmationBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.PREF_WAITING_EMAIL_CONFIRMATION
import com.arnyminerz.escalaralcoiaicomtat.generic.awaitTask
import com.arnyminerz.escalaralcoiaicomtat.generic.doAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.uiContext
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import timber.log.Timber

class EmailConfirmationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmailConfirmationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Firebase.dynamicLinks
            .getDynamicLink(intent)
            .addOnSuccessListener(this) { pendingDynamicLinkData ->
                // Get deep link from result (may be null if no link is found)
                var deepLink: Uri? = null
                if (pendingDynamicLinkData != null)
                    deepLink = pendingDynamicLinkData.link

                Timber.v("Got deep link: $deepLink")
            }
            .addOnFailureListener(this) { e -> Timber.w(e, "getDynamicLink:onFailure") }
    }

    override fun onResume() {
        super.onResume()

        doAsync {
            Timber.v("Getting currently logged in user...")
            val user = Firebase.auth.currentUser
            try {
                user?.reload()?.awaitTask()
            } catch (_: FirebaseAuthInvalidUserException) {
                uiContext {
                    Timber.w("The user has been removed, loading app...")
                    PREF_WAITING_EMAIL_CONFIRMATION.put(false)
                    startActivity(Intent(this, LoadingActivity::class.java))
                }
            }
            uiContext {
                if (user == null) {
                    Timber.v("There's no logged in user, loading app...")
                    PREF_WAITING_EMAIL_CONFIRMATION.put(false)
                    startActivity(Intent(this, LoadingActivity::class.java))
                } else {
                    Timber.v("Checking if verified")
                    val verified = user.isEmailVerified
                    if (verified) {
                        Timber.v("The user's email has been verified, loading app...")
                        PREF_WAITING_EMAIL_CONFIRMATION.put(false)
                        startActivity(Intent(this, LoadingActivity::class.java))
                    }
                }
            }
        }
    }
}
