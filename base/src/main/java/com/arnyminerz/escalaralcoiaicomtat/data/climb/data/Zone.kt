package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import android.os.Parcel
import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.TIMESTAMP_FORMAT
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toLatLng
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toTimestamp
import com.arnyminerz.escalaralcoiaicomtat.generic.fixTildes
import com.mapbox.mapboxsdk.geometry.LatLng
import com.parse.ParseObject
import java.util.*

@Suppress("UNCHECKED_CAST")
data class Zone(
    override val objectId: String,
    override val displayName: String,
    override val timestamp: Date?,
    val image: String,
    override val kmlAddress: String?,
    val position: LatLng?,
    private val downloaded: Boolean = false
) : DataClass<Sector, Area>(
    objectId,
    displayName,
    timestamp,
    image,
    kmlAddress,
    R.drawable.ic_tall_placeholder,
    R.drawable.ic_tall_placeholder,
    NAMESPACE
) {
    val transitionName = objectId + displayName.replace(" ", "_")

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString().toTimestamp(),
        parcel.readString()!!,
        parcel.readString()!!,
        LatLng(parcel.readDouble(), parcel.readDouble())
    ) {
        parcel.readList(children, Sector::class.java.classLoader)
    }

    /**
     * Creates a new zone from the data of a ParseObject.
     * Note: This doesn't add children
     * @author Arnau Mora
     * @since 20210312
     * @param parseObject The object to get data from. It must be of class Zone
     * @see ParseObject
     */
    constructor(parseObject: ParseObject) : this(
        parseObject.objectId,
        parseObject.getString("displayName")!!.fixTildes(),
        parseObject.updatedAt,
        parseObject.getString("image")!!.fixTildes(),
        parseObject.getString("kmlAddress")!!.fixTildes(),
        parseObject.getParseGeoPoint("location").toLatLng()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(objectId)
        parcel.writeString(displayName)
        parcel.writeString(timestamp?.let { TIMESTAMP_FORMAT.format(timestamp) })
        parcel.writeString(image)
        parcel.writeString(kmlAddress)
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

        const val NAMESPACE = "Zone"
    }
}
