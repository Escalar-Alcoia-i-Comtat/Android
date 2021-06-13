package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.request

import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.CompletionType
import com.google.firebase.auth.FirebaseUser

/**
 * Contains the data for marking a Path as completed.
 * @author Arnau Mora
 * @since 20210430
 */
class MarkCompletedData(
    user: FirebaseUser,
    private val attemptsFalls: Pair<Int, Int>,
    val grade: String,
    val type: CompletionType,
    comment: String?,
    notes: String?
) : MarkingDataInt(user, comment, notes) {
    val attempts: Int
        get() = attemptsFalls.first

    val falls: Int
        get() = attemptsFalls.second
}
