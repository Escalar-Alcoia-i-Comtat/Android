package com.arnyminerz.escalaralcoiaicomtat.fragment.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentLoginBinding
import com.arnyminerz.escalaralcoiaicomtat.list.viewListOf

class LoginFragment private constructor() : Fragment() {
    private var _binding: FragmentLoginBinding? = null

    private val binding: FragmentLoginBinding = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
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

        binding.passwordEditText.setOnEditorActionListener { _, _, _ ->
            binding.loginButton.performClick()

            true
        }

        binding.loginButton.setOnClickListener {
            val fields = viewListOf(binding.emailEditText, binding.passwordEditText)
            fields.clearFocus()
        }
    }

    companion object {
        fun newInstance(): LoginFragment = LoginFragment()
    }
}
