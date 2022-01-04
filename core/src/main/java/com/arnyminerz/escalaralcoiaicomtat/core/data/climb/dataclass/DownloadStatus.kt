package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.FileDownloadDone
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.arnyminerz.escalaralcoiaicomtat.core.R

/**
 * Specifies the download status of a [DataClass].
 * @author Arnau Mora
 * @since 20210413
 */
enum class DownloadStatus {
    /**
     * The status of the [DataClass] is not known.
     * @author Arnau Mora
     * @since 20220104
     */
    UNKNOWN,

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
    val downloaded: Boolean
        get() = this == DOWNLOADED

    /**
     * Checks if the [DataClass] is partially downloaded.
     * @author Arnau Mora
     * @since 20210413
     */
    val partialDownload: Boolean
        get() = this == PARTIALLY

    /**
     * Checks if the [DataClass] is being downloaded.
     * @author Arnau Mora
     * @since 20210820
     */
    val downloading: Boolean
        get() = this == DOWNLOADING

    /**
     * Gets the icon that should be displayed for the download status.
     * @author Arnau Mora
     * @since 20210820
     */
    @DrawableRes
    @Deprecated("Should use Compose icons")
    fun getIcon(): Int =
        when (this) {
            NOT_DOWNLOADED -> R.drawable.download
            DOWNLOADED -> R.drawable.cloud_check
            DOWNLOADING -> R.drawable.cloud_sync
            PARTIALLY -> R.drawable.cloud_braces
            else -> R.drawable.round_close_24
        }

    /**
     * Gets the icon that represents the [DownloadStatus].
     * @author Arnau Mora
     * @since 20220104
     */
    fun getStateIcon(): ImageVector =
        when (this) {
            NOT_DOWNLOADED -> Icons.Rounded.Cloud
            DOWNLOADED -> Icons.Rounded.FileDownloadDone
            DOWNLOADING -> Icons.Rounded.Downloading
            PARTIALLY -> Icons.Rounded.SystemUpdateAlt
            else -> Icons.Rounded.Close
        }

    /**
     * Gets the icon that represents what can be done with the [DownloadStatus].
     * @author Arnau Mora
     * @since 20220104
     */
    fun getActionIcon(): ImageVector =
        when (this) {
            NOT_DOWNLOADED -> Icons.Rounded.Download
            DOWNLOADED -> Icons.Rounded.FileDownloadDone
            DOWNLOADING -> Icons.Rounded.Downloading
            PARTIALLY -> Icons.Rounded.SystemUpdateAlt
            else -> Icons.Rounded.Close
        }

    /**
     * Gets the text that should be displayed on a button that represents the download status.
     * @author Arnau Mora
     * @since 20220104
     */
    @Composable
    fun getText(): String = stringResource(
        when (this) {
            NOT_DOWNLOADED -> R.string.action_download
            DOWNLOADED -> R.string.status_downloaded
            DOWNLOADING -> R.string.status_downloading
            PARTIALLY -> R.string.action_download
            else -> R.string.status_loading
        }
    )
}
