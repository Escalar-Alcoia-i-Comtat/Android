package com.arnyminerz.escalaralcoiaicomtat.data.climb.area

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClassMetadata
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.UIMetadata
import com.arnyminerz.escalaralcoiaicomtat.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.TIMESTAMP_FORMAT
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toTimestamp
import com.arnyminerz.escalaralcoiaicomtat.generic.fixTildes
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Creates a new Area instance.
 * @author Arnau Mora
 * @since 20210416
 * @param objectId The id of the object
 * @param displayName The Area's display name
 * @param timestamp The update date of the Area
 * @param kmzReferenceUrl The reference url from Firebase Storage for the Area's KMZ file
 * @param documentPath The path in Firebase Firestore of the Area
 */
class Area(
    objectId: String,
    displayName: String,
    timestamp: Date,
    image: String,
    kmzReferenceUrl: String,
    documentPath: String,
) : DataClass<Zone, DataClassImpl>(
    displayName,
    timestamp,
    image,
    kmzReferenceUrl,
    UIMetadata(
        R.drawable.ic_wide_placeholder,
        R.drawable.ic_wide_placeholder,
    ),
    DataClassMetadata(
        objectId,
        NAMESPACE,
        documentPath
    )
) {
    val transitionName
        get() = objectId + displayName.replace(" ", "_")

    @WorkerThread
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString().toTimestamp()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!
    ) {
        parcel.readList(innerChildren, Zone::class.java.classLoader)
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
        data.getDate("created")!!,
        data.getString("image")!!.fixTildes(),
        data.getString("kmz")!!,
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
    override suspend fun loadChildren(firestore: FirebaseFirestore): Flow<Zone> = flow {
        Timber.v("Loading Area's children.")

        Timber.d("Fetching...")
        val ref = firestore
            .document(metadata.documentPath)
            .collection("Zones")
            .orderBy("displayName")
        val childTask = ref.get()
        try {
            Timber.v("Awaiting results...")
            val snapshot = suspendCoroutine<QuerySnapshot> { cont ->
                childTask
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            Timber.v("Got children result")
            val zones = snapshot.documents
            Timber.d("Got ${zones.size} elements. Processing result")
            for (l in zones.indices) {
                val zoneData = zones[l]
                Timber.d("Processing zone #$l")
                val zone = Zone(zoneData)
                emit(zone)
            }
            Timber.d("Finished loading zones")
        } catch (e: Exception) {
            Timber.w(e, "Could not get.")
            e.let { throw it }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(objectId)
        parcel.writeString(displayName)
        parcel.writeString(TIMESTAMP_FORMAT.format(timestamp))
        parcel.writeString(imageUrl)
        parcel.writeString(kmzReferenceUrl)
        parcel.writeString(metadata.documentPath)
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
 * Gets the [Zone]s list from an [Area]s [Iterable].
 * Must be ran async.
 * @author Arnau Mora
 * @since 20210411
 */
@WorkerThread
suspend fun Iterable<Area>.getZones(firestore: FirebaseFirestore): Flow<Zone> = flow {
    for (area in this@getZones)
        emitAll(area.getChildren(firestore))
}

/**
 * Checks if an [Area] list contains an [Area] with an specific id.
 * @author Arnau Mora
 * @since 20210413
 * @param areaId The id to search
 */
fun Iterable<Area>.has(areaId: String): Boolean {
    for (area in this)
        if (area.objectId == areaId)
            return true
    return false
}

/**
 * Finds an [Area] inside a list with an specific id. If it's not found, a [ClassNotFoundException]
 * is thrown.
 * @author Arnau Mora
 * @since 20210413
 * @param areaId The id to search
 * @throws ClassNotFoundException If the [areaId] doesn't exist in [this]
 */
@Throws(ClassNotFoundException::class)
fun Iterable<Area>.ensureGet(areaId: String): Area {
    for (area in this)
        if (area.objectId == areaId)
            return area
    throw ClassNotFoundException("Could not find an area with id $areaId")
}

/**
 * Finds an [Area] inside a list with an specific id. If it's not found, null is returned.
 * @author Arnau Mora
 * @since 20210413
 * @param areaId The id to search
 */
operator fun Iterable<Area>.get(areaId: String): Area? {
    for (area in this)
        if (area.objectId == areaId)
            return area
    return null
}
