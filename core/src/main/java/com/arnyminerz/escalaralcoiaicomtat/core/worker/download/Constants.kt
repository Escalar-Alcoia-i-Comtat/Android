package com.arnyminerz.escalaralcoiaicomtat.core.worker.download


const val DOWNLOAD_DISPLAY_NAME = "display_name"
const val DOWNLOAD_NAMESPACE = "namespace"
const val DOWNLOAD_PATH = "path"
const val DOWNLOAD_OVERWRITE = "overwrite"
const val DOWNLOAD_QUALITY = "quality"

/**
 * When the DownloadWorker was ran with missing data
 * @since 20210313
 */
const val ERROR_MISSING_DATA = "missing_data"

/**
 * When old data was tried to be deleted but was not possible
 * @since 20210313
 */
const val ERROR_DELETE_OLD = "delete_old"

/**
 * When the target download could not be found
 * @since 20210313
 */
const val ERROR_NOT_FOUND = "not_found"

/**
 * When the target download has already been downloaded and overwrite is false
 * @since 20210313
 */
const val ERROR_ALREADY_DOWNLOADED = "already_downloaded"

/**
 * When trying to store an image, and the parent dir could not be created.
 * @since 20210323
 */
const val ERROR_CREATE_PARENT = "create_parent"

/**
 * When trying to fetch data from the server
 * @since 20210411
 */
const val ERROR_DATA_FETCH = "data_fetch"

/**
 * When there's an unkown error while storing the image.
 * @since 20210411
 */
const val ERROR_STORE_IMAGE = "store_image"

/**
 * When the specified namespace is not downloadable.
 * @since 20210412
 */
const val ERROR_UNKNOWN_NAMESPACE = "unknown_namespace"

/**
 * When the image reference could not be updated.
 * @since 20210422
 */
const val ERROR_UPDATE_IMAGE_REF = "update_image_ref"
