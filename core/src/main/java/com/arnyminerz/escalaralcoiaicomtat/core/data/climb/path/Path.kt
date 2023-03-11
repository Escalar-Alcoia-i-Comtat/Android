package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.parceler.BlockingTypeParceler
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.parceler.PitchParceler
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.*
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.utils.delLn
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getBooleanOrNull
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getDate
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getStringOrNull
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject
import timber.log.Timber

/**
 * Creates a new [Path] instance.
 * @author Arnau Mora
 * @since 20210724
 */
@Parcelize
@Entity(tableName = "Paths")
@TypeParceler<Pitch, PitchParceler>
@TypeParceler<BlockingType, BlockingTypeParceler>
class Path(
    @PrimaryKey override val objectId: String,
    @ColumnInfo(name = "last_edit") override val timestampMillis: Long,
    @ColumnInfo(name = "sketchId") val sketchId: Long,
    @ColumnInfo(name = "displayName") override val displayName: String,
    @ColumnInfo(name = "grade") val rawGrades: String,
    @ColumnInfo(name = "height") val rawHeights: String,
    @ColumnInfo(name = "ending") val rawEndings: String,
    @ColumnInfo(name = "pitch_info") val rawPitches: String,
    @Ignore val fixedSafesData: FixedSafesData,
    @Ignore val requiredSafesData: RequiredSafesData,
    @ColumnInfo(name = "show_description", defaultValue = "false") val showDescription: Boolean,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "builtBy") val rawBuilt: String?,
    @ColumnInfo(name = "rebuilders") val rawReBuilt: String?,
    @ColumnInfo(name = "downloaded") val downloaded: Boolean = false,
    @ColumnInfo(name = "sector") val parentSectorId: String,
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
        data.getDate("last_edit")!!.time,
        data.getString("sketchId").toLongOrNull() ?: 0L,
        data.getString("displayName"),
        data.getString("grade"),
        data.getString("height"),
        data.getString("ending"),
        data.getStringOrNull("pitch_info") ?: "",
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
        data.getBooleanOrNull("show_description") ?: false,
        data.getStringOrNull("description"),
        data.getStringOrNull("builtBy"),
        data.getStringOrNull("rebuilders"),
        parentSectorId = data.getString("sector")
    )

    constructor(
        objectId: String,
        timestampMillis: Long,
        sketchId: Long,
        displayName: String,
        rawGrades: String,
        rawHeights: String,
        rawEndings: String,
        rawPitches: String,
        quickdrawCount: Long,
        paraboltCount: Long,
        spitCount: Long,
        tensorCount: Long,
        pitonCount: Long,
        burilCount: Long,
        lanyardRequired: Boolean,
        crackerRequired: Boolean,
        friendRequired: Boolean,
        stripsRequired: Boolean,
        pitonRequired: Boolean,
        nailRequired: Boolean,
        showDescription: Boolean,
        description: String?,
        rawBuilt: String?,
        rawReBuilt: String?,
        downloaded: Boolean,
        parentSectorId: String,
    ) : this(
        objectId,
        timestampMillis,
        sketchId,
        displayName,
        rawGrades,
        rawHeights,
        rawEndings,
        rawPitches,
        FixedSafesData(
            quickdrawCount,
            paraboltCount,
            spitCount,
            tensorCount,
            pitonCount,
            burilCount
        ),
        RequiredSafesData(
            lanyardRequired,
            crackerRequired,
            friendRequired,
            stripsRequired,
            pitonRequired,
            nailRequired
        ),
        showDescription,
        description,
        rawBuilt,
        rawReBuilt,
        downloaded,
        parentSectorId
    )

    /**
     * Used internally for determining if pitches is initialized, and used for intermediate storage.
     * @author Arnau Mora
     * @since 20220316
     */
    @Ignore
    @IgnoredOnParcel
    private var _generalHeight: Long? = null

    /**
     * The general height of the path. May be null if not set.
     * @author Arnau Mora
     * @since 20220223
     */
    @get:Ignore
    @IgnoredOnParcel
    val generalHeight: Long?
        get() = _generalHeight ?: run { processPitches(); _generalHeight }

    /**
     * Used internally for determining if pitches is initialized, and used for intermediate storage.
     * @author Arnau Mora
     * @since 20220316
     */
    @Ignore
    @IgnoredOnParcel
    private var _generalGrade: String? = null

    /**
     * The general grade of the path.
     * @author Arnau Mora
     * @since 20220223
     */
    @get:Ignore
    @IgnoredOnParcel
    val generalGrade: String
        get() = _generalGrade ?: run {
            processPitches()
            _generalGrade ?: "¿?".also { _generalGrade = it }
        }

    /**
     * Used internally for determining if pitches is initialized, and used for intermediate storage.
     * @author Arnau Mora
     * @since 20220316
     */
    @Ignore
    @IgnoredOnParcel
    private var _generalEnding: String? = null

    /**
     * The general ending of the path.
     * @author Arnau Mora
     * @since 20220223
     */
    @get:Ignore
    @IgnoredOnParcel
    val generalEnding: String?
        get() = _generalEnding ?: run { processPitches(); _generalEnding }

    /**
     * Used internally for determining if pitches is initialized, and used for intermediate storage.
     * @author Arnau Mora
     * @since 20220316
     */
    @Ignore
    @IgnoredOnParcel
    private var _pitches: Array<Pitch> = emptyArray()

    /**
     * A list of the data of each pitch of the path.
     * @author Arnau Mora
     * @since 20220223
     */
    @get:Ignore
    @IgnoredOnParcel
    val pitches: Array<Pitch>
        get() = _pitches.takeIf { it.isNotEmpty() } ?: run { processPitches(); _pitches }

    /**
     * Contains data about who built the path.
     * @author Arnau Mora
     * @since 20220323
     */
    @Ignore
    @IgnoredOnParcel
    var buildPatch: Patch? = null

    /**
     * All the patches made on the path.
     * @author Arnau Mora
     * @since 20220223
     */
    @Ignore
    @IgnoredOnParcel
    var patches: List<Patch> = emptyList()
        private set

    @IgnoredOnParcel
    @ColumnInfo(name = "stringCount")
    val quickdrawCount: Long = fixedSafesData.quickdrawCount

    @IgnoredOnParcel
    @ColumnInfo(name = "paraboltCount")
    val paraboltCount: Long = fixedSafesData.paraboltCount

    @IgnoredOnParcel
    @ColumnInfo(name = "spitCount")
    val spitCount: Long = fixedSafesData.spitCount

    @IgnoredOnParcel
    @ColumnInfo(name = "tensorCount")
    val tensorCount: Long = fixedSafesData.tensorCount

    @IgnoredOnParcel
    @ColumnInfo(name = "pitonCount")
    val pitonCount: Long = fixedSafesData.pitonCount

    @IgnoredOnParcel
    @ColumnInfo(name = "burilCount")
    val burilCount: Long = fixedSafesData.burilCount

    @IgnoredOnParcel
    @ColumnInfo(name = "lanyardRequired")
    val lanyardRequired: Boolean = requiredSafesData.lanyardRequired

    @IgnoredOnParcel
    @ColumnInfo(name = "crackerRequired")
    val crackerRequired: Boolean = requiredSafesData.crackerRequired

    @IgnoredOnParcel
    @ColumnInfo(name = "friendRequired")
    val friendRequired: Boolean = requiredSafesData.friendRequired

    @IgnoredOnParcel
    @ColumnInfo(name = "stripsRequired")
    val stripsRequired: Boolean = requiredSafesData.stripsRequired

    @IgnoredOnParcel
    @ColumnInfo(name = "pitonRequired")
    val pitonRequired: Boolean = requiredSafesData.pitonRequired

    @IgnoredOnParcel
    @ColumnInfo(name = "nailRequired")
    val nailRequired: Boolean = requiredSafesData.nailRequired

    /**
     * Processes the value of [rawText] to be added into pitches and its general term if applicable.
     * @author Arnau Mora
     * @since 200220316
     * @param rawValue The text to parse.
     * @param conversion Used for converting the found values into [T].
     * @param setGeneral Should update the value of the general term into the passed one.
     * @param setter Should update the specific value of [_pitches] at the passed index.
     */
    private fun <T> processPitch(
        rawValue: String,
        conversion: (value: String) -> T?,
        setGeneral: ((value: T) -> Unit)?,
        setter: (index: Int, value: T) -> Unit
    ) {
        if (rawValue == "NULL")
            return

        val rawText = rawValue.replace("\r", "")

        // Example for rawText:
        // >6b+
        // 1>6b
        // 2>6a+
        // 3>6c

        var position = rawText.indexOf('>')
        if (position < 0 && setGeneral != null)
            conversion(rawText.delLn())?.let { setGeneral(it) }
        else while (position >= 0) {
            // Obtain the text between the current '>' and the line jump. If there's no line jump,
            // take until the end of the string. It means that it's the last line of the value.
            val item = rawText.substring(
                position,
                rawText.indexOf('\n', position).takeIf { it >= 0 } ?: rawText.length
            )
            Timber.v("Analysis item for position $position: $item")

            // If position is on the starting point of the string, just check if it's '>'. If it's
            // somewhere in the middle of rawText, check if the previous char is a number, if it is,
            // it mens that the row is not general.
            val isGeneral = if (position > 0) !rawText[position - 1].isDigit() else item[0] == '>'

            // If it has been determined that the item is the general row, and setGeneral is not
            // null, run it with the current line except the starting '>'.
            if (isGeneral && setGeneral != null)
                conversion(item.substring(1).delLn())?.let { setGeneral(it) }
            else {
                Timber.d("Converting item \"$item\" of $objectId")
                // Get the number that has the current row as prefix
                val index = rawText.substring(0, position)
                    .let { chain ->
                        Timber.v("Getting index from chain: $chain")
                        var builder = ""
                        var c = chain.length - 1
                        while (c >= 0 && chain[c].isDigit()) {
                            builder += chain[c]
                            c--
                        }
                        Timber.v("Index conversion result: $builder")
                        builder
                    }
                    .toInt()

                val convertedElem = conversion(item.substring(1))

                // This ensures the size matches, and that all items of the array are initialized.
                if (_pitches.size <= index) {
                    @Suppress("UNCHECKED_CAST")
                    _pitches = _pitches
                        .copyOf(index + 1)
                        .let { newPitches ->
                            for ((i, ir) in newPitches.withIndex())
                                if (ir == null)
                                    newPitches[i] = Pitch()
                            newPitches
                        } as Array<Pitch>
                }

                if (convertedElem != null)
                    setter(index, convertedElem)
            }

            // Take next '>'
            position = rawText.indexOf('>', position + 1)
        }
    }

    /**
     * Processes all the elements from the constructor parameters and adds them to [_pitches]. This
     * builds the array only whenever it's necessary.
     * @author Arnau Mora
     * @since 20220316
     */
    private fun processPitches() {
        // Transform the height format to the list.
        processPitch(
            rawHeights,
            { it.toLongOrNull() },
            { _generalHeight = it },
            { index, value -> _pitches[index].height = value },
        )

        // Transform the grades format to the list.
        processPitch(
            rawGrades,
            { it.takeIf { it.isNotBlank() } },
            { _generalGrade = it },
            { index, value -> _pitches[index].grade = value },
        )

        // Transform raw endings into pitches
        processPitch(
            rawEndings,
            { it.takeIf { it.isNotBlank() } },
            { _generalEnding = it },
            { index, value -> _pitches[index].ending = value },
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
            { index, value -> _pitches[index].endingData = value },
        )
    }

    init {
        // Process builder
        if (rawBuilt != null) {
            val splitBuilder = rawBuilt.split(";")
            buildPatch = Patch(
                splitBuilder.getOrNull(0).takeUnless { it == "NULL" },
                splitBuilder.getOrNull(1).takeUnless { it == "NULL" },
            )
        }

        // Process re-builders
        if (rawReBuilt != null) {
            val newPatches = arrayListOf<Patch>()
            val reBuiltLines = rawReBuilt
                .replace("\r", "")
                .split("\n")
            for (line in reBuiltLines) {
                val splitLine = line.split(";")
                newPatches.add(Patch(splitLine.getOrNull(0), splitLine.getOrNull(1)))
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

    override fun toString(): String = "P/$objectId"

    override fun compareTo(other: Path): Int =
        when {
            sketchId > other.sketchId -> 1
            sketchId < other.sketchId -> -1
            else -> 0
        }

    override fun hashCode(): Int {
        var result = objectId.hashCode()
        result = 31 * result + objectId.hashCode()
        result = 31 * result + timestampMillis.hashCode()
        result = 31 * result + sketchId.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + rawGrades.hashCode()
        result = 31 * result + rawHeights.hashCode()
        result = 31 * result + rawEndings.hashCode()
        result = 31 * result + rawPitches.hashCode()
        result = 31 * result + fixedSafesData.hashCode()
        result = 31 * result + requiredSafesData.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + rawBuilt.hashCode()
        result = 31 * result + rawReBuilt.hashCode()
        result = 31 * result + downloaded.hashCode()
        result = 31 * result + parentSectorId.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Path

        if (objectId != other.objectId) return false
        if (timestampMillis != other.timestampMillis) return false
        if (sketchId != other.sketchId) return false
        if (displayName != other.displayName) return false
        if (rawGrades != other.rawGrades) return false
        if (rawHeights != other.rawHeights) return false
        if (rawEndings != other.rawEndings) return false
        if (rawPitches != other.rawPitches) return false
        if (fixedSafesData != other.fixedSafesData) return false
        if (requiredSafesData != other.requiredSafesData) return false
        if (description != other.description) return false
        if (rawBuilt != other.rawBuilt) return false
        if (rawReBuilt != other.rawReBuilt) return false
        if (downloaded != other.downloaded) return false
        if (parentSectorId != other.parentSectorId) return false
        if (generalHeight != other.generalHeight) return false
        if (generalGrade != other.generalGrade) return false
        if (generalEnding != other.generalEnding) return false
        if (!pitches.contentEquals(other.pitches)) return false
        if (buildPatch != other.buildPatch) return false
        if (patches != other.patches) return false

        return true
    }

    companion object {
        val NAMESPACE = Namespace.PATH

        const val SAMPLE_PATH_OBJECT_ID = "04BXQMNxFV4cjLILJk3p"

        const val SAMPLE_MULTIPITCH_PATH_OBJECT_ID = "b17a1385776c863a657e"

        val SAMPLE_PATH = Path(
            JSONObject("{\"created\":\"2021-04-11T15:03:26.000Z\",\"last_edit\":\"2022-02-17T16:42:14.000Z\",\"displayName\":\"Regall Impenetrable\",\"sketchId\":52,\"grade\":\"7c+\",\"height\":\"\",\"builtBy\":\"NULL\",\"rebuilders\":\"\",\"description\":\"This is a testing description for the path. **Markdown** annotations should be working.\",\"showDescription\":false,\"stringCount\":0,\"paraboltCount\":1,\"burilCount\":0,\"pitonCount\":0,\"spitCount\":0,\"tensorCount\":0,\"crackerRequired\":false,\"friendRequired\":false,\"lanyardRequired\":false,\"nailRequired\":false,\"pitonRequired\":false,\"stripsRequired\":false,\"ending\":\"chain_carabiner\",\"pitch_info\":\"NULL\",\"sector\":\"B9zNqbw6REYVxGZxlYwh\"}"),
            SAMPLE_PATH_OBJECT_ID
        )

        val SAMPLE_PATH_MULTIPITCH = Path(
            JSONObject("{\"created\":\"2022-06-14T21:59:17.000Z\",\"last_edit\":\"2022-06-27T18:36:56.000Z\",\"displayName\":\"Eternitat\",\"sketchId\":1,\"grade\":\">6c+\r\n1>7a+(6a A1)\r\n2>5º\r\n3>6c+(5+ A1)\r\n4>5+\r\n5>6b+/c\",\"height\":\">165\r\n1>35\r\n2>15\r\n3>35\r\n4>25\r\n5>45\",\"builtBy\":\"Àlex Mora; 2022\",\"rebuilders\":null,\"description\":null,\"showDescription\":false,\"stringCount\":22,\"paraboltCount\":999,\"burilCount\":0,\"pitonCount\":999,\"spitCount\":0,\"tensorCount\":0,\"crackerRequired\":false,\"friendRequired\":false,\"lanyardRequired\":false,\"nailRequired\":false,\"pitonRequired\":false,\"stripsRequired\":false,\"ending\":\"plate_ring\",\"pitch_info\":null,\"sector\":\"787d42fe7f6f949fddf5\"}"),
            SAMPLE_MULTIPITCH_PATH_OBJECT_ID
        )
    }
}
