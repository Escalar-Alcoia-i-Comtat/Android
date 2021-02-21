package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import android.os.Parcel
import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.async.EXTENDED_API_URL
import com.arnyminerz.escalaralcoiaicomtat.data.climb.enum.BlockingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.enum.EndingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.enum.Grade
import com.arnyminerz.escalaralcoiaicomtat.exception.JSONResultException
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.exception.NotLoggedInException
import com.arnyminerz.escalaralcoiaicomtat.exception.UserNotFoundException
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.getStringSafe
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toTimestamp
import com.arnyminerz.escalaralcoiaicomtat.generic.jsonFromUrl
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import org.json.JSONObject
import java.io.Serializable
import java.text.SimpleDateFormat
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
data class CompletedPath(
    val id: Int,
    val timestamp: Date?,
    val userUid: String,
    val type: CompletedType,
    val attempts: Int,
    val hangs: Int,
    val path: Path
) : Serializable {
    companion object {
        fun fromDB(json: JSONObject): CompletedPath? {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val date = format.parse(json.getString("timestamp"))

            val id = json.getInt("id")
            val type = json.getInt("type")
            val attempts = json.getInt("attempts")
            val hangs = json.getInt("hangs")
            val user = json.getString("user")
            val path = json.get("path")
            return if (path is JSONObject) {
                CompletedPath(
                    id,
                    date,
                    user,
                    CompletedType.find(type)!!,
                    attempts,
                    hangs,
                    Path.fromDB(path)
                )
            } else
                null
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
    suspend fun isBlocked(networkState: ConnectivityProvider.NetworkState): BlockingType {
        if (networkState.hasInternet) {
            val json = jsonFromUrl("$EXTENDED_API_URL/path/$id")
            val blocked = json.getStringSafe("blocked")

            return BlockingType.find(blocked)
        } else
            throw NoInternetAccessException()
    }

    fun hasInfo(): Boolean = description != null || builtBy != null

    /**
     * Checks if the path's been completed by the logged in user
     * @param networkState The network state
     * @throws NotLoggedInException If the user's not logged in
     * @throws NoInternetAccessException If there's no internet access
     * @throws UserNotFoundException If the logged in user was not found in the database
     * @throws JSONResultException If there was an error loading the user's data
     * @return The completed path if found, or null if not.
     */
    @Throws(
        NotLoggedInException::class,
        NoInternetAccessException::class,
        UserNotFoundException::class,
        JSONResultException::class
    )
    suspend fun isCompleted(
        networkState: ConnectivityProvider.NetworkState
    ): CompletedPath? {
        throw NotLoggedInException()
    }

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

        suspend fun fromId(id: Int): Path {
            val json = jsonFromUrl("$EXTENDED_API_URL/path/$id")

            return fromDB(json)
        }
    }
}