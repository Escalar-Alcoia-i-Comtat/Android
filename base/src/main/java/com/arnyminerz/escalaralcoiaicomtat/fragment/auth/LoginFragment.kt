package com.arnyminerz.escalaralcoiaicomtat.fragment.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.profile.AuthActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentAuthLoginBinding
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.isEmail
import com.arnyminerz.escalaralcoiaicomtat.generic.finishActivityWithResult
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.intent.GoogleLoginRequestContract
import com.arnyminerz.escalaralcoiaicomtat.list.viewListOf
import com.arnyminerz.escalaralcoiaicomtat.shared.RESULT_CODE_LOGGED_IN
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import timber.log.Timber

class LoginFragment private constructor() : Fragment() {
    private lateinit var auth: FirebaseAuth

    private var _binding: FragmentAuthLoginBinding? = null

    private val binding: FragmentAuthLoginBinding
        get() = _binding!!

    private val fields
        get() = viewListOf(
            binding.emailEditText,
            binding.passwordEditText,
            binding.loginButton,
            binding.registerButton,
            binding.googleButton
        )

    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleLoginRequest =
        registerForActivityResult(GoogleLoginRequestContract()) { intent ->
            GoogleSignIn.getSignedInAccountFromIntent(intent)
                .addOnSuccessListener { account ->
                    Timber.d("firebaseAuthWithGoogle:${account.id}")
                    firebaseAuthWithGoogle(account.idToken!!)
                }
                .addOnFailureListener { e ->
                    // Google Sign In failed, update UI appropriately
                    Timber.e(e, "Google sign in failed")
                    toast(R.string.toast_error_login_google)
                    fields.enable()
                }
        }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener(requireActivity(), loginSuccessListener)
            .addOnFailureListener {
                fields.enable()
                Timber.e(it, "Could not login with Google.")
                toast(R.string.toast_error_login_google)
            }
    }

    private val loginSuccessListener: OnSuccessListener<AuthResult> =
        OnSuccessListener { authResult ->
            val user = authResult.user ?: return@OnSuccessListener Timber.e("Auth result is null")
            val am = AccountManager.get(requireContext())
            val account = Account(user.email, BuildConfig.APPLICATION_ID)
            user.getIdToken(false)
                .addOnSuccessListener { tokenResult ->
                    val token = tokenResult.token
                    val accountAdded =
                        am.addAccountExplicitly(account, token, Bundle().apply {
                            putString("profileImage", user.photoUrl.toString())
                            putString("displayName", user.displayName)
                        })
                    if (accountAdded)
                        am.notifyAccountAuthenticated(account)
                    else {
                        toast(R.string.toast_error_account_manager_store)
                        Timber.e("Could not store the account's data in AccountManager")
                    }
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Could not get user's token.")
                }
                .addOnCompleteListener {
                    activity.finishActivityWithResult(RESULT_CODE_LOGGED_IN, null)
                }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = Firebase.auth

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_auth_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        binding.emailEditText.setOnEditorActionListener { _, _, _ ->
            binding.passwordEditText.requestFocus()

            true
        }
        binding.emailEditText.setOnFocusChangeListener { _, focused ->
            if (focused) {
                binding.emailTextField.error = null
                binding.emailTextField.isErrorEnabled = false
            }
        }

        binding.passwordEditText.setOnEditorActionListener { _, _, _ ->
            binding.loginButton.performClick()

            true
        }
        binding.passwordEditText.setOnFocusChangeListener { _, focused ->
            if (focused) {
                binding.passwordTextField.error = null
                binding.passwordTextField.isErrorEnabled = false
            }
        }

        binding.registerButton.setOnClickListener {
            (activity as? AuthActivity?)?.changePage(AuthActivity.PAGE_REGISTER)
        }
        binding.loginButton.setOnClickListener {
            fields.clearFocus()
            fields.disable()

            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (email.isBlank()) {
                showError(
                    binding.emailTextField,
                    R.string.login_error_email_required
                )
            } else if (!email.isEmail()) {
                showError(
                    binding.emailTextField,
                    R.string.login_error_email_invalid
                )
            } else if (password.isBlank()) {
                showError(
                    binding.passwordTextField,
                    R.string.login_error_password_required
                )
            } else
                try {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener(loginSuccessListener)
                        .addOnFailureListener { exception ->
                            Timber.w(exception, "Could not login.")
                            val e = exception as FirebaseAuthException
                            when (e.errorCode) {
                                "ERROR_USER_NOT_FOUND" ->
                                    showError(
                                        binding.emailTextField,
                                        R.string.login_error_user_not_found
                                    )
                                "ERROR_INVALID_CREDENTIAL" ->
                                    showError(
                                        binding.passwordTextField,
                                        R.string.login_error_invalid_credentials
                                    )
                                "ERROR_USER_DISABLED" ->
                                    showError(
                                        binding.emailTextField,
                                        R.string.login_error_user_disabled
                                    )
                                else -> toast(context, R.string.toast_error_internal)
                            }
                        }
                        .addOnCompleteListener {
                            fields.enable()
                        }
                } catch (_: IllegalArgumentException) {
                    showError(
                        binding.emailTextField,
                        R.string.login_error_email_invalid
                    )
                }
        }
        binding.googleButton.setOnClickListener {
            fields.clearFocus()
            fields.disable()

            googleLoginRequest.launch(googleSignInClient)
        }
    }

    /**
     * Shows an error to the user through a text field.
     * @author Arnau Mora
     * @since 20210425
     * @param field The field to update
     * @param error The string resource of the message to show
     */
    private fun showError(field: TextInputLayout, @StringRes error: Int) {
        field.isErrorEnabled = true
        field.error = getString(error)
        fields.enable()
    }

    companion object {
        /**
         * Initializes a new instance of the [LoginFragment].
         * @author Arnau Mora
         * @since 20210425
         */
        fun newInstance(): LoginFragment = LoginFragment()
    }
}
