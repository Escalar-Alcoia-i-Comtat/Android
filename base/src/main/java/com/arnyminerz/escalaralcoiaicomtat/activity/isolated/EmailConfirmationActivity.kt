package com.arnyminerz.escalaralcoiaicomtat.activity.isolated

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.LoadingActivity
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.shared.CONFIRMATION_EMAIL_DYNAMIC
import com.arnyminerz.escalaralcoiaicomtat.core.shared.CONFIRMATION_EMAIL_URL
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityEmailConfirmationBinding
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@ExperimentalMaterial3Api
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
                user?.reload()?.await()
            } catch (_: FirebaseAuthInvalidUserException) {
                Timber.w("The user has been removed, loading app...")
                PreferencesModule
                    .systemPreferencesRepository
                    .setWaitingForEmailConfirmation(false)
                uiContext {
                    launch(LoadingActivity::class.java)
                }
            }
            if (user == null) {
                Timber.v("There's no logged in user, loading app...")
                PreferencesModule
                    .systemPreferencesRepository
                    .setWaitingForEmailConfirmation(false)
                uiContext {
                    launch(LoadingActivity::class.java)
                }
            } else {
                Timber.v("Checking if verified")
                val verified = user.isEmailVerified
                if (verified) {
                    Timber.v("The user's email has been verified, loading app...")
                    PreferencesModule
                        .systemPreferencesRepository
                        .setWaitingForEmailConfirmation(false)
                    uiContext {
                        launch(LoadingActivity::class.java)
                    }
                }
            }
        }
    }
}
