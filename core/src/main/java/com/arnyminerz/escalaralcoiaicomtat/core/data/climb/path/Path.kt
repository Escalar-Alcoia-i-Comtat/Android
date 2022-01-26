package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

import androidx.annotation.WorkerThread
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.exceptions.AppSearchException
import androidx.work.await
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.EndingType
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.parceler.BlockingTypeParceler
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.parceler.PitchParceler
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.FixedSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.RequiredSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getDate
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.json.JSONException
import org.json.JSONObject
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
    val parentSectorId: String,
) : DataClassImpl(objectId, NAMESPACE, timestampMillis, displayName, documentPath),
    Comparable<Path> {
    /**
     * Creates a new [Path] from the data of a [DocumentSnapshot].
     * @author Arnau Mora
     * @since 20210411
     * @param data The object to get data from
     */
    @Deprecated("Use JSON from data module")
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
        try {
            data.getString("description")
        } catch (e: JSONException) {
            null
        },
        try {
            data.getString("builtBy")
        } catch (e: JSONException) {
            null
        },
        "", // This gets initialized later
        documentPath = data.reference.path,
        parentSectorId = data.reference.parent.parent!!.id
    ) {
        val pathData: Map<String, Any>? = data.data

        Timber.d("Loading heights for Path $objectId")
        val heights = pathData?.get("height") as List<*>?
        heights?.forEach { height ->
            height.toString().toLongOrNull()?.let {
                this.heights.add(it)
            }
        } ?: Timber.w("Heights is null")

        Timber.d("Loading endings for Path $objectId")
        val endingsList = pathData?.get("ending") as List<*>?
        endingsList?.forEachIndexed { i, _ ->
            endings.add(endingsList[i].toString().lowercase())
        }

        Timber.d("Loading rebuilders...")
        val rebuilders = pathData?.get("rebuiltBy") as List<*>?
        val d = rebuilders?.joinToString(separator = ",")
        rebuiltBy = d
    }

    /**
     * Initializes the Path from the values gotten from the Data module.
     * @author Arnau Mora
     * @since 20211224
     * @param data The JSON content to parse.
     * @param path The path of the Path inside the data module.
     * @param parentSectorId The ID of the Sector which the Path is contained in.
     */
    constructor(data: JSONObject, path: String, parentSectorId: String) : this(
        path.split("/").last(),
        data.getDate("created")!!.time,
        data.getString("sketchId").toLongOrNull() ?: 0L,
        data.getString("displayName"),
        data.getString("grade"),
        arrayListOf(),
        arrayListOf(),
        data.getString("ending_artifo"),
        FixedSafesData(
            data.getLong("stringCount"),
            data.getLong("paraboltCount"),
            data.getLong("spitCount"),
            data.getLong("tensorCount"),
            data.getLong("pitonCount"),
            data.getLong("burilCount"),
        ),
        RequiredSafesData(
            data.getBoolean("lanyardRequired"),
            data.getBoolean("crackerRequired"),
            data.getBoolean("friendRequired"),
            data.getBoolean("stripsRequired"),
            data.getBoolean("pitonRequired"),
            data.getBoolean("nailRequired"),
        ),
        try {
            data.getString("description")
        } catch (e: JSONException) {
            null
        },
        try {
            data.getString("builtBy")
        } catch (e: JSONException) {
            null
        },
        "", // This gets initialized later
        documentPath = path,
        parentSectorId = parentSectorId
    ) {
        Timber.d("Loading heights for Path $objectId")
        val heights = data.getJSONArray("height")
        for (k in 0 until heights.length()) {
            val height = heights[k]
            height.toString().toLongOrNull()?.let {
                this.heights.add(it)
            }
        }

        Timber.d("Loading endings for Path $objectId")
        val endingsList = data.getJSONArray("ending")
        for (i in 0 until endingsList.length()) {
            endings.add(endingsList.getString(i).lowercase())
        }

        Timber.d("Loading rebuilders...")
        try {
            val reBuildersList = data.getJSONArray("rebuiltBy")
            val rebuilders = arrayListOf<String>()
            for (k in 0 until reBuildersList.length()) {
                rebuilders.add(reBuildersList.getString(k))
            }
            val d = rebuilders.joinToString(separator = ",")
            rebuiltBy = d
        } catch (_: JSONException) {
        }
    }

    /**
     * Returns the [Path]'s [Grade]s as a [List].
     * @author Arnau Mora
     * @since 20210811
     */
    val grades: List<Grade>
        get() {
            // Grades should be split in each L
            val gradeValues = rawGrades
                .replace("\n", "") // Remove all line jumps
                .replace("Â", "") // Remove corrupt characters
                .split("L").toMutableList()
            if (gradeValues.size > 1)
                for (i in 1 until gradeValues.size)
                    gradeValues[i] = 'L' + gradeValues[i]
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
     * Returns [rebuiltBy] as a list of strings.
     * @author Arnau Mora
     * @since 20210830
     */
    val rebuilders: List<String>
        get() = rebuiltBy?.let {
            arrayListOf<String>().apply {
                val rebuilders = it.split(',')
                for (rebuilder in rebuilders)
                    add(rebuilder)
            }
        } ?: emptyList()

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
        return areas[docPath[1]]
            ?.get(application.searchSession, docPath[3])
            ?.get(application.searchSession, docPath[5])
    }

    override fun toString(): String = displayName

    override fun compareTo(other: Path): Int =
        when {
            sketchId > other.sketchId -> 1
            sketchId < other.sketchId -> -1
            else -> 0
        }

    /**
     * Checks if the Path has a description, built by or rebuilt by information
     * @author Arnau Mora
     * @since 20210316
     * @return True if the path has information
     */
    fun hasInfo(): Boolean = (description != null && description.isNotBlank()) ||
            (builtBy != null && builtBy.isNotBlank()) ||
            (rebuilders.isNotEmpty())

    /**
     * Gets the first [Grade] from the [grades] list.
     * @author Arnau Mora
     * @since 20210811
     * @throws NoSuchElementException When [grades] is empty.
     */
    @Throws(NoSuchElementException::class)
    fun grade(): Grade = grades.first()

    /**
     * Fetches the [BlockingType] of the [Path] from the server.
     * @author Arnau Mora
     * @since 20210824
     * @param firestore The [FirebaseFirestore] instance to fetch the data from.
     * @throws RuntimeException If the blocked parameter of the found item is not a string.
     * @throws FirebaseFirestoreException When there was an error while fetching the data from the
     * server.
     */
    @WorkerThread
    @Throws(RuntimeException::class, FirebaseFirestoreException::class)
    suspend fun singleBlockStatusFetch(firestore: FirebaseFirestore): BlockingType {
        Timber.v("$this > Getting path document from the server...")
        val document = firestore.document(documentPath).get().await()
        Timber.v("$this > Extracting blocked from document...")
        val blocked = document.getString("blocked")
        Timber.v("$this > Searching for blocking type...")
        return BlockingType.find(blocked)
    }

    /**
     * Fetches the blocking status of the path from the server. Also updates [blockingType] with the
     * result.
     * @author Arnau Mora
     * @since 20210824
     */
    @WorkerThread
    suspend fun getBlockStatus(
        searchSession: AppSearchSession,
        firestore: FirebaseFirestore
    ): BlockingType = try {
        Timber.v("$this > Getting block status...")
        Timber.v("$this > Building search spec...")
        val searchSpec = SearchSpec.Builder()
            .addFilterNamespaces(BlockingData.NAMESPACE) // Search for BlockingData
            .setResultCountPerPage(1) // Get just one result
            .build()
        Timber.v("$this > Searching for path in session...")
        val searchResults = searchSession.search(objectId, searchSpec)
        Timber.v("$this > Awaiting for page results...")
        val searchPage = searchResults.nextPage.await()
        if (searchPage.isEmpty()) {
            Timber.v("$this > There's no block status in search session.")
            val blockingType = singleBlockStatusFetch(firestore)
            Timber.v("$this > Blocking type: $blockingType")
            blockingType
        } else {
            Timber.v("$this > Getting first search result...")
            val searchResult = searchPage[0]
            Timber.v("$this > Getting generic document...")
            val document = searchResult.genericDocument
            Timber.v("$this > Converting document class...")
            val data = document.toDocumentClass(BlockingData::class.java)
            Timber.v("$this > Converting to BlockingType...")
            data.blockingType
        }
    } catch (e: AppSearchException) {
        Timber.e(e, "$this > Could not get blocking type.")
        BlockingType.UNKNOWN
    }

    companion object {
        @Namespace
        const val NAMESPACE = "Path"

        val SAMPLE_PATH = Path(
            objectId = "04BXQMNxFV4cjLILJk3p",
            timestampMillis = 1618153406000,
            sketchId = 52,
            displayName = "Regall Impenetrable",
            rawGrades = "7c+",
            heights = arrayListOf(),
            endings = arrayListOf("chain_carabiner"),
            rawPitches = null,
            fixedSafesData = FixedSafesData(
                0, 1, 0, 0, 0, 0,
            ),
            requiredSafesData = RequiredSafesData(
                lanyardRequired = false,
                crackerRequired = false,
                friendRequired = false,
                stripsRequired = false,
                pitonRequired = false,
                nailRequired = false,
            ),
            description = null,
            builtBy = "",
            rebuiltBy = "",
            documentPath = "/Areas/PL5j43cBRP7F24ecXGOR/Zones/3DmHnKBlDRwqlH1KK85C/Sectors/B9zNqbw6REYVxGZxlYwh/Paths/04BXQMNxFV4cjLILJk3p",
            parentSectorId = "B9zNqbw6REYVxGZxlYwh",
        )
    }
}
