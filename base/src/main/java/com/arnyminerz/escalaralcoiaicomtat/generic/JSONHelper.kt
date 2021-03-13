package com.arnyminerz.escalaralcoiaicomtat.generic

import android.os.Build
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gets the contents of a url
 * @author Arnau Mora
 * @param url The target url
 * @see BufferedReader
 * @see URL
 * @throws IOException If it occurs an error during the load
 * @return The loaded string
 */
private fun fetch(url: URL): String {
    val conn = url.openConnection() as HttpURLConnection
    conn.apply {
        HttpURLConnection.setFollowRedirects(true)
        instanceFollowRedirects = true
    }
    val inputStream = conn.inputStream
    var reader: BufferedReader? = null
    try {
        reader = inputStream.bufferedReader()
        val builder = StringBuilder()

        var read = reader.readLine()
        while (read != null) {
            builder.appendLine(read)
            read = reader.readLine()
        }

        return builder.toString()
    } catch (e: IOException) {
        reader?.close()
        throw e
    }
}

/**
 * Gets a JSON Object from a URL
 * @author Arnau Mora
 * @param url The target url
 * @see JSONObject
 * @see URL
 * @throws IOException If it occurs an error during the load
 * @return The converted JSONObject
 */
@Throws(IOException::class)
private fun jsonFromUrl(url: URL): JSONObject {
    Timber.v("Getting JSON from %s", url)
    return JSONObject(fetch(url))
}

fun jsonFromUrl(url: String): JSONObject =
    try {
        jsonFromUrl(URL(url))
    } catch (e: IOException) {
        Timber.w(e, "Could not get JSON from %s", url)
        jsonFromUrl(
            URL(url.replace("https", "http"))
        ) // Replace the https address with the http one
    }

private fun jsonArrayFromURL(url: URL): JSONArray {
    Timber.v("Getting JSON Array from %s", url)
    val jsonRaw = fetch(url)
    try {
        return JSONArray(jsonRaw)
    } catch (ex: JSONException) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
            throw JSONException("Could not parse JSONArray from \"$url\".", ex)
        throw JSONException("Could not parse JSONArray from \"$url\". Trace: ${ex.localizedMessage}")
    }
}

fun jsonArrayFromURL(url: String): JSONArray =
    try {
        jsonArrayFromURL(URL(url))
    } catch (e: IOException) {
        jsonArrayFromURL(
            URL(url.replace("https", "http"))
        ) // Replace the https address with the http one
    }

fun jsonArrayFromFile(file: File): JSONArray =
    JSONArray(file.readText())
