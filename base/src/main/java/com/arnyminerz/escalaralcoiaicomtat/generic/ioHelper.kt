package com.arnyminerz.escalaralcoiaicomtat.generic

import com.arnyminerz.escalaralcoiaicomtat.generic.extension.times
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

fun File.tree(height: Int = 0): String {
    val builder = StringBuilder()
    if (isDirectory && listFiles() != null)
        for (file in listFiles()!!) {
            builder.append((' ' * height) + "-" + file + "\n")
            if (file.isDirectory)
                builder.append(file.tree(height + 1) + "\n")
        }
    else
        return name
    return builder.toString()
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

fun humanReadableByteCountBin(bytes: Long, locale: Locale = Locale.getDefault()): String? {
    val absB = if (bytes == Long.MIN_VALUE) Long.MAX_VALUE else abs(bytes)
    if (absB < 1024)
        return "$bytes B"

    var value = absB
    val ci: CharacterIterator = StringCharacterIterator("KMGTPE")
    var i = 40
    while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
        value = value shr 10
        ci.next()
        i -= 10
    }
    value *= java.lang.Long.signum(bytes).toLong()

    return String.format(locale, "%.1f %ciB", value / 1024.0, ci.current())
}

fun File.size(): Long = if (isDirectory) dirSize(this) else length()

fun File.sizeString(): String? = humanReadableByteCountBin(size())