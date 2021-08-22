package com.arnyminerz.escalaralcoiaicomtat.activity.profile

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.auth.setProfileImage
import com.arnyminerz.escalaralcoiaicomtat.core.data.auth.User
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_USER_UID
import com.arnyminerz.escalaralcoiaicomtat.core.shared.HUNDRED
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getBitmapFromUri
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityProfileBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.activity.LastActivityFragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageException
import timber.log.Timber

/**
 * Shows a select user's personal data, such as username, profile image, and latest completions and posts.
 *
 * Must be launched once the user has been logged in, or using the [Intent]'s extra [EXTRA_USER_UID].
 * @author Arnau Mora
 * @since 20210719
 * @see EXTRA_USER_UID
 */
class ProfileActivity : AppCompatActivity() {
    companion object {
        var hasProfileImageBeenChanged: Boolean = false
    }

    private lateinit var binding: ActivityProfileBinding

    private lateinit var firestore: FirebaseFirestore

    private val progressIndicator
        get() = binding.profileProgressIndicator

    private val openProfileImageRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val resultIntent = result.data
            resultIntent?.data?.let { uri ->
                val bitmap = getBitmapFromUri(contentResolver, uri)
                if (bitmap != null) {
                    binding.profileImageImageView.background = bitmap.toDrawable(resources)

                    updateProfileImage(bitmap)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uid = intent.getExtra(EXTRA_USER_UID)
        var loggedIn = false
        val userUid = if (uid != null)
            uid
        else {
            val authUser = Firebase.auth.currentUser
            if (authUser == null) {
                Timber.w("Not logged in.")
                onBackPressed()
                return
            }
            loggedIn = true
            authUser.uid
        }
        val user = User(userUid)

        firestore = Firebase.firestore

        binding.backFab.setOnClickListener { onBackPressed() }

        progressIndicator.visibility(true)

        supportFragmentManager
            .beginTransaction()
            .add(R.id.last_activity_layout, LastActivityFragment.newInstance(userUid))
            .commit()

        doAsync {
            try {
                val visibleUserData = user.getVisibleUserData()
                uiContext {
                    binding.profileNameTextView.text = visibleUserData.displayName
                }

                val profileImage = visibleUserData.profileImage()
                uiContext {
                    binding.profileImageImageView.background = profileImage.toDrawable(resources)
                }
            } catch (e: StorageException) {
                when (e.errorCode) {
                    StorageException.ERROR_OBJECT_NOT_FOUND -> uiContext {
                        binding.profileImageImageView.setBackgroundResource(R.drawable.ic_profile_image)
                    }
                    else -> Timber.e(e, "Could not load profile image")
                }
            } catch (e: FirebaseFunctionsException) {
                uiContext { toast(R.string.toast_error_internal) }
            } finally {
                uiContext { progressIndicator.visibility(false) }
            }
        }

        if (loggedIn)
            binding.profileImageImageView.setOnClickListener {
                openProfileImageRequest.launch(
                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "image/*"
                    }
                )
            }
        else binding.profileImageChangeIndicator.visibility(false)
    }

    /**
     * Sets the profile image of the user to a new one.
     * @author Arnau Mora
     * @since 20210425
     */
    private fun updateProfileImage(bitmap: Bitmap) {
        val user = Firebase.auth.currentUser
        if (user != null) {
            progressIndicator.visibility(false)
            progressIndicator.isIndeterminate = false
            progressIndicator.max = HUNDRED
            progressIndicator.visibility(true)

            doAsync {
                try {
                    setProfileImage(firestore, user, bitmap) { progress ->
                        uiContext {
                            progressIndicator.progress = progress.percentage
                        }
                    }
                    uiContext {
                        progressIndicator.visibility(false)
                        toast(R.string.toast_profile_image_updated)
                    }
                    hasProfileImageBeenChanged = true
                } catch (e: StorageException) {
                    uiContext {
                        Timber.e(e, "Could not update profile image.")
                        progressIndicator.visibility(false)
                        toast(R.string.toast_error_profile_image_update)
                    }
                }
            }
        }
    }
}
