package com.arnyminerz.escalaralcoiaicomtat.data.info

import com.arnyminerz.escalaralcoiaicomtat.BuildConfig

class Version(version: String) {
    val major: Int
    val minor: Int
    val build: IntArray
    val tags: String?

    init {
        val tagsSplit = if (version.contains('-'))
            version.split("-") else null
        val versionNumbers = tagsSplit?.first() ?: version
        val splittedVersion = versionNumbers.split(".")
        major = splittedVersion[0].toInt()
        minor = splittedVersion[1].toInt()
        build = splittedVersion.subList(2, splittedVersion.count()).let { builds ->
            val lst = arrayListOf<Int>()
            for (e in builds)
                e.toIntOrNull()?.let { lst.add(it) }
            lst.toIntArray()
        }
        tags = tagsSplit?.get(1)
    }
}

fun version() = Version(BuildConfig.VERSION_NAME)