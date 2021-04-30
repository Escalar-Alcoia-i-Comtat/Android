package com.arnyminerz.escalaralcoiaicomtat.auth

import android.content.Context
import android.net.Uri
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.generic.ValueMax
import com.arnyminerz.escalaralcoiaicomtat.generic.WEBP_LOSSY_LEGACY
import com.arnyminerz.escalaralcoiaicomtat.generic.cropToSquare
import com.arnyminerz.escalaralcoiaicomtat.generic.doAsync
import com.arnyminerz.escalaralcoiaicomtat.shared.PROFILE_IMAGE_COMPRESSION_QUALITY
import com.arnyminerz.escalaralcoiaicomtat.shared.PROFILE_IMAGE_SIZE
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import timber.log.Timber
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Uploads the user's default profile image.
 * @author Arnau Mora
 * @since 20210424
 * @param context The context where the function is getting called.
 * @param user The user to update.
 * @param progressListener This will notify you about the image upload progress.
 */
@WorkerThread
suspend fun setDefaultProfileImage(
    context: Context,
    user: FirebaseUser,
    progressListener: ((progress: ValueMax<Long>) -> Unit)?
): UploadTask.TaskSnapshot = suspendCoroutine { cont ->
    try {
        Timber.v("Registration has been successful, setting default profile image...")
        Timber.v("Starting profile image upload...")
        val storageRef = Firebase.storage.reference
        val profileImageRef = storageRef.child("users/${user.uid}/profile.webp")
        val d = ContextCompat.getDrawable(context, R.drawable.ic_profile_image)
        val profileImage = d!!.toBitmap()
            .cropToSquare()
            ?.scale(PROFILE_IMAGE_SIZE.toInt(), PROFILE_IMAGE_SIZE.toInt())
        val baos = ByteArrayOutputStream()
        profileImage?.compress(WEBP_LOSSY_LEGACY, PROFILE_IMAGE_COMPRESSION_QUALITY, baos)
        val data = baos.toByteArray()

        Timber.v("Uploading profile image...")
        profileImageRef.putBytes(data)
            .addOnProgressListener { task ->
                val progress = ValueMax(task.bytesTransferred, task.totalByteCount)
                Timber.v("Upload: ${progress.percentage()}%")
                progressListener?.invoke(progress)
            }
            .addOnSuccessListener {
                Timber.v("Finished uploading image. Updating in profile...")
                doAsync {
                    try {
                        updateProfileImage(
                            user,
                            "gs://escalaralcoiaicomtat.appspot.com" + profileImageRef.path
                        )
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                }
                Timber.v("Profile updated. Resuming...")
                cont.resume(it)
            }
            .addOnFailureListener {
                Timber.e(it, "Could not upload profile image")
                cont.resumeWithException(it)
            }
    } catch (e: Exception) {
        Timber.e(e, "Could not update profile image.")
        cont.resumeWithException(e)
    }
}

/**
 * Updates the user's profile image address.
 * @author Arnau Mora
 * @since 20210425
 * @param user The user to update.
 * @param imageDownloadUrl The uploaded image url.
 */
@UiThread
suspend fun updateProfileImage(user: FirebaseUser, imageDownloadUrl: String) =
    suspendCoroutine<Void> { cont ->
        Timber.v("Processing uri...")
        val profileImageUri = Uri.parse(imageDownloadUrl)
        Timber.v("Updating profile image of ${user.uid} to $profileImageUri...")
        Timber.v("Creating change request...")
        val changeRequest =
            userProfileChangeRequest {
                photoUri = profileImageUri
            }
        Timber.v("Submitting change request...")
        user.updateProfile(changeRequest)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // TODO: Update Firestore reference
                    Timber.v("Updated profile image successfully.")
                    cont.resume(task.result)
                } else {
                    val e = task.exception!!
                    Timber.e(e, "Could not update profile image.")
                    cont.resumeWithException(e)
                }
            }
    }

/**
 * Updates the user's profile image address.
 * @author Arnau Mora
 * @since 20210425
 * @param user The user to update.
 * @param displayName The new display name of the user.
 * @throws FirebaseAuthInvalidUserException Thrown if the current user's account has been disabled,
 * deleted, or its credentials are no longer valid.
 */
@UiThread
@Throws(FirebaseAuthInvalidUserException::class)
suspend fun updateDisplayName(user: FirebaseUser, displayName: String) =
    suspendCoroutine<Void> { cont ->
        user.updateProfile(
            userProfileChangeRequest {
                this.displayName = displayName
            }
        ).addOnSuccessListener {
            cont.resume(it)
        }.addOnFailureListener {
            Timber.e(it, "Could not update profile data.")
            cont.resumeWithException(it)
        }
    }

/**
 * Creates a reference in Firestore for getting a limited amount of user's data. Just display
 * name and profile image.
 * @author Arnau Mora
 * @since 20210430
 */
@UiThread
suspend fun createFirestoreUserReference(firestore: FirebaseFirestore, user: FirebaseUser) =
    suspendCoroutine<Void> { cont ->
        firestore.collection("Users")
            .document(user.uid)
            .set(
                hashMapOf(
                    "displayName" to user.displayName,
                    "profileImage" to user.photoUrl?.toString()
                )
            )
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }
