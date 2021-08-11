package com.arnyminerz.escalaralcoiaicomtat.core.annotations

import androidx.annotation.IntDef
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AFTERNOON
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ALL_DAY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.MORNING
import com.arnyminerz.escalaralcoiaicomtat.core.shared.NO_SUN

@IntDef(ALL_DAY, MORNING, AFTERNOON, NO_SUN)
@Retention(AnnotationRetention.SOURCE)
annotation class SunTime
