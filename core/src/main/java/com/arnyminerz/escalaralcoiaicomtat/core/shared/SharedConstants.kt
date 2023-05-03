package com.arnyminerz.escalaralcoiaicomtat.core.shared

import com.arnyminerz.escalaralcoiaicomtat.core.data.SemVer

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
const val REST_API_URL = "https://server.escalaralcoiaicomtat.org"

/**
 * The REST API endpoint for fetching the blocking state of a path. At the end /:pathId should be
 * added.
 * @author Arnau Mora
 * @since 20220221
 */
const val REST_API_BLOCKING_ENDPOINT = "$REST_API_URL/v2/blocking"

/**
 * The REST API endpoint for downloading files from the server. At the end, the path of the file to
 * download should be added.
 * @author Arnau Mora
 * @since 20220221
 */
const val REST_API_DOWNLOAD_ENDPOINT = "$REST_API_URL/v1/files/download?path="

/**
 * The REST API endpoint for fetching data from the server. At the end, the type of data should be
 * added (/Areas, /Zones, /Sectors or /Paths). Also, after this, an slash and an object id can be
 * added. The answer will contain the data type selected, that has as parent the set objectId.
 * @author Arnau Mora
 * @since 20220222
 */
const val REST_API_DATA_LIST = "$REST_API_URL/v1/list"

/**
 * The REST API endpoint for fetching data of a select object from server. At the end, the type of
 * data should be added (/Areas, /Zones, /Sectors or /Paths), and then, the id of the element.
 * @author Arnau Mora
 * @since 20220222
 */
const val REST_API_DATA_FETCH = "$REST_API_URL/v1/data/"

/**
 * The REST API endpoint for getting information about the server, such as if it's in production,
 * its running version, among others.
 * @author Arnau Mora
 * @since 20220627
 */
const val REST_API_INFO_ENDPOINT = "$REST_API_URL/v1/info"

const val IMAGE_MAX_ZOOM_KEY = "IMAGE_MAX_ZOOM"
const val IMAGE_MAX_ZOOM_DEFAULT = 7.0

/**
 * Stores the version of the server with which this app is compatible. If the given by the server
 * and this one do not match, data will not be downloaded for enforcing compatibility.
 * @author Arnau Mora
 * @since 20220627
 */
val EXPECTED_SERVER_VERSION = SemVer(1, 0, 7)

var IMAGE_MAX_ZOOM_LEVEL = IMAGE_MAX_ZOOM_DEFAULT

/**
 * Sets the default values for Remote Config
 */
val REMOTE_CONFIG_DEFAULTS = mapOf(
    IMAGE_MAX_ZOOM_KEY to IMAGE_MAX_ZOOM_DEFAULT,
)
const val REMOTE_CONFIG_MIN_FETCH_INTERVAL = 43200L // 12 hours

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
const val ALL_DAY = "day"
const val MORNING = "morning"
const val AFTERNOON = "afternoon"
const val NO_SUN = "none"

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
