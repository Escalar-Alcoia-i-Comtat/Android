package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.os.Parcel
import android.os.Parcelable
import android.util.MalformedJsonException
import com.arnyminerz.escalaralcoiaicomtat.generic.generateUUID
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import idroid.android.mapskit.model.CommonMarker
import timber.log.Timber
import java.io.InvalidObjectException

fun load(symbol: CommonMarker): MapObjectWindowData {
    // TODO: Tag is not getting stored
    val json =
        symbol.tag as? JsonObject ?: throw InvalidObjectException("Symbol doesn't have any data")
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

data class MapObjectWindowData(
    var title: String,
    var message: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()
    )

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

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(title)
        dest.writeString(message)
    }

    companion object CREATOR : Parcelable.Creator<MapObjectWindowData> {
        override fun createFromParcel(parcel: Parcel): MapObjectWindowData {
            return MapObjectWindowData(parcel)
        }

        override fun newArray(size: Int): Array<MapObjectWindowData?> {
            return arrayOfNulls(size)
        }
    }
}
