package com.arnyminerz.escalaralcoiaicomtat.fragment.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentIntroMainBinding

class MainIntroFragment : Fragment() {
    private var _binding: FragmentIntroMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIntroMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.introMainTitleTextView.text = getString(R.string.intro_main_title, getString(R.string.app_name))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
