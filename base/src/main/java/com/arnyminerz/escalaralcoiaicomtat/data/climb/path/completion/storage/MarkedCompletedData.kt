package com.arnyminerz.escalaralcoiaicomtat.data.climb.path.completion.storage

import com.arnyminerz.escalaralcoiaicomtat.auth.VisibleUserData
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.completion.CompletionType
import com.google.firebase.Timestamp

/**
 * Contains the data for marking a Path as completed.
 * @author Arnau Mora
 * @since 20210430
 */
class MarkedCompletedData(
    documentPath: String,
    timestamp: Timestamp?,
    user: VisibleUserData,
    val attempts: Long,
    val falls: Long,
    val grade: String,
    val type: CompletionType,
    comment: String?,
    notes: String?,
    likedBy: List<String>,
) : MarkedDataInt(documentPath, timestamp, user, comment, notes, likedBy.toMutableList())
