package com.arnyminerz.escalaralcoiaicomtat.activity.isolated

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.LoadingActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityEmailConfirmationBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.PREF_WAITING_EMAIL_CONFIRMATION
import com.arnyminerz.escalaralcoiaicomtat.generic.awaitTask
import com.arnyminerz.escalaralcoiaicomtat.generic.doAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.generic.uiContext
import com.arnyminerz.escalaralcoiaicomtat.shared.CONFIRMATION_EMAIL_DYNAMIC
import com.arnyminerz.escalaralcoiaicomtat.shared.CONFIRMATION_EMAIL_URL
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import timber.log.Timber

class EmailConfirmationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmailConfirmationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.resendConfirmationMailButton.setOnClickListener {
            binding.resendConfirmationMailButton.isEnabled = false
            val user = Firebase.auth.currentUser
            user?.sendEmailVerification(
                ActionCodeSettings.newBuilder()
                    .setUrl(CONFIRMATION_EMAIL_URL)
                    .setDynamicLinkDomain(CONFIRMATION_EMAIL_DYNAMIC)
                    .setHandleCodeInApp(true)
                    .setAndroidPackageName(BuildConfig.APPLICATION_ID, true, null)
                    .build()
            )
                ?.addOnSuccessListener {
                    toast(R.string.toast_confirmation_sent)
                }
                ?.addOnFailureListener {
                    Timber.e(it, "Could not send verification mail")
                    toast(R.string.toast_error_confirmation_send)
                    binding.resendConfirmationMailButton.isEnabled = true
                }
        }
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
