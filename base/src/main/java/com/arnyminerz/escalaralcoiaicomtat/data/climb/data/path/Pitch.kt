package com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path

import android.content.Context
import androidx.annotation.DrawableRes
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.safes.PitchEndingData
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.safes.PitchEndingOrientation
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.safes.PitchEndingRappel

data class Pitch(
    private val endingData: PitchEndingData
) {
    companion object {
        /**
         * @param string Should be formatted "{inclination} {descent type}"
         */
        fun fromEndingDataString(string: String): Pitch? {
            val split = string.replace("\r", "").split(" ")

            if (split.size < 2) return null

            val inclination = PitchEndingOrientation.find(split[0])
            val descent = PitchEndingRappel.find(split[1])
            return if (inclination == null || descent == null)
                null
            else
                Pitch(PitchEndingData(inclination, descent))
        }
    }

    fun getDisplayText(context: Context): String {
        return endingData.orientation.toString(context) + " " +
            endingData.rappel.toString(context)
    }

    @DrawableRes
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

            else -> R.drawable.transparent
        }
    }
}
