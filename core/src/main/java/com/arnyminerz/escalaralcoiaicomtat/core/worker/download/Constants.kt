package com.arnyminerz.escalaralcoiaicomtat.core.worker.download


const val DOWNLOAD_DISPLAY_NAME = "display_name"
const val DOWNLOAD_OVERWRITE = "overwrite"
const val DOWNLOAD_QUALITY = "quality"

/**
 * The tag added to all workers that work on app data downloads.
 * @author Arnau Mora
 * @since 20210926
 */
const val WORKER_TAG_DOWNLOAD = "DataDownload"

/**
 * When the DownloadWorker was ran with missing data
 * @since 20210313
 */
const val ERROR_MISSING_DATA = "missing_data"

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
 * When the type of data gotten from the server was not expected
 * @author Arnau Mora
 * @since 20210928
 */
const val ERROR_DATA_TYPE = "data_type"

/**
 * When there's missing data on the server
 * @author Arnau Mora
 * @since 20210928
 */
const val ERROR_DATA_FRAGMENTED = "data_fragmented"

/**
 * When there's an unkown error while storing the image.
 * @since 20210411
 */
const val ERROR_STORE_IMAGE = "store_image"

/**
 * When the Bitmap gotten from the server could not be compressed to the target file.
 * @author Arnau Mora
 * @since 20210822
 */
const val ERROR_COMPRESS_IMAGE = "compress_image"

/**
 * When there was an error while fetching the image from the server.
 * @author Arnau Mora
 * @since 20210822
 */
const val ERROR_FETCH_IMAGE = "fetch_image"

/**
 * When the specified namespace is not downloadable.
 * @since 20210412
 */
const val ERROR_UNKNOWN_NAMESPACE = "unknown_namespace"

/**
 * When there has been an error while transferring the downloaded data values from the download
 * function into the conclusion function.
 * @author Arnau Mora
 * @since 20211231
 */
const val ERROR_DATA_TRANSFERENCE = "data_transference"
