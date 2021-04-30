package com.arnyminerz.escalaralcoiaicomtat.activity.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.auth.setDefaultProfileImage
import com.arnyminerz.escalaralcoiaicomtat.auth.setProfileImage
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityProfileBinding
import com.arnyminerz.escalaralcoiaicomtat.generic.MEGABYTE
import com.arnyminerz.escalaralcoiaicomtat.generic.doAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.getBitmapFromUri
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.generic.uiContext
import com.arnyminerz.escalaralcoiaicomtat.shared.REQUEST_CODE_SELECT_PROFILE_IMAGE
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.ktx.storage
import timber.log.Timber

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding

    private lateinit var firestore: FirebaseFirestore

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

        binding.profileNameTextView.text = user.displayName

        val profileImageUrl = user.photoUrl
        if (profileImageUrl != null) {
            binding.profileProgressIndicator.visibility(true)
            Firebase.storage.getReferenceFromUrl(profileImageUrl.toString())
                .getBytes(MEGABYTE * 5)
                .addOnSuccessListener { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    binding.profileImageImageView.background = bitmap.toDrawable(resources)
                }
                .addOnFailureListener {
                    val e = it as StorageException
                    if (e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                        binding.profileProgressIndicator.visibility(false)
                        binding.profileProgressIndicator.isIndeterminate = false
                        binding.profileProgressIndicator.max = 100
                        binding.profileProgressIndicator.visibility(true)
                        binding.profileImageImageView.setBackgroundResource(R.drawable.ic_profile_image)
                        doAsync {
                            Timber.e(e, "Could not find the profile image. Setting default...")
                            setDefaultProfileImage(
                                this@ProfileActivity,
                                firestore,
                                user
                            ) { progress ->
                                runOnUiThread {
                                    binding.profileProgressIndicator.progress =
                                        progress.percentage()
                                }
                            }
                        }
                    } else
                        Timber.e(it, "Could not load profile image")
                }
                .addOnCompleteListener {
                    binding.profileProgressIndicator.visibility(false)
                }
        } else binding.profileProgressIndicator.visibility(false)

        binding.profileImageImageView.setOnClickListener {
            startActivityForResult(
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                }, REQUEST_CODE_SELECT_PROFILE_IMAGE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == REQUEST_CODE_SELECT_PROFILE_IMAGE && resultCode == RESULT_OK)
            resultData?.data?.let { uri ->
                val bitmap = getBitmapFromUri(contentResolver, uri)
                if (bitmap != null) {
                    binding.profileImageImageView.background = bitmap.toDrawable(resources)

                    updateProfileImage(bitmap)
                }
            }
        else super.onActivityResult(requestCode, resultCode, resultData)
    }

    /**
     * Sets the profile image of the user to a new one.
     * @author Arnau Mora
     * @since 20210425
     */
    private fun updateProfileImage(bitmap: Bitmap) {
        val user = Firebase.auth.currentUser
        if (user != null) {
            binding.profileProgressIndicator.visibility(false)
            binding.profileProgressIndicator.isIndeterminate = false
            binding.profileProgressIndicator.max = 100
            binding.profileProgressIndicator.visibility(true)

            doAsync {
                try {
                    setProfileImage(firestore, user, bitmap) { progress ->
                        runOnUiThread {
                            binding.profileProgressIndicator.progress = progress.percentage()
                        }
                    }
                    uiContext {
                        binding.profileProgressIndicator.visibility(false)
                        toast(R.string.toast_profile_image_updated)
                    }
                } catch (e: StorageException) {
                    uiContext {
                        Timber.e(e, "Could not update profile image.")
                        binding.profileProgressIndicator.visibility(false)
                        toast(R.string.toast_error_profile_image_update)
                    }
                }
            }
        }
    }
}
