package com.arnyminerz.escalaralcoiaicomtat.shared

import com.arnyminerz.escalaralcoiaicomtat.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.generic.DataExtra

val EXTRA_AREA = DataExtra<String>("area")
val EXTRA_ZONE = DataExtra<String>("zone")
val EXTRA_SECTOR_COUNT = DataExtra<Int>("sector_count")
val EXTRA_SECTOR_INDEX = DataExtra<Int>("sector_index")
val EXTRA_PATH = DataExtra<String>("path")
val EXTRA_PATH_DOCUMENT = DataExtra<String>("path_document")

val EXTRA_POSITION = DataExtra<Int>("position")

val EXTRA_ZONE_TRANSITION_NAME = DataExtra<String>("zone_transition")
val EXTRA_AREA_TRANSITION_NAME = DataExtra<String>("area_transition")
val EXTRA_SECTOR_TRANSITION_NAME = DataExtra<String>("sector_transition")

val UPDATE_AREA = DataExtra<Area>("update_area")
val UPDATE_ZONE = DataExtra<Zone>("update_zone")
val UPDATE_SECTOR = DataExtra<Sector>("update_sector")
val UPDATE_IMAGES = DataExtra<Boolean>("update_images")

val QUIET_UPDATE = DataExtra<Boolean>("quiet_update")

val EXTRA_KMZ_FILE = DataExtra<String>("KMZFle")
val EXTRA_ICON_SIZE_MULTIPLIER = DataExtra<Float>("IconSize")
val EXTRA_ZONE_NAME = DataExtra<String>("ZneNm")
val EXTRA_CENTER_CURRENT_LOCATION = DataExtra<Boolean>("CenterLocation")
