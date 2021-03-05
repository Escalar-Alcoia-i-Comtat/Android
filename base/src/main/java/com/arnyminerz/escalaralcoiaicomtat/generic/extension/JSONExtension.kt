package com.arnyminerz.escalaralcoiaicomtat.generic.extension

import com.mapbox.mapboxsdk.geometry.LatLng
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

fun JSONObject.getBooleanFromString(key: String): Boolean {
    return when (val obj = get(key, false)) {
        is Boolean -> obj
        is Int -> obj == 1
        else -> getString(key).toBoolean()
    }
}

fun JSONObject.get(key: String, defaultValue: Any): Any =
    try {
        get(key)
    } catch (ex: JSONException) {
        defaultValue
    }

fun JSONObject.getInt(key: String, defaultValue: Int): Int =
    try {
        getInt(key)
    } catch (ex: JSONException) {
        defaultValue
    }

fun JSONObject.getJSONArray(key: String, defaultValue: JSONArray): JSONArray =
    try {
        getJSONArray(key)
    } catch (ex: JSONException) {
        defaultValue
    }

fun JSONObject.getJSONArrayOrEmpty(key: String): JSONArray =
    getJSONArray(key, JSONArray())

fun JSONObject.getStringSafe(key: String): String? =
    if (!has(key) || get(key) !is String)
        null
    else
        getString(key)

fun JSONObject.getTimestampSafe(key: String): Date? = getStringSafe(key)?.toTimestamp()

fun JSONObject.getLatLngSafe(key: String): LatLng? =
    getStringSafe(key)?.let {
        val spl = it.replace(" ", "").split(",")
        return LatLng(spl[0].toDouble(), spl[1].toDouble())
    }

fun JSONArray.sort(keyName: String): JSONArray {
    val sortedJsonArray = JSONArray()
    val list = arrayListOf<Any>()
    for (i in 0 until length())
        list.add(getJSONObject(i))

    Collections.sort(list, object : Comparator<Any> {
        override fun compare(a: Any, b: Any): Int {
            if (a !is JSONObject || b !is JSONObject)
                return -1

            var str1 = String()
            var str2 = String()
            try {
                str1 = a[keyName] as String
                str2 = b[keyName] as String
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return str1.compareTo(str2)
        }
    })
    for (i in 0 until length())
        sortedJsonArray.put(list[i])

    return sortedJsonArray
}

fun JSONArray.isEmpty(): Boolean = length() <= 0
fun JSONArray.isNotEmpty(): Boolean = !isEmpty()