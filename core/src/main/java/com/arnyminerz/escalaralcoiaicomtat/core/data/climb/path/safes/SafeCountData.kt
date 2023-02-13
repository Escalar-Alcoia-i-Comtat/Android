package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.LongDef
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

/**
 * Used as a data class for passing display data to the UI renderer.
 * @author Arnau Mora
 * @since 20210916
 * @param count The amount of safes counted.
 * @param countableLabelRes The string resource that matches the string to use as display for the
 * count.
 * @param uncountableLabelRes The string resource that matches the string to use when no count is
 * available.
 * @param image The drawable resource that matches the icon that represents the count.
 */
@Parcelize
data class SafeCountData(
    @SafeRequirement val count: Long,
    @StringRes val countableLabelRes: Int?,
    @StringRes val uncountableLabelRes: Int,
    @DrawableRes val image: Int
) : Parcelable {
    /**
     * Builds the class using a boolean value instead of the count.
     * @author Arnau Mora
     * @since 20210916
     * @param required Whether or not the safe is marked as required, usually used in required safes.
     * @param countableLabelRes The string resource that matches the string to use as display for the
     * count.
     * @param uncountableLabelRes The string resource that matches the string to use when no count is
     * available.
     * @param image The drawable resource that matches the icon that represents the count.
     */
    constructor(
        required: Boolean,
        @StringRes countableLabelRes: Int?,
        @StringRes uncountableLabelRes: Int,
        @DrawableRes image: Int
    ) : this(
        if (required) REQUIRED else NOT_REQUIRED,
        countableLabelRes,
        uncountableLabelRes,
        image
    )

    @LongDef(REQUIRED, NOT_REQUIRED)
    internal annotation class SafeRequirement

    companion object {
        /**
         * Marks a [SafeCountData.count] attribute as required.
         * @author Arnau Mora
         * @since 20210916
         */
        const val REQUIRED = 1L

        /**
         * Marks a [SafeCountData.count] attribute as not required.
         * @author Arnau Mora
         * @since 20210916
         */
        const val NOT_REQUIRED = 0L
    }
}
