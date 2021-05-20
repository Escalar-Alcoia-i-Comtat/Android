package com.arnyminerz.escalaralcoiaicomtat.generic

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import com.arnyminerz.escalaralcoiaicomtat.exception.CouldNotCreateNewFileException
import com.arnyminerz.escalaralcoiaicomtat.exception.CouldNotDeleteFileException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
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
 * Stores a bitmap into a file
 * @param file The file to store at
 * @param overwrite If there's already a file, it should be deleted
 * @param mkdirs If the parent dirs of the file should be created
 * @param format The format to write with
 * @param quality The quality to use
 *
 * @throws FileAlreadyExistsException When the file already exists and overwrite is set to false
 * @throws CouldNotDeleteFileException If an error occurs while deleting the file if it exists
 * @throws CouldNotCreateNewFileException If an error occurs while trying to create the image file
 */
@Throws(
    FileAlreadyExistsException::class,
    CouldNotDeleteFileException::class,
    CouldNotCreateNewFileException::class
)
fun Bitmap.storeToFile(
    file: File,
    overwrite: Boolean = true,
    mkdirs: Boolean = true,
    format: CompressFormat = CompressFormat.JPEG,
    quality: Int = 100
) {
    if (file.exists())
        if (overwrite) {
            if (!file.delete())
                throw CouldNotDeleteFileException(file)
        } else throw FileAlreadyExistsException(file)

    if (mkdirs)
        file.parentFile?.mkdirs()

    if (!file.createNewFile())
        throw CouldNotCreateNewFileException(file)

    val bos = ByteArrayOutputStream()
    compress(format, quality, bos)
    val bitmapData = bos.toByteArray()

    val fos = FileOutputStream(file)
    fos.write(bitmapData)
    fos.flush()
    fos.close()
}

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
