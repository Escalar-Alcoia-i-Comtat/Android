package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.storage

import com.arnyminerz.escalaralcoiaicomtat.core.data.auth.VisibleUserData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.CompletionType
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

/**
 * Contains the data for marking a Path as completed.
 * @author Arnau Mora
 * @since 20210430
 */
@Parcelize
class MarkedCompletedData(
    override val documentPath: String,
    override val timestamp: Timestamp?,
    override val user: VisibleUserData,
    val attempts: Long,
    val falls: Long,
    val grade: String,
    val type: CompletionType,
    override val comment: String?,
    override val notes: String?,
    override var likedBy: List<String>,
) : MarkedDataInt(documentPath, timestamp, user, comment, notes, likedBy.toMutableList())
