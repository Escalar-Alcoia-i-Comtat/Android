package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.util.MalformedJsonException
import com.arnyminerz.escalaralcoiaicomtat.generic.generateUUID
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import timber.log.Timber
import java.io.InvalidObjectException
import java.io.Serializable

const val WINDOW_DATA_KEY = "window_data"

data class MapObjectWindowData(
    var title: String,
    var message: String?
) : Serializable {
    companion object {
        fun load(symbol: Symbol): MapObjectWindowData {
            val json = symbol.data ?: throw InvalidObjectException("Symbol doesn't have any data")
            if (!json.isJsonObject)
                throw MalformedJsonException("Data is not a json object")
            val obj = json.asJsonObject
            if (!obj.has(WINDOW_DATA_KEY))
                throw MalformedJsonException("Data doesn't contain")
            val windowData = obj.getAsJsonObject(WINDOW_DATA_KEY)
            if (!windowData.has("title"))
                throw MalformedJsonException("Window data doesn't contain title")
            return MapObjectWindowData(
                windowData.get("title").asString,
                if (windowData.has("message"))
                    windowData.get("message").asString
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                else null
            )
        }
    }

    private var layerUUID: String = generateUUID()

    @Suppress("unused")
    fun getLayerUUID(): String = layerUUID

    private val json: String
        get() = "{\"$WINDOW_DATA_KEY\":{\"title\":\"$title\"" +
                (message?.let { ",\"message\":\"${it.replace("\\", "\\\\").replace("\"", "\\\"")}\"" } ?: "") +
                "}}"
    fun data(): JsonElement =
        try {
            Timber.d("Parsing: $json")
            JsonParser.parseString(json)
        } catch (e: com.google.gson.stream.MalformedJsonException) {
            Timber.e(e, "Could not parse JSON. Source: $json")
            throw e
        }
}
