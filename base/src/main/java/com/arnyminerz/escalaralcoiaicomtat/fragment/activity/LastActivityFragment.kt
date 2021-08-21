package com.arnyminerz.escalaralcoiaicomtat.fragment.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.UserActivity
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ARGUMENT_ACTIVITY
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentLastActivityBinding

/**
 * Displays to the user the last activity the user has done.
 * @author Arnau Mora
 * @since 20210821
 */
class LastActivityFragment private constructor() : Fragment() {
    companion object {
        /**
         * Creates a new instance of the [LastActivityFragment] with the specified arguments.
         * @author Arnau Mora
         * @since 20210821
         * @param userActivity The last activity the user has performed.
         */
        fun newInstance(userActivity: UserActivity): LastActivityFragment =
            LastActivityFragment().apply {
                val args = arguments ?: return@apply
                args.putParcelable(ARGUMENT_ACTIVITY, userActivity)
                arguments = args
            }
    }

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