package com.arnyminerz.escalaralcoiaicomtat.data.climb.path

import com.google.firebase.Timestamp
import com.google.firebase.auth.UserRecord

/**
 * Contains the data for marking a Path as completed.
 * @author Arnau Mora
 * @since 20210430
 */
data class MarkedCompletedData(
    val timestamp: Timestamp?,
    val user: UserRecord,
    val attempts: Long,
    val falls: Long,
    val comment: String?,
    val notes: String?
) : MarkedDataInt
