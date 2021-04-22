package com.arnyminerz.escalaralcoiaicomtat.fragment.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentLoginBinding

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

    companion object {
        fun newInstance(): LoginFragment = LoginFragment()
    }
}
