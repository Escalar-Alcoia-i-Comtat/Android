package com.arnyminerz.escalaralcoiaicomtat.exception

import java.lang.Exception

class UserNotFoundException(uid: String?): Exception(if(uid == null) "The user was not found" else "The user ($uid) was not found") {
    constructor(): this(null)
}