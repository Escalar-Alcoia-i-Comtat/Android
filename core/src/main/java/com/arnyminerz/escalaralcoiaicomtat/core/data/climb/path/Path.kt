package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.exceptions.AppSearchException
import androidx.work.await
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.parceler.BlockingTypeParceler
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.parceler.PitchParceler
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.FixedSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.PitchEndingData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.PitchEndingOrientation
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.PitchEndingRappel
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.RequiredSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REST_API_BLOCKING_ENDPOINT
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getDate
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getJson
import kotlinx.parcelize.IgnoredOnParcel
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
    private val rawGrades: String,
    private val rawHeights: String,
    private val rawEndings: String,
    private val rawPitches: String,
    val fixedSafesData: FixedSafesData,
    val requiredSafesData: RequiredSafesData,
    val description: String?,
    private val rawBuilt: String?,
    private val rawReBuilt: String?,
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
        data.getString("height"),
        data.getString("ending"),
        try {
            data.getString("pitch_info")
        } catch (e: JSONException) {
            ""
        },
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
        try {
            data.getString("rebuilders")
        } catch (e: JSONException) {
            null
        },
        parentSectorId = data.getString("sector")
    )

    /**
     * The general height of the path. May be null if not set.
     * @author Arnau Mora
     * @since 20220223
     */
    @IgnoredOnParcel
    var generalHeight: Long? = null
        private set

    /**
     * The general grade of the path.
     * @author Arnau Mora
     * @since 20220223
     */
    @IgnoredOnParcel
    var generalGrade: String = "¿?"
        private set

    /**
     * The general ending of the path.
     * @author Arnau Mora
     * @since 20220223
     */
    @IgnoredOnParcel
    var generalEnding: String? = null
        private set

    /**
     * A list of the data of each pitch of the path.
     * @author Arnau Mora
     * @since 20220223
     */
    @IgnoredOnParcel
    var pitches: Array<Pitch> = emptyArray()
        private set

    /**
     * Contains data about who built the path.
     * @author Arnau Mora
     * @since 20220323
     */
    @IgnoredOnParcel
    var buildPatch: Patch? = null

    /**
     * All the patches made on the path.
     * @author Arnau Mora
     * @since 20220223
     */
    @IgnoredOnParcel
    var patches: List<Patch> = emptyList()
        private set

    private fun <T> processPitch(
        rawText: String,
        conversion: (value: String) -> T?,
        setGeneral: ((value: T) -> Unit)?,
        setter: (index: Int, value: T) -> Unit
    ) {
        if (rawText == "NULL")
            return

        val split = rawText
            .replace("\r", "")
            .split("\n")
        if (split.size == 1 && setGeneral != null)
            conversion(rawHeights)?.let { setGeneral(it) }
        else for (item in split)
            if (item.startsWith(">") && setGeneral != null)
                conversion(item.substring(1))?.let { setGeneral(it) }
            else
                item.indexOf('>')
                    .takeUnless { it < 0 }
                    ?.let { signPos ->
                        Timber.d("Converting item \"$item\" of $objectId")
                        val index = item.substring(0, signPos).toInt()
                        val convertedElem = conversion(item.substring(signPos + 1))

                        // This ensures the size matches, and that all items of the array are initialized.
                        @Suppress("UNCHECKED_CAST")
                        pitches = pitches
                            .copyOf(index + 1)
                            .let { newPitches ->
                                for ((i, ir) in newPitches.withIndex())
                                    if (ir == null)
                                        newPitches[i] = Pitch()
                                newPitches
                            } as Array<Pitch>

                        if (convertedElem != null)
                            setter(index, convertedElem)
                    }
                    ?: run {
                        Timber.w("Could not take row row \"%s\". Path: %s", item, objectId)
                    }
    }

    init {
        // Transform the height format to the list.
        processPitch(
            rawHeights,
            { it.toLongOrNull() },
            { generalHeight = it },
            { index, value -> pitches[index].height = value },
        )

        // Transform the grades format to the list.
        processPitch(
            rawGrades,
            { it.takeIf { it.isNotBlank() } },
            { generalGrade = it },
            { index, value -> pitches[index].grade = value },
        )

        // Transform raw endings into pitches
        processPitch(
            rawEndings,
            { it.takeIf { it.isNotBlank() } },
            { generalEnding = it },
            { index, value -> pitches[index].ending = value },
        )

        // Put pitch info into pitches
        processPitch(
            rawPitches,
            {
                it.split(" ").let { splitPitch ->
                    val rappel = PitchEndingRappel.find(splitPitch[0])
                    val orientation = PitchEndingOrientation.find(splitPitch[1])
                    if (rappel != null && orientation != null)
                        PitchEndingData(rappel, orientation)
                    else {
                        Timber.w("Could not find rappel \"${splitPitch[0]}\" or orientation \"${splitPitch[1]}\" at $objectId")
                        null
                    }
                }
            },
            null,
            { index, value -> pitches[index].endingData = value },
        )

        // Process builder
        if (rawBuilt != null && rawBuilt.contains(";")) {
            val splitBuilder = rawBuilt.split(";")
            buildPatch = Patch(splitBuilder[0], splitBuilder[1])
        }

        // Process re-builders
        if (rawReBuilt != null) {
            val newPatches = arrayListOf<Patch>()
            val reBuiltLines = rawReBuilt.replace("\r", "").split("\n")
            for (line in reBuiltLines) {
                if (!line.contains(";")) continue
                val splitLine = line.split(";")
                newPatches.add(Patch(splitLine[0], splitLine[1]))
            }
            patches = newPatches
        }
    }

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
     * Fetches the [BlockingType] of the [Path] from the server.
     * @author Arnau Mora
     * @since 20210824
     * @throws RuntimeException If the blocked parameter of the found item is not a string.
     */
    @WorkerThread
    @Throws(RuntimeException::class)
    suspend fun singleBlockStatusFetch(context: Context): BlockingType {
        Timber.v("$this > Getting path blocking from the server...")
        val fetchResult = context.getJson("$REST_API_BLOCKING_ENDPOINT/$objectId")
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
        context: Context,
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
            val blockingType = singleBlockStatusFetch(context)
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

    fun data(): PathData {
        return PathData(
            objectId,
            timestampMillis,
            sketchId,
            displayName,
            rawGrades,
            rawHeights,
            rawEndings,
            rawPitches,
            fixedSafesData.stringCount,
            fixedSafesData.paraboltCount,
            fixedSafesData.spitCount,
            fixedSafesData.tensorCount,
            fixedSafesData.pitonCount,
            fixedSafesData.burilCount,
            requiredSafesData.lanyardRequired,
            requiredSafesData.crackerRequired,
            requiredSafesData.friendRequired,
            requiredSafesData.stripsRequired,
            requiredSafesData.pitonRequired,
            requiredSafesData.nailRequired,
            description ?: "",
            rawBuilt ?: "",
            rawReBuilt ?: "",
            downloaded,
            parentSectorId,
        )
    }

    companion object {
        @Namespace
        const val NAMESPACE = "Path"

        val SAMPLE_PATH = Path(
            JSONObject("{\"created\":\"2021-04-11T15:03:26.000Z\",\"last_edit\":\"2022-02-17T16:42:14.000Z\",\"displayName\":\"Regall Impenetrable\",\"sketchId\":52,\"grade\":\"7c+\",\"height\":\"\",\"builtBy\":\"NULL\",\"rebuilders\":\"\",\"description\":\"NULL\",\"showDescription\":false,\"stringCount\":0,\"paraboltCount\":1,\"burilCount\":0,\"pitonCount\":0,\"spitCount\":0,\"tensorCount\":0,\"crackerRequired\":false,\"friendRequired\":false,\"lanyardRequired\":false,\"nailRequired\":false,\"pitonRequired\":false,\"stripsRequired\":false,\"ending\":\"chain_carabiner\",\"pitch_info\":\"NULL\",\"sector\":\"B9zNqbw6REYVxGZxlYwh\"}"),
            "04BXQMNxFV4cjLILJk3p"
        )
    }
}
