package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

/**
 * Stores the configuration in which the [DataClass] should be displayed.
 * @author Arnau Mora
 * @since 20210819
 * @param columns The amount of columns that should be shown to the user.
 * @param downloadable Whether or not the [DataClass] is downloadable.
 * @param showLocation Whether or not the location button should be visible.
 */
data class DataClassDisplayOptions(
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
)
