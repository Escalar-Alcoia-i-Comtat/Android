package com.arnyminerz.escalaralcoiaicomtat.auth

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.exception.CouldNotCompressImageException
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PROFILE_IMAGE_COMPRESSION_QUALITY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PROFILE_IMAGE_SIZE
import com.arnyminerz.escalaralcoiaicomtat.core.utils.ValueMax
import com.arnyminerz.escalaralcoiaicomtat.core.utils.WEBP_LOSSY_LEGACY
import com.arnyminerz.escalaralcoiaicomtat.core.utils.cropToSquare
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import timber.log.Timber
import java.io.ByteArrayOutputStream
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Uploads the user's default profile image.
 * @author Arnau Mora
 * @since 20210424
 * @param context The context where the function is getting called.
 * @param firestore The Firestore instance to update references.
 * @param user The user to update.
 * @param progressListener This will notify you about the image upload progress.
 */
@WorkerThread
suspend fun setDefaultProfileImage(
    context: Context,
    firestore: FirebaseFirestore,
    user: FirebaseUser,
    progressListener: (@WorkerThread suspend CoroutineContext.(progress: ValueMax<Long>) -> Unit)?
): UploadTask.TaskSnapshot = setProfileImage(
    firestore,
    user,
    ContextCompat.getDrawable(context, R.drawable.ic_profile_image)!!.toBitmap(),
    progressListener
)

/**
 * Uploads and updates the user's profile image.
 * @author Arnau Mora
 * @since 20210424
 * @param firestore The Firestore instance to update references.
 * @param user The user to update.
 * @param image The image to use
 * @param progressListener This will notify you about the image upload progress.
 * @throws IllegalArgumentException When there has been an error while processing the image crop size.
 * @throws CouldNotCompressImageException When the image could not have been compressed.
 * @throws StorageException When there has been an error while uploading the new profile image.
 * @throws IllegalStateException If launched incorrectly.
 * @throws FirebaseAuthInvalidUserException When the current user's account has been disabled,
 * deleted, or its credentials are no longer valid.
 * @throws FirebaseFirestoreException When there has been an error updating the profile's data.
 */
@WorkerThread
@Throws(
    IllegalArgumentException::class,
    CouldNotCompressImageException::class,
    StorageException::class,
    FirebaseAuthInvalidUserException::class,
    IllegalStateException::class,
    FirebaseFirestoreException::class
)
suspend fun setProfileImage(
    firestore: FirebaseFirestore,
    user: FirebaseUser,
    image: Bitmap,
    progressListener: (@WorkerThread suspend CoroutineContext.(progress: ValueMax<Long>) -> Unit)?
): UploadTask.TaskSnapshot {
    val c = coroutineContext
    return suspendCoroutine { cont ->
        try {
            Timber.v("Starting profile image upload...")
            val storageRef = Firebase.storage.reference
            val profileImageRef = storageRef.child("users/${user.uid}/profile.webp")
            val profileImage = image
                .cropToSquare()
                ?.scale(PROFILE_IMAGE_SIZE.toInt(), PROFILE_IMAGE_SIZE.toInt())
            val baos = ByteArrayOutputStream()
            val compressed =
                profileImage?.compress(WEBP_LOSSY_LEGACY, PROFILE_IMAGE_COMPRESSION_QUALITY, baos)
            if (compressed != true)
                throw CouldNotCompressImageException("Could not compress the image to WebP.")
            val data = baos.toByteArray()

            Timber.v("Uploading profile image...")
            profileImageRef.putBytes(data)
                .addOnProgressListener { task ->
                    val progress = ValueMax(task.bytesTransferred, task.totalByteCount)
                    Timber.v("Upload: ${progress.percentage}%")
                    // TODO: Find another way of doing this, seems a bit jenky
                    doAsync { progressListener?.invoke(c, progress) }
                }
                .addOnSuccessListener {
                    Timber.v("Finished uploading image. Updating in profile...")
                    doAsync {
                        try {
                            updateProfileImage(
                                firestore,
                                user,
                                "gs://escalaralcoiaicomtat.appspot.com" + profileImageRef.path
                            )
                        } catch (e: IllegalStateException) {
                            cont.resumeWithException(e)
                        } catch (e: FirebaseAuthInvalidUserException) {
                            cont.resumeWithException(e)
                        } catch (e: FirebaseFirestoreException) {
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
        } catch (e: IllegalArgumentException) {
            cont.resumeWithException(e)
        }
    }
}

/**
 * Updates the user's profile image address.
 * @author Arnau Mora
 * @since 20210425
 * @param firestore The Firestore instance to update references.
 * @param user The user to update.
 * @param imageDownloadUrl The uploaded image url.
 * @throws IllegalStateException If launched incorrectly.
 * @throws FirebaseAuthInvalidUserException When the current user's account has been disabled,
 * deleted, or its credentials are no longer valid.
 * @throws FirebaseFirestoreException When there has been an error updating the profile's data.
 */
@WorkerThread
@Throws(
    IllegalStateException::class,
    FirebaseAuthInvalidUserException::class,
    FirebaseFirestoreException::class
)
suspend fun updateProfileImage(
    firestore: FirebaseFirestore,
    user: FirebaseUser,
    imageDownloadUrl: String
) =
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
            .addOnSuccessListener { void ->
                Timber.v("Updated profile image successfully. Updating Firestore reference...")
                firestore.collection("Users")
                    .document(user.uid)
                    .update("profileImage", imageDownloadUrl)
                    .addOnSuccessListener {
                        Timber.v("Updated reference! Resuming...")
                        cont.resume(it)
                    }
                    .addOnFailureListener {
                        Timber.e(it, "Could not update reference.")
                        if (it is FirebaseFirestoreException && it.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                            Timber.i("There's no firestore reference for user")
                            doAsync {
                                try {
                                    createFirestoreUserReference(firestore, user)
                                    cont.resume(void)
                                } catch (e: FirebaseFirestoreException) {
                                    Timber.e(e, "Could not update the user's profile.")
                                    cont.resumeWithException(e)
                                }
                            }
                        } else {
                            cont.resumeWithException(it)
                        }
                    }
            }
            .addOnFailureListener { e ->
                Timber.e(e, "Could not update profile image.")
                cont.resumeWithException(e)
            }
    }

/**
 * Updates the user's profile image address.
 * @author Arnau Mora
 * @since 20210425
 * @param firestore The Firestore instance to update references.
 * @param user The user to update.
 * @param displayName The new display name of the user.
 * @throws FirebaseAuthInvalidUserException Thrown if the current user's account has been disabled,
 * deleted, or its credentials are no longer valid.
 */
@WorkerThread
@Throws(FirebaseAuthInvalidUserException::class)
suspend fun updateDisplayName(
    firestore: FirebaseFirestore,
    user: FirebaseUser,
    displayName: String
) =
    suspendCoroutine<Void> { cont ->
        user.updateProfile(
            userProfileChangeRequest {
                this.displayName = displayName
            }
        ).addOnSuccessListener {
            firestore.collection("Users")
                .document(user.uid)
                .update("displayName", displayName)
                .addOnSuccessListener { result ->
                    Timber.v("Updated reference! Resuming...")
                    cont.resume(result)
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Could not update reference.")
                    cont.resumeWithException(e)
                }
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
 * @param firestore The Firestore instance to update references.
 * @param user The user to store the reference of.
 * @throws FirebaseFirestoreException When there has been an exception while setting the user's data.
 */
@WorkerThread
@Throws(FirebaseFirestoreException::class)
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
