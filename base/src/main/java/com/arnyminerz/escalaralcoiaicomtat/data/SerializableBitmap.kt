package com.arnyminerz.escalaralcoiaicomtat.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import java.io.Serializable

class SerializableBitmap(bitmap: Bitmap) : Serializable {
    private val pixels: IntArray

    private val width: Int = bitmap.width
    private val height: Int = bitmap.height

    val bitmap: Bitmap
        get() = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

    init {
        pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    }

    fun drawable(context: Context): BitmapDrawable = BitmapDrawable(context.resources, bitmap)
}