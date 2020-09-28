package com.arnyminerz.escalaralcoiaicomtat.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.arnyminerz.escalaralcoiaicomtat.generic.toStringLineJumps
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toCollection
import java.io.*
import java.nio.charset.StandardCharsets

fun filesDir(context: Context): File = context.filesDir
fun dataDir(context: Context): File = File(filesDir(context), "data")

const val SECTORS_IMAGES_PATH = "images/sector"
const val ZONES_IMAGES_PATH = "images/zone"
const val AREAS_IMAGES_PATH = "images/area"


@Throws(IOException::class)
fun readFile(file: File): String {
    if (!file.exists())
        throw IOException("File doesn't exist!")
    val sb = StringBuilder()
    val br = InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)
    for (line in br.readLines()) {
        sb.append(line)
        sb.append('\n')
    }
    br.close()
    return sb.toString()
}

suspend fun syncReadFile(file: File): String {
    val lines = mutableListOf<String>()
    asyncReadFile(file).toCollection(lines) // Read the file
    return lines.toStringLineJumps()
}

/**
 * Reads all the lines of the file, and starts emitting them
 */
suspend fun asyncReadFile(file: File): Flow<String> = flow {
    val lines = file.readLines()
    for (line in lines)
        emit(line)
}

fun readBitmap(file: File): Bitmap =
    BitmapFactory.decodeFile(file.path)

fun storeFile(file: File, stream: InputStream) {
    file.parentFile?.mkdirs()
    val output: OutputStream = FileOutputStream(file)

    val data = ByteArray(1024)
    var count: Int

    while (stream.read(data).also { count = it } != -1)
        output.write(data, 0, count)

    output.close()
    stream.close()
}