package com.arnyminerz.escalaralcoiaicomtat.core.utils.storage

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

/**
 * Calculates the size that should be used for decoding a [Bitmap] so no memory is wasted.
 * @author Android Developers
 * @since 20220401
 * @param options The options object to use.
 * @param reqWidth The width required to place the Bitmap in.
 * @param reqHeight The height required to place the Bitmap in.
 * @see <a href="https://developer.android.com/topic/performance/graphics/load-bitmap.html">Android Documentation</a>
 */
private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    // Raw height and width of image
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

/**
 * Reads the contents of a file and converts them into a [Bitmap].
 * @author Arnau Mora
 * @since 20210713
 * @param file The file to read from.
 * @param size The size of the container where the image will be loaded at. This can be null, and
 * the whole image will be loaded, but it's recommended to select a size, so no memory is wasted.
 * @param scale The scale to load the image at. This will optimize memory usage, and make the device
 * be not that much stressed.
 * @return The read [Bitmap], or null in case of an error.
 */
fun readBitmap(file: File, size: Pair<Int, Int>? = null, scale: Int? = null): Bitmap? = when {
    size != null -> BitmapFactory.Options().run {
        inJustDecodeBounds = true

        BitmapFactory.decodeFile(file.path, this)
        inSampleSize = calculateInSampleSize(this, size.first, size.second)

        inJustDecodeBounds = false
        BitmapFactory.decodeFile(file.path, this)
    }
    scale != null -> BitmapFactory.Options().run {
        inSampleSize = scale
        BitmapFactory.decodeFile(file.path, this)
    }
    else -> BitmapFactory.decodeFile(file.path)
}

/**
 * Reads the contents of a file and converts them into a [Bitmap].
 * If there's a failure while decoding the file, a [RuntimeException] will be thrown.
 * @author Arnau Mora
 * @since 20220104
 * @param file The file to read the [Bitmap] from.
 * @param size The size of the container where the image will be loaded at. This can be null, and
 * the whole image will be loaded, but it's recommended to select a size, so no memory is wasted.
 * @param scale The scale to load the image at. This will optimize memory usage, and make the device
 * be not that much stressed.
 * @return The read [Bitmap].
 * @throws RuntimeException If the [Bitmap] could not be read.
 * @see readBitmap This is a non-null version of that function.
 */
fun ensureBitmapRead(file: File, size: Pair<Int, Int>? = null, scale: Int? = null): Bitmap =
    readBitmap(file, size, scale)
        ?: throw RuntimeException("The image at \"$file\" could not be read.")

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
