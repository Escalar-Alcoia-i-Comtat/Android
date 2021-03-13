package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.MainThread
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.AREAS
import com.arnyminerz.escalaralcoiaicomtat.data.climb.types.EndingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.types.Grade
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.TIMESTAMP_FORMAT
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toTimestamp
import com.arnyminerz.escalaralcoiaicomtat.generic.fixTildes
import com.parse.*
import timber.log.Timber
import java.util.*

private const val DEBUG = false

const val DATA_FIX_LABEL = "climbData"
const val PATHS_BATCH_SIZE = 1000

private fun find(list: List<DataClassImpl>, objectId: String): Int {
    for ((i, item) in list.withIndex())
        if (item.objectId == objectId)
            return i
    return -1
}

private fun log(msg: String, vararg arguments: Any) =
    if (DEBUG)
        Timber.d(msg, arguments)
    else null

/**
 * Loads all the areas available in the server.
 * @author Arnau Mora
 * @since 20210313
 * @see PATHS_BATCH_SIZE
 * @return A collection of areas
 * @throws ParseException If there's an error while fetching from parse
 */
@MainThread
@Throws(ParseException::class)
fun loadAreasFromCache(progressCallback: (current: Int, total: Int) -> Unit, callback: () -> Unit) {
    Timber.d("Querying paths...")
    val query = ParseQuery.getQuery<ParseObject>("Path")
    query.addAscendingOrder("sketchId")

    // Data will be fetched in packs of PATHS_BATCH_SIZE
    AREAS.clear()
    query.limit = PATHS_BATCH_SIZE
    query.fromLocalDatastore().findInBackground().continueWithTask { task ->
        val error = task.error
        val objects = task.result
        if (error != null) {
            if (error is ParseException && error.code == ParseException.CACHE_MISS) {
                Timber.w("No stored data found. Fetching from network.")
                return@continueWithTask query.fromNetwork().findInBackground()
            } else Timber.e(error, "Could not fetch data.")
        }
        if (objects.size <= 0) {
            Timber.w("No stored data found. Fetching from network.")
            return@continueWithTask query.fromNetwork().findInBackground()
        }
        return@continueWithTask task
    }.continueWithTask { task ->
        val objects = task.result
        Timber.d("Got ${objects.size} paths. Processing...")
        log("Calling callback with progress 0")
        progressCallback(0, objects.size)
        log("Iterating paths...")
        for ((p, pathData) in objects.withIndex()) {
            log("Loading height list...")
            val heights = arrayListOf<Int>()
            heights.addAll(pathData.getList("height")!!)

            log("Loading grades list...")
            val gradeValue = pathData.getString("grade")!!.fixTildes()
            val gradeValues = gradeValue.split(" ")
            val grades = Grade.listFromStrings(gradeValues)

            log("Loading ending list...")
            val endings = arrayListOf<EndingType>()
            val endingsList = pathData.getList<ParseObject>("ending")
            if (endingsList != null)
                for (e in endingsList) {
                    val ending = e.fetchIfNeeded<ParseObject>()
                    val endingName = ending.getString("name")?.fixTildes()
                    val endingType = EndingType.find(endingName)
                    endings.add(endingType)
                }

            log("Loading pitches list...")
            val pitches = arrayListOf<Pitch>()
            val endingArtifo = pathData.getString("endingArtifo")?.fixTildes()
            endingArtifo?.let {
                val artifos = it.replace("\r", "").split("\n")
                for (artifo in artifos)
                    Pitch.fromEndingDataString(artifo)
                        ?.let { artifoEnding -> pitches.add(artifoEnding) }
            }

            log("Loading rebuilder...")
            val rebuiltBy = pathData.getList<String>("rebuiltBy")?.joinToString(separator = ", ")

            log("Loading data...")
            val objectId = pathData.objectId
            val updatedAt = pathData.updatedAt
            val sketchId = (pathData.getString("sketchId")?.fixTildes() ?: "0").toInt()
            val displayName = pathData.getString("displayName")!!.fixTildes()
            val description = pathData.getString("description")?.fixTildes()
            val builtBy = pathData.getString("builtBy")?.fixTildes()

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

            log("Instantiating path...")
            val path = Path(
                objectId, updatedAt, sketchId, displayName, grades, heights,
                endings, pitches, fixedSafesData, requiredSafesData,
                description, builtBy, rebuiltBy
            )

            log("Fetching data...")
            val sectorData = pathData.getParseObject("sector")?.fetchIfNeeded<ParseObject>()!!
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
            log("Building areas...")
            when {
                areaTarget == null -> AREAS[areaId] = Area(areaData)
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

            progressCallback(p, objects.size)
        }
        Timber.v("Pinning...")
        ParseObject.pinAllInBackground(objects) { error ->
            if (error != null)
                Timber.w(error, "Could not pin data!")
            else {
                Timber.v("Pinned data.")
                callback()
            }
        }
        task
    }
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
        parseObject.getString("displayName")!!.fixTildes(),
        parseObject.updatedAt,
        parseObject.getString("image")!!.fixTildes(),
        parseObject.getString("kmlAddress")?.fixTildes()
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

fun Map<String, Area>.getZones(): ArrayList<Zone> {
    val zones = arrayListOf<Zone>()

    for (area in values)
        zones.addAll(area.children)

    return zones
}
