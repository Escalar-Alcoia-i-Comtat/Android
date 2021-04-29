package com.arnyminerz.escalaralcoiaicomtat.data.climb.path

import com.google.firebase.auth.FirebaseUser

/**
 * Contains the data for marking a Path as completed.
 * @author Arnau Mora
 * @since 20210430
 */
data class MarkCompletedData(
    val user: FirebaseUser,
    val attempts: Int,
    val falls: Int,
    val comment: String?,
    val notes: String?
) : MarkingDataInt
