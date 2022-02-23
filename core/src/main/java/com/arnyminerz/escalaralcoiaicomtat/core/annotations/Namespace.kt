package com.arnyminerz.escalaralcoiaicomtat.core.annotations

import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone

@Target(
    AnnotationTarget.TYPE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER
)
@Retention(AnnotationRetention.SOURCE)
annotation class Namespace

/**
 * Gets the parent namespace according to the current namespace.
 * @author Arnau Mora
 * @since 20220222
 */
@get:Namespace
@Namespace
val @receiver:Namespace String.ParentNamespace: String
    get() =
        when (this) {
            Zone.NAMESPACE -> Area.NAMESPACE
            Sector.NAMESPACE -> Zone.NAMESPACE
            Path.NAMESPACE -> Sector.NAMESPACE
            else -> ""
        }

/**
 * Gets the namespace of the children according to the current namespace.
 * @author Arnau Mora
 * @since 20220222
 */
@get:Namespace
@Namespace
val @receiver:Namespace String.ChildrenNamespace: String
    get() =
        when (this) {
            Area.NAMESPACE -> Zone.NAMESPACE
            Zone.NAMESPACE -> Sector.NAMESPACE
            Sector.NAMESPACE -> Path.NAMESPACE
            else -> ""
        }

/**
 * Checks if the set namespace may have children.
 * @author Arnau Mora
 * @since 20220222
 */
@Namespace
val @receiver:Namespace String.HasChildren: Boolean
    get() =
        when (this) {
            Area.NAMESPACE, Zone.NAMESPACE, Sector.NAMESPACE -> true
            else -> false
        }
