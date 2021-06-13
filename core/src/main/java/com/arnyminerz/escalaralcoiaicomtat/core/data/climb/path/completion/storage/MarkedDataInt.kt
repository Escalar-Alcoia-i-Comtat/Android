package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.storage

import com.arnyminerz.escalaralcoiaicomtat.core.data.auth.VisibleUserData
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

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
open class MarkedDataInt(
    val documentPath: String,
    val timestamp: Timestamp?,
    val user: VisibleUserData,
    val comment: String?,
    val notes: String?,
    val likedBy: MutableList<String>
) {
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
        if (likedBy.contains(userUid))
            likedBy.remove(userUid)
        else
            likedBy.add(userUid)
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
}
