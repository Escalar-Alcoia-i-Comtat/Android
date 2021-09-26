package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
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

/**
 * Creates the directory named by this abstract pathname, including any necessary but nonexistent
 * parent directories. Note that if this operation fails it may have succeeded in creating some of
 * the necessary parent directories.
 * @author Arnau Mora
 * @since 20210416
 * @param shouldDeleteIfNotDir If the target [File] already exists, but it's not a directory, and
 * [shouldDeleteIfNotDir] is true, the already existing path will be deleted and recreated.
 */
fun File.mkdirsIfNotExists(shouldDeleteIfNotDir: Boolean = true): Boolean {
    return if (exists())
        when {
            isDirectory -> true
            shouldDeleteIfNotDir -> delete() && mkdirs()
            else -> false
        }
    else mkdirs()
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

fun File.sizeString(): String = humanReadableByteCountBin(size())

/**
 * Gets the MD5 file hash from the instance.
 * @author Arnau Mora
 * @since 20210926
 * @throws NoSuchAlgorithmException When the MD5 algorithm is not available in the device.
 * @throws IOException When there's an exception while processing the file.
 */
@Throws(NoSuchAlgorithmException::class, IOException::class)
fun File.md5Hash(): String {
    val digest = MessageDigest.getInstance("MD5")

    if (!exists())
        throw FileNotFoundException("The file at \"$path\" doesn't exist.")

    val stream = inputStream()
    val buffer = ByteArray(8192)
    var read = stream.read(buffer)
    while (read > 0) {
        digest.update(buffer, 0, read)
        read = stream.read(buffer)
    }
    val md5sum = digest.digest()
    val bigInt = BigInteger(1, md5sum)
    val output = bigInt.toString(16)
    return output.format("%32s").replace(' ', '0')
}

/**
 * Gets the file name from an Uri
 * @author Arnau Mora
 * @since 20210318
 * @param context The context to call from
 * @return The file name, or null if it could not be gotten
 */
fun Uri.fileName(context: Context): String? {
    if (scheme.equals("file")) {
        return lastPathSegment
    } else {
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                this, arrayOf(
                    MediaStore.Images.ImageColumns.DISPLAY_NAME
                ), null, null, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME))
            }
        } finally {
            cursor?.close()
        }
    }
    return null
}

/**
 * Gets the extension of the uri, based on its mime type
 * @author Arnau Mora
 * @since 20210318
 * @param context The context to call from
 * @return The file extension for the uri
 */
fun Uri.extension(context: Context): String? {
    val cr = context.contentResolver
    val mime = MimeTypeMap.getSingleton()
    return mime.getExtensionFromMimeType(cr.getType(this))
}

/**
 * Gets the mime type of the uri
 * @author Arnau Mora
 * @since 20210318
 * @param context The context to call from
 * @return The mime type of the uri
 */
fun Uri.mime(context: Context): String? {
    val cr = context.contentResolver
    return cr.getType(this)
}
