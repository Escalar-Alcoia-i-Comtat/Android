package com.arnyminerz.escalaralcoiaicomtat.core.data.map

import android.os.Parcel
import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.core.utils.generateUUID
import com.google.android.gms.maps.model.Marker
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import timber.log.Timber
import java.io.InvalidObjectException

/**
 * Extracts [MapObjectWindowData] from [symbol].
 * @author Arnau Mora
 * @since 20210602
 * @param symbol The symbol to extract from
 * @throws InvalidObjectException When [symbol] doesn't have any data (tag).
 */
fun load(symbol: Marker): MapObjectWindowData =
    symbol.tag as? MapObjectWindowData
        ?: throw InvalidObjectException("Symbol doesn't have any data")

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
