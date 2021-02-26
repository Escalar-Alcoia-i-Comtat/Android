package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import android.os.Parcel
import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.async.EXTENDED_API_URL
import com.arnyminerz.escalaralcoiaicomtat.data.climb.enum.BlockingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.enum.EndingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.enum.Grade
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.getStringSafe
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toTimestamp
import com.arnyminerz.escalaralcoiaicomtat.generic.jsonFromUrl
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import org.json.JSONObject
import java.io.Serializable
import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.ArrayList

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

@ExperimentalUnsignedTypes
data class Path @ExperimentalUnsignedTypes constructor(
    val id: Int,
    val timestamp: Date?,
    val sectorId: Int,
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
) : Serializable, Comparable<Path> {

    fun hasSafeCount(): Boolean {
        var anyGreaterThanOne = false

        for (c in 0 until fixedSafesData.count())
            fixedSafesData[c]?.let { data ->
                if (data.count > 1u)
                    anyGreaterThanOne = true
            }

        return anyGreaterThanOne
    }

    override fun toString(): String {
        return displayName
    }

    override fun compareTo(other: Path): Int {
        return when {
            sketchId > other.sketchId -> 1
            sketchId < other.sketchId -> -1
            else -> 0
        }
    }

    @Throws(NoInternetAccessException::class)
    fun isBlocked(networkState: ConnectivityProvider.NetworkState): BlockingType {
        if (networkState.hasInternet) {
            val json = jsonFromUrl("$EXTENDED_API_URL/path/$id")
            val blocked = json.getStringSafe("blocked")

            return BlockingType.find(blocked)
        } else
            throw NoInternetAccessException()
    }

    fun hasInfo(): Boolean = description != null || builtBy != null

    fun grade(): Grade =
        if (grades.size > 0) grades.first() else throw NoSuchElementException("Grades list is empty")

    @ExperimentalUnsignedTypes
    companion object CREATOR : Parcelable.Creator<Zone> {
        override fun createFromParcel(parcel: Parcel): Zone = Zone(parcel)
        override fun newArray(size: Int): Array<Zone?> = arrayOfNulls(size)

        fun fromDB(json: JSONObject): Path {
            val showDescription =
                if (json.has("show_description")) json.getInt("show_description") == 1 else false
            return Path(
                json.getInt("id"),
                json.getString("timestamp").toTimestamp(),
                json.getInt("sector_id"),
                json.getInt("sketch_id"),
                json.getString("display_name"),
                if (json.has("grade")) Grade.fromDB(json.getString("grade")) else Grade.gradesListOf(),
                json.getStringSafe("height")?.let {
                    val list = ArrayList<Int>()
                    if (it.contains("\n"))
                        for (ln in it
                            .replace("\r", "")
                            .split("\n"))
                            list.add(ln.toInt())
                    else
                        if (it.toLowerCase(Locale.getDefault()) != "null" && it.isNotEmpty())
                            list.add(it.toInt())
                    list
                } ?: arrayListOf(),
                if (json.has("ending")) EndingType.fromDB(json.getString("ending")) else arrayListOf(),
                Pitch.fromDB(json.getString("ending_artifo")),
                FixedSafesData.fromDB(json),
                RequiredSafesData.fromDB(json),
                if (showDescription) json.getString("description") else null,
                if (showDescription) json.getString("built_by") else null,
                if (showDescription) json.getString("rebuilt_by") else null
            )
        }
    }
}