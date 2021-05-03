package com.arnyminerz.escalaralcoiaicomtat.shared

import com.arnyminerz.escalaralcoiaicomtat.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.generic.IntentExtra

val EXTRA_AREA = IntentExtra<String>("area")
val EXTRA_ZONE = IntentExtra<String>("zone")
val EXTRA_SECTOR_COUNT = IntentExtra<Int>("sector_count")
val EXTRA_SECTOR_INDEX = IntentExtra<Int>("sector_index")
val EXTRA_PATH = IntentExtra<String>("path")
val EXTRA_PATH_DOCUMENT = IntentExtra<String>("path_document")

val EXTRA_POSITION = IntentExtra<Int>("position")

val EXTRA_ZONE_TRANSITION_NAME = IntentExtra<String>("zone_transition")
val EXTRA_AREA_TRANSITION_NAME = IntentExtra<String>("area_transition")
val EXTRA_SECTOR_TRANSITION_NAME = IntentExtra<String>("sector_transition")

val UPDATE_AREA = IntentExtra<Area>("update_area")
val UPDATE_ZONE = IntentExtra<Zone>("update_zone")
val UPDATE_SECTOR = IntentExtra<Sector>("update_sector")
val UPDATE_IMAGES = IntentExtra<Boolean>("update_images")

val QUIET_UPDATE = IntentExtra<Boolean>("quiet_update")

val EXTRA_KMZ_FILE = IntentExtra<String>("KMZFle")
val EXTRA_ICON_SIZE_MULTIPLIER = IntentExtra<Float>("IconSize")
val EXTRA_ZONE_NAME = IntentExtra<String>("ZneNm")
val EXTRA_CENTER_CURRENT_LOCATION = IntentExtra<Boolean>("CenterLocation")
