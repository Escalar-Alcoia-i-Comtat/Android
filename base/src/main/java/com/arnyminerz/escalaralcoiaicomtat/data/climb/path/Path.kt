package com.arnyminerz.escalaralcoiaicomtat.data.climb.path

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.safes.FixedSafesData
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.safes.RequiredSafesData
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toTimestamp
import com.arnyminerz.escalaralcoiaicomtat.generic.fixTildes
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeUnit

class Path(
    override val objectId: String,
    val timestamp: Date?,
    val sketchId: Long,
    val displayName: String,
    val grades: Grade.GradesList,
    val heights: ArrayList<Long>,
    val endings: ArrayList<EndingType>,
    val pitches: ArrayList<Pitch>,
    val fixedSafesData: FixedSafesData,
    val requiredSafesData: RequiredSafesData,
    val description: String?,
    val builtBy: String?,
    rebuiltBy: String?,
    val downloaded: Boolean = false,
    val pointer: String,
) : DataClassImpl(objectId, NAMESPACE), Comparable<Path> {
    var rebuiltBy: String? = rebuiltBy
        private set

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString().toTimestamp(),
        parcel.readLong(),
        parcel.readString()!!,
        Grade.GradesList(),
        arrayListOf(),
        arrayListOf(),
        arrayListOf(),
        parcel.readParcelable<FixedSafesData>(FixedSafesData::class.java.classLoader)!!,
        parcel.readParcelable<RequiredSafesData>(RequiredSafesData::class.java.classLoader)!!,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readInt() == 1,
        parcel.readString()!!,
    )

    /**
     * Creates a new [Path] from the data of a [DocumentSnapshot].
     * @author Arnau Mora
     * @since 20210411
     * @param data The object to get data from
     */
    constructor(data: DocumentSnapshot) : this(
        data.id,
        data.getDate("created"),
        data.getString("sketchId")?.toLongOrNull() ?: 0L,
        data.getString("displayName")?.fixTildes() ?: "",
        Grade.GradesList(),
        arrayListOf(),
        arrayListOf(),
        arrayListOf(),
        FixedSafesData(
            data.getLong("stringCount") ?: 0,
            data.getLong("paraboltCount") ?: 0,
            data.getLong("spitCount") ?: 0,
            data.getLong("tensorCount") ?: 0,
            data.getLong("pitonCount") ?: 0,
            data.getLong("burilCount") ?: 0,
        ),
        RequiredSafesData(
            data.getBoolean("lanyardRequired") ?: false,
            data.getBoolean("crackerRequired") ?: false,
            data.getBoolean("friendRequired") ?: false,
            data.getBoolean("stripsRequired") ?: false,
            data.getBoolean("pitonRequired") ?: false,
            data.getBoolean("nailRequired") ?: false,
        ),
        data.getString("description")?.fixTildes(),
        data.getString("builtBy")?.fixTildes(),
        "",
        pointer = data.reference.path
    ) {
        val pathData = data.data

        Timber.d("Loading heights for Path $objectId")
        val heights = pathData?.get("height") as List<*>?
        if (heights != null)
            for (h in heights.indices)
                this.heights.add((heights[h].toString()).toLong())
        else Timber.w("Heights is null")

        Timber.d("Loading grade for Path $objectId")
        val gradeValue = data.getString("grade")!!.fixTildes()
        val gradeValues = gradeValue.split(" ")
        grades.addAll(Grade.listFromStrings(gradeValues))

        Timber.d("Loading endings for Path $objectId")
        val endingsList = pathData?.get("ending") as List<*>?
        if (endingsList != null)
            for (e in endingsList.indices) {
                val ending = endingsList[e].toString()
                val endingType = EndingType.find(ending)
                endings.add(endingType)
            }
        else Timber.w("Endings list is null")

        Timber.d("Loading artifo endings for Path $objectId")
        val endingArtifo = data.getString("ending_artifo")?.fixTildes()
        endingArtifo?.let {
            val artifos = it.replace("\r", "").split("\n")
            for (artifo in artifos)
                Pitch.fromEndingDataString(artifo)
                    ?.let { artifoEnding -> pitches.add(artifoEnding) }
        }

        Timber.d("Loading rebuilders...")
        val rebuilders = pathData?.get("rebuiltBy") as List<*>?
        val d = rebuilders?.joinToString(separator = ", ")
        rebuiltBy = d
    }

    /**
     * Checks if the Path has safe count. This doesn't include the safe types marked with a 1, since
     * the 1 is used to mark as "undetermined amount".
     * @author Arnau Mora
     * @since 20210316
     * @return If the path has safes count.
     */
    fun hasSafeCount(): Boolean = fixedSafesData.hasSafeCount()

    override fun toString(): String = displayName

    override fun compareTo(other: Path): Int =
        when {
            sketchId > other.sketchId -> 1
            sketchId < other.sketchId -> -1
            else -> 0
        }

    /**
     * Checks if the path is blocked or not
     * @author Arnau Mora
     * @since 20210316
     * @return A matching BlockingType class
     */
    @WorkerThread
    fun isBlocked(): BlockingType {
        Timber.d("Getting FirebaseDatabase Instance...")
        val firebaseDatabase = Firebase.firestore

        Timber.d("Fetching...")
        val ref = firebaseDatabase.document(pointer)

        Timber.v("Checking if $pointer is blocked...")
        return try {
            // TODO: Get blocking type
            /*Timber.d("Creating ParseQuery for Path...")
            val query = ParseQuery<ParseObject>("Path")
            query.limit = 1
            query.whereEqualTo("objectId", objectId)
            Timber.d("Fetching pin $pin")
            val l = query.fetchPinOrNetworkSync(pin, shouldPin = false, timeout = BLOCKED_TIMEOUT)
            if (l.isNotEmpty()) {
                Timber.d("Path found! Getting blocked...")
                val path = l[0]
                val blocked = path.getParseObject("blocked")!!.fetch<ParseObject>()
                Timber.d("Getting name...")
                val blockedName = blocked.getString("name")
                Timber.d("Got block status: $blockedName")
                BlockingType.find(blockedName)
            } else*/
            BlockingType.UNKNOWN
        } catch (_: NoInternetAccessException) {
            Timber.d("Could not get block status since Internet is not available")
            BlockingType.UNKNOWN
        }
    }

    /**
     * Checks if the Path has a description or built by information
     * @author Arnau Mora
     * @since 20210316
     * @return True if the path has information
     */
    fun hasInfo(): Boolean =
        (description != null && description.isNotBlank()) || (builtBy != null && builtBy.isNotBlank())

    fun grade(): Grade =
        if (grades.size > 0) grades.first() else throw NoSuchElementException("Grades list is empty")

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.apply {
            writeString(objectId)
            writeSerializable(timestamp)
            writeLong(sketchId)
            writeString(displayName)
            writeList(grades)
            writeList(heights)
            writeList(endings)
            writeList(pitches)
            writeParcelable(fixedSafesData, 0)
            writeParcelable(requiredSafesData, 0)
            writeString(description)
            writeString(builtBy)
            writeString(rebuiltBy)
            writeInt(if (downloaded) 1 else 0)
            writeString(pointer)
        }
    }

    companion object CREATOR : Parcelable.Creator<Path> {
        override fun createFromParcel(parcel: Parcel): Path = Path(parcel)
        override fun newArray(size: Int): Array<Path?> = arrayOfNulls(size)

        val BLOCKED_TIMEOUT = 10L to TimeUnit.SECONDS
        const val NAMESPACE = "Path"
    }
}
