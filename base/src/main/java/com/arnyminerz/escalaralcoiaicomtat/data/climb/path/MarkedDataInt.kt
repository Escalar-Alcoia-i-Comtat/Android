package com.arnyminerz.escalaralcoiaicomtat.data.climb.path

import com.arnyminerz.escalaralcoiaicomtat.auth.VisibleUserData
import com.google.firebase.Timestamp

/**
 * Serves as a model for all the types of completions available ([MarkedCompletedData] and
 * [MarkedProjectData]).
 * @author Arnau Mora
 * @since 20210501
 * @param timestamp When the completion was marked
 * @param user The user data who made the comment.
 * @param comment The comment that the user made.
 * @param notes The notes the user took.
 * @param likedBy The user uids that have liked the publication.
 */
open class MarkedDataInt(
    val timestamp: Timestamp?,
    val user: VisibleUserData,
    val comment: String?,
    val notes: String?,
    val likedBy: List<String>
) {
    val likes: Int
        get() = likedBy.size
}
