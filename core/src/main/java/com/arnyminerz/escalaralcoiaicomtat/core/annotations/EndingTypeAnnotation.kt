package com.arnyminerz.escalaralcoiaicomtat.core.annotations

import androidx.annotation.DrawableRes
import androidx.annotation.StringDef
import androidx.annotation.StringRes
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

/**
 * Gets the display text the the ending.
 * @author Arnau Mora
 * @since 20220106
 */
val @receiver:EndingType String.text: Int
    @StringRes
    get() = when (this) {
        ENDING_TYPE_PLATE -> R.string.path_ending_plate
        ENDING_TYPE_PLATE_RING -> R.string.path_ending_plate_ring
        ENDING_TYPE_PLATE_LANYARD -> R.string.path_ending_plate_lanyard
        ENDING_TYPE_LANYARD -> R.string.path_ending_lanyard
        ENDING_TYPE_CHAIN_RING -> R.string.path_ending_chain_ring
        ENDING_TYPE_CHAIN_CARABINER -> R.string.path_ending_chain_carabiner
        ENDING_TYPE_PITON -> R.string.path_ending_piton
        ENDING_TYPE_WALKING -> R.string.path_ending_walking
        ENDING_TYPE_RAPPEL -> R.string.path_ending_rappel
        ENDING_TYPE_UNKNOWN -> R.string.path_ending_unknown
        else -> R.string.path_ending_none
    }
