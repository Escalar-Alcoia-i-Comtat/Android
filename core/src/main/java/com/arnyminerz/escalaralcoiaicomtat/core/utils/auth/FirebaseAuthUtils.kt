package com.arnyminerz.escalaralcoiaicomtat.core.utils.auth

import com.google.firebase.auth.FirebaseAuth

/**
 * Checks if the user is currently logged in.
 * @author Arnau Mora
 * @since 20211123
 */
val FirebaseAuth.loggedIn: Boolean
    get() = currentUser?.isAnonymous == false
