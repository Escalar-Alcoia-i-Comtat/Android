package com.arnyminerz.escalaralcoiaicomtat.core.shared

import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.AreaData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.BlockingData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.PathData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.SectorData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.ZoneData
import com.arnyminerz.escalaralcoiaicomtat.core.utils.MEGABYTE

const val APPLICATION_ID = "com.arnyminerz.escalaralcoiaicomtat"

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
)

const val ARGUMENT_AREA_ID = "area_id"
const val ARGUMENT_ZONE_ID = "zone_id"
const val ARGUMENT_SECTOR_ID = "sector_id"

/**
 * Used as an argument in fragments for passing the id of a user.
 * @author Arnau Mora
 * @since 20210821
 */
const val ARGUMENT_USER_ID = "user_id"

const val MAP_MARKERS_BUNDLE_EXTRA = "Markers"
const val MAP_GEOMETRIES_BUNDLE_EXTRA = "Geometries"

const val TAB_ITEM_HOME = 0
const val TAB_ITEM_MAP = 1
const val TAB_ITEM_DOWNLOADS = 2
const val TAB_ITEM_SETTINGS = 3
const val TAB_ITEM_EXTRA = -1

const val PREVIEW_SCALE_PREFERENCE_MULTIPLIER = 10
const val SECTOR_THUMBNAIL_SIZE = .8f

const val ERROR_VIBRATE: Long = 500
const val INFO_VIBRATION: Long = 20

const val TOGGLE_ANIMATION_DURATION: Long = 300
const val CROSSFADE_DURATION = 50

const val ROTATION_A = 90f
const val ROTATION_B = -90f

/**
 * The compression quality for a profile image.
 * @author Arnau Mora
 * @since 20210425
 */
const val PROFILE_IMAGE_COMPRESSION_QUALITY = 85

/**
 * The maximum download size allowed for the profile image.
 * @author Arnau Mora
 * @since 20210519
 */
const val PROFILE_IMAGE_MAX_SIZE = MEGABYTE * 5

const val LOCATION_PERMISSION_REQUEST_CODE = 3 // This number was chosen by Dono

/**
 * Requests the user to get logged in
 * @author Arnau Mora
 * @since 20210425
 */
const val REQUEST_CODE_LOGIN = 5

/**
 * Requests the user to select an image for its profile.
 * @author Arnau Mora
 * @since 20210425
 */
const val REQUEST_CODE_SELECT_PROFILE_IMAGE = 3

const val PERMISSION_DIALOG_TAG = "PERM_TAG"

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

/**
 * Amount of meters the circumference of the planet measures.
 * @author Arnau Mora
 * @since 20210405
 */
const val EARTHS_CIRCUMFERENCE = 40075017

/**
 * The amount of meters there are in a degree of latitude nor longitude.
 * @author Arnau Mora
 * @since 20210405
 */
const val METERS_PER_LAT_LON_DEGREE = EARTHS_CIRCUMFERENCE / 360

// Downloads Constants
const val DOWNLOAD_OVERWRITE_DEFAULT = true
const val DOWNLOAD_QUALITY_DEFAULT = 85
const val DOWNLOAD_QUALITY_MIN = 1
const val DOWNLOAD_QUALITY_MAX = 100

/**
 * The amount of margin in meters there should be left in the downloaded map around a marker.
 * @author Arnau Mora
 * @since 20210405
 */
const val DOWNLOAD_MARKER_MARGIN = 100
const val DOWNLOAD_MARKER_MIN_ZOOM = 10.0
const val DOWNLOAD_MARKER_MAX_ZOOM = 20.0

/**
 * The time that will be delayed inside the inner children load while, for not overflowing the thread.
 * In millis
 * @author Arnau Mora
 * @since 20210417
 */
const val DATACLASS_WAIT_CHILDREN_DELAY = 10L

/**
 * The time that will be delayed inside the Path block status load, for not overflowing the thread.
 * In millis
 * @author Arnau Mora
 * @since 20210514
 */
const val DATACLASS_WAIT_BLOCK_STATUS_DELAY = 10L

/**
 * The scale in which the DataClasses should be loaded when they are shown as a thumbnail.
 * @author Arnau Mora
 * @since 20210822
 */
const val DATACLASS_PREVIEW_SCALE = .5f

/**
 * Specifies the redirect url for the confirmation mails
 * @author Arnau Mora
 * @since 20210425
 */
const val CONFIRMATION_EMAIL_URL = "https://escalaralcoiaicomtat.page.link/email"

/**
 * Specifies the dynamic links domain for the confirmation mails
 * @author Arnau Mora
 * @since 20210425
 */
const val CONFIRMATION_EMAIL_DYNAMIC = "escalaralcoiaicomtat.page.link"

/**
 * This is used as constant for any place that requires a 100, just for making the compiler happy :)
 * @author Arnau Mora
 * @since 20210519
 */
const val HUNDRED = 100

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
 * The meta name from the Manifest for specifying the AreaActivity package name.
 * @author Arnau Mora
 * @since 20210615
 */
const val ACTIVITY_AREA_META = "com.arnyminerz.escalaralcoiaicomtat.core.activity.AreaActivity"

/**
 * The meta name from the Manifest for specifying the ZoneActivity package name.
 * @author Arnau Mora
 * @since 20210615
 */
const val ACTIVITY_ZONE_META = "com.arnyminerz.escalaralcoiaicomtat.core.activity.ZoneActivity"

/**
 * The meta name from the Manifest for specifying the SectorActivity package name.
 * @author Arnau Mora
 * @since 20210615
 */
const val ACTIVITY_SECTOR_META = "com.arnyminerz.escalaralcoiaicomtat.core.activity.SectorActivity"
