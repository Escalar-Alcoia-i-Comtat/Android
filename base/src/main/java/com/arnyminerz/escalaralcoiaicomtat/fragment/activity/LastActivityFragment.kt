package com.arnyminerz.escalaralcoiaicomtat.fragment.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentLastActivityBinding

/**
 * Displays to the user the last activity the user has done.
 * @author Arnau Mora
 * @since 20210821
 */
class LastActivityFragment : Fragment() {
    /**
     * Stores the View Binding for updating the UI.
     * @author Arnau Mora
     * @since 20210821
     */
    private var binding: FragmentLastActivityBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLastActivityBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}