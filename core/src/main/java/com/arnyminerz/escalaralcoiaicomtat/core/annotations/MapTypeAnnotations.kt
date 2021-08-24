package com.arnyminerz.escalaralcoiaicomtat.core.annotations

import androidx.annotation.IntDef
import com.google.android.gms.maps.GoogleMap

/**
 * Serves for annotating all Map surface types.
 * @author Arnau Mora
 * @since 20210825
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    GoogleMap.MAP_TYPE_SATELLITE,
    GoogleMap.MAP_TYPE_HYBRID,
    GoogleMap.MAP_TYPE_NONE,
    GoogleMap.MAP_TYPE_NORMAL,
    GoogleMap.MAP_TYPE_TERRAIN
)
annotation class MapType
