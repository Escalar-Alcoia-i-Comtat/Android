package com.arnyminerz.escalaralcoiaicomtat.data.climb.types

import android.content.Context
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
import java.io.Serializable

fun Collection<Grade>.toGradesList(): Grade.GradesList {
    return Grade.GradesList(this)
}

@Suppress("unused")
class Grade(val displayName: String) : Serializable {
    companion object {

        fun gradesListOf(vararg grades: Grade): GradesList {
            val list = GradesList()
            list.addAll(grades)
            return list
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

        fun listFromStrings(strings: ArrayList<String>): GradesList {
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

    override fun toString(): String = displayName

    fun toJSON(): String = "{ \"displayName\":\"$displayName\" }"

    fun color(): Int = with(startingColorCombinations) {
        for (combination in this)
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
            } catch (ex: IllegalArgumentException) {
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
                            val gradePiece = grade.substring(3)
                            Timber.v("  It is pitch! GradePiece: $gradePiece")
                            spannable.setSpan(
                                ForegroundColorSpan(getColor(context, gradeColor(prefix))),
                                charCounter,
                                charCounter + 3,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            spannable.setSpan(
                                ForegroundColorSpan(getColor(context, gradeColor(gradePiece))),
                                charCounter + 3, // Adding 3 for starting after L#
                                charCounter + 3 + gradePiece.length, // Should be the 3 added before and then -1 for the indexing of length
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
}
