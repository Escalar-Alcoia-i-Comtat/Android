package com.arnyminerz.escalaralcoiaicomtat.data.climb.types

enum class DownloadStatus {
    NOT_DOWNLOADED, DOWNLOADED, DOWNLOADING;

    override fun toString(): String = name

    operator fun not(): Boolean = this != DOWNLOADED

    fun isDownloaded(): Boolean = this == DOWNLOADED
}
