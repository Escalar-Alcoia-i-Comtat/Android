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
import com.arnyminerz.escalaralcoiaicomtat.generic.awaitTask
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.TIMESTAMP_FORMAT
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toLatLng
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toTimestamp
import com.arnyminerz.escalaralcoiaicomtat.generic.fixTildes
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.util.Date

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
    val transitionName = objectId + displayName.replace(" ", "_")

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
        data.getString("displayName")!!.fixTildes(),
        data.getDate("created")!!,
        data.getString("image")!!.fixTildes(),
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
    override suspend fun loadChildren(firestore: FirebaseFirestore): Flow<Sector> = flow {
        Timber.d("Fetching...")
        val ref = firestore
            .document(metadata.documentPath)
            .collection("Sectors")
            .orderBy("weight")
        val childTask = ref.get()
        val snapshot = childTask.awaitTask()
        val e = childTask.exception
        if (!childTask.isSuccessful || snapshot == null) {
            Timber.w(e, "Could not get.")
            e?.let { throw it }
        } else {
            val sectors = snapshot.documents
            Timber.d("Got ${sectors.size} elements.")
            for (l in sectors.indices)
                emit(Sector(sectors[l]))
        }
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
