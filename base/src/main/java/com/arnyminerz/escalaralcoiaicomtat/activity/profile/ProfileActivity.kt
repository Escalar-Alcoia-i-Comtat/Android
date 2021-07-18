package com.arnyminerz.escalaralcoiaicomtat.activity.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.auth.setDefaultProfileImage
import com.arnyminerz.escalaralcoiaicomtat.auth.setProfileImage
import com.arnyminerz.escalaralcoiaicomtat.core.shared.HUNDRED
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PROFILE_IMAGE_MAX_SIZE
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getBitmapFromUri
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityProfileBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.ktx.storage
import timber.log.Timber

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

        val user = Firebase.auth.currentUser
        if (user == null) {
            Timber.w("Not logged in.")
            return
        }

        firestore = Firebase.firestore

        binding.backFab.setOnClickListener { onBackPressed() }

        binding.profileNameTextView.text = user.displayName

        val profileImageUrl = user.photoUrl
        if (profileImageUrl != null) {
            progressIndicator.visibility(true)
            Firebase.storage.getReferenceFromUrl(profileImageUrl.toString())
                .getBytes(PROFILE_IMAGE_MAX_SIZE)
                .addOnSuccessListener { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    binding.profileImageImageView.background = bitmap.toDrawable(resources)
                }
                .addOnFailureListener {
                    val e = it as StorageException
                    when (e.errorCode) {
                        StorageException.ERROR_OBJECT_NOT_FOUND -> {
                            progressIndicator.visibility(false)
                            progressIndicator.isIndeterminate = false
                            progressIndicator.max = HUNDRED
                            progressIndicator.visibility(true)
                            binding.profileImageImageView.setBackgroundResource(R.drawable.ic_profile_image)
                            doAsync {
                                Timber.e(e, "Could not find the profile image. Setting default...")
                                setDefaultProfileImage(
                                    this@ProfileActivity,
                                    firestore,
                                    user
                                ) { progress ->
                                    uiContext {
                                        progressIndicator.progress = progress.percentage()
                                    }
                                }
                            }
                        }
                        else -> Timber.e(it, "Could not load profile image")
                    }
                }
                .addOnCompleteListener {
                    progressIndicator.visibility(false)
                }
        } else progressIndicator.visibility(false)

        binding.profileImageImageView.setOnClickListener {
            openProfileImageRequest.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                }
            )
        }
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
                            progressIndicator.progress = progress.percentage()
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
