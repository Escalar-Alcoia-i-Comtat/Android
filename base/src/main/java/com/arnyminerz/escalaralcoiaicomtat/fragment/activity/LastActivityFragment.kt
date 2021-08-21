package com.arnyminerz.escalaralcoiaicomtat.fragment.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.UserActivity
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ARGUMENT_ACTIVITY
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentLastActivityBinding
import timber.log.Timber

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

    /**
     * Stores the [UserActivity] that should be displayed to the user.
     * @author Arnau Mora
     * @since 20210821
     */
    private lateinit var userActivity: UserActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLastActivityBinding.inflate(inflater, container, false)
        return binding?.root
    }

    /**
     * This should be called internally when there's a problem loading the fragment. It notifies
     * that there has been an error while initializing the fragment's data, and destroys the
     * fragment.
     * @author Arnau Mora
     * @since 20210821
     * @param msg The message to log
     * @param toastMsg If not null, the message to toast to the user.
     */
    @UiThread
    private fun initException(msg: String, @StringRes toastMsg: Int? = null) {
        Timber.e(msg)
        toastMsg?.let { toast(it) }
        parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
    }

    /**
     * Loads the user activity from the fragment's arguments, and stores it into [userActivity].
     * @author Arnau Mora
     * @since 20210821
     */
    @UiThread
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args =
            arguments ?: return initException("Arguments is null", R.string.toast_error_internal)
        val activity = args.getParcelable<UserActivity>(ARGUMENT_ACTIVITY)
            ?: return initException(
                "Activity not found in arguments",
                R.string.toast_error_internal
            )
        userActivity = activity

        Timber.i("Loaded user activity successfully.")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}