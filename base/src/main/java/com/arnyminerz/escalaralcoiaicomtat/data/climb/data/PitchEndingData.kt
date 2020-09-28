package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import com.arnyminerz.escalaralcoiaicomtat.data.climb.enum.PitchEndingOrientation
import com.arnyminerz.escalaralcoiaicomtat.data.climb.enum.PitchEndingRappel

class PitchEndingData(val orientation: PitchEndingOrientation, val rappel: PitchEndingRappel) {
    override fun toString(): String = dbValue()

    private fun dbValue(): String = "$orientation $rappel"
}