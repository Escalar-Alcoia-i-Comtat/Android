package com.arnyminerz.escalaralcoiaicomtat.exception

import java.lang.Exception

class NoInternetAccessException(msg: String) : Exception(msg) {
    constructor(): this("No Internet connection was found")
}