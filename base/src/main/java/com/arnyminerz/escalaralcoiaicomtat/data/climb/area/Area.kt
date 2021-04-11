package com.arnyminerz.escalaralcoiaicomtat.data.climb.area

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.TIMESTAMP_FORMAT
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toTimestamp
import com.arnyminerz.escalaralcoiaicomtat.generic.fixTildes
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import timber.log.Timber
import java.util.Date

class Area(
    objectId: String,
    displayName: String,
    timestamp: Date?,
    val image: String,
    kmlAddress: String?,
    private val downloaded: Boolean = false,
    documentPath: String,
) : DataClass<Zone, DataClassImpl>(
    objectId,
    displayName,
    timestamp,
    image,
    kmlAddress,
    R.drawable.ic_wide_placeholder,
    R.drawable.ic_wide_placeholder,
    NAMESPACE,
    documentPath
) {
    val transitionName
        get() = objectId + displayName.replace(" ", "_")

    @WorkerThread
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString().toTimestamp(),
        parcel.readString()!!,
        parcel.readString(),
        parcel.readInt() == 1,
        parcel.readString()!!
    ) {
        parcel.readList(getChildren(), Zone::class.java.classLoader)
    }

    /**
     * Creates a new [Area] from the data of a [DocumentSnapshot].
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
        data.getString("kmlAddress")!!.fixTildes(),
        documentPath = data.reference.path
    )

    /**
     * Loads the [Area]s's children [Zone]s
     * @author Arnau Mora
     * @since 20210411
     * @return The loaded [Zone] list
     * @see Zone
     */
    @WorkerThread
    override fun loadChildren(): List<Zone> {
        Timber.v("Loading Area's children.")
        val result = arrayListOf<Zone>()

        Timber.d("Getting Firestore Instance...")
        val firebaseDatabase = Firebase.firestore

        Timber.d("Fetching...")
        val ref = firebaseDatabase
            .document(documentPath)
            .collection("Zones")
        val childTask = ref.get()
        Timber.v("Awaiting results...")
        Tasks.await(childTask)
        Timber.v("Got children result")
        val snapshot = childTask.result
        val e = childTask.exception
        if (!childTask.isSuccessful || snapshot == null) {
            Timber.w(e, "Could not get.")
            e?.let { throw it }
        } else {
            val zones = snapshot.documents
            Timber.d("Got ${zones.size} elements. Processing result")
            for (l in zones.indices) {
                val zoneData = zones[l]
                Timber.d("Processing zone #$l")
                val zone = Zone(zoneData)
                result.add(zone)
            }
            Timber.d("Finished loading zones")
        }
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(objectId)
        parcel.writeString(displayName)
        parcel.writeString(timestamp?.let { TIMESTAMP_FORMAT.format(timestamp) })
        parcel.writeString(image)
        parcel.writeString(kmlAddress)
        parcel.writeInt(if (downloaded) 1 else 0)
        parcel.writeString(documentPath)
        parcel.writeList(innerChildren)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Area> {
        override fun createFromParcel(parcel: Parcel): Area = Area(parcel)
        override fun newArray(size: Int): Array<Area?> = arrayOfNulls(size)

        const val NAMESPACE = "Area"
    }
}

/**
 * Gets the [Zone]s list from an [Area]s [Map].
 * Must be ran async.
 * @author Arnau Mora
 * @since 20210411
 */
@WorkerThread
fun Map<String, Area>.getZones(): List<Zone> {
    val zones = arrayListOf<Zone>()

    for (area in values)
        zones.addAll(area.getChildren())

    return zones
}
