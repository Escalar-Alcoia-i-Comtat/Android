package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.EndingType
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

/**
 * Returns the ending type's index. This can be used for fetching values from arrays.
 * @author Arnau Mora
 * @since 20210811
 */
val @receiver:EndingType String.index: Int
    get() = when (this) {
        ENDING_TYPE_UNKNOWN -> 0
        ENDING_TYPE_PLATE -> 1
        ENDING_TYPE_PLATE_RING -> 2
        ENDING_TYPE_PLATE_LANYARD -> 3
        ENDING_TYPE_CHAIN_RING -> 4
        ENDING_TYPE_CHAIN_CARABINER -> 5
        ENDING_TYPE_PITON -> 6
        ENDING_TYPE_WALKING -> 7
        ENDING_TYPE_RAPPEL -> 8
        ENDING_TYPE_LANYARD -> 9
        ENDING_TYPE_NONE -> 10
        else -> -1
    }

/**
 * Returns the
 */
val @receiver:EndingType String.image: Int
    @DrawableRes get() = when (this) {
        ENDING_TYPE_UNKNOWN -> R.drawable.transparent
        ENDING_TYPE_PLATE -> R.drawable.ic_reunio_xapes_24
        ENDING_TYPE_PLATE_RING -> R.drawable.ic_reunio_xapesargolla
        ENDING_TYPE_PLATE_LANYARD -> R.drawable.ic_reunio_pont_de_roca
        ENDING_TYPE_CHAIN_RING -> R.drawable.ic_reunio_cadenaargolla
        ENDING_TYPE_CHAIN_CARABINER -> R.drawable.ic_reunio_cadenamosqueto
        ENDING_TYPE_PITON -> R.drawable.ic_reunio_clau
        ENDING_TYPE_WALKING -> R.drawable.ic_descens_caminant
        ENDING_TYPE_RAPPEL -> R.drawable.ic_via_rappelable
        ENDING_TYPE_LANYARD -> R.drawable.ic_reunio_pont_de_roca
        else -> R.drawable.round_close_24 // This includes NONE
    }

val @receiver:EndingType String.displayName: Int
    @StringRes get() = when (this) {
        ENDING_TYPE_UNKNOWN -> R.string.path_ending_unknown
        ENDING_TYPE_PLATE -> R.string.path_ending_plate
        ENDING_TYPE_PLATE_RING -> R.string.path_ending_plate_ring
        ENDING_TYPE_PLATE_LANYARD -> R.string.path_ending_plate_lanyard
        ENDING_TYPE_CHAIN_RING -> R.string.path_ending_chain_ring
        ENDING_TYPE_CHAIN_CARABINER -> R.string.path_ending_chain_carabiner
        ENDING_TYPE_PITON -> R.string.path_ending_piton
        ENDING_TYPE_WALKING -> R.string.path_ending_walking
        ENDING_TYPE_RAPPEL -> R.string.path_ending_rappel
        ENDING_TYPE_LANYARD -> R.string.path_ending_lanyard
        ENDING_TYPE_NONE -> R.string.path_ending_none
        else -> R.string.path_ending_unknown
    }

val @receiver:EndingType String.isUnknown: Boolean
    get() = this == ENDING_TYPE_UNKNOWN
