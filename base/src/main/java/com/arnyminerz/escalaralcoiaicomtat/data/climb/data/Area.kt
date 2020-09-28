package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.activity.EXTRA_SECTOR
import com.arnyminerz.escalaralcoiaicomtat.activity.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.AreaActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.SectorActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.ZoneActivity
import com.arnyminerz.escalaralcoiaicomtat.async.EXTENDED_API_URL
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.*
import com.arnyminerz.escalaralcoiaicomtat.generic.jsonArrayFromFile
import com.arnyminerz.escalaralcoiaicomtat.generic.jsonFromUrl
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.Serializable
import java.util.*

/**
 * Loads all the areas available in the server.
 * @param context The context to call from
 * @return A flow of areas.
 */
@ExperimentalUnsignedTypes
fun loadAreas(context: Context): Collection<Area> {
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
                Timber.d("  Emitting area $a")
                areas.add(area)
            }
    }

    return areas
}

data class DataClassScanHeights(
    val areaIndex: Int? = null,
    val zoneIndex: Int? = null,
    val sectorIndex: Int? = null
) {
    @ExperimentalUnsignedTypes
    fun launchActivity(context: Context) {
        when {
            sectorIndex != null -> context.startActivity(
                Intent(context, SectorActivity::class.java).apply {
                    putExtra(EXTRA_AREA, areaIndex!!)
                    putExtra(EXTRA_ZONE, zoneIndex!!)
                    putExtra(EXTRA_SECTOR, sectorIndex)
                }
            )
            zoneIndex != null -> context.startActivity(
                Intent(context, ZoneActivity::class.java).apply {
                    putExtra(EXTRA_AREA, areaIndex!!)
                    putExtra(EXTRA_ZONE, zoneIndex)
                }
            )
            areaIndex != null -> context.startActivity(
                Intent(context, AreaActivity::class.java).apply {
                    putExtra(EXTRA_AREA, areaIndex)
                }
            )
            else -> Timber.e("Can't find valid context to launch scan result")
        }
    }
}

/**
 * This gets the heights indexes for a data class.
 * @author ArnyminerZ
 * @date 2020/08/31
 * @param dataClass The class to search for
 * @return A scan result with the height set
 */
@ExperimentalUnsignedTypes
fun Collection<Area>.find(dataClass: DataClass<*, *>?): DataClassScanHeights {
    if (dataClass != null)
        for ((a, area) in this.withIndex())
            if (area == dataClass)
                return DataClassScanHeights(a)
            else if (area.isNotEmpty())
                for ((z, zone) in area.withIndex())
                    if (zone == dataClass)
                        return DataClassScanHeights(a, z)
                    else if (zone.isNotEmpty())
                        for ((s, sector) in zone.withIndex())
                            if (sector == dataClass)
                                return DataClassScanHeights(a, z, s)

    return DataClassScanHeights()
}

@Suppress("UNCHECKED_CAST")
@ExperimentalUnsignedTypes
data class Area(
    override val id: Int,
    override val version: Int,
    override val displayName: String,
    override val timestamp: Date?,
    val image: String,
    val kmlAddress: String?,
    override val parentId: Int,
    private val downloaded: Boolean = false
) : DataClass<Zone, Serializable>(
    id,
    version,
    displayName,
    timestamp,
    image,
    R.drawable.ic_wide_placeholder,
    R.drawable.ic_wide_placeholder,
    parentId,
    "area"
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

        suspend fun fromId(id: Int): Area {
            val json = jsonFromUrl("$EXTENDED_API_URL/area/$id")

            return fromDB(json)
        }
    }
}

@ExperimentalUnsignedTypes
fun Collection<Area>.getZones(): ArrayList<Zone> {
    val zones = arrayListOf<Zone>()

    for (area in this)
        zones.addAll(area.children)

    return zones
}