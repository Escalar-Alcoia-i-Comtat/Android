package com.arnyminerz.escalaralcoiaicomtat.activity.profile

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityProfileBinding
import com.arnyminerz.escalaralcoiaicomtat.generic.MEGABYTE
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import timber.log.Timber

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
        if (profileImageUrl != null)
            Firebase.storage.getReferenceFromUrl(profileImageUrl.toString())
                .getBytes(MEGABYTE * 5)
                .addOnSuccessListener { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    binding.profileImageImageView.background = bitmap.toDrawable(resources)
                }
                .addOnFailureListener {
                    Timber.e(it, "Could not load profile image")
                }
    }
}
