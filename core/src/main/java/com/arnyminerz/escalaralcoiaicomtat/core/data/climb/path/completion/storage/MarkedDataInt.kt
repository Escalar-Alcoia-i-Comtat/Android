package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.storage

import android.os.Parcelable
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.core.data.auth.User
import com.arnyminerz.escalaralcoiaicomtat.core.data.auth.VisibleUserData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.CompletionType
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Serves as a model for all the types of completions available ([MarkedCompletedData] and
 * [MarkedProjectData]).
 * @author Arnau Mora
 * @since 20210501
 * @param documentPath The path in Firestore where the completion is stored at
 * @param timestamp When the completion was marked
 * @param user The user data who made the comment.
 * @param comment The comment that the user made.
 * @param notes The notes the user took.
 * @param likedBy The user uids that have liked the publication.
 */
abstract class MarkedDataInt(
    open val documentPath: String,
    open val timestamp: Timestamp?,
    open val user: VisibleUserData,
    open val comment: String?,
    open val notes: String?,
    open var likedBy: List<String>
) : Parcelable {
    companion object {
        /**
         * Initializes a new [MarkedDataInt] subinstance based on the data of [document].
         * @author Arnau Mora
         * @since 20210719
         * @param document The document to get the data from.
         * @return A [MarkedDataInt] class if everything went correctly. Null otherwise.
         */
        suspend fun newInstance(document: DocumentSnapshot): MarkedDataInt? {
            val documentPath = document.reference.path
            val objectId = document.reference.id
            val timestamp = document.getTimestamp("timestamp")
            val userUid = document.getString("user")!!
            val attempts = document.getLong("attempts") ?: 0
            val falls = document.getLong("falls") ?: 0
            val comment = document.getString("comment")
            val notes = document.getString("notes")
            val grade = document.getString("grade") ?: ""
            val project = document.getBoolean("project") ?: false
            val typeRaw = document.getString("type")
            // TODO: Load liked by

            var type: CompletionType? = null
            for (t in CompletionType.values())
                if (t.id.equals(typeRaw, true))
                    type = t

            if (type == null)
                Timber.w("$objectId > Could not find type for \"$typeRaw\"")

            Timber.v("$objectId > Got completion data for \"$documentPath\".")
            val user = try {
                Timber.v("$objectId > Loading user data ($userUid) from server...")
                val user = User(userUid)
                user.getVisibleUserData()
            } catch (e: IllegalArgumentException) {
                Timber.w(e)
                null
            } catch (e: FirebaseFunctionsException) {
                Timber.e(e, "$objectId > Could not get VisibleUserData!")
                null
            }

            if (user == null) {
                Timber.i("$objectId > Could not find user, continuing loop.")
                return null
            }

            Timber.i("$objectId > Processing path completed data result...")
            return when {
                project -> MarkedProjectData(
                    document.reference.path,
                    timestamp,
                    user,
                    comment,
                    notes,
                    listOf()
                )
                type != null -> MarkedCompletedData(
                    document.reference.path,
                    timestamp,
                    user,
                    attempts,
                    falls,
                    grade,
                    type,
                    comment,
                    notes,
                    listOf()
                )
                else -> null
            }
        }
    }

    /**
     * Gets the amount of likes people have made on the publication.
     * @author Arnau Mora
     * @since 20210501
     */
    val likesCount: Int
        get() = likedBy.size

    /**
     * Makes a user like the post. It toggles the current status, so if the user has liked the post,
     * it won't be liked anymore, and in the other way.
     * @author Arnau Mora
     * @since 20210528
     * @param firestore The Firestore instance where the post is stored at.
     * @param user The user that has to make the like.
     * @return The task that runs the like.
     */
    fun like(firestore: FirebaseFirestore, user: FirebaseUser): Task<Void> {
        val userUid = user.uid
        val likedBy = this.likedBy.toMutableList()
        if (likedBy.contains(userUid))
            likedBy.remove(userUid)
        else
            likedBy.add(userUid)
        this.likedBy = likedBy
        return firestore.document(documentPath)
            .update("likedBy", likedBy)
    }

    /**
     * Deletes the post.
     * @author Arnau Mora
     * @since 20210528
     * @param firestore The Firestore instance where the post is stored at.
     */
    fun delete(firestore: FirebaseFirestore) =
        firestore.document(documentPath)
            .delete()

    /**
     * Fetches the Path data for the completion.
     * @author Arnau Mora
     * @since 20210821
     */
    @WorkerThread
    suspend fun getPath(): Path {
        val firestore = Firebase.firestore
        val pathDocument = firestore.document(documentPath)
            .get()
            .await()
        return Path(pathDocument)
    }
}
