package com.arnyminerz.escalaralcoiaicomtat.connection.web

import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

fun download(src: String): InputStream =
    runBlocking {
        val url = URL(src)
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        connection.inputStream
    }
