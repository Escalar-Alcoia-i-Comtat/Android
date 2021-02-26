package com.arnyminerz.escalaralcoiaicomtat.generic

import android.content.Context
import androidx.fragment.app.Fragment

fun Fragment.runOnUiThread(callback: Context.() -> Unit){
    context?.runOnUiThread(callback)
}