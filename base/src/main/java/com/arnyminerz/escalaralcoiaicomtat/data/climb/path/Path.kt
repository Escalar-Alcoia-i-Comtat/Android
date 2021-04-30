package com.arnyminerz.escalaralcoiaicomtat.data.climb.path

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.WorkerThread
import androidx.collection.arrayMapOf
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.safes.FixedSafesData
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.safes.RequiredSafesData
import com.arnyminerz.escalaralcoiaicomtat.generic.awaitTask
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toTimestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserRecord
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeUnit

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
    suspend fun isBlocked(firestore: FirebaseFirestore): BlockingType {
        Timber.d("Fetching...")
        val ref = firestore.document(documentPath)

        Timber.v("Checking if \"$documentPath\" is blocked...")
        val task = ref.get()
        val result = task.awaitTask()
        return if (!task.isSuccessful) {
            val e = task.exception!!
            Timber.w(e, "Could not check if path is blocked")
            BlockingType.UNKNOWN
        } else {
            val blocked = result!!.getString("blocked")
            val blockingType = BlockingType.find(blocked)
            Timber.v("Blocking status for \"$displayName\": $blockingType")
            blockingType
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
     * Requests the server to mark a path as completed.
     * @author Arnau Mora
     * @since 20210430
     * @param firestore The [FirebaseFirestore] instance to update the data.
     * @param data The data for the marking.
     */
    suspend fun markCompleted(firestore: FirebaseFirestore, data: MarkCompletedData) {
        val user = data.user
        val attempts = data.attempts
        val falls = data.falls
        val comment = data.comment
        val notes = data.notes

        firestore
            .document(documentPath)
            .collection("Completions")
            .add(
                hashMapOf(
                    "timestamp" to FieldValue.serverTimestamp(),
                    "user" to user.uid,
                    "attempts" to attempts,
                    "falls" to falls,
                    "comment" to comment,
                    "notes" to notes,
                    "project" to false
                )
            )
            .awaitTask()
        Timber.i("Marked \"$documentPath\" as complete!")
    }

    /**
     * Requests the server to mark a path as project.
     * @author Arnau Mora
     * @since 20210430
     * @param firestore The [FirebaseFirestore] instance to update the data.
     * @param data The data for the marking.
     */
    suspend fun markProject(firestore: FirebaseFirestore, data: MarkProjectData) {
        val user = data.user
        val comment = data.comment
        val notes = data.notes

        firestore
            .document(documentPath)
            .collection("Completions")
            .add(
                hashMapOf(
                    "timestamp" to FieldValue.serverTimestamp(),
                    "user" to user.uid,
                    "comment" to comment,
                    "notes" to notes,
                    "project" to true,
                )
            )
            .awaitTask()
        Timber.i("Marked \"$documentPath\" as complete!")
    }

    /**
     * Fetches all the completions that have been requested by the users.
     * @author Arnau Mora
     * @since 20210430
     * @param firestore The [FirebaseFirestore] instance from where to load the data.
     * @param auth The [FirebaseAuth] instance for fetching the user's data.
     * @throws FirebaseAuthException When there was an exception while loading an user from Firebase.
     */
    @Throws(FirebaseAuthException::class)
    suspend fun getCompletions(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): Flow<MarkedDataInt> = flow {
        val completionsData = firestore
            .document(documentPath)
            .collection("Completions")
            .get()
            .awaitTask()
        val cachedUsers = arrayMapOf<String, UserRecord>()
        if (completionsData != null)
            for (document in completionsData.documents) {
                val timestamp = document.getTimestamp("timestamp")
                val userUid = document.getString("user")
                val attempts = document.getLong("attempts") ?: 0
                val falls = document.getLong("falls") ?: 0
                val comment = document.getString("comment")
                val notes = document.getString("notes")
                val project = document.getBoolean("project") ?: false

                Timber.v("Got completion data.")
                val user = if (cachedUsers.containsKey(userUid))
                    cachedUsers[userUid]!!
                else
                    try {
                        Timber.v("Loading user data from server...")
                        val loadedUser = auth.getUser(userUid)
                        Timber.v("Got user! Caching and returning...")
                        cachedUsers[userUid] = loadedUser
                        loadedUser
                    } catch (_: IllegalArgumentException) {
                        continue
                    }

                val result = if (project)
                    MarkedProjectData(timestamp, user, comment, notes)
                else
                    MarkedCompletedData(timestamp, user, attempts, falls, comment, notes)
                Timber.v("Processed result. Emitting...")
                emit(result)
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

        val BLOCKED_TIMEOUT = 10L to TimeUnit.SECONDS
        const val NAMESPACE = "Path"
    }
}
