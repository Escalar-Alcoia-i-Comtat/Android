package com.arnyminerz.escalaralcoiaicomtat.generic

import android.os.Build
import com.arnyminerz.escalaralcoiaicomtat.async.EXTENDED_API_URL
import com.arnyminerz.escalaralcoiaicomtat.async.EXTENDED_API_URL_NO_SECURE
import com.arnyminerz.escalaralcoiaicomtat.data.user.UserData
import org.jetbrains.anko.getStackTraceString
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.URL

private suspend fun jsonFromUrl(url: URL): JSONObject {
    Timber.v("Getting JSON from %s", url)
    val jsonRaw = url.readText()
    return JSONObject(jsonRaw)
}

suspend fun jsonFromUrl(url: String): JSONObject =
    try {
        jsonFromUrl(URL(url))
    } catch (e: IOException) {
        Timber.e(e, "Could not get JSON from %s", url)
        jsonFromUrl(
            URL(
                if (url.contains(EXTENDED_API_URL))
                    url.replace(
                        EXTENDED_API_URL,
                        EXTENDED_API_URL_NO_SECURE
                    )
                else url.replace("https", "http")
            )
        ) // Replace the https address with the http one
    }

private suspend fun jsonArrayFromURL(url: URL): JSONArray {
    Timber.v("Getting JSON Array from %s", url)
    val jsonRaw = url.readText()
    try {
        return JSONArray(jsonRaw)
    }catch (ex: JSONException){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
            throw JSONException("Could not parse JSONArray from \"$url\".", ex)
        throw JSONException("Could not parse JSONArray from \"$url\". Trace: ${ex.getStackTraceString()}")
    }
}

suspend fun jsonArrayFromURL(url: String): JSONArray =
    try {
        jsonArrayFromURL(URL(url))
    } catch (e: IOException) {
        jsonArrayFromURL(
            URL(
                if (url.contains(EXTENDED_API_URL))
                    url.replace(
                        EXTENDED_API_URL,
                        EXTENDED_API_URL_NO_SECURE
                    )
                else url.replace("https", "http")
            )
        ) // Replace the https address with the http one
    }

fun jsonArrayFromFile(file: File): JSONArray =
    JSONArray(file.readText())

fun JSONObject.getError(): JSONObject = getJSONObject("error")
fun JSONObject.getUserData(key: String): UserData = UserData(getJSONObject(key))
fun JSONArray.isEmpty(): Boolean = length() <= 0
fun JSONArray.isNotEmpty(): Boolean = !isEmpty()