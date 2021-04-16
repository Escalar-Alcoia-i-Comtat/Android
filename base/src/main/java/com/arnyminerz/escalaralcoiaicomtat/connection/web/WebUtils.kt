package com.arnyminerz.escalaralcoiaicomtat.connection.web

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

fun download(src: String): InputStream {
    val url = URL(src)
    val connection = url.openConnection() as HttpURLConnection
    connection.doInput = true
    connection.connect()
    return connection.inputStream
}
