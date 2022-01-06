package com.arnyminerz.escalaralcoiaicomtat.core.annotations

import androidx.annotation.DrawableRes
import androidx.annotation.StringDef
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_CHAIN_CARABINER
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_CHAIN_RING
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_LANYARD
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_NONE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_PITON
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_PLATE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_PLATE_LANYARD
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_PLATE_RING
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_RAPPEL
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_UNKNOWN
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENDING_TYPE_WALKING

@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
@StringDef(
    ENDING_TYPE_UNKNOWN,
    ENDING_TYPE_PLATE,
    ENDING_TYPE_PLATE_RING,
    ENDING_TYPE_PLATE_LANYARD,
    ENDING_TYPE_CHAIN_RING,
    ENDING_TYPE_CHAIN_CARABINER,
    ENDING_TYPE_PITON,
    ENDING_TYPE_WALKING,
    ENDING_TYPE_RAPPEL,
    ENDING_TYPE_LANYARD,
    ENDING_TYPE_NONE
)
@Retention(AnnotationRetention.SOURCE)
annotation class EndingType

val @receiver:EndingType String.drawable: Int
    @DrawableRes
    get() = when (this) {
        ENDING_TYPE_PLATE -> R.drawable.ic_reunio_xapes_24
        ENDING_TYPE_PLATE_RING -> R.drawable.ic_reunio_xapesargolla
        ENDING_TYPE_PLATE_LANYARD -> R.drawable.ic_lanyard
        ENDING_TYPE_LANYARD -> R.drawable.ic_reunio_pont_de_roca
        ENDING_TYPE_CHAIN_RING -> R.drawable.ic_reunio_cadenaargolla
        ENDING_TYPE_CHAIN_CARABINER -> R.drawable.ic_reunio_cadenamosqueto
        ENDING_TYPE_PITON -> R.drawable.ic_reunio_clau
        ENDING_TYPE_WALKING -> R.drawable.ic_descens_caminant
        ENDING_TYPE_RAPPEL -> R.drawable.ic_via_rappelable
        else -> R.drawable.round_close_24
    }
