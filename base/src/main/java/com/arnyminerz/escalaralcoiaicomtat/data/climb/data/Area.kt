package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.async.EXTENDED_API_URL
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.*
import com.arnyminerz.escalaralcoiaicomtat.generic.jsonArrayFromFile
import com.arnyminerz.escalaralcoiaicomtat.generic.jsonFromUrl
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.Serializable
import java.util.*

/**
 * Loads all the areas available in the server.
 * @param context The context to call from
 * @return A collection of areas
 */
@ExperimentalUnsignedTypes
fun loadAreasFromCache(context: Context): Collection<Area> {
    val areas = arrayListOf<Area>()

    val storageDataDir = context.filesDir
    val areasDataFile = File(storageDataDir, "cache.json")
    if (!areasDataFile.exists())
        Timber.e("Areas data file doesn't exist!")
    else {
        Timber.d("Loading areas from JSON file...")

        val areasJSON = jsonArrayFromFile(areasDataFile).sort("display_name")
        val toLoad = areasJSON.length()
        Timber.d("  Will load $toLoad areas.")
        if (toLoad > 0)
            for (a in 0 until toLoad) {
                val json = areasJSON.getJSONObject(a)
                val area = Area.fromDB(json)
                Timber.d("  Adding area $a")
                areas.add(area)
            }
    }

    return areas
}

@Suppress("UNCHECKED_CAST")
@ExperimentalUnsignedTypes
data class Area(
    override val id: Int,
    override val version: Int,
    override val displayName: String,
    override val timestamp: Date?,
    val image: String,
    override val kmlAddress: String?,
    override val parentId: Int,
    private val downloaded: Boolean = false
) : DataClass<Zone, Serializable>(
    id,
    version,
    displayName,
    timestamp,
    image,
    kmlAddress,
    R.drawable.ic_wide_placeholder,
    R.drawable.ic_wide_placeholder,
    parentId,
    NAMESPACE
) {
    val transitionName
        get() = id.toString() + displayName.replace(" ", "_")

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString().toTimestamp(),
        parcel.readString()!!,
        parcel.readString(),
        parcel.readInt()
    ) {
        parcel.readList(children, Zone::class.java.classLoader)
    }

    constructor(json: JSONObject) : this(
        json.getInt("id"),
        json.getInt("version", 0),
        json.getString("display_name"),
        json.getTimestampSafe("timestamp"),
        json.getString("image"),
        json.getStringSafe("kml_address"),
        -1
    ) {
        val area = fromDB(json)
        this.children.addAll(area.children)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(version)
        parcel.writeString(displayName)
        parcel.writeString(timestamp?.let { TIMESTAMP_FORMAT.format(timestamp) })
        parcel.writeString(image)
        parcel.writeString(kmlAddress)
        parcel.writeInt(parentId)
        parcel.writeList(children)
    }

    override fun describeContents(): Int = 0

    fun addZone(zone: Zone) = children.add(zone)

    companion object CREATOR : Parcelable.Creator<Area> {
        override fun createFromParcel(parcel: Parcel): Area = Area(parcel)
        override fun newArray(size: Int): Array<Area?> = arrayOfNulls(size)

        fun fromDB(json: JSONObject): Area {
            val area = Area(
                json.getInt("id"),
                json.getInt("version", 0),
                json.getString("display_name"),
                null,
                json.getString("image"),
                json.getString("kml_address"),
                -1
            )
            if (json.has("zones")) {
                val zones = json.getJSONArray("zones")
                Timber.v("Area has zones, adding them. Count: ${zones.length()}")
                for (z in 0 until zones.length()) {
                    val zone = zones.getJSONObject(z)
                    area.addZone(Zone.fromDB(zone))
                }
            }
            return area
        }

        fun fromId(id: Int): Area {
            val json = jsonFromUrl("$EXTENDED_API_URL/area/$id")

            return fromDB(json)
        }

        const val NAMESPACE = "area"
    }
}

@ExperimentalUnsignedTypes
fun Collection<Area>.getZones(): ArrayList<Zone> {
    val zones = arrayListOf<Zone>()

    for (area in this)
        zones.addAll(area.children)

    return zones
}
