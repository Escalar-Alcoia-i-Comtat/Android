package com.arnyminerz.escalaralcoiaicomtat.data.climb.path.completion.storage

import com.arnyminerz.escalaralcoiaicomtat.auth.VisibleUserData
import com.google.firebase.Timestamp

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
}
