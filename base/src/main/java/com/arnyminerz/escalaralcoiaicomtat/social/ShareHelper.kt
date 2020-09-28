package com.arnyminerz.escalaralcoiaicomtat.social

import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_STREAM
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.arnyminerz.escalaralcoiaicomtat.R
import java.io.ByteArrayOutputStream
import java.io.File

fun shareBitmap(context: Context, bitmap: Bitmap) {
    val share = Intent(Intent.ACTION_SEND)
    share.type = "image/jpeg"

    val bytes = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)

    val f = File(context.cacheDir, "temp.jpeg")
    if (f.exists()) f.delete()
    if (f.parentFile?.exists() != true) f.parentFile?.mkdirs()

    f.createNewFile()
    val fo = f.outputStream()
    fo.write(bytes.toByteArray())

    share.putExtra(
        EXTRA_STREAM,
        FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", f)
    )
    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    context.startActivity(
        Intent.createChooser(
            share,
            context.getString(R.string.action_share_with)
        )
    )
}