package com.arnyminerz.escalaralcoiaicomtat.generic

import android.content.Context
import androidx.fragment.app.Fragment
import org.jetbrains.anko.runOnUiThread

fun Fragment.runOnUiThread(callback: Context.() -> Unit){
    context?.runOnUiThread(callback)
}