package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes

import androidx.annotation.ColorRes
import androidx.annotation.IntDef
import com.arnyminerz.escalaralcoiaicomtat.core.R

@IntDef(EquipmentType.FIXED, EquipmentType.REQUIRED)
annotation class EquipmentType() {
    companion object {
        /**
         * Sets a safe type as fixed, which is already installed on the path, and can't be removed.
         * @author Arnau Mora
         * @since 20210916
         */
        const val FIXED = 0

        /**
         * Sets a safe type as required, which is not available on the path, and should be brought
         * by the climber. May be recommended or mandatory to climb.
         * @author Arnau Mora
         * @since 20210916
         */
        const val REQUIRED = 1
    }
}

@ColorRes
fun @receiver:EquipmentType Int.getColor(): Int = when (this) {
    EquipmentType.REQUIRED -> R.color.dialog_blue
    EquipmentType.FIXED -> R.color.dialog_green
    else -> -1 // Should never be returned
}
