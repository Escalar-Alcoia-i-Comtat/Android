package com.arnyminerz.escalaralcoiaicomtat.core.utils

import java.io.File
import java.text.CharacterIterator
import java.text.StringCharacterIterator
import java.util.*
import kotlin.math.abs

fun deleteDir(dir: File?): Boolean {
    if (dir == null || !dir.exists())
        return false
    when {
        dir.isDirectory -> {
            val files = dir.listFiles()
            if (files != null)
                for (d in files)
                    if (!deleteDir(d))
                        return false
            return dir.delete()
        }
        dir.isFile -> return dir.delete()
        else -> return false
    }
}

fun File.deleteDirAndContents(): Boolean {
    return deleteDir(this)
}

fun File.deleteIfExists(): Boolean {
    return if (exists())
        deleteDirAndContents()
    else true
}

fun dirSize(file: File): Long {
    if (file.exists()) {
        var result: Long = 0
        val fileList: Array<File>? = file.listFiles()
        if (fileList != null)
            for (i in fileList.indices)
            // Recursive call if it's a directory
                result += if (fileList[i].isDirectory)
                    dirSize(fileList[i])
                else
                    fileList[i].length() // Sum the file size in bytes
        return result // return the file size
    }
    return 0
}

private const val KBYTE = 1024
private const val KBYTE_D = 1024.0
private const val FOURTY = 40
private const val TEN = 10
private const val POINTER = 0xfffccccccccccccL
fun humanReadableByteCountBin(bytes: Long, locale: Locale = Locale.getDefault()): String {
    val absB = if (bytes == Long.MIN_VALUE) Long.MAX_VALUE else abs(bytes)
    if (absB < KBYTE)
        return "$bytes B"

    var value = absB
    val ci: CharacterIterator = StringCharacterIterator("KMGTPE")
    var i = FOURTY
    while (i >= 0 && absB > POINTER shr i) {
        value = value shr TEN
        ci.next()
        i -= TEN
    }
    value *= java.lang.Long.signum(bytes).toLong()

    return String.format(locale, "%.1f %ciB", value / KBYTE_D, ci.current())
}

fun File.size(): Long = if (isDirectory) dirSize(this) else length()
