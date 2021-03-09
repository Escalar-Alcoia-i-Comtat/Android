package com.arnyminerz.escalaralcoiaicomtat.data.climb.types

import android.content.Context
import com.arnyminerz.escalaralcoiaicomtat.R

enum class PitchEndingOrientation(val key: String) {
    VERTICAL("vertical"), SHELF("horizontal"), INCLINED("diagonal");

    companion object {
        fun find(key: String): PitchEndingOrientation? {
            for (orientation in values())
                if (orientation.key.equals(key, true))
                    return orientation
            return null
        }
    }

    override fun toString(): String {
        return key
    }

    fun toString(context: Context): String =
        when (this) {
            VERTICAL -> context.getString(R.string.path_ending_pitch_vertical)
            SHELF -> context.getString(R.string.path_ending_pitch_shelf)
            INCLINED -> context.getString(R.string.path_ending_pitch_inclined)
        }
}

enum class PitchEndingRappel(val key: String) {
    RAPPEL("rappel"), EQUIPPED("equipped"), CLEAN("clean");

    companion object {
        fun find(key: String): PitchEndingRappel? {
            for (rappel in values())
                if (rappel.key.equals(key, true))
                    return rappel
            return null
        }
    }

    override fun toString(): String = key

    fun toString(context: Context): String =
        when (this) {
            RAPPEL -> context.getString(R.string.path_ending_pitch_rappeleable)
            EQUIPPED -> ""
            CLEAN -> context.getString(R.string.path_ending_pitch_clean)
        }
}
