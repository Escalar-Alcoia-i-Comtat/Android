package com.arnyminerz.escalaralcoiaicomtat.fragment.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.profile.AuthActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentAuthLoginBinding
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.list.viewListOf
import com.arnyminerz.escalaralcoiaicomtat.shared.LOGGED_IN_REQUEST_CODE
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseError
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import timber.log.Timber

class LoginFragment private constructor() : Fragment() {
    private var _binding: FragmentAuthLoginBinding? = null

    private val binding: FragmentAuthLoginBinding = _binding!!

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
            val fields = viewListOf(
                binding.emailEditText,
                binding.passwordEditText,
                binding.loginButton,
                binding.registerButton
            )
            fields.clearFocus()
            fields.disable()

            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            Firebase.auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { _ ->
                    requireActivity().finishActivity(LOGGED_IN_REQUEST_CODE)
                }
                .addOnFailureListener { exception ->
                    Timber.w(exception, "Could not login.")
                    val e = exception as FirebaseAuthException
                    when (e.errorCode.toInt()) {
                        FirebaseError.ERROR_USER_NOT_FOUND ->
                            showError(
                                binding.emailTextField,
                                R.string.login_error_user_not_found
                            )
                        FirebaseError.ERROR_INVALID_CREDENTIAL ->
                            showError(
                                binding.passwordTextField,
                                R.string.login_error_invalid_credentials
                            )
                        FirebaseError.ERROR_USER_DISABLED ->
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
