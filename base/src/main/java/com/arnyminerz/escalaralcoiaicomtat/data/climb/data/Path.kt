package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import android.os.Parcel
import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.async.EXTENDED_API_URL
import com.arnyminerz.escalaralcoiaicomtat.data.climb.types.BlockingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.types.EndingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.types.Grade
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.getStringSafe
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toTimestamp
import com.arnyminerz.escalaralcoiaicomtat.generic.jsonFromUrl
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
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

data class Path (
    val objectId: String,
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
) : DataClassImpl, Comparable<Path> {
    /**
     * Creates a new sector from the data of a ParseObject.
     * Note: This doesn't add children
     * @author Arnau Mora
     * @since 20210312
     * @param parseObject The object to get data from. It must be of class Sector
     * @see ParseObject
     */
    constructor(parseObject: ParseObject) : this(
        parseObject.getString("objectId")!!,
        parseObject.getDate("updatedAt"),
        parseObject.getInt("sketchId"),
        parseObject.getString("displayName")!!,
        Grade.listFromStrings(parseObject.getString("grade")!!.split(' ')),
        arrayListOf(*parseObject.getList<Int>("height")!!.toTypedArray()),
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
        parseObject.getString("description"),
        parseObject.getString("builtBy"),
        parseObject.getList<String>("rebuiltBy")?.joinToString(separator = ", ")
    ) {
        // endings, pitches
        val endingsList = parseObject.getList<ParseObject>("ending")
        endingsList?.let {
            for (ending in it)
                endings.add(EndingType.find(ending.getString("name")))
        }
        val endingArtifo = parseObject.getString("endingArtifo")
        endingArtifo?.let {
            val artifos = it.replace("\r", "").split("\n")
            for (artifo in artifos)
                Pitch.fromEndingDataString(artifo)?.let { it1 -> pitches.add(it1) }
        }
    }

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

    fun hasSafeCount(): Boolean {
        var anyGreaterThanOne = false

        for (c in 0 until fixedSafesData.count())
            fixedSafesData[c].let { data ->
                if (data.count > 1)
                    anyGreaterThanOne = true
            }

        return anyGreaterThanOne
    }

    override fun toString(): String {
        return displayName
    }

    override fun compareTo(other: Path): Int =
        when {
            sketchId > other.sketchId -> 1
            sketchId < other.sketchId -> -1
            else -> 0
        }

    @Throws(NoInternetAccessException::class)
    fun isBlocked(networkState: ConnectivityProvider.NetworkState): BlockingType {
        // TODO: Move to Parse
        if (networkState.hasInternet) {
            val json = jsonFromUrl("$EXTENDED_API_URL/path/$objectId")
            val blocked = json.getStringSafe("blocked")

            return BlockingType.find(blocked)
        } else
            throw NoInternetAccessException()
    }

    fun hasInfo(): Boolean = description != null || builtBy != null

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

    @ExperimentalUnsignedTypes
    companion object CREATOR : Parcelable.Creator<Path> {
        override fun createFromParcel(parcel: Parcel): Path = Path(parcel)
        override fun newArray(size: Int): Array<Path?> = arrayOfNulls(size)
    }
}
