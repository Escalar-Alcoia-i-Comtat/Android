package com.arnyminerz.escalaralcoiaicomtat.core.worker.download

import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_OVERWRITE_DEFAULT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_QUALITY_DEFAULT

class DownloadData
/**
 * Initializes the class with specific parameters
 * @param displayName The display name of the DataClass.
 * @param overwrite If the download should be overwritten if already downloaded. Note that if this
 * is false, if the download already exists the task will fail.
 * @param quality The compression quality of the image
 * @see DOWNLOAD_OVERWRITE_DEFAULT
 * @see DOWNLOAD_QUALITY_DEFAULT
 */
constructor(
    val displayName: String,
    val overwrite: Boolean = DOWNLOAD_OVERWRITE_DEFAULT,
    val quality: Int = DOWNLOAD_QUALITY_DEFAULT
)
