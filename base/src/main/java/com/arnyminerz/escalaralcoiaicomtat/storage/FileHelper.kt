package com.arnyminerz.escalaralcoiaicomtat.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

const val KBYTE = 1024

fun filesDir(context: Context): File = context.filesDir
fun dataDir(context: Context): File = File(filesDir(context), "data")

fun readBitmap(file: File): Bitmap =
    BitmapFactory.decodeStream(file.inputStream())

fun storeFile(file: File, stream: InputStream) {
    file.parentFile?.mkdirs()
    val output: OutputStream = FileOutputStream(file)

    val data = ByteArray(KBYTE)
    var count: Int

    while (stream.read(data).also { count = it } != -1)
        output.write(data, 0, count)

    output.close()
    stream.close()
}
