package com.arnyminerz.escalaralcoiaicomtat.data.climb.path.completion.request

import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.CompletionType
import com.google.firebase.auth.FirebaseUser

/**
 * Contains the data for marking a Path as completed.
 * @author Arnau Mora
 * @since 20210430
 */
class MarkCompletedData(
    user: FirebaseUser,
    val attempts: Int,
    val falls: Int,
    val grade: String,
    val type: CompletionType,
    comment: String?,
    notes: String?
) : MarkingDataInt(user, comment, notes)
