package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

/**
 * Represents a change in a Path, may be an opener, a re-builder...
 * @author Arnau Mora
 * @since 20220223
 * @param name The name of who made the patch.
 * @param date When the patch was made.
 */
data class Patch(val name: String, val date: String)
