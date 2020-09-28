package com.arnyminerz.escalaralcoiaicomtat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.data.user.UserData
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.generic.runOnUiThread
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_auth.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

@ExperimentalUnsignedTypes
class AuthFragment : NetworkChangeListenerFragment(), FirebaseAuth.AuthStateListener {
    companion object {
        var user: UserData? = null
            private set
    }

    var profileFragment: ProfileFragment? = null
    var refreshing = false

    fun refresh() {
        if (refreshing) return

        refreshing = true
        Timber.v("Refreshing AuthFragment...")
        if (MainActivity.loggedIn()) {
            Timber.v("User is logged in.")
            GlobalScope.launch {
                try {
                    Timber.v("Getting user...")
                    val user = UserData.fromUID(networkState, MainActivity.user()!!.uid)
                    AuthFragment.user = user

                    try {
                        Timber.d("Refreshed user data for AuthFragment.")
                        Timber.v("Subbing to the user's topic (%s).", user.topic)

                        if (profileFragment == null) {
                            Timber.d("Creating profile fragment...")
                            profileFragment = ProfileFragment.newInstance(user)
                        }

                        user.listenMessages()

                        Timber.d("Subbed correctly")
                        runOnUiThread {
                            try {
                                parentFragmentManager.beginTransaction()
                                    .replace(R.id.auth_frame, profileFragment!!)
                                    .commit()
                                visibility(auth_fragment_loader, false)
                            } catch (ex: IllegalArgumentException) {
                                Timber.e(ex, "Could not show LoginFragment")
                            } finally {
                                refreshing = false
                            }
                        }
                    } catch (error: Exception) {
                        Timber.e(error, "Could not load user nor subscribe")
                    } finally {
                        refreshing = false
                    }
                } catch (error: NoInternetAccessException) {
                    runOnUiThread {
                        toast(context, R.string.toast_error_no_internet)
                        visibility(auth_fragment_loader, false)
                    }
                } finally {
                    refreshing = false
                }
            }
        } else {
            Timber.e("User not logged in, falling to login fragment.")
            runOnUiThread {
                try {
                    parentFragmentManager.beginTransaction()
                        .replace(
                            R.id.auth_frame,
                            LoginFragment(
                                this@AuthFragment
                            )
                        )
                        .commit()
                    visibility(auth_fragment_loader, false)
                } catch (ex: IllegalArgumentException) {
                    Timber.e(ex, "Could not show LoginFragment")
                } finally {
                    refreshing = false
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_auth, container, false)

    override fun onResume() {
        super.onResume()
        if (isResumed)
            refresh()

        MainActivity.auth.addAuthStateListener(this)
    }

    override fun onPause() {
        super.onPause()

        MainActivity.auth.removeAuthStateListener(this)
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        refresh()
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)
        val hasInternet = state.hasInternet

        Timber.v("Network changed! Internet connection? $hasInternet")

        visibility(auth_frame, hasInternet)
        visibility(auth_no_internet_cardView, !hasInternet)
        refresh()
    }
}