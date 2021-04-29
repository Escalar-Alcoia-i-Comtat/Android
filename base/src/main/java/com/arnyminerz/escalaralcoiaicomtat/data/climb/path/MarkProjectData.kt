package com.arnyminerz.escalaralcoiaicomtat.data.climb.path

import com.google.firebase.auth.FirebaseUser

/**
 * Contains the data for marking a Path as project.
 * @author Arnau Mora
 * @since 20210430
 */
data class MarkProjectData(
    val user: FirebaseUser,
    val comment: String?,
    val notes: String?
)
