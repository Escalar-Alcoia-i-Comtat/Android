package com.arnyminerz.escalaralcoiaicomtat.social

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Path
import com.arnyminerz.escalaralcoiaicomtat.image.drawMultilineText
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.CompletedPathBigAdapter
import java.io.FileNotFoundException

/**
 * Generates an image from a completed path so it can be shared in the social media.
 * @param path The path that has been completed
 * @param info The completion info
 * @throws FileNotFoundException if the file doesn't exist.
 */
@ExperimentalUnsignedTypes
@Throws(FileNotFoundException::class)
fun generateSocialImage(
    context: Context,
    path: Path,
    info: CompletedPathBigAdapter.CompletedPathInfo,
    bgImage: Bitmap
): Bitmap {
    val dest = Bitmap.createBitmap(bgImage.width, bgImage.height, Bitmap.Config.ARGB_8888)
    val canvasWidth = bgImage.width
    val canvasHeight = bgImage.height

    val cs = Canvas(dest)

    // Draw the src image into the canvas
    cs.drawBitmap(bgImage, 0f, 0f, null)

    // Apply green filter to the image
    cs.drawRect(Rect(0, 0, bgImage.width, bgImage.height), Paint().apply {
        color = Color.rgb(142, 211, 31)
        style = Paint.Style.FILL
        alpha = 64
    })

    // Draw the path's name
    val tp = TextPaint()
    tp.apply {
        alpha = 255
        textSize = 100f
        color = Color.WHITE
        style = Paint.Style.FILL
        typeface = ResourcesCompat.getFont(context, R.font.alegreya_sans)
    }
    val text = path.displayName

    // This will make sure no text overflows the photo
    cs.drawMultilineText(text, tp, bgImage.width - 100, 50f, 15f)

    // Calculate the path grade values
    val pathGrade = path.grade()
    val gradeText = pathGrade.displayName
    val gradeTextBounds = Rect()
    tp.getTextBounds(gradeText, 0, gradeText.length, gradeTextBounds)
    val gradeTextWidth = gradeTextBounds.width()
    val gradeTextHeight = gradeTextBounds.height()
    val gradeTextX = canvasWidth - gradeTextWidth - 20f
    val gradeTextY = canvasHeight - gradeTextHeight - 20f

    // Draw the path's background circle
    tp.apply {
        textSize = 130f
        color = pathGrade.color()
    }
    cs.drawOval(
        gradeTextX - 10f,
        gradeTextY - 10f,
        gradeTextX + gradeTextWidth + 10f,
        gradeTextY + gradeTextHeight + 10f,
        tp
    )

    // Draw the path's grade
    tp.apply {
        color = Color.WHITE
    }
    cs.drawText(gradeText, gradeTextX, gradeTextY, tp)

    // Store it into the fs
    //dest.compress(Bitmap.CompressFormat.JPEG, 100, targetImageFile.outputStream())

    // Return the drawn bitmap
    return dest
}