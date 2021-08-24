package com.arnyminerz.escalaralcoiaicomtat.core.annotations

import androidx.annotation.IntDef
import com.google.android.gms.maps.model.JointType

/**
 * Serves for annotating all Map's geometry joint type.
 * @author Arnau Mora
 * @since 20210825
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(JointType.BEVEL, JointType.DEFAULT, JointType.ROUND)
annotation class JointType
