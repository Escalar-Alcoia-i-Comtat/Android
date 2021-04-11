package com.arnyminerz.escalaralcoiaicomtat.shared

import androidx.collection.arrayMapOf
import com.arnyminerz.escalaralcoiaicomtat.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.generic.IntentExtra

const val APP_UPDATE_MAX_TIME_DAYS_KEY = "APP_UPDATE_MAX_TIME_DAYS"
const val APP_UPDATE_MAX_TIME_DAYS_DEFAULT = 7L

/**
 * The maximum amount of days that will be allowed to the user not having updated the app
 * before forcing an update.
 */
var APP_UPDATE_MAX_TIME_DAYS = APP_UPDATE_MAX_TIME_DAYS_DEFAULT

val REMOTE_CONFIG_DEFAULTS = mapOf(
    APP_UPDATE_MAX_TIME_DAYS_KEY to APP_UPDATE_MAX_TIME_DAYS_DEFAULT
)
const val REMOTE_CONFIG_MIN_FETCH_INTERVAL = 3600L

val EXTRA_AREA = IntentExtra<String>("area")
val EXTRA_ZONE = IntentExtra<String>("zone")
val EXTRA_SECTOR_COUNT = IntentExtra<Int>("sector_count")
val EXTRA_SECTOR_INDEX = IntentExtra<Int>("sector_index")

val EXTRA_POSITION = IntentExtra<Int>("position")

val EXTRA_ZONE_TRANSITION_NAME = IntentExtra<String>("zone_transition")
val EXTRA_AREA_TRANSITION_NAME = IntentExtra<String>("area_transition")
val EXTRA_SECTOR_TRANSITION_NAME = IntentExtra<String>("sector_transition")

val UPDATE_AREA = IntentExtra<Area>("update_area")
val UPDATE_ZONE = IntentExtra<Zone>("update_zone")
val UPDATE_SECTOR = IntentExtra<Sector>("update_sector")
val UPDATE_IMAGES = IntentExtra<Boolean>("update_images")

val QUIET_UPDATE = IntentExtra<Boolean>("quiet_update")

val EXTRA_KML_ADDRESS = IntentExtra<String>("KMLAddr")
val EXTRA_KMZ_FILE = IntentExtra<String>("KMZFle")
val EXTRA_ICON_SIZE_MULTIPLIER = IntentExtra<Float>("IconSize")
val EXTRA_ZONE_NAME = IntentExtra<String>("ZneNm")
val EXTRA_CENTER_CURRENT_LOCATION = IntentExtra<Boolean>("CenterLocation")

const val ARGUMENT_AREA_ID = "area_id"
const val ARGUMENT_ZONE_ID = "zone_id"
const val ARGUMENT_SECTOR_INDEX = "sector_index"

const val MAP_MARKERS_BUNDLE_EXTRA = "Markers"
const val MAP_GEOMETRIES_BUNDLE_EXTRA = "Geometries"

const val TAB_ITEM_HOME = 0
const val TAB_ITEM_MAP = 1
const val TAB_ITEM_DOWNLOADS = 2
const val TAB_ITEM_SETTINGS = 3
const val TAB_ITEM_EXTRA = -1

val AREAS = arrayMapOf<String, Area>()

const val PREVIEW_SCALE_PREFERENCE_MULTIPLIER = 10
const val SECTOR_THUMBNAIL_SIZE = .8f

const val ERROR_VIBRATE: Long = 500
const val INFO_VIBRATION: Long = 20

const val TOGGLE_ANIMATION_DURATION: Long = 300
const val CROSSFADE_DURATION = 50

const val ROTATION_A = 90f
const val ROTATION_B = -90f

const val DEFAULT_BITMAP_COMPRESSION = 100

const val LOCATION_PERMISSION_REQUEST_CODE = 3 // This number was chosen by Dono
const val FOLDER_ACCESS_PERMISSION_REQUEST_CODE = 7

const val PERMISSION_DIALOG_TAG = "PERM_TAG"

const val MIME_TYPE_KML = "application/vnd.google-earth.kml+xml"
const val MIME_TYPE_KMZ = "application/vnd.google-earth.kmz"
const val MIME_TYPE_GPX = "application/gpx+xml"

// Sun Time constants
const val ALL_DAY_INDEX = 0
const val MORNING_INDEX = 1
const val AFTERNOON_INDEX = 2
const val NO_SUN_INDEX = 3

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

/**
 * The amount of margin in meters there should be left in the downloaded map around a marker.
 * @author Arnau Mora
 * @since 20210405
 */
const val DOWNLOAD_MARKER_MARGIN = 100
const val DOWNLOAD_MARKER_MIN_ZOOM = 10.0
const val DOWNLOAD_MARKER_MAX_ZOOM = 20.0
