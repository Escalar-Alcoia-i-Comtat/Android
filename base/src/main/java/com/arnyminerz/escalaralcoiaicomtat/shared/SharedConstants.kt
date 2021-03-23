package com.arnyminerz.escalaralcoiaicomtat.shared

import androidx.collection.arrayMapOf
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.area.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.generic.IntentExtra

const val APP_UPDATE_MAX_TIME_DAYS_DEFAULT = 7

/**
 * The maximum amount of days that will be allowed to the user not having updated the app
 * before forcing an update.
 */
var APP_UPDATE_MAX_TIME_DAYS = APP_UPDATE_MAX_TIME_DAYS_DEFAULT

val EXTRA_AREA = IntentExtra<String>("area")
val EXTRA_ZONE = IntentExtra<String>("zone")
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

const val UPDATE_CHECKER_WORK_NAME = "update_checker"
const val UPDATE_CHECKER_TAG = "update"
const val UPDATE_CHECKER_FLEX_MINUTES: Long = 15

val AREAS = arrayMapOf<String, Area>()

const val PREVIEW_SCALE_PREFERENCE_MULTIPLIER = 10
const val THUMBNAIL_SIZE = .1f

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

/**
 * Specifies the DataClass' pin prefix
 */
const val DATA_FIX_LABEL = "climbData"

/**
 * Specifies the maximum amount of items that should be loaded on a single batch from Parse.
 */
const val MAX_BATCH_SIZE = 1000

const val MIME_TYPE_KML = "application/vnd.google-earth.kml+xml"
const val MIME_TYPE_KMZ = "application/vnd.google-earth.kmz"
const val MIME_TYPE_GPX = "application/gpx+xml"

// Sun Time constants
const val ALL_DAY_INDEX = 0
const val MORNING_INDEX = 1
const val AFTERNOON_INDEX = 2
const val NO_SUN_INDEX = 3

// Downloads Constants
const val DOWNLOAD_OVERWRITE_DEFAULT = true
const val DOWNLOAD_QUALITY_DEFAULT = 85
