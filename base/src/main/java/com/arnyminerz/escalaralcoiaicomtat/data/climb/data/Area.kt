package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.AREAS
import com.arnyminerz.escalaralcoiaicomtat.data.climb.types.EndingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.types.Grade
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.TIMESTAMP_FORMAT
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toTimestamp
import com.parse.ParseObject
import com.parse.ParseQuery
import timber.log.Timber
import java.util.*

const val PATHS_BATCH_SIZE = 100

private fun find(list: List<DataClassImpl>, objectId: String): Int {
    for ((i, item) in list.withIndex())
        if (item.objectId == objectId)
            return i
    return -1
}

/**
 * Loads all the areas available in the server.
 * @see PATHS_BATCH_SIZE
 * @return A collection of areas
 */
@WorkerThread
fun loadAreasFromCache() {
    Timber.d("Querying paths...")
    val query = ParseQuery.getQuery<ParseObject>("Path")
    query.addAscendingOrder("sector")
    query.addAscendingOrder("sketchId")

    // Data will be fetched in packs of PATHS_BATCH_SIZE
    AREAS.clear()
    var results: List<ParseObject>
    query.limit = PATHS_BATCH_SIZE
    query.skip = -PATHS_BATCH_SIZE
    do {
        query.skip += PATHS_BATCH_SIZE
        results = query.find()

        Timber.d("Got ${results.size} paths. Processing...")
        for ((p, pathData) in results.withIndex()) {
            Timber.d("Initializing path #$p...")
            Timber.d("Initialized Path. Adding lists...")
            val heights = arrayListOf<Int>()
            heights.addAll(pathData.getList("height")!!)

            val gradeValue = pathData.getString("grade")!!
            val gradeValues = gradeValue.split(" ")
            val grades = Grade.listFromStrings(gradeValues)

            val endings = arrayListOf<EndingType>()
            val endingsList = pathData.getList<ParseObject>("ending")
            if (endingsList != null)
                for (e in endingsList) {
                    val ending = e.fetchIfNeeded<ParseObject>()
                    val endingName = ending.getString("name")
                    val endingType = EndingType.find(endingName)
                    endings.add(endingType)
                }

            val pitches = arrayListOf<Pitch>()
            val endingArtifo = pathData.getString("endingArtifo")
            endingArtifo?.let {
                val artifos = it.replace("\r", "").split("\n")
                for (artifo in artifos)
                    Pitch.fromEndingDataString(artifo)
                        ?.let { artifoEnding -> pitches.add(artifoEnding) }
            }

            val rebuiltBy = pathData.getList<String>("rebuiltBy")?.let {
                Timber.v("Path has rebuilt data")
                it.joinToString(separator = ", ")
            }

            val objectId = pathData.objectId
            val updatedAt = pathData.getDate("updatedAt")
            val sketchId = (pathData.getString("sketchId") ?: "0").toInt()
            val displayName = pathData.getString("displayName")!!
            val description = pathData.getString("description")
            val builtBy = pathData.getString("builtBy")

            val stringCount = pathData.getInt("stringCount")
            val paraboltCount = pathData.getInt("paraboltCount")
            val spitCount = pathData.getInt("spitCount")
            val tensorCount = pathData.getInt("tensorCount")
            val pitonCount = pathData.getInt("pitonCount")
            val burilCount = pathData.getInt("burilCount")
            val fixedSafesData = FixedSafesData(
                stringCount,
                paraboltCount,
                spitCount,
                tensorCount,
                pitonCount,
                burilCount
            )

            val lanyardRequired = pathData.getBoolean("lanyardRequired")
            val crackerRequired = pathData.getBoolean("crackerRequired")
            val friendRequired = pathData.getBoolean("friendRequired")
            val stripsRequired = pathData.getBoolean("stripsRequired")
            val pitonRequired = pathData.getBoolean("pitonRequired")
            val nailRequired = pathData.getBoolean("nailRequired")
            val requiredSafesData = RequiredSafesData(
                lanyardRequired,
                crackerRequired,
                friendRequired,
                stripsRequired,
                pitonRequired,
                nailRequired
            )

            val path = Path(
                objectId, updatedAt, sketchId, displayName, grades, heights,
                endings, pitches, fixedSafesData, requiredSafesData,
                description, builtBy, rebuiltBy
            )

            Timber.d("Building AREAS...")
            pathData.getParseObject("sector")?.fetchIfNeeded<ParseObject>()?.let { sectorData ->
                val sectorId = sectorData.objectId
                val zoneData = sectorData.getParseObject("zone")!!.fetchIfNeeded<ParseObject>()
                val zoneId = zoneData.objectId
                val areaData = zoneData.getParseObject("area")!!.fetchIfNeeded<ParseObject>()
                val areaId = areaData.objectId
                val areaTarget = AREAS[areaId]
                val zoneTarget = try {
                    areaTarget?.get(zoneId)
                } catch (_: IllegalStateException) {
                    null
                } catch (_: IndexOutOfBoundsException) {
                    null
                }
                val sectorTarget = try {
                    zoneTarget?.get(sectorId)
                } catch (_: IllegalStateException) {
                    null
                } catch (_: IndexOutOfBoundsException) {
                    null
                }
                when {
                    areaTarget == null -> {
                        AREAS[areaId] = Area(areaData)
                    }
                    zoneTarget == null -> {
                        AREAS[areaId]!!.addZone(Zone(zoneData))
                    }
                    sectorTarget == null -> {
                        AREAS[areaId]!![zoneId].addSector(Sector(sectorData))
                    }
                    else -> {
                        AREAS[areaId]!![zoneId!!][sectorId!!].children.add(path)
                    }
                }
            }
        }
    } while (results.size == PATHS_BATCH_SIZE)
}

@Suppress("UNCHECKED_CAST")
class Area(
    override val objectId: String,
    override val displayName: String,
    override val timestamp: Date?,
    val image: String,
    override val kmlAddress: String?,
    private val downloaded: Boolean = false
) : DataClass<Zone, DataClassImpl>(
    objectId,
    displayName,
    timestamp,
    image,
    kmlAddress,
    R.drawable.ic_wide_placeholder,
    R.drawable.ic_wide_placeholder,
    NAMESPACE
) {
    val transitionName
        get() = objectId + displayName.replace(" ", "_")

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString().toTimestamp(),
        parcel.readString()!!,
        parcel.readString()
    ) {
        parcel.readList(children, Zone::class.java.classLoader)
    }

    /**
     * Creates a new area from the data of a ParseObject.
     * Note: This doesn't add children
     * @author Arnau Mora
     * @since 20210312
     * @param parseObject The object to get data from. It must be of class Area
     * @see ParseObject
     */
    constructor(parseObject: ParseObject) : this(
        parseObject.objectId,
        parseObject.getString("displayName")!!,
        parseObject.getDate("updatedAt"),
        parseObject.getString("image")!!,
        parseObject.getString("kmlAddress")
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(objectId)
        parcel.writeString(displayName)
        parcel.writeString(timestamp?.let { TIMESTAMP_FORMAT.format(timestamp) })
        parcel.writeString(image)
        parcel.writeString(kmlAddress)
        parcel.writeList(children)
    }

    override fun describeContents(): Int = 0

    fun addZone(zone: Zone) = children.add(zone)

    companion object CREATOR : Parcelable.Creator<Area> {
        override fun createFromParcel(parcel: Parcel): Area = Area(parcel)
        override fun newArray(size: Int): Array<Area?> = arrayOfNulls(size)

        const val NAMESPACE = "Area"
    }
}

@ExperimentalUnsignedTypes
fun Map<String, Area>.getZones(): ArrayList<Zone> {
    val zones = arrayListOf<Zone>()

    for (area in values)
        zones.addAll(area.children)

    return zones
}
