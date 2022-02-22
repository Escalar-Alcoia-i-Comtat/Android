package com.arnyminerz.escalaralcoiaicomtat.core.shared

import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.AreaData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.downloads.DownloadedData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.BlockingData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.PathData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.SectorData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.ZoneData

const val APPLICATION_ID = "com.arnyminerz.escalaralcoiaicomtat"

/**
 * The email used for contacting.
 * @author Arnau Mora
 * @since 20220126
 */
const val CONTACT_EMAIL = "arnyminer.z@gmail.com"

/**
 * The URL of the REST API to make requests to the server.
 * @author Arnau Mora
 * @since 20220221
 */
const val REST_API_URL = "http://arnyminerz.com:3000"

/**
 * The REST API endpoint for fetching the blocking state of a path. At the end /:pathId should be
 * added.
 * @author Arnau Mora
 * @since 20220221
 */
const val REST_API_BLOCKING_ENDPOINT = "$REST_API_URL/api/info/blocking"

/**
 * The REST API endpoint for downloading files from the server. At the end, the path of the file to
 * download should be added.
 * @author Arnau Mora
 * @since 20220221
 */
const val REST_API_DOWNLOAD_ENDPOINT = "$REST_API_URL/api/files/download?path="

const val APP_UPDATE_MAX_TIME_DAYS_KEY = "APP_UPDATE_MAX_TIME_DAYS"
const val APP_UPDATE_MAX_TIME_DAYS_DEFAULT = 7L

const val SHOW_NON_DOWNLOADED_KEY = "SHOW_NON_DOWNLOADED"
const val SHOW_NON_DOWNLOADED_DEFAULT = false

const val ENABLE_AUTHENTICATION_KEY = "ENABLE_AUTHENTICATION"
const val ENABLE_AUTHENTICATION_DEFAULT = false

const val PROFILE_IMAGE_SIZE_KEY = "PROFILE_IMAGE_SIZE"
const val PROFILE_IMAGE_SIZE_DEFAULT = 512L

/**
 * The maximum amount of days that will be allowed to the user not having updated the app
 * before forcing an update.
 */
var APP_UPDATE_MAX_TIME_DAYS = APP_UPDATE_MAX_TIME_DAYS_DEFAULT

/**
 * Sets if the non-downloaded items should show in the downloads page.
 */
var SHOW_NON_DOWNLOADED = SHOW_NON_DOWNLOADED_DEFAULT

/**
 * Sets if the authentication features should be enabled.
 * @since 20210422
 */
var ENABLE_AUTHENTICATION = ENABLE_AUTHENTICATION_DEFAULT

/**
 * Sets the width and height that the profile images should be resized to.
 * @since 20210430
 */
var PROFILE_IMAGE_SIZE = PROFILE_IMAGE_SIZE_DEFAULT

/**
 * Sets the default values for Remote Config
 */
val REMOTE_CONFIG_DEFAULTS = mapOf(
    APP_UPDATE_MAX_TIME_DAYS_KEY to APP_UPDATE_MAX_TIME_DAYS_DEFAULT,
    SHOW_NON_DOWNLOADED_KEY to SHOW_NON_DOWNLOADED_DEFAULT,
    ENABLE_AUTHENTICATION_KEY to ENABLE_AUTHENTICATION_DEFAULT,
    PROFILE_IMAGE_SIZE_KEY to PROFILE_IMAGE_SIZE_DEFAULT,
)
const val REMOTE_CONFIG_MIN_FETCH_INTERVAL = 43200L // 12 hours

/**
 * The database name for the app search engine.
 * @author Arnau Mora
 * @since 20210811
 * @see <a href="https://developer.android.com/guide/topics/search/appsearch">AppSearch docs</a>
 */
const val SEARCH_DATABASE_NAME = "escalaralcoiaicomtat_v3"

/**
 * Stores all the schemas that are used by the search engine.
 * @author Arnau Mora
 * @since 20210828
 */
val SEARCH_SCHEMAS = listOf<Class<*>>(
    AreaData::class.java,
    ZoneData::class.java,
    SectorData::class.java,
    PathData::class.java,
    BlockingData::class.java,
    DownloadedData::class.java,
)

/**
 * Returned by [DataClassActivity] when no [EXTRA_DATACLASS] is passed through the [Activity.Intent].
 * @author Arnau Mora
 * @since 20220105
 */
const val REQUEST_CODE_ERROR_NO_DATACLASS = 1

const val MIME_TYPE_KML = "application/vnd.google-earth.kml+xml"
const val MIME_TYPE_KMZ = "application/vnd.google-earth.kmz"
const val MIME_TYPE_GPX = "application/gpx+xml"

// Sun Time constants
const val ALL_DAY = 0
const val MORNING = 1
const val AFTERNOON = 2
const val NO_SUN = 3

// Ending Type constants
const val ENDING_TYPE_UNKNOWN = "NULL"
const val ENDING_TYPE_PLATE = "plate"
const val ENDING_TYPE_PLATE_RING = "plate_ring"
const val ENDING_TYPE_PLATE_LANYARD = "plate_lanyard"
const val ENDING_TYPE_CHAIN_RING = "chain_ring"
const val ENDING_TYPE_CHAIN_CARABINER = "chain_carabiner"
const val ENDING_TYPE_PITON = "piton"
const val ENDING_TYPE_WALKING = "walking"
const val ENDING_TYPE_RAPPEL = "rappel"
const val ENDING_TYPE_LANYARD = "lanyard"
const val ENDING_TYPE_NONE = "none"

// Downloads Constants
const val DOWNLOAD_OVERWRITE_DEFAULT = true
const val DOWNLOAD_QUALITY_DEFAULT = 85
const val DOWNLOAD_QUALITY_MIN = 1
const val DOWNLOAD_QUALITY_MAX = 100

/**
 * The scale in which the DataClasses should be loaded when they are shown as a thumbnail.
 * @author Arnau Mora
 * @since 20210822
 */
const val DATACLASS_PREVIEW_SCALE = .3f

/**
 * The domain used in Firebase Dynamic Links.
 * @author Arnau Mora
 * @since 20210521
 */
const val DYNAMIC_LINKS_DOMAIN = "https://escalaralcoiaicomtat.page.link"

/**
 * The hostname of the website.
 * @author Arnau Mora
 * @since 20210521
 */
const val ESCALAR_ALCOIA_I_COMTAT_HOSTNAME = "escalaralcoiaicomtat.centrexcursionistalcoi.org"

/**
 * The name of the data module, for requesting installs and uninstalls.
 * @author Arnau Mora
 * @since 20211227
 */
const val DATA_MODULE_NAME = "data"
