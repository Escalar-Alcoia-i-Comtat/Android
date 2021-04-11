package com.arnyminerz.escalaralcoiaicomtat.data.climb.zone

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.TIMESTAMP_FORMAT
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toLatLng
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toTimestamp
import com.arnyminerz.escalaralcoiaicomtat.generic.fixTildes
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.mapboxsdk.geometry.LatLng
import timber.log.Timber
import java.util.Date

@Suppress("UNCHECKED_CAST")
class Zone(
    objectId: String,
    displayName: String,
    timestamp: Date?,
    val image: String,
    kmlAddress: String?,
    val position: LatLng?,
    private val downloaded: Boolean = false,
    documentPath: String
) : DataClass<Sector, Area>(
    objectId,
    displayName,
    timestamp,
    image,
    kmlAddress,
    R.drawable.ic_tall_placeholder,
    R.drawable.ic_tall_placeholder,
    NAMESPACE,
    documentPath
) {
    val transitionName = objectId + displayName.replace(" ", "_")

    @WorkerThread
    private constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString().toTimestamp(),
        parcel.readString()!!,
        parcel.readString()!!,
        LatLng(parcel.readDouble(), parcel.readDouble()),
        parcel.readInt() == 1,
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
        data.getString("displayName")!!.fixTildes(),
        data.getDate("created"),
        data.getString("image")!!.fixTildes(),
        data.getString("kmlAddress")?.fixTildes(),
        data.getGeoPoint("location")?.toLatLng(),
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
    override fun loadChildren(firestore: FirebaseFirestore): List<Sector> {
        val result = arrayListOf<Sector>()

        Timber.d("Fetching...")
        val ref = firestore
            .document(documentPath)
            .collection("Sectors")
        val childTask = ref.get()
        Tasks.await(childTask)
        val snapshot = childTask.result
        val e = childTask.exception
        if (!childTask.isSuccessful || snapshot == null) {
            Timber.w(e, "Could not get.")
            e?.let { throw it }
        } else {
            val sectors = snapshot.documents
            Timber.d("Got ${sectors.size} elements.")
            for (l in sectors.indices)
                result.add(Sector(sectors[l]))
        }
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(objectId)
        parcel.writeString(displayName)
        parcel.writeString(timestamp?.let { TIMESTAMP_FORMAT.format(timestamp) })
        parcel.writeString(image)
        parcel.writeString(kmlAddress)
        parcel.writeDouble(position?.latitude ?: 0.0)
        parcel.writeDouble(position?.longitude ?: 0.0)
        parcel.writeInt(if (downloaded) 1 else 0)
        parcel.writeString(documentPath)
        parcel.writeList(innerChildren)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Zone> {
        override fun createFromParcel(parcel: Parcel): Zone = Zone(parcel)
        override fun newArray(size: Int): Array<Zone?> = arrayOfNulls(size)

        const val NAMESPACE = "Zone"
    }
}
