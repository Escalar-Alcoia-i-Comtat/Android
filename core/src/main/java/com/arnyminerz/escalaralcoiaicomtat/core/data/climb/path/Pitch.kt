package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

import android.content.Context
import androidx.annotation.DrawableRes
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.PitchEndingData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.PitchEndingOrientation
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.PitchEndingRappel
import org.json.JSONObject

data class Pitch(
    val endingData: PitchEndingData
) {
    companion object {
        /**
         * @param string Should be formatted "{inclination} {descent type}"
         */
        fun fromEndingDataString(string: String): Pitch? {
            val split = string
                .replace("\r", "")
                .replace("\n", "")
                .split(" ")

            if (split.size < 2) return null

            val inclination = PitchEndingOrientation.find(split[0])
            val descent = PitchEndingRappel.find(split[1])
            return if (inclination == null || descent == null)
                null
            else
                Pitch(PitchEndingData(inclination, descent))
        }
    }

    constructor(json: JSONObject) : this(
        PitchEndingData(
            PitchEndingOrientation.find(json.getString("orientation"))!!,
            PitchEndingRappel.find(json.getString("rappel"))!!
        )
    )

    fun getDisplayText(context: Context): String {
        return endingData.orientation.toString(context) + " " +
                endingData.rappel.toString(context)
    }

    fun toJSON(): JSONObject = JSONObject().apply {
        put("orientation", endingData.orientation.key)
        put("rappel", endingData.rappel.key)
    }

    /**
     * Uses the [endingData] to get a representation image for the artifo ending dialog.
     * @author Arnau Mora
     * @since 20210322
     * @return The drawable res of the image
     * @throws IllegalStateException When the current [endingData] doesn't match any valid image
     * @see DrawableRes
     * @see PitchEndingData
     */
    @DrawableRes
    @Throws(IllegalStateException::class)
    fun getRappelImage(): Int {
        return when {
            endingData.rappel == PitchEndingRappel.RAPPEL &&
                    endingData.orientation == PitchEndingOrientation.VERTICAL ->
                R.drawable.ic_reunio_rappel_vertical
            endingData.rappel == PitchEndingRappel.RAPPEL &&
                    endingData.orientation == PitchEndingOrientation.INCLINED ->
                R.drawable.ic_reunio_rappel_inclinada
            endingData.rappel == PitchEndingRappel.RAPPEL &&
                    endingData.orientation == PitchEndingOrientation.SHELF ->
                R.drawable.ic_reunio_rappel_repisa

            endingData.rappel == PitchEndingRappel.EQUIPPED &&
                    endingData.orientation == PitchEndingOrientation.VERTICAL ->
                R.drawable.ic_reunio_equipada_vertical
            endingData.rappel == PitchEndingRappel.EQUIPPED &&
                    endingData.orientation == PitchEndingOrientation.INCLINED ->
                R.drawable.ic_reunio_equipada_inclinada
            endingData.rappel == PitchEndingRappel.EQUIPPED &&
                    endingData.orientation == PitchEndingOrientation.SHELF ->
                R.drawable.ic_reunio_equipada_repisa

            endingData.rappel == PitchEndingRappel.CLEAN &&
                    endingData.orientation == PitchEndingOrientation.VERTICAL ->
                R.drawable.ic_reunio_neta_vertical
            endingData.rappel == PitchEndingRappel.CLEAN &&
                    endingData.orientation == PitchEndingOrientation.INCLINED ->
                R.drawable.ic_reunio_neta_inclinada
            endingData.rappel == PitchEndingRappel.CLEAN &&
                    endingData.orientation == PitchEndingOrientation.SHELF ->
                R.drawable.ic_reunio_neta_repisa

            else -> throw IllegalStateException("Current endingData doesn't match any valid image")
        }
    }
}
