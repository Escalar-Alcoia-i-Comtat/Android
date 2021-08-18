package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

import android.app.Activity
import androidx.annotation.UiThread
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.EndingType
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.parceler.BlockingTypeParceler
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.parceler.PitchParceler
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.storage.MarkedDataInt
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.FixedSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.RequiredSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import timber.log.Timber

/**
 * Creates a new [Path] instance.
 * @author Arnau Mora
 * @since 20210724
 */
@Parcelize
@TypeParceler<Pitch, PitchParceler>
@TypeParceler<BlockingType, BlockingTypeParceler>
class Path internal constructor(
    override val objectId: String,
    override val timestampMillis: Long,
    val sketchId: Long,
    override val displayName: String,
    val rawGrades: String,
    val heights: ArrayList<Long>,
    val endings: ArrayList<@EndingType String>,
    val rawPitches: String?,
    val fixedSafesData: FixedSafesData,
    val requiredSafesData: RequiredSafesData,
    val description: String?,
    val builtBy: String?,
    var rebuiltBy: String?,
    val downloaded: Boolean = false,
    override val documentPath: String,
) : DataClassImpl(objectId, NAMESPACE, timestampMillis, displayName, documentPath),
    Comparable<Path> {
    /**
     * Stores is the path is blocked.
     * @author Arnau Mora
     * @since 20210724
     */
    @IgnoredOnParcel
    var blockingType: BlockingType = BlockingType.UNKNOWN
        private set

    /**
     * Creates a new [Path] from the data of a [DocumentSnapshot].
     * @author Arnau Mora
     * @since 20210411
     * @param data The object to get data from
     */
    constructor(data: DocumentSnapshot) : this(
        data.id,
        data.getDate("created")!!.time,
        data.getString("sketchId")?.toLongOrNull() ?: 0L,
        data.getString("displayName")!!,
        data.getString("grade")!!,
        arrayListOf(),
        arrayListOf(),
        data.getString("ending_artifo"),
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
        val pathData: Map<String, Any>? = data.data

        Timber.d("Loading heights for Path $objectId")
        val heights = pathData?.get("height") as List<*>?
        heights?.forEach { this.heights.add(it.toString().toLong()) } ?: Timber.w("Heights is null")

        Timber.d("Loading endings for Path $objectId")
        val endingsList = pathData?.get("ending") as List<*>?
        endingsList?.forEachIndexed { i, _ -> endings.add(endingsList[i].toString()) }

        Timber.d("Loading rebuilders...")
        val rebuilders = pathData?.get("rebuiltBy") as List<*>?
        val d = rebuilders?.joinToString(separator = ", ")
        rebuiltBy = d
    }

    /**
     * Returns the [Path]'s [Grade]s as a [List].
     * @author Arnau Mora
     * @since 20210811
     */
    val grades: List<Grade>
        get() {
            val gradeValues = rawGrades.split(" ")
            return Grade.listFromStrings(gradeValues)
        }

    /**
     * Returns the [Path]'s [Pitch]es as a [List].
     * @author Arnau Mora
     * @since 20210811
     */
    val pitches: List<Pitch>
        get() {
            return if (rawPitches == null)
                emptyList()
            else
                arrayListOf<Pitch>().apply {
                    val artifos = rawPitches.replace("\r", "").split("\n")
                    for (artifo in artifos)
                        Pitch.fromEndingDataString(artifo)
                            ?.let { artifoEnding -> add(artifoEnding) }
                }
        }

    /**
     * Returns the parent [Sector] of the [Path].
     * @author Arnau Mora
     * @since 20210817
     * @param application The [App] class for fetching areas.
     * @return The parent [Sector], or null if not found.
     */
    suspend fun getParent(application: App): Sector? {
        val areas = application.getAreas()
        val docPath = documentPath.split("/")
        return areas[docPath[1]]?.get(application, docPath[3])?.get(application, docPath[5])
    }

    override fun toString(): String = displayName

    override fun compareTo(other: Path): Int =
        when {
            sketchId > other.sketchId -> 1
            sketchId < other.sketchId -> -1
            else -> 0
        }

    /**
     * Checks if the Path has a description or built by information
     * @author Arnau Mora
     * @since 20210316
     * @return True if the path has information
     */
    fun hasInfo(): Boolean =
        (description != null && description.isNotBlank()) || (builtBy != null && builtBy.isNotBlank())

    /**
     * Gets the first [Grade] from the [grades] list.
     * @author Arnau Mora
     * @since 20210811
     * @throws NoSuchElementException When [grades] is empty.
     */
    @Throws(NoSuchElementException::class)
    fun grade(): Grade = grades.first()

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
        if (completionsData != null) {
            val documents = completionsData.documents
            Timber.v("$objectId > Got ${documents.size} completions.")
            for (document in documents) {
                Timber.v("$objectId > Creating new MarkedDataInt instance...")
                val result = MarkedDataInt.newInstance(document)

                Timber.v("$objectId > Processed result. Emitting...")
                if (result != null)
                    emit(result)
                else
                    Timber.w("$objectId > Won't emit since result is null.")
            }
        }
    }

    /**
     * Gets updates for when a completion is added to the [Path].
     * @author Arnau Mora
     * @since 20210719
     * @param firestore The [FirebaseFirestore] reference for fetching updates.
     * @param activity The [Activity] to attach the listener to. When the activity is destroyed,
     * the observer will also be removed.
     * @param listener This will get called when a new completed path is added.
     * @return The listener registration for cancelling the listener when needed.
     */
    fun observeCompletions(
        firestore: FirebaseFirestore,
        activity: Activity,
        @UiThread listener: (data: MarkedDataInt) -> Unit
    ) =
        firestore.document(documentPath)
            .collection("Completions")
            .addSnapshotListener(activity) { value, error ->
                if (error != null)
                    Timber.e(error, "An error occurred while adding a new snapshot.")
                else if (value != null) {
                    val documentChanges = value.documentChanges
                    doAsync {
                        for (documentChange in documentChanges) {
                            val document = documentChange.document
                            val markedDataInt = MarkedDataInt.newInstance(document)
                            if (markedDataInt != null)
                                uiContext { listener(markedDataInt) }
                        }
                    }
                }
            }

    /**
     * Observes the block status of the path, and notifies the app whenever it changes.
     * @author Arnau Mora
     * @since 20210730
     * @param firestore The [FirebaseFirestore] reference to fetch the updates from the server.
     * @param activity The [Activity] to attach the listener to. When the activity is destroyed,
     * the observer will also be removed.
     * @param listener This will get called when a new completed path is added.
     * @return The listener registration for cancelling the listener when needed.
     */
    fun observeBlockStatus(
        firestore: FirebaseFirestore,
        activity: Activity,
        @UiThread listener: (blocking: BlockingType) -> Unit
    ) =
        firestore.document(documentPath)
            .addSnapshotListener(activity) { snapshot, error ->
                Timber.v("$this > Got snapshot update!")
                if (error != null)
                    Timber.e(error, "$this > Detected an error in a snapshot.")
                else if (snapshot != null && snapshot.exists()) {
                    Timber.v("$this > Processing blocked status...")
                    val blocked = snapshot.getString("blocked")
                    val blockingType = BlockingType.find(blocked)
                    this.blockingType = blockingType
                    listener(blockingType)
                }
            }

    companion object {
        const val NAMESPACE = "Path"
    }
}
