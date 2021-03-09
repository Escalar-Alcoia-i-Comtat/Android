package com.arnyminerz.escalaralcoiaicomtat.exception

class NoInternetAccessException(msg: String) : Exception(msg) {
    constructor() : this("No Internet connection was found")
}
