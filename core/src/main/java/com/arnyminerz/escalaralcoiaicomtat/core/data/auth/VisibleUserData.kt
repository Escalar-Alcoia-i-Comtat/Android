package com.arnyminerz.escalaralcoiaicomtat.core.data.auth

/**
 * Stores the user's data that can be displayed to other users
 * @author Arnau Mora
 * @since 20210430
 */
data class VisibleUserData(
    val uid: String,
    val displayName: String,
    val profileImagePath: String,
)
