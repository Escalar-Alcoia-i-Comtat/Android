package com.arnyminerz.escalaralcoiaicomtat.fragment.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.profile.UserActivityActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.auth.User
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.storage.MarkedDataInt
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.storage.MarkedProjectData
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ARGUMENT_USER_ID
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.hide
import com.arnyminerz.escalaralcoiaicomtat.core.view.show
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
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
         * @param userId The id of the user to fetch the last activity from.
         */
        fun newInstance(userId: String): LastActivityFragment =
            LastActivityFragment().apply {
                val args = arguments ?: Bundle()
                args.putString(ARGUMENT_USER_ID, userId)
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
     * Stores the id of the user to display the last activity from.
     * @author Arnau Mora
     * @since 20210821
     */
    private lateinit var userId: String

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
        userId = args.getString(ARGUMENT_USER_ID)
            ?: return initException(
                "User ID not found in arguments",
                R.string.toast_error_internal
            )

        Timber.i("Loaded user activity successfully.")
    }

    /**
     * Updates the UI according to [userId].
     * @author Arnau Mora
     * @since 20210821
     */
    @UiThread
    override fun onStart() {
        super.onStart()

        if (binding == null)
            return Timber.e("Binding is null, won't update UI.")
        if (!this::userId.isInitialized)
            return Timber.e("userId not initialized.")

        binding!!.lastActivityLayout.hide()
        binding!!.noActivitiesTextView.hide()
        binding!!.lastActivityProgressIndicator.visibility(true)

        // Load the last activity
        doAsync {
            Timber.v("Getting data of user $userId...")
            val user = User(userId)
            val completedPaths = user.getCompletedPaths()
            Timber.v("Got ${completedPaths.size} completed paths.")

            if (completedPaths.isEmpty())
                return@doAsync uiContext {
                    if (binding == null)
                        Timber.e("Binding is null, won't update UI.")
                    else {
                        Timber.v("Showing no activities text view.")
                        binding!!.lastActivityProgressIndicator.visibility(false)
                        binding!!.noActivitiesTextView.show()
                    }
                }

            val completedPath = completedPaths[0]
            Timber.v("Getting completed path...")
            val zone = completedPath.getZone()
            var completions = 0
            var projects = 0
            val activityCompletions = arrayListOf<MarkedDataInt>()
                .apply {
                    for (completion in completedPaths) {
                        val zoneReference = completion.zoneReference()
                        val zonePath = zoneReference.path
                        if (// Zone is the desired one
                            zone.documentPath == zonePath &&
                            // And path is not the selected one
                            completion.documentPath != completedPath.documentPath
                        ) {
                            add(completion)
                            if (completion is MarkedProjectData)
                                projects++
                            else
                                completions++
                        }
                    }
                }
            Timber.v("Processing completions that match zone...")
            uiContext {
                if (binding == null)
                    Timber.e("Binding is null, won't update UI.")
                else {
                    binding!!.lastActivityPlaceTextView.text = zone.displayName
                    binding!!.lastActivitySummaryTextView.text = resources.getString(
                        R.string.profile_last_activity_summary,
                        completions, projects
                    )
                    binding!!.viewActivityButton.setOnClickListener {
                        UserActivityActivity.intent(
                            requireContext(),
                            activityCompletions,
                            completions,
                            projects
                        )
                    }

                    binding!!.lastActivityProgressIndicator.visibility(false)
                    binding!!.lastActivityLayout.show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}