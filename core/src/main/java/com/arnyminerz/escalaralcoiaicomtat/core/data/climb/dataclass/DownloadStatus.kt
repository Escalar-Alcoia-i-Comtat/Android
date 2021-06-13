package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

/**
 * Specifies the download status of a [DataClass].
 * @author Arnau Mora
 * @since 20210413
 */
enum class DownloadStatus {
    /**
     * The [DataClass] is not downloaded.
     * @author Arnau Mora
     * @since 20210413
     */
    NOT_DOWNLOADED,

    /**
     * The [DataClass] is downloaded.
     * @author Arnau Mora
     * @since 20210413
     */
    DOWNLOADED,

    /**
     * The [DataClass] is currently being downloaded.
     * @author Arnau Mora
     * @since 20210413
     */
    DOWNLOADING,

    /**
     * The [DataClass] is not completely downloaded, this means that maybe a child or more is
     * downloaded, but the whole class isn't.
     * @author Arnau Mora
     * @since 20210413
     */
    PARTIALLY;

    override fun toString(): String = name

    operator fun not(): Boolean = this != DOWNLOADED

    /**
     * Checks if the [DataClass] is completely downloaded.
     * @author Arnau Mora
     * @since 20210413
     */
    fun isDownloaded(): Boolean = this == DOWNLOADED

    /**
     * Checks if the [DataClass] is partially downloaded.
     * @author Arnau Mora
     * @since 20210413
     */
    fun partialDownload(): Boolean = this == PARTIALLY
}
