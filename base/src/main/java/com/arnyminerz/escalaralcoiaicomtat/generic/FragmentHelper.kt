package com.arnyminerz.escalaralcoiaicomtat.generic

import android.content.Context
import androidx.fragment.app.Fragment

fun Fragment.onUiThread(callback: Context.() -> Unit){
    context?.onUiThread(callback)
}