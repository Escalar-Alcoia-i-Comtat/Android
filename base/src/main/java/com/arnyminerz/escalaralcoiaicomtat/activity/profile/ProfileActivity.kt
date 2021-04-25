package com.arnyminerz.escalaralcoiaicomtat.activity.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityProfileBinding
import com.arnyminerz.escalaralcoiaicomtat.generic.MEGABYTE
import com.arnyminerz.escalaralcoiaicomtat.generic.WEBP_LOSSY_LEGACY
import com.arnyminerz.escalaralcoiaicomtat.generic.cropToSquare
import com.arnyminerz.escalaralcoiaicomtat.generic.getBitmapFromUri
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.shared.PROFILE_IMAGE_COMPRESSION_QUALITY
import com.arnyminerz.escalaralcoiaicomtat.shared.REQUEST_CODE_SELECT_PROFILE_IMAGE
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import timber.log.Timber
import java.io.ByteArrayOutputStream

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = Firebase.auth.currentUser
        if (user == null) {
            Timber.w("Not logged in.")
            return
        }

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
            binding.profileProgressIndicator.visibility(true)

            val profileImageRef = Firebase.storage.getReference("profile/")
            val baos = ByteArrayOutputStream()
            bitmap.cropToSquare()
                ?.compress(WEBP_LOSSY_LEGACY, PROFILE_IMAGE_COMPRESSION_QUALITY, baos)
            val bitmapBytes = baos.toByteArray()
            bitmap.recycle()
            profileImageRef.putBytes(bitmapBytes)
                .addOnProgressListener { task ->
                    binding.profileProgressIndicator.progress = task.bytesTransferred.toInt()
                    binding.profileProgressIndicator.max = task.totalByteCount.toInt()
                }
                .addOnSuccessListener {
                    binding.profileProgressIndicator.visibility(false)
                    toast(R.string.toast_profile_image_updated)
                }
                .addOnFailureListener {
                    toast(R.string.toast_error_profile_image_update)
                }
        }
    }
}
