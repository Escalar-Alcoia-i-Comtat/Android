package com.arnyminerz.escalaralcoiaicomtat.device

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

@Suppress("DEPRECATION")
fun vibrate(context: Context, duration: Long) {
    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    else
        v.vibrate(duration)
}
