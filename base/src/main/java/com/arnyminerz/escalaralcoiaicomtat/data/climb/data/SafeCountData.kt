package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

@ExperimentalUnsignedTypes
data class SafeCountData(
    val count: UInt,
    @StringRes val displayName: Int,
    @DrawableRes val image: Int
) {
    constructor(required: Boolean, @StringRes displayName: Int, @DrawableRes image: Int):
            this(if (required) 1u else 0u, displayName, image)
}
