package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

import com.arnyminerz.escalaralcoiaicomtat.core.annotations.EndingType
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.PitchEndingData

data class Pitch(
    var height: Long? = null,
    var grade: String? = null,
    @EndingType var ending: String? = null,
    var endingData: PitchEndingData? = null,
)
