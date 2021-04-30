package com.arnyminerz.escalaralcoiaicomtat.data.climb.path

import com.arnyminerz.escalaralcoiaicomtat.auth.VisibleUserData
import com.google.firebase.Timestamp

/**
 * Contains the data for marking a Path as completed.
 * @author Arnau Mora
 * @since 20210430
 */
class MarkedCompletedData(
    val timestamp: Timestamp?,
    user: VisibleUserData,
    val attempts: Long,
    val falls: Long,
    comment: String?,
    notes: String?
) : MarkedDataInt(user, comment, notes)
