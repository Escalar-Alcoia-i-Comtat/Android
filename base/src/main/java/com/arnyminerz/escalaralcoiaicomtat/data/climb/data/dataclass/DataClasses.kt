package com.arnyminerz.escalaralcoiaicomtat.data.climb.data.dataclass

import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.area.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.zone.Zone

enum class DataClasses(val namespace: String) {
    AREA(Area.NAMESPACE),
    ZONE(Zone.NAMESPACE),
    SECTOR(Sector.NAMESPACE);

    companion object {
        fun find(namespace: String): DataClasses? {
            for (c in values())
                if (c.namespace == namespace)
                    return c
            return null
        }
    }
}
