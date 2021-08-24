package com.arnyminerz.escalaralcoiaicomtat.core.annotations

import androidx.annotation.StringDef
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
 * Serves for annotating all Path's Ending Type strings.
 * @author Arnau Mora
 * @since 20210825
 */
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
