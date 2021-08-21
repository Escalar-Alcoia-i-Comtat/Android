package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

/**
 * Stores the configuration in which the [DataClass] should be displayed.
 * @author Arnau Mora
 * @since 20210819
 * @param columns The amount of columns that should be shown to the user.
 * @param downloadable Whether or not the [DataClass] is downloadable.
 */
data class DataClassDisplayOptions(val columns: Int, val downloadable: Boolean)
