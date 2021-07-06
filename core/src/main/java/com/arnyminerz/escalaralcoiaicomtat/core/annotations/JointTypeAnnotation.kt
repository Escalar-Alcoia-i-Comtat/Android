package com.arnyminerz.escalaralcoiaicomtat.core.annotations

import androidx.annotation.IntDef
import com.google.android.gms.maps.model.JointType

@Retention(AnnotationRetention.SOURCE)
@IntDef(JointType.BEVEL, JointType.DEFAULT, JointType.ROUND)
annotation class JointType
