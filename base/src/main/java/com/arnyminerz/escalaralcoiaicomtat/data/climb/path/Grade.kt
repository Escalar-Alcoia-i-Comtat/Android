package com.arnyminerz.escalaralcoiaicomtat.data.climb.path

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorRes
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.join
import com.arnyminerz.escalaralcoiaicomtat.view.getColor
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

fun Collection<Grade>.toGradesList(): Grade.GradesList = Grade.GradesList(this)

private const val PATH_GRADE_SPAN_PADDING = 3

/**
 * Contains a list of all the possible grades there are.
 * @author Arnau Mora
 * @since 20210429
 */
val GRADES_LIST = listOf(
    "5º",
    "5+",
    "6a",
    "6a+",
    "6b",
    "6b+",
    "6c",
    "6c+",
    "7a",
    "7a+",
    "7b",
    "7b+",
    "7c",
    "7c+",
    "8a",
    "8a+",
    "8b",
    "8b+",
    "8c",
    "8c+"
)

@Suppress("unused")
class Grade(val displayName: String) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readString()!!)

    override fun toString(): String = displayName

    fun toJSON(): String = "{ \"displayName\":\"$displayName\" }"

    fun color(): Int {
        for (combination in startingColorCombinations)
            if (displayName[0] == combination.first)
                return combination.second

        return defaultGradeColor
    }

    /**
     * Gets the grades colored
     * @param context The context to call from
     * @param count The amount of items to show, set to -1 for all
     * @return The colored text
     */
    fun getSpannable(context: Context, count: Int = Int.MAX_VALUE): SpannableString =
        gradesListOf(this).sublist(count).getSpannable(context, count)

    class GradesList() : ArrayList<Grade>() {
        constructor(items: Collection<Grade>) : this() {
            addAll(items)
        }

        fun sublist(count: Int): GradesList {
            return try {
                take(count)
            } catch (_: IllegalArgumentException) {
                this
            }.toGradesList()
        }

        fun addAllHere(grades: MutableList<Grade>): GradesList {
            this.addAll(grades)
            return this
        }

        fun gradeNames(): ArrayList<String> {
            val list = arrayListOf<String>()
            for (grade in this)
                list.add(grade.displayName)
            return list
        }

        fun toJSONStringArray(): String {
            var result = "["

            for (u in this)
                result += "\"${u.toJSON()}\","
            result = result.substring(0, result.length - 1)

            result += "]"
            return result
        }

        override fun toString(): String {
            val builder = StringBuilder()

            for (item in this)
                builder.append(item.toString() + "\n")

            return builder.toString()
        }

        fun getSpannable(context: Context, count: Int = Int.MAX_VALUE): SpannableString {
            val spannable = SpannableString(toString().split("\n").take(count).join("\n"))
            var charCounter = 0
            for (line in toString().split("\n").take(count))
                if (line.isNotEmpty())
                    for (grade in line.split("/")) {
                        if (grade.isEmpty()) continue

                        Timber.v("Generating spannable for \"$grade\". Current char: $charCounter")
                        if (grade.indexOf(" ") >= 0) {
                            val prefix = grade.substring(0, 1)
                            val gradePiece = grade.substring(PATH_GRADE_SPAN_PADDING)
                            Timber.v("  It is pitch! GradePiece: $gradePiece")
                            spannable.setSpan(
                                ForegroundColorSpan(getColor(context, gradeColor(prefix))),
                                charCounter,
                                charCounter + PATH_GRADE_SPAN_PADDING,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            spannable.setSpan(
                                ForegroundColorSpan(getColor(context, gradeColor(gradePiece))),
                                // Adding 3 for starting after L#
                                charCounter + PATH_GRADE_SPAN_PADDING,
                                // Should be the 3 added before and then -1 for the indexing of length
                                charCounter + PATH_GRADE_SPAN_PADDING + gradePiece.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        } else {
                            spannable.setSpan(
                                ForegroundColorSpan(getColor(context, gradeColor(grade))),
                                charCounter,
                                charCounter + grade.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        charCounter += grade.length + 1 // Line jump
                    }

            return spannable
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(displayName)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Grade> {
        override fun createFromParcel(parcel: Parcel): Grade {
            return Grade(parcel)
        }

        override fun newArray(size: Int): Array<Grade?> = arrayOfNulls(size)

        /**
         * Creates a [GradesList] with [grades] as contents.
         * @author Arnau Mora
         * @since 20210406
         * @param grades The grades to add to the [GradesList]
         * @return A new [GradesList] populated with [grades].
         */
        fun gradesListOf(vararg grades: Grade): GradesList =
            GradesList().apply {
                addAll(grades)
            }

        fun fromDB(obj: String): GradesList {
            val list = GradesList()
            if (obj.contains("\n"))
                for (ln in obj
                    .replace("\r", "")
                    .split("\n"))
                    list.add(Grade(ln))
            else
                list.add(Grade(obj))
            return list
        }

        @Throws(JSONException::class)
        fun fromJSON(json: JSONObject): Grade {
            return Grade(json.getString("displayName"))
        }

        @Throws(JSONException::class)
        fun fromJSON(json: String): Grade {
            val parsed = JSONObject(json)
            return fromJSON(parsed)
        }

        @Throws(JSONException::class)
        fun fromJSONArrayList(list: JSONArray): GradesList {
            val lst = gradesListOf()
            for (o in 0 until list.length())
                with(list[o]) {
                    if (this is String) {
                        lst.add(Grade(this))
                    } else
                        lst.add(fromJSON(list.getJSONObject(o)))
                }

            return lst
        }

        fun listFromStrings(strings: Collection<String>): GradesList {
            val grades = gradesListOf()
            for (string in strings)
                with(Grade(string)) {
                    grades.add(this)
                }
            return grades
        }

        @ColorRes
        fun gradeColor(text: String): Int {
            if (text.isNotEmpty())
                for (combination in startingColorCombinations)
                    if (text[0] == combination.first)
                        return combination.second

            return defaultGradeColor
        }

        private const val gradeLColor = R.color.black
        private const val defaultGradeColor = R.color.grade_purple
        private val startingColorCombinations = arrayListOf(
            Pair('3', R.color.grade_green),
            Pair('4', R.color.grade_green),
            Pair('5', R.color.grade_green),
            Pair('6', R.color.grade_blue),
            Pair('7', R.color.grade_red),
            Pair('8', R.color.grade_black),
            Pair('A', R.color.grade_yellow),
            Pair('L', gradeLColor)
        )
    }
}
