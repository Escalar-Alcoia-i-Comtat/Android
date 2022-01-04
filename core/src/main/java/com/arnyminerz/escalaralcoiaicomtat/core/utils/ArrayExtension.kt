package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.content.Context
import androidx.annotation.StringRes
import androidx.collection.ArrayMap
import androidx.work.Data

fun ArrayList<String>.replace(find: String, replace: String): ArrayList<String> {
    for (u in this)
        u.replace(find, replace)
    return this
}

fun ArrayList<String>.replace(find: Regex, replace: String): ArrayList<String> {
    for (u in this)
        u.replace(find, replace)
    return this
}

data class LinePattern(val context: Context, @StringRes val text: Int)

fun <E> ArrayList<E>.toStringLineJumping(
    from: Int = 0,
    eachLineHolder: LinePattern? = null
): String {
    var result = ""
    var c = -1
    for (item in this) {
        c++
        if (c < from) continue
        result += (eachLineHolder?.context?.getString(eachLineHolder.text, item.toString())
            ?: item.toString()) + "\n"
    }
    result = if (result.isNotEmpty()) result.substring(0, result.length - 1) else result
    return result
}

fun java.util.ArrayList<String>.split(delimiter: String): java.util.ArrayList<String> {
    val list = arrayListOf<String>()
    for (item in this)
        list.addAll(item.split(delimiter))
    return list
}

fun ArrayMap<String, Any?>.toWorkData(): Data =
    Data.Builder().apply {
        this.putAll(this@toWorkData as Map<String, Any?>)
    }.build()
