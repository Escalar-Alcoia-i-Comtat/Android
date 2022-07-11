package com.arnyminerz.escalaralcoiaicomtat.core.data

import androidx.annotation.IntDef

/**
 * An object for holding Semantic Versioning version names.
 *
 * Follows the Semanting Versioning rules: https://semver.org/
 * @author Arnau Mora
 * @since 20220711
 */
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
) {
    companion object {
        /**
         * Initializes a new [SemVer] object from a string.
         * The version must follow the Semantic Versioning (https://semver.org/): `MAJOR.MINOR.PATCH`.
         *
         * Example:
         * * `1.5.6`
         * @author Arnau Mora
         * @since 20220711
         * @param versionString The version string to parse.
         * @throws IllegalArgumentException When the format of the input string is not valid.
         * @throws IllegalStateException When a part of the version is not numeric.
         */
        @Throws(IllegalArgumentException::class, IllegalStateException::class)
        fun fromString(versionString: String): SemVer {
            val split = versionString
                .split('.')
                .takeIf { it.size == 3 }
                ?: throw IllegalArgumentException("The format of the version is not valid: $versionString")

            val major = split[0].toIntOrNull()
                ?: throw IllegalStateException("Major version is not numeric: ${split[0]}")
            val minor = split[1].toIntOrNull()
                ?: throw IllegalStateException("Minor version is not numeric: ${split[1]}")
            val patch = split[2].toIntOrNull()
                ?: throw IllegalStateException("Patch version is not numeric: ${split[2]}")

            return SemVer(major, minor, patch)
        }

        const val DIFF_EQUAL = 0
        const val DIFF_PATCH = 1
        const val DIFF_MINOR = 2
        const val DIFF_MAJOR = 3

        @Target(AnnotationTarget.TYPE)
        @IntDef(DIFF_EQUAL, DIFF_MAJOR, DIFF_MINOR, DIFF_PATCH)
        annotation class SemVerComparison
    }

    override fun toString(): String = "$major.$minor.$patch"

    /**
     * Compares two [SemVer]s and returns a [SemVerComparison] according to the differences. One of
     * the following will happen:
     * - If the versions are equal, [DIFF_EQUAL] is returned.
     * - If the version change is of type patch, [DIFF_PATCH] is returned. This is intended for small
     * fixes, and versions with patch changes should be compatible.
     * - If the version change is of type minor, [DIFF_MINOR] is returned. This is intended for
     * changes bigger than patch, but that might still be compatible, however, a warning is recommended.
     * - If the version change is of type major, [DIFF_MAJOR] is returned. This is intended for big
     * structural changes, and usually indicates incompatibility between versions. Caution is advised.
     * @author Arnau Mora
     * @since 20220711
     * @param other The other version to compare against to.
     * @return One of: [DIFF_EQUAL], [DIFF_PATCH], [DIFF_MINOR] or [DIFF_MAJOR].
     */
    fun compare(other: SemVer): @SemVerComparison Int =
        if (major != other.major)
            DIFF_MAJOR
        else if (minor != other.minor)
            DIFF_MINOR
        else if (patch != other.patch)
            DIFF_PATCH
        else
            DIFF_EQUAL
}