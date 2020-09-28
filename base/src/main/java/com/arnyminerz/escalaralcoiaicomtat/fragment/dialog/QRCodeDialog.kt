package com.arnyminerz.escalaralcoiaicomtat.fragment.dialog

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.generic.getDisplaySize
import com.arnyminerz.escalaralcoiaicomtat.image.dpToPx
import com.arnyminerz.escalaralcoiaicomtat.image.encodeAsQrCodeBitmap
import kotlinx.android.synthetic.main.dialog_image.*
import timber.log.Timber


class QRCodeDialog(private val activity: Activity, private val text: String) : Dialog(activity) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_image)

        try {
            val display = getDisplaySize(activity)

            val size = display.first.coerceAtMost(display.second)

            val overlay = ContextCompat.getDrawable(context, R.drawable.qr_icon)
                ?.toBitmap(72.dpToPx(), 72.dpToPx())

            val bitmap = text.encodeAsQrCodeBitmap(size, overlay)
            dialog_imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Timber.e(e, "Could not load QR")
        }
    }
}