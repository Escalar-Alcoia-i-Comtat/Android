package com.arnyminerz.escalaralcoiaicomtat.network

import java.io.IOException
import java.net.URL

fun URL.ping(timeout: Int = 3000): Boolean {
    return try {
        val conn = openConnection()
        conn.connectTimeout = timeout
        conn.connect()
        true
    } catch (ex: IOException) {
        false
    }
}
