package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.FileDescriptor
import java.io.IOException

/**
 * Tries to use the WEB_LOSSLESS codec, if API level is less than R, uses WEBP
 * @author Arnau Mora
 * @since 20210323
 * @see CompressFormat.WEBP_LOSSLESS
 * @see CompressFormat.WEBP
 */
@Suppress("DEPRECATION")
val WEBP_LOSSLESS_LEGACY: CompressFormat
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        CompressFormat.WEBP_LOSSLESS
    else CompressFormat.WEBP

/**
 * Tries to use the WEBP_LOSSY codec, if API level is less than R, uses WEBP
 * @author Arnau Mora
 * @since 20210323
 * @see CompressFormat.WEBP_LOSSY
 * @see CompressFormat.WEBP
 */
@Suppress("DEPRECATION")
val WEBP_LOSSY_LEGACY: CompressFormat
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        CompressFormat.WEBP_LOSSY
    else CompressFormat.WEBP

/**
 * Crops the Bitmap into a square image.
 * @author Arnau Mora
 * @since 20210520
 * @return The new cropped image, or null if there has been an error.
 * @throws IllegalArgumentException When there has been an error while processing the new image's
 * size.
 */
@Throws(IllegalArgumentException::class)
fun Bitmap.cropToSquare(): Bitmap? {
    val newWidth = if (height > width) width else height
    val newHeight = if (height > width) height - (height - width) else height
    var cropW = (width - height) / 2
    cropW = if (cropW < 0) 0 else cropW
    var cropH = (height - width) / 2
    cropH = if (cropH < 0) 0 else cropH
    return Bitmap.createBitmap(this, cropW, cropH, newWidth, newHeight)
}

/**
 * Extracts a [Bitmap] from an [Uri].
 * @author Arnau Mora
 * @since 20210425
 * @param contentResolver The content resolve
 * @throws IOException When there was an error while loading the bitmap.
 */
@Throws(IOException::class)
fun getBitmapFromUri(contentResolver: ContentResolver, uri: Uri): Bitmap? {
    val parcelFileDescriptor: ParcelFileDescriptor =
        contentResolver.openFileDescriptor(uri, "r") ?: return null
    val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
    val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
    parcelFileDescriptor.close()
    return image
}

/**
 * Resizes the Bitmap to the selected size.
 * @author Arnau Mora
 * @since 20210604
 * @param width The desired width for the new image.
 * @param height The desired height for the new image.
 * @return The resized [Bitmap].
 */
fun Bitmap.resize(width: Int, height: Int): Bitmap =
    Bitmap.createScaledBitmap(this, width, height, false)

/**
 * Scales a [Bitmap] to the desired scale.
 * @author Arnau Mora
 * @since 20210612
 * @param scale The desired scale for the new image.
 */
fun Bitmap.scale(scale: Float): Bitmap {
    val newWidth = width * scale
    val newHeight = height * scale
    return resize(newWidth.toInt(), newHeight.toInt())
}
