package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import android.os.Parcel
import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.async.EXTENDED_API_URL
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.*
import com.arnyminerz.escalaralcoiaicomtat.generic.jsonFromUrl
import com.mapbox.mapboxsdk.geometry.LatLng
import org.json.JSONObject
import timber.log.Timber
import java.util.*

@Suppress("UNCHECKED_CAST")
@ExperimentalUnsignedTypes
data class Zone(
    override val id: Int,
    override val version: Int,
    override val displayName: String,
    override val timestamp: Date?,
    val image: String,
    override val kmlAddress: String?,
    override val parentId: Int,
    val position: LatLng?,
    private val downloaded: Boolean = false
) : DataClass<Sector, Area>(
    id,
    version,
    displayName,
    timestamp,
    image,
    kmlAddress,
    R.drawable.ic_tall_placeholder,
    R.drawable.ic_tall_placeholder,
    parentId,
    NAMESPACE
) {
    val transitionName = id.toString() + displayName.replace(" ", "_")

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString().toTimestamp(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readInt(),
        LatLng(parcel.readDouble(), parcel.readDouble())
    ) {
        parcel.readList(children, Sector::class.java.classLoader)
    }

    constructor(json: JSONObject) : this(
        json.getInt("id"),
        json.getInt("version", 0),
        json.getString("display_name"),
        json.getTimestampSafe("timestamp"),
        json.getString("image"),
        json.getStringSafe("kml_address"),
        json.getInt("area_id"),
        json.getLatLngSafe("location")
    ) {
        val zone = fromDB(json)
        this.children.addAll(zone.children)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(version)
        parcel.writeString(displayName)
        parcel.writeString(timestamp?.let { TIMESTAMP_FORMAT.format(timestamp) })
        parcel.writeString(image)
        parcel.writeString(kmlAddress)
        parcel.writeInt(parentId)
        parcel.writeDouble(position?.latitude ?: 0.0)
        parcel.writeDouble(position?.longitude ?: 0.0)
        parcel.writeList(children)
    }

    override fun describeContents(): Int = 0

    fun addSector(sector: Sector) {
        children.add(sector)
    }

    companion object CREATOR : Parcelable.Creator<Zone> {
        override fun createFromParcel(parcel: Parcel): Zone = Zone(parcel)
        override fun newArray(size: Int): Array<Zone?> = arrayOfNulls(size)

        fun fromDB(json: JSONObject): Zone {
            val zone = Zone(
                json.getInt("id"),
                json.getInt("version", 0),
                json.getString("display_name"),
                json.getTimestampSafe("timestamp"),
                json.getString("image"),
                json.getString("kml_address"),
                json.getInt("area_id"),
                json.getLatLngSafe("location")
            )
            if (json.has("sectors")) {
                val sectors = json.getJSONArray("sectors")
                Timber.v("Zone has sectors, adding them. Count: ${sectors.length()}")
                sectors.sort("weigth") // Sort the list with the key "weigth"
                for (z in 0 until sectors.length()) {
                    val sector = sectors.getJSONObject(z)
                    zone.addSector(Sector.fromDB(sector))
                }
            }
            return zone
        }

        fun fromId(id: Int): Zone {
            val json = jsonFromUrl("$EXTENDED_API_URL/zone/$id")

            return fromDB(json)
        }

        const val NAMESPACE = "zone"
    }
}
