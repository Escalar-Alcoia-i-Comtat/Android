package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import android.os.Parcel
import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.data.climb.types.BlockingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.types.EndingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.types.Grade
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toTimestamp
import com.arnyminerz.escalaralcoiaicomtat.generic.fixTildes
import com.parse.ParseObject
import java.util.*
import kotlin.NoSuchElementException

enum class CompletedType(val index: Int) {
    FIRST(0),
    TOP_ROPE(1),
    LEAD(2);

    companion object {
        fun find(index: Int): CompletedType? {
            return when (index) {
                0 -> FIRST
                1 -> TOP_ROPE
                2 -> LEAD
                else -> null
            }
        }
    }
}

data class Path(
    override val objectId: String,
    val timestamp: Date?,
    val sketchId: Int,
    val displayName: String,
    val grades: Grade.GradesList,
    val heights: ArrayList<Int>,
    val endings: ArrayList<EndingType>,
    val pitches: ArrayList<Pitch>,
    val fixedSafesData: FixedSafesData,
    val requiredSafesData: RequiredSafesData,
    val description: String?,
    val builtBy: String?,
    val rebuiltBy: String?,
    val downloaded: Boolean = false
) : DataClassImpl(objectId), Comparable<Path> {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString().toTimestamp(),
        parcel.readInt(),
        parcel.readString()!!,
        Grade.GradesList(),
        arrayListOf(),
        arrayListOf(),
        arrayListOf(),
        parcel.readParcelable<FixedSafesData>(FixedSafesData::class.java.classLoader)!!,
        parcel.readParcelable<RequiredSafesData>(RequiredSafesData::class.java.classLoader)!!,
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    constructor(parseObject: ParseObject) : this(
        parseObject.objectId,
        parseObject.updatedAt,
        (parseObject.getString("sketchId")?.fixTildes() ?: "0").toInt(),
        parseObject.getString("displayName")!!.fixTildes(),
        Grade.GradesList(),
        arrayListOf(),
        arrayListOf(),
        arrayListOf(),
        FixedSafesData(
            parseObject.getInt("stringCount"),
            parseObject.getInt("paraboltCount"),
            parseObject.getInt("spitCount"),
            parseObject.getInt("tensorCount"),
            parseObject.getInt("pitonCount"),
            parseObject.getInt("burilCount")
        ),
        RequiredSafesData(
            parseObject.getBoolean("lanyardRequired"),
            parseObject.getBoolean("crackerRequired"),
            parseObject.getBoolean("friendRequired"),
            parseObject.getBoolean("stripsRequired"),
            parseObject.getBoolean("pitonRequired"),
            parseObject.getBoolean("nailRequired")
        ),
        parseObject.getString("description")?.fixTildes(),
        parseObject.getString("builtBy")?.fixTildes(),
        parseObject.getList<String>("rebuiltBy")?.joinToString(separator = ", ")
    ) {
        heights.addAll(parseObject.getList("height")!!)

        val gradeValue = parseObject.getString("grade")!!.fixTildes()
        val gradeValues = gradeValue.split(" ")
        grades.addAll(Grade.listFromStrings(gradeValues))

        val endingsList = parseObject.getList<ParseObject>("ending")
        if (endingsList != null)
            for (e in endingsList) {
                val ending = e.fetchIfNeeded<ParseObject>()
                val endingName = ending.getString("name")?.fixTildes()
                val endingType = EndingType.find(endingName)
                endings.add(endingType)
            }

        val endingArtifo = parseObject.getString("endingArtifo")?.fixTildes()
        endingArtifo?.let {
            val artifos = it.replace("\r", "").split("\n")
            for (artifo in artifos)
                Pitch.fromEndingDataString(artifo)
                    ?.let { artifoEnding -> pitches.add(artifoEnding) }
        }
    }

    /**
     * Checks if the Path has safe count. This doesn't include the safe types marked with a 1, since
     * the 1 is used to mark as "undetermined amount".
     * @author Arnau Mora
     * @since 20210316
     * @return If the path has safes count.
     */
    fun hasSafeCount(): Boolean {
        for ((_, value) in fixedSafesData)
            if (value > 1)
                return true

        return false
    }

    override fun toString(): String = displayName

    override fun compareTo(other: Path): Int =
        when {
            sketchId > other.sketchId -> 1
            sketchId < other.sketchId -> -1
            else -> 0
        }

    @Throws(NoInternetAccessException::class)
    fun isBlocked(): BlockingType {
        // TODO: Move to Parse
        return BlockingType.UNKNOWN
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
            writeInt(sketchId)
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
        }
    }

    companion object CREATOR : Parcelable.Creator<Path> {
        override fun createFromParcel(parcel: Parcel): Path = Path(parcel)
        override fun newArray(size: Int): Array<Path?> = arrayOfNulls(size)

        const val NAMESPACE = "Path"
    }
}
