package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.content.res.Resources
import android.util.TypedValue
import androidx.compose.ui.unit.Dp

/**
 * Converts the Dp value into Px.
 * @author Arnau Mora
 * @since 20220329
 */
val Dp.px
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value,
        Resources.getSystem().displayMetrics
    )
