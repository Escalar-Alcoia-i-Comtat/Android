package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

import androidx.annotation.StringRes
import com.arnyminerz.escalaralcoiaicomtat.core.R

enum class BlockingType(val idName: String) {
    UNKNOWN("NULL"),
    DRY("dry"),
    BUILD("build"),
    BIRD("bird"),
    OLD("old"),
    ROCKS("rocks"),
    PLANTS("plants"),
    ROPE_LENGTH("rope_length");

    companion object {
        fun find(idName: String?): BlockingType {
            if (idName != null)
                for (type in values())
                    if (type.idName.startsWith(idName))
                        return type
            return UNKNOWN
        }
    }

    val index: Int
        get() {
            for ((t, type) in values().withIndex())
                if (type.idName.startsWith(idName))
                    return t - 1
            return -1
        }

    override fun toString(): String = idName

    /**
     * Gives a short description of the [BlockingType].
     * @author Arnau Mora
     * @since 20220330
     */
    val contentDescription: Int
        @StringRes
        get() = when (this) {
            UNKNOWN -> R.string.path_warning_description_unknown
            DRY -> R.string.path_warning_description_dry
            BUILD -> R.string.path_warning_description_build
            BIRD -> R.string.path_warning_description_bird
            OLD -> R.string.path_warning_description_old
            ROCKS -> R.string.path_warning_description_rocks
            PLANTS -> R.string.path_warning_description_plants
            ROPE_LENGTH -> R.string.path_warning_description_rope_length
        }

    /**
     * Gives a developed explanation of the [BlockingType].
     * @author Arnau Mora
     * @since 20220330
     */
    val explanation: Int
        @StringRes
        get() = when (this) {
            UNKNOWN -> R.string.path_warning_description_unknown
            DRY -> R.string.path_warning_dry
            BUILD -> R.string.path_warning_build
            BIRD -> R.string.path_warning_bird
            OLD -> R.string.path_warning_old
            ROCKS -> R.string.path_warning_rocks
            PLANTS -> R.string.path_warning_plants
            ROPE_LENGTH -> R.string.path_warning_rope_length
        }
}
