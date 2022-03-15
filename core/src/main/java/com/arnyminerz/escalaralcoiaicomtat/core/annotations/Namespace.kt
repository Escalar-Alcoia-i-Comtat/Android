package com.arnyminerz.escalaralcoiaicomtat.core.annotations

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class Namespace(val namespace: String, val tableName: String) : Parcelable {
    AREA("Area", "Areas"),
    ZONE("Zone", "Zones"),
    SECTOR("Sector", "Sectors"),
    PATH("Path", "Paths");

    companion object {
        /**
         * Tries to find the [Namespace] that has [namespace] or [tableName] equal to [query].
         * @author Arnau Mora
         * @since 20220315
         */
        fun find(query: String) =
            values().find { it.namespace == query || it.tableName == query }

        /**
         * Tries to find the [Namespace] that starts with [char].
         * @author Arnau Mora
         * @since 20220315
         */
        fun find(char: Char) =
            values().find { it.namespace[0].equals(char, true) }
    }

    override fun toString(): String = namespace

    /**
     * Gets the parent namespace according to the current namespace.
     * @author Arnau Mora
     * @since 20220222
     */
    val ParentNamespace: Namespace?
        get() = when (this) {
            ZONE -> AREA
            SECTOR -> ZONE
            PATH -> SECTOR
            else -> null
        }

    /**
     * Gets the namespace of the children according to the current namespace.
     * @author Arnau Mora
     * @since 20220222
     */
    val ChildrenNamespace: Namespace?
        get() = when (this) {
            AREA -> ZONE
            ZONE -> SECTOR
            SECTOR -> PATH
            else -> null
        }

    /**
     * Checks if the set namespace may have children.
     * @author Arnau Mora
     * @since 20220222
     */
    val HashChildren: Boolean
        get() = when (this) {
            AREA, ZONE, SECTOR -> true
            else -> false
        }
}
