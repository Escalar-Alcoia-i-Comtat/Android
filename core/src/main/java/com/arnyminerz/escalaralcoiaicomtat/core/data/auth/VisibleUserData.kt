package com.arnyminerz.escalaralcoiaicomtat.core.data.auth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcelable
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PROFILE_IMAGE_MAX_SIZE
import com.arnyminerz.escalaralcoiaicomtat.core.utils.WEBP_LOSSY_LEGACY
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.ktx.storage
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Stores the user's data that can be displayed to other users
 * @author Arnau Mora
 * @since 20210430
 */
@Parcelize
class VisibleUserData(
    val uid: String,
    val displayName: String,
    val profileImagePath: String,
) : Parcelable {
    /**
     * Returns the cache file for the profile image.
     * @author Arnau Mora
     * @since 20210821
     */
    private fun cacheImageFile(context: Context): File = File(context.cacheDir, "profile-$uid")

    /**
     * Gets the [User]'s profile image.
     * @author Arnau Mora
     * @since 20210719
     * @throws FirebaseFunctionsException When there is an exception while getting the user's profile data.
     * @throws StorageException When there is an exception while getting the user's profile image.
     */
    @WorkerThread
    @Throws(FirebaseFunctionsException::class, StorageException::class)
    suspend fun profileImage(): Bitmap {
        return suspendCoroutine { cont ->
            val profileImage = profileImagePath

            val storage = Firebase.storage
            val context = storage.app.applicationContext
            val cacheFile = cacheImageFile(context)

            if (cacheFile.exists()) {
                Timber.v("Profile image for $uid is cached, decoding...")
                val bitmap = BitmapFactory.decodeFile(cacheFile.path)
                cont.resume(bitmap)
            } else
                storage.getReferenceFromUrl(profileImage)
                    .getBytes(PROFILE_IMAGE_MAX_SIZE)
                    .addOnFailureListener { e ->
                        Timber.e(e, "Could not get profile image for $uid.")
                        cont.resumeWithException(e)
                    }
                    .addOnSuccessListener { bytes ->
                        Timber.v("Got profile image, decoding Bitmap...")
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        Timber.v("Storing to cache...")
                        bitmap.compress(WEBP_LOSSY_LEGACY, 85, cacheFile.outputStream())
                        Timber.v("Resuming with bitmap...")
                        cont.resume(bitmap)
                    }
        }
    }
}
