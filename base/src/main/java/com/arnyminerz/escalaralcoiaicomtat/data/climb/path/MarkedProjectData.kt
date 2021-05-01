package com.arnyminerz.escalaralcoiaicomtat.data.climb.path

import com.arnyminerz.escalaralcoiaicomtat.auth.VisibleUserData
import com.google.firebase.Timestamp

/**
 * Contains the data for marking a Path as project.
 * @author Arnau Mora
 * @since 20210430
 */
class MarkedProjectData(
    documentPath: String,
    timestamp: Timestamp?,
    user: VisibleUserData,
    comment: String?,
    notes: String?,
    likedBy: List<String>
) : MarkedDataInt(documentPath, timestamp, user, comment, notes, likedBy.toMutableList())
