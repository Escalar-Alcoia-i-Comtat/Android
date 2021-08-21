package com.arnyminerz.escalaralcoiaicomtat.core.data.auth

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcelable
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PROFILE_IMAGE_MAX_SIZE
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.ktx.storage
import kotlinx.parcelize.Parcelize
import timber.log.Timber
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
            storage.getReferenceFromUrl(profileImage)
                .getBytes(PROFILE_IMAGE_MAX_SIZE)
                .addOnFailureListener { e ->
                    Timber.e(e, "Could not get ")
                    cont.resumeWithException(e)
                }
                .addOnSuccessListener { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    cont.resume(bitmap)
                }
        }
    }
}
