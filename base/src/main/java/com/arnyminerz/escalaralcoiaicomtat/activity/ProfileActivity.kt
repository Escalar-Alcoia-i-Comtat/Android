package com.arnyminerz.escalaralcoiaicomtat.activity

import android.os.Bundle
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity.Companion.user
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerFragmentActivity
import com.arnyminerz.escalaralcoiaicomtat.data.user.UserData
import com.arnyminerz.escalaralcoiaicomtat.exception.UserNotFoundException
import com.arnyminerz.escalaralcoiaicomtat.fragment.ProfileFragment
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import timber.log.Timber


@ExperimentalUnsignedTypes
class ProfileActivity : NetworkChangeListenerFragmentActivity() {
    companion object {
        const val BUNDLE_EXTRA_USER_UID = "UserUid"
        const val BUNDLE_EXTRA_ADDING_FRIEND = "AddingFriend"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        if (intent == null || user() == null) {
            Timber.e("Intent nor user is null.")
            onBackPressed()
            return
        }
        val userUid = intent.getStringExtra(BUNDLE_EXTRA_USER_UID)
        if (userUid == null) {
            Timber.e("User UID not set")
            onBackPressed()
            return
        }

        back_fab.setOnClickListener {
            onBackPressed()
        }

        /*val addingFriend = intent.getBooleanExtra(BUNDLE_EXTRA_ADDING_FRIEND, false)
        if(addingFriend)
            LoadingDialog.show(this)*/

        GlobalScope.launch {
            try {
                Timber.v("Loading profile from ProfileActivity. UID: $userUid")
                val user = UserData.fromUID(networkState, userUid)

                Timber.v("Got user data.")
                runOnUiThread {
                    try {
                        supportFragmentManager.beginTransaction().apply {
                            replace(
                                R.id.fragment_container,
                                ProfileFragment.newInstance(user)
                            )
                            commit()
                        }
                    } catch (ex: IllegalArgumentException) {
                        Timber.e(ex, "Could not show LoginFragment")
                    }
                }
            } catch (error: java.lang.Exception) {
                if (error is UserNotFoundException) {
                    Timber.e(error)
                    toast(R.string.toast_error_user_not_found)
                } else
                    Timber.e(error, "Could not get user data")
                onBackPressed()
            }
        }
    }
}