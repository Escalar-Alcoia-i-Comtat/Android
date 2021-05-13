package com.arnyminerz.escalaralcoiaicomtat.data.climb.zone

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClassMetadata
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.UIMetadata
import com.arnyminerz.escalaralcoiaicomtat.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.TIMESTAMP_FORMAT
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toLatLng
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toTimestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.mapbox.mapboxsdk.geometry.LatLng
import timber.log.Timber
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class Zone(
    objectId: String,
    displayName: String,
    timestamp: Date,
    val image: String,
    kmzReferenceUrl: String,
    val position: LatLng,
    documentPath: String
) : DataClass<Sector, Area>(
    displayName,
    timestamp,
    image,
    kmzReferenceUrl,
    UIMetadata(
        R.drawable.ic_tall_placeholder,
        R.drawable.ic_tall_placeholder,
    ),
    DataClassMetadata(
        objectId,
        NAMESPACE,
        documentPath
    )
) {
    @WorkerThread
    private constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString().toTimestamp()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        LatLng(parcel.readDouble(), parcel.readDouble()),
        parcel.readString()!!
    ) {
        parcel.readList(innerChildren, Sector::class.java.classLoader)
    }

    /**
     * Creates a new [Zone] from the data of a [DocumentSnapshot].
     * Note: This doesn't add children
     * @author Arnau Mora
     * @since 20210411
     * @param data The object to get data from
     */
    constructor(data: DocumentSnapshot) : this(
        data.id,
        data.getString("displayName")!!,
        data.getDate("created")!!,
        data.getString("image")!!,
        data.getString("kmz")!!,
        data.getGeoPoint("location")!!.toLatLng(),
        documentPath = data.reference.path
    )

    /**
     * Loads the [Zone]s's children [Sector]s
     * @author Arnau Mora
     * @since 20210411
     * @return The loaded [Sector] list
     * @see Sector
     */
    @WorkerThread
    override suspend fun loadChildren(firestore: FirebaseFirestore): List<Sector> {
        val sectors = arrayListOf<Sector>()
        Timber.v("Loading Zone's children.")

        Timber.d("Fetching...")
        val ref = firestore
            .document(metadata.documentPath)
            .collection("Sectors")
            .orderBy("weight")
        val childTask = ref.get()
        try {
            Timber.v("Awaiting results...")
            val snapshot = suspendCoroutine<QuerySnapshot> { cont ->
                childTask
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            Timber.v("Got children result")
            val sectorsDocs = snapshot.documents
            Timber.d("Got ${sectorsDocs.size} elements. Processing result")
            for (l in sectorsDocs.indices) {
                val sectorData = sectorsDocs[l]
                Timber.d("Processing sector #$l")
                val sector = Sector(sectorData)
                sectors.add(sector)
            }
            Timber.d("Finished loading sectors")
        } catch (e: Exception) {
            Timber.w(e, "Could not get.")
            e.let { throw it }
        }
        return sectors
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(objectId)
        parcel.writeString(displayName)
        parcel.writeString(TIMESTAMP_FORMAT.format(timestamp))
        parcel.writeString(image)
        parcel.writeString(kmzReferenceUrl)
        parcel.writeDouble(position.latitude)
        parcel.writeDouble(position.longitude)
        parcel.writeString(metadata.documentPath)
        parcel.writeList(innerChildren)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Zone> {
        override fun createFromParcel(parcel: Parcel): Zone = Zone(parcel)
        override fun newArray(size: Int): Array<Zone?> = arrayOfNulls(size)

        const val NAMESPACE = "Zone"
    }
}

operator fun Collection<Zone>.get(id: String): Zone? {
    for (zone in this)
        if (zone.objectId == id)
            return zone
    return null
}
