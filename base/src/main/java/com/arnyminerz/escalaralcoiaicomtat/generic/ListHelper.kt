package com.arnyminerz.escalaralcoiaicomtat.generic

fun MutableCollection<String>.toStringLineJumps(): String {
    val sb = StringBuilder()
    for (line in this)
        sb.append(line); sb.append('\n')
    return sb.toString()
}

fun Collection<Boolean>.allTrue(): Boolean {
    for (i in this)
        if (!i)
            return false

    return true
}