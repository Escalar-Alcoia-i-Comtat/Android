package com.arnyminerz.escalaralcoiaicomtat.core.data.auth

import androidx.annotation.WorkerThread
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

fun FirebaseUser.user(): User = User(uid)

class User(val uid: String) {
    /**
     * Fetches the [User]'s [VisibleUserData] from the server.
     * @author Arnau Mora
     * @since 20210719
     * @throws FirebaseFunctionsException When there is an exception while calling the loader function.
     */
    @WorkerThread
    @Throws(FirebaseFunctionsException::class)
    suspend fun getVisibleUserData(): VisibleUserData = suspendCoroutine { cont ->
        val functions = Firebase.functions
        functions.getHttpsCallable("getUserData")
            .call(mapOf("uid" to uid))
            .addOnFailureListener { e ->
                Timber.e(e, "Could not get visible user data.")
                cont.resumeWithException(e)
            }
            .addOnSuccessListener { result ->
                val data = result.data as HashMap<*, *>

                val displayName = data["displayName"] as String
                val profileImagePath = data["profileImage"] as String
                val visibleUserData = VisibleUserData(uid, displayName, profileImagePath)
                cont.resume(visibleUserData)
            }
    }
}