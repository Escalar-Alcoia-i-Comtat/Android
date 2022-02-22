package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

import androidx.annotation.WorkerThread
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.exceptions.AppSearchException
import androidx.work.await
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.EndingType
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.parceler.BlockingTypeParceler
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.parceler.PitchParceler
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.FixedSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.RequiredSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REST_API_BLOCKING_ENDPOINT
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getDate
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getJson
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
    val parentSectorId: String,
) : DataClassImpl(objectId, NAMESPACE, timestampMillis, displayName), Comparable<Path> {
    /**
     * Initializes the Path from the values gotten from the Data module.
     * @author Arnau Mora
     * @since 20211224
     * @param data The JSON content to parse.
     * @param pathId The ID of the Path.
     */
    constructor(data: JSONObject, @ObjectId pathId: String) : this(
        pathId,
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
        parentSectorId = data.getString("zone")
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
    suspend fun getParent(application: App): Sector? =
        application.getSector(parentSectorId)

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
     * @throws RuntimeException If the blocked parameter of the found item is not a string.
     */
    @WorkerThread
    @Throws(RuntimeException::class)
    suspend fun singleBlockStatusFetch(): BlockingType {
        Timber.v("$this > Getting path blocking from the server...")
        val fetchResult = getJson("$REST_API_BLOCKING_ENDPOINT/$objectId")
        Timber.v("$this > Extracting blocked from document...")
        return if (fetchResult.has("result")) {
            val blocked = fetchResult.getBoolean("blocked")
            if (blocked) {
                val type = fetchResult.getString("type")
                Timber.v("$this > Searching for blocking type...")
                BlockingType.find(type)
            } else
                BlockingType.UNKNOWN
        } else
            BlockingType.UNKNOWN
    }

    /**
     * Fetches the blocking status of the path from the server. Also updates [blockingType] with the
     * result.
     * @author Arnau Mora
     * @since 20210824
     */
    @WorkerThread
    suspend fun getBlockStatus(
        searchSession: AppSearchSession
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
            val blockingType = singleBlockStatusFetch()
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
            parentSectorId = "B9zNqbw6REYVxGZxlYwh",
        )
    }
}
