package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.AREAS
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.TIMESTAMP_FORMAT
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toTimestamp
import com.parse.ParseObject
import com.parse.ParseQuery
import java.util.*

/**
 * Loads all the areas available in the server.
 * @return A collection of areas
 */
@WorkerThread
@ExperimentalUnsignedTypes
fun loadAreasFromCache(): Collection<Area> {
    val areas = arrayListOf<Area>()

    val query = ParseQuery.getQuery<ParseObject>("Path")
    query.addAscendingOrder("sector")
    query.addAscendingOrder("sketchId")
    val results = query.find()

    AREAS.clear()
    for (pathData in results) {

    }

    return areas
}

@Suppress("UNCHECKED_CAST")
@ExperimentalUnsignedTypes
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
        parseObject.getString("objectId")!!,
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
fun Collection<Area>.getZones(): ArrayList<Zone> {
    val zones = arrayListOf<Zone>()

    for (area in this)
        zones.addAll(area.children)

    return zones
}
