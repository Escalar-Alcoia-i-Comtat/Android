package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorRes
import androidx.annotation.IntRange
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.view.getColor
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

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

    fun toJSON(): JSONObject = JSONObject("{ \"displayName\":\"$displayName\" }")

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
    fun getSpannable(
        context: Context,
        @IntRange(from = 0, to = Int.MAX_VALUE.toLong()) count: Int = Int.MAX_VALUE
    ): SpannableString = listOf(this).take(count).getSpannable(context)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(displayName)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Grade> {
        override fun createFromParcel(parcel: Parcel): Grade {
            return Grade(parcel)
        }

        override fun newArray(size: Int): Array<Grade?> = arrayOfNulls(size)

        fun fromDB(obj: String): List<Grade> {
            val list = arrayListOf<Grade>()
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
        fun fromJSONArrayList(list: JSONArray): List<Grade> {
            val lst = arrayListOf<Grade>()
            for (o in 0 until list.length())
                with(list[o]) {
                    if (this is String) {
                        lst.add(Grade(this))
                    } else
                        lst.add(fromJSON(list.getJSONObject(o)))
                }

            return lst
        }

        fun listFromStrings(strings: Collection<String>): List<Grade> {
            val grades = arrayListOf<Grade>()
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

        private val gradeLColor = R.color.black
        private val defaultGradeColor = R.color.grade_purple
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

fun Iterable<Grade>?.string(): String {
    val builder = StringBuilder()

    if (this != null)
        for (item in this)
            builder.append(item.toString() + "\n")

    return builder.toString()
}

fun Iterable<Grade>.getSpannable(context: Context): SpannableString {
    // This is the full grade as a String
    val str = string()
        .replace("\n", "") // Remove all line jumps
    Timber.v("Spanning grade text: $str")
    // Sample str 1: 6b+(A1e)L1 6b+ (A1e)L2 6a+ (A1e)
    // Sample str 2: 6b+
    val lSplittedMutable = str.split('L').toMutableList()
    // Splitted sample 1: [6b+(A1e),1 6b+ (A1e),2 6a+ (A1e)]
    // Splitted sample 2: [6b+]
    // Remove the first item if starts with L, since it's added wrongly
    if (str.startsWith("L"))
        lSplittedMutable.removeAt(0)
    // Add all the "L"s again
    val startingIndex = if (str.startsWith("L")) 0 else 1
    if (lSplittedMutable.size > 1)
        for (i in startingIndex until lSplittedMutable.size)
            lSplittedMutable[i] = "L" + lSplittedMutable[i]
    // Splitted sample 1: [6b+(A1e),L1 6b+ (A1e),L2 6a+ (A1e)]
    // Splitted sample 2: [6b+]
    // Builds a string with each parameter in new lines.
    val stringBuilder = StringBuilder()
    for (piece in lSplittedMutable)
        stringBuilder.appendLine(piece)
    // Transforms the builder into a string, and removes the last line jump.
    val jumpedString = stringBuilder.toString().substringBeforeLast('\n')

    // Create a new spannable with the built string
    val spannable = SpannableString(jumpedString)

    // Start coloring
    val maxLength = jumpedString.length
    var charCounter = 0
    while (charCounter < maxLength) {
        val char = jumpedString[charCounter]
        if (char == '\n') {
            // If it's a line jump, ignore
            charCounter++
        } else if (char.isDigit() || char == 'A') {
            // If the character is a number, such as in "..[6]b+.."
            val followingChar = jumpedString.getOrNull(charCounter + 1) // Get next char
            if (followingChar == null) {
                // If the next char was not found, can't do anything with the text, let default color
                charCounter++
                continue
            }
            // Get the full chain
            var subCharCounter = charCounter
            while (subCharCounter < maxLength) {
                val subChar = jumpedString[subCharCounter]
                if (subChar.isLetterOrDigit() || subChar == '+' || subChar == '-')
                    subCharCounter++
                else break
            }
            val gradeChain = jumpedString.substring(charCounter, subCharCounter)
            Timber.v("Detected grade $gradeChain. Painting...")
            spannable.setSpan(
                ForegroundColorSpan(getColor(context, Grade.gradeColor(gradeChain))),
                charCounter,
                subCharCounter,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            charCounter = subCharCounter + 1
        } else if (char == 'L') {
            // If it's an L indicator
            // Get remaining text
            val cut = jumpedString.substring(charCounter)
            // Cut until next space
            val lspace = cut.substringBefore(' ')
            val subCharCount = charCounter + lspace.length
            Timber.v("Detected L marker $lspace. Painting...")
            spannable.setSpan(
                ForegroundColorSpan(getColor(context, Grade.gradeColor(lspace))),
                charCounter,
                subCharCount,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            charCounter = subCharCount + 1
        } else charCounter++
    }
    return spannable
}
