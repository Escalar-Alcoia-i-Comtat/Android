package com.arnyminerz.escalaralcoiaicomtat.fragment.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arnyminerz.escalaralcoiaicomtat.R
import kotlinx.android.synthetic.main.fragment_intro_main.view.*

class MainIntroFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_intro_main, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.intro_main_title_textView.text = getString(R.string.intro_main_title, getString(R.string.app_name))
    }
}