package com.arnyminerz.escalaralcoiaicomtat.image

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import androidx.annotation.ColorInt
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.*

@Throws(WriterException::class)
fun String.encodeAsQrCodeBitmap(
    dimension: Int,
    overlayBitmap: Bitmap? = null,
    @ColorInt color1: Int = Color.BLACK,
    @ColorInt color2: Int = Color.WHITE
): Bitmap? {

    val result: BitMatrix
    try {
        result = MultiFormatWriter().encode(
            this,
            BarcodeFormat.QR_CODE,
            dimension,
            dimension,
            hashMapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H)
        )
    } catch (e: IllegalArgumentException) {
        // Unsupported format
        return null
    }

    val w = result.width
    val h = result.height
    val pixels = IntArray(w * h)
    for (y in 0 until h) {
        val offset = y * w
        for (x in 0 until w) {
            pixels[offset + x] = if (result.get(x, y)) color1 else color2
        }
    }
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, dimension, 0, 0, w, h)

    return if (overlayBitmap != null) {
        bitmap.addOverlayToCenter(overlayBitmap)
    } else {
        bitmap
    }
}

fun Bitmap.addOverlayToCenter(overlayBitmap: Bitmap): Bitmap {

    val bitmap2Width = overlayBitmap.width
    val bitmap2Height = overlayBitmap.height
    val marginLeft = (this.width * 0.5 - bitmap2Width * 0.5).toFloat()
    val marginTop = (this.height * 0.5 - bitmap2Height * 0.5).toFloat()
    val canvas = Canvas(this)
    canvas.drawBitmap(this, Matrix(), null)
    canvas.drawBitmap(overlayBitmap, marginLeft, marginTop, null)
    return this
}

fun Int.dpToPx(): Int {
    return (this * Resources.getSystem().displayMetrics.density).toInt()
}

class QRGenerator {
    companion object {
        fun createQRImage(text: String, side: Int): Bitmap {
            val hintMap: Hashtable<EncodeHintType, ErrorCorrectionLevel?> = Hashtable()
            hintMap[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.L
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, side, side, hintMap)

            val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.RGB_565)
            for (x in 0 until side)
                for (y in 0 until side)
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)

            return bitmap
        }

        fun Bitmap.addOverlayToCenter(overlayBitmap: Bitmap): Bitmap {
            val bitmap2Width = overlayBitmap.width
            val bitmap2Height = overlayBitmap.height
            val marginLeft = (this.width * 0.5 - bitmap2Width * 0.5).toFloat()
            val marginTop = (this.height * 0.5 - bitmap2Height * 0.5).toFloat()
            val canvas = Canvas(this)
            canvas.drawBitmap(this, Matrix(), null)
            canvas.drawBitmap(overlayBitmap, marginLeft, marginTop, null)
            return this
        }
    }
}