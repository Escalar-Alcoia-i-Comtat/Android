package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.AREAS
import com.arnyminerz.escalaralcoiaicomtat.appNetworkState
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
    val query = ParseQuery.getQuery<ParseObject>("Area")
    query.addAscendingOrder("displayName")
    AREAS.clear()
    query.limit = PATHS_BATCH_SIZE
    query.fetchPinOrNetwork(DATA_FIX_LABEL) { objects, error ->
        if (error != null) {
            Timber.e(error, "Could not fetch")
            return@fetchPinOrNetwork
        }

        Timber.d("Got ${objects.size} areas. Processing...")
        progressCallback(0, objects.size)
        for ((a, areaData) in objects.withIndex()) {
            val area = Area(areaData)
            AREAS[area.objectId] = area
            progressCallback(a, objects.size)
        }

        Timber.v("Pinning...")
        progressCallback(0, -1)
        ParseObject.pinAllInBackground(DATA_FIX_LABEL, objects) { err ->
            if (err != null)
                Timber.w(err, "Could not pin data!")
            else {
                Timber.v("Pinned data.")
                callback()
            }
        }
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
    NAMESPACE,
    Zone.NAMESPACE
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

    @WorkerThread
    override fun loadChildren(): List<Zone> {
        val key = namespace.toLowerCase(Locale.getDefault())
        Timber.d("Loading elements from \"$childrenNamespace\", where $key=$objectId")

        val parentQuery = ParseQuery.getQuery<ParseObject>(namespace)
        parentQuery.whereEqualTo("objectId", objectId)

        val query = ParseQuery.getQuery<ParseObject>(childrenNamespace)
        query.addAscendingOrder("displayName")
        query.whereMatchesQuery(key, parentQuery)

        val loads = query.fetchPinOrNetwork(appNetworkState, pin, true)
        Timber.d("Got ${loads.size} elements.")
        val result = arrayListOf<Zone>()
        for (load in loads)
            result.add(Zone(load))
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(objectId)
        parcel.writeString(displayName)
        parcel.writeString(timestamp?.let { TIMESTAMP_FORMAT.format(timestamp) })
        parcel.writeString(image)
        parcel.writeString(kmlAddress)
        parcel.writeList(innerChildren)
    }

    override fun describeContents(): Int = 0

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
