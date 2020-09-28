package com.arnyminerz.escalaralcoiaicomtat.fragment

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.async.FirebaseException
import com.arnyminerz.escalaralcoiaicomtat.auth.firebaseAuthWithGoogle
import com.arnyminerz.escalaralcoiaicomtat.data.user.UserData
import com.arnyminerz.escalaralcoiaicomtat.device.vibrate
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.disable
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.enable
import com.arnyminerz.escalaralcoiaicomtat.generic.runOnUiThread
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_login.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.jetbrains.anko.toast
import timber.log.Timber
import java.io.FileNotFoundException

@ExperimentalUnsignedTypes
class LoginFragment(private val authFragment: AuthFragment) : Fragment() {
    companion object {
        const val RC_SIGN_IN = 5
    }

    private suspend fun login(
        email: String,
        password: String
    ): AuthResult = MainActivity.auth.signInWithEmailAndPassword(email, password).await()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_login, container, false).apply {
            fun exceptionHandler(error: Exception?) {
                arrayListOf(
                    emailEditText,
                    passwordEditText,
                    passwordConfirmEditText,
                    login_button
                ).enable()
                Timber.e(error, "Could not register.")

                if ((error as? FirebaseException)?.code == "auth/user-not-found") {
                    visibility(passwordConfirmEditText, true)
                    context?.toast(R.string.toast_login_confirm_password)
                    return
                }

                when (error) {
                    is FirebaseAuthInvalidUserException -> {
                        // Email doesn't exist
                        requireActivity().toast(R.string.toast_login_confirm_password)
                    }
                    is FirebaseAuthInvalidCredentialsException -> {
                        // Password is wrong
                        requireActivity().toast(R.string.toast_login_password_wrong)
                    }
                    else -> {
                        Timber.e(error, "Could not register. Error: ")
                    }
                }
            }

            login_button.setOnClickListener {
                arrayListOf(
                    emailEditText,
                    passwordEditText,
                    passwordConfirmEditText,
                    login_button
                ).disable()
                if (emailEditText.text != null && emailEditText.text!!.isNotEmpty() &&
                    passwordEditText.text != null && passwordEditText.text!!.isNotEmpty()
                )
                    if (visibility(passwordConfirmTextInput)) {
                        if (passwordConfirmEditText.text != null && passwordConfirmEditText.text!!.isNotEmpty()) {
                            if (passwordConfirmEditText.text!!.toString() == passwordEditText.text!!.toString())
                                GlobalScope.launch {
                                    try {
                                        UserData.login(
                                            authFragment.networkState,
                                            emailEditText.text!!.toString(),
                                            passwordEditText.text!!.toString(),
                                            true
                                        )
                                        try {
                                            login(
                                                emailEditText.text.toString(),
                                                passwordEditText.text.toString()
                                            )
                                            arrayListOf(
                                                emailEditText,
                                                passwordEditText,
                                                passwordConfirmEditText,
                                                login_button
                                            ).enable(context)
                                            authFragment.refresh()
                                        } catch (error: Exception) {
                                            arrayListOf(
                                                emailEditText,
                                                passwordEditText,
                                                passwordConfirmEditText,
                                                login_button
                                            ).enable(context)
                                            when (error) {
                                                is FirebaseAuthInvalidUserException -> {
                                                    // Email doesn't exist
                                                    requireActivity().toast(R.string.toast_login_confirm_password)
                                                }
                                                is FirebaseAuthInvalidCredentialsException -> {
                                                    // Password is wrong
                                                    requireActivity().toast(R.string.toast_login_password_wrong)
                                                }
                                                else -> {
                                                    Timber.e(
                                                        error,
                                                        "Could not login. Error: "
                                                    )
                                                }
                                            }
                                        }
                                    } catch (error: Exception) {
                                        exceptionHandler(error)
                                    }
                                }
                            else {
                                arrayListOf(
                                    emailEditText,
                                    passwordEditText,
                                    passwordConfirmEditText,
                                    login_button
                                ).enable()
                                requireActivity().toast(R.string.toast_login_password_match)
                                vibrate(requireContext(), 50)
                            }
                        } else {
                            arrayListOf(
                                emailEditText,
                                passwordEditText,
                                passwordConfirmEditText,
                                login_button
                            ).enable()
                            requireActivity().toast(R.string.toast_login_confirm_password)
                            vibrate(requireContext(), 50)
                        }
                    } else GlobalScope.launch {
                        try {
                            UserData.login(
                                authFragment.networkState,
                                emailEditText.text!!.toString(),
                                passwordEditText.text!!.toString()
                            )
                            try {
                                login(
                                    emailEditText.text.toString(),
                                    passwordEditText.text.toString()
                                )

                                arrayListOf(
                                    emailEditText,
                                    passwordEditText,
                                    passwordConfirmEditText,
                                    login_button
                                ).enable(context)
                                authFragment.refresh()
                            } catch (error: Exception) {
                                arrayListOf(
                                    emailEditText,
                                    passwordEditText,
                                    passwordConfirmEditText,
                                    login_button
                                ).enable(context)
                                when (error) {
                                    is FirebaseAuthInvalidUserException ->
                                        // Email doesn't exist
                                        toast(context, R.string.toast_login_confirm_password)
                                    is FirebaseAuthInvalidCredentialsException ->
                                        // Password is wrong
                                        toast(context, R.string.toast_login_password_wrong)
                                    is FileNotFoundException ->
                                        // There's an error on the server
                                        toast(context, R.string.toast_login_server_error)
                                    else -> {
                                        Timber.e(error, "Could not login. Error: ")
                                        toast(context, R.string.toast_error_internal)
                                    }
                                }
                            }
                        } catch (error: Exception) {
                            runOnUiThread {
                                exceptionHandler(error)
                            }
                        }
                    }
                else {
                    vibrate(requireContext(), 50)
                    arrayListOf(
                        emailEditText,
                        passwordEditText,
                        passwordConfirmEditText,
                        login_button
                    ).enable()
                }
            }

            terms_textView.movementMethod = LinkMovementMethod.getInstance()

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.auth_client_id))
                .requestEmail()
                .build()

            google_login_button.setOnClickListener {
                val googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(
                    signInIntent,
                    RC_SIGN_IN
                )
            }
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            Timber.d("Got Google SignIn result.")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Timber.d("Google Firebase Auth: %s", account.id)
                firebaseAuthWithGoogle(account.idToken) { user ->
                    Timber.d("Authenticated with Firebase with Google Service. User: %s", user)
                    if (user != null)
                        authFragment.refresh()
                    else Timber.e("User is null")
                }
            } catch (ex: ApiException) {
                Timber.e(ex, "Could not get sign in result")
                authFragment.refresh()
            }
        } else
            Timber.w(
                "Got activity result without handler. Request Code: %s. Result Code: %s",
                requestCode,
                resultCode
            )
    }

    override fun onResume() {
        super.onResume()
        arrayListOf(emailEditText, passwordEditText, passwordConfirmEditText, login_button).enable()
    }
}