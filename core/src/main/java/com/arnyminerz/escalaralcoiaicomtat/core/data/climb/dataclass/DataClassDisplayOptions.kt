package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

import androidx.annotation.DrawableRes

/**
 * Stores the configuration in which the [DataClass] should be displayed.
 * @author Arnau Mora
 * @since 20210819
 * @param placeholderDrawable The drawable resource of the image to display while the real one is
 * being loaded.
 * @param errorPlaceholderDrawable The drawable resource of the image to load when there's an error
 * while loading the real one.
 * @param columns The amount of columns that should be shown to the user.
 * @param downloadable Whether or not the [DataClass] is downloadable.
 * @param showLocation Whether or not the location button should be visible.
 */
data class DataClassDisplayOptions(
    /**
     * The drawable resource of the image to display while the real one is being loaded.
     * @author Arnau Mora
     * @since 20210830
     */
    @DrawableRes val placeholderDrawable: Int,
    /**
     * The drawable resource of the image to load when there's an error while loading the real one.
     * @author Arnau Mora
     * @since 20210830
     */
    @DrawableRes val errorPlaceholderDrawable: Int,
    /**
     * The amount of columns that should be shown to the user.
     * @author Arnau Mora
     * @since 20210819
     */
    val columns: Int,
    /**
     * Whether or not the [DataClass] is downloadable.
     * This determines if the download button should be shown.
     * @author Arnau Mora
     * @since 20210819
     */
    val downloadable: Boolean,
    /**
     * Whether or not the location button should be visible.
     * @author Arnau Mora
     * @since 20210830
     */
    val showLocation: Boolean
) {
    /**
     * Provides a hash code value.
     * @author Arnau Mora
     * @since 20210830
     */
    override fun hashCode(): Int {
        var result = placeholderDrawable.hashCode()
        result = 31 * result + errorPlaceholderDrawable.hashCode()
        result = 31 * result + columns.hashCode()
        result = 31 * result + downloadable.hashCode()
        result = 31 * result + showLocation.hashCode()
        return result
    }

    /**
     * Checks if the current [DataClassDisplayOptions] is equal to another one.
     * @author Arnau Mora
     * @since 20210830
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataClassDisplayOptions

        if (placeholderDrawable != other.placeholderDrawable) return false
        if (errorPlaceholderDrawable != other.errorPlaceholderDrawable) return false
        if (columns != other.columns) return false
        if (downloadable != other.downloadable) return false
        if (showLocation != other.showLocation) return false

        return true
    }
}
