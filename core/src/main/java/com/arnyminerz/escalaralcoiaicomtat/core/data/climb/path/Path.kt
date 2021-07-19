package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.storage.MarkedDataInt
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.FixedSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.RequiredSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.shared.cache
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toTimestamp
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date

class Path(
    objectId: String,
    timestamp: Date,
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
    val documentPath: String,
) : DataClassImpl(objectId, NAMESPACE, timestamp), Comparable<Path> {
    var rebuiltBy: String? = rebuiltBy
        private set

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString().toTimestamp()!!,
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
        data.getDate("created")!!,
        data.getString("sketchId")?.toLongOrNull() ?: 0L,
        data.getString("displayName")!!,
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
        data.getString("description"),
        data.getString("builtBy"),
        "",
        documentPath = data.reference.path
    ) {
        val pathData = data.data

        Timber.d("Loading heights for Path $objectId")
        val heights = pathData?.get("height") as List<*>?
        if (heights != null)
            for (h in heights.indices)
                this.heights.add((heights[h].toString()).toLong())
        else Timber.w("Heights is null")

        Timber.d("Loading grade for Path $objectId")
        val gradeValue = data.getString("grade")!!
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
        val endingArtifo = data.getString("ending_artifo")
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
     * @param firestore The Firestore instance to fetch new data
     * @return A matching BlockingType class
     */
    @WorkerThread
    suspend fun isBlocked(firestore: FirebaseFirestore): BlockingType {
        var blockStatus = cache.getBlockStatus(objectId)
        return if (blockStatus != null) {
            Timber.v("There's already an stored block status for $objectId: $blockStatus")
            blockStatus
        } else {
            Timber.d("Fetching...")
            val ref = firestore.document(documentPath)

            Timber.v("Checking if \"$documentPath\" is blocked...")
            val task = ref.get()
            val result = task.await()
            blockStatus = if (!task.isSuccessful) {
                val e = task.exception!!
                Timber.w(e, "Could not check if path is blocked")
                BlockingType.UNKNOWN
            } else {
                val blocked = result!!.getString("blocked")
                val blockingType = BlockingType.find(blocked)
                Timber.v("Blocking status for \"$displayName\": $blockingType")
                blockingType
            }
            cache.storeBlockStatus(objectId, blockStatus)
            blockStatus
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

    /**
     * Fetches all the completions that have been requested by the users.
     * @author Arnau Mora
     * @since 20210430
     * @param firestore The [FirebaseFirestore] instance from where to load the data.
     * @throws FirebaseAuthException When there was an exception while loading an user from Firebase.
     * @throws FirebaseFirestoreException When there was an exception while loading data from Firestore.
     */
    @Throws(FirebaseAuthException::class, FirebaseFirestoreException::class)
    suspend fun getCompletions(
        firestore: FirebaseFirestore
    ): Flow<MarkedDataInt> = flow {
        val completionsData = firestore
            .document(documentPath)
            .collection("Completions")
            .get()
            .await()
        if (completionsData != null)
            for (document in completionsData.documents) {
                Timber.v("$objectId > Creating new MarkedDataInt instance...")
                val result = MarkedDataInt.newInstance(document)

                Timber.v("$objectId > Processed result. Emitting...")
                if (result != null)
                    emit(result)
            }
    }

    /**
     * Gets updates for when a completion is added to the [Path].
     * @author Arnau Mora
     * @since 20210719
     * @param firestore The [FirebaseFirestore] reference for fetching updates.
     * @param listener This will get called when a new completed path is added.
     */
    fun observeCompletions(firestore: FirebaseFirestore, listener: (data: MarkedDataInt) -> Unit) {
        firestore.document(documentPath)
            .collection("Completions")
            .addSnapshotListener { value, error ->
                if (error != null)
                    Timber.e(error, "An error occurred while adding a new snapshot.")
                else if (value != null) {
                    val documentChanges = value.documentChanges
                    doAsync {
                        for (documentChange in documentChanges) {
                            val document = documentChange.document
                            val markedDataInt = MarkedDataInt.newInstance(document)
                            if (markedDataInt != null)
                                listener(markedDataInt)
                        }
                    }
                }
            }
    }

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
            writeString(documentPath)
        }
    }

    companion object CREATOR : Parcelable.Creator<Path> {
        override fun createFromParcel(parcel: Parcel): Path = Path(parcel)
        override fun newArray(size: Int): Array<Path?> = arrayOfNulls(size)

        const val NAMESPACE = "Path"
    }
}
