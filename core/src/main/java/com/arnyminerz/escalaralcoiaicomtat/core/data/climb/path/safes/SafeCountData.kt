package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes

import android.icu.text.MessageFormat
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.LongDef
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.parcelize.Parcelize


/**
 * Used as a data class for passing display data to the UI renderer.
 * @author Arnau Mora
 * @since 20210916
 * @param count The amount of safes counted.
 * @param label The string resource that matches the string to use as display for the count.
 * @param image The drawable resource that matches the icon that represents the count.
 * @param description A long description of the safe type.
 */
@Parcelize
data class SafeCountData(
    @SafeRequirement val count: Long,
    @StringRes val label: Int,
    @DrawableRes val image: Int,
    @StringRes val description: Int,
) : Parcelable {
    /**
     * Builds the class using a boolean value instead of the count.
     * @author Arnau Mora
     * @since 20210916
     * @param required Whether or not the safe is marked as required, usually used in required safes.
     * @param label The string resource that matches the string to use as display for the count.
     * @param image The drawable resource that matches the icon that represents the count.
     */
    constructor(
        required: Boolean,
        @StringRes label: Int,
        @DrawableRes image: Int,
        @StringRes description: Int,
    ) : this(
        if (required) REQUIRED else NOT_REQUIRED,
        label,
        image,
        description,
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

        /**
         * Formats the string [label] provided in [MessageFormat].
         */
        @VisibleForTesting(VisibleForTesting.PRIVATE)
        fun formatCountLabel(label: String, count: Long): String =
            MessageFormat.format(label, count.takeIf { it in 0 until 999 } ?: 0)

        @Composable
        fun stringResource(@StringRes label: Int, count: Long): String {
            val fmt: String = stringResource(label)
            return formatCountLabel(fmt, count)
        }
    }

    @Composable
    fun stringResource(): String = stringResource(label, count)
}
