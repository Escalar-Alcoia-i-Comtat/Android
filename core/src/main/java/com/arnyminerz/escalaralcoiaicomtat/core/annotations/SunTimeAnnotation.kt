package com.arnyminerz.escalaralcoiaicomtat.core.annotations

import androidx.annotation.IntDef
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AFTERNOON
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ALL_DAY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.MORNING
import com.arnyminerz.escalaralcoiaicomtat.core.shared.NO_SUN

/**
 * Serves for annotating all Path's sun time strings.
 * @author Arnau Mora
 * @since 20210825
 */
@IntDef(ALL_DAY, MORNING, AFTERNOON, NO_SUN)
@Retention(AnnotationRetention.SOURCE)
annotation class SunTime
