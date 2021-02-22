package com.arnyminerz.escalaralcoiaicomtat.exception.auth

class UserNotFoundException(uid: String? = null): AuthenticationException(if(uid == null) "The user was not found" else "The user ($uid) was not found") {
    constructor(): this(null)
}