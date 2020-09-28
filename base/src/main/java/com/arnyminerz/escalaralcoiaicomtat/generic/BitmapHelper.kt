package com.arnyminerz.escalaralcoiaicomtat.generic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.arnyminerz.escalaralcoiaicomtat.exception.CouldNotCreateNewFileException
import com.arnyminerz.escalaralcoiaicomtat.exception.CouldNotDeleteFileException
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.io.*
import java.net.HttpURLConnection
import java.net.URL


fun textAsBitmap(text: String, textSize: Float, textColor: Int): Bitmap {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.textSize = textSize
    paint.color = textColor
    paint.textAlign = Paint.Align.LEFT
    val baseline: Float = -paint.ascent() // ascent() is negative
    val width = (paint.measureText(text) + 0.5f).toInt() // round
    val height = (baseline + paint.descent() + 0.5f).toInt()
    val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(image)
    canvas.drawText(text, 0f, baseline, paint)
    return image
}

fun drawableToBitmap(drawable: Drawable): Bitmap? {
    if (drawable is BitmapDrawable) {
        if (drawable.bitmap != null) {
            return drawable.bitmap
        }
    }
    val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
        Bitmap.createBitmap(
            1,
            1,
            Bitmap.Config.ARGB_8888
        ) // Single color bitmap will be created of 1x1 pixel
    } else {
        Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
    }
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

fun scaleBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap =
    Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

@Deprecated("Use Glide")
suspend fun getBitmapFromURL(src: String): Bitmap? {
    val url = URL(src)
    val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
    connection.doInput = true
    connection.connect()
    val input: InputStream = connection.inputStream
    return BitmapFactory.decodeStream(input)
}

@Deprecated("Use Glide")
suspend fun getBitmapFromURL(
    glide: RequestManager,
    url: String
): Bitmap {
    val futureBitmap = glide
        .asBitmap()
        .load(url)
        .submit()

    while (!futureBitmap.isDone);

    return futureBitmap.get()
}

fun getBitmapFromURL(context: Context, src: String, callback: (bitmap: Bitmap) -> Unit) {
    Glide.with(context)
        .asBitmap()
        .load(src)
        .into(object : CustomTarget<Bitmap>() {
            override fun onLoadCleared(placeholder: Drawable?) {}

            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                callback(resource)
            }

        })
}

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

fun Bitmap.scale(maxWidth: Int, maxHeight: Int): Bitmap =
    scaleBitmap(this, maxWidth, maxHeight)

fun Bitmap.cutSquare(): Bitmap {
    val width: Int = width
    val height: Int = height
    val newWidth = if (height > width) width else height
    val newHeight = if (height > width) height - (height - width) else height
    var cropW = (width - height) / 2
    cropW = if (cropW < 0) 0 else cropW
    var cropH = (height - width) / 2
    cropH = if (cropH < 0) 0 else cropH

    return Bitmap.createBitmap(this, cropW, cropH, newWidth, newHeight)
}

fun Bitmap.toInputStream(): InputStream {
    val bos = ByteArrayOutputStream()
    compress(CompressFormat.PNG, 0 /*ignored for PNG*/, bos)
    val bitmapdata: ByteArray = bos.toByteArray()
    return ByteArrayInputStream(bitmapdata)
}

fun Drawable?.toBitmap(): Bitmap? = this?.let { drawableToBitmap(this) }