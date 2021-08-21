package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.content.res.Resources
import android.util.TypedValue

/**
 * Converts dp to px.
 * @author Arnau Mora
 * @since prasobh
 * @see <a href="https://stackoverflow.com/a/6327095/5717211">StackOverflow</a>
 */
val Number.toPx
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    )
