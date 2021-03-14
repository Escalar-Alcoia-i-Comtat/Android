package com.arnyminerz.escalaralcoiaicomtat.generic

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.os.Build
import com.arnyminerz.escalaralcoiaicomtat.exception.CouldNotCreateNewFileException
import com.arnyminerz.escalaralcoiaicomtat.exception.CouldNotDeleteFileException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

val WEBP_LOSSLESS_LEGACY: CompressFormat
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        CompressFormat.WEBP_LOSSLESS
    else CompressFormat.WEBP

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
 * Stores a bitmap into the file
 * @param bitmap The image to store
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
fun File.storeBitmap(
    bitmap: Bitmap,
    overwrite: Boolean = true,
    mkdirs: Boolean = true,
    format: CompressFormat = CompressFormat.JPEG,
    quality: Int = 100
) =
    bitmap.storeToFile(this, overwrite, mkdirs, format, quality)
