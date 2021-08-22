package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.storage

import com.arnyminerz.escalaralcoiaicomtat.core.data.auth.VisibleUserData
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

/**
 * Contains the data for marking a Path as project.
 * @author Arnau Mora
 * @since 20210430
 */
@Parcelize
class MarkedProjectData(
    override val documentPath: String,
    override val timestamp: Timestamp?,
    override val user: VisibleUserData,
    override val comment: String?,
    override val notes: String?,
    override var likedBy: List<String>
) : MarkedDataInt(documentPath, timestamp, user, comment, notes, likedBy.toMutableList())
