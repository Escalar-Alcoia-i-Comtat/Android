package com.arnyminerz.escalaralcoiaicomtat.core.shared

import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.utils.DataExtra

val EXTRA_AREA = DataExtra<String>("area")
val EXTRA_ZONE = DataExtra<String>("zone")
val EXTRA_SECTOR = DataExtra<String>("sector")
val EXTRA_PATH = DataExtra<String>("path")
val EXTRA_PATH_DOCUMENT = DataExtra<String>("path_document")

val EXTRA_POSITION = DataExtra<Int>("position")

val EXTRA_ZONE_TRANSITION_NAME = DataExtra<String>("zone_transition")
val EXTRA_AREA_TRANSITION_NAME = DataExtra<String>("area_transition")
val EXTRA_SECTOR_TRANSITION_NAME = DataExtra<String?>("sector_transition")

val UPDATE_AREA = DataExtra<Area>("update_area")
val UPDATE_ZONE = DataExtra<Zone>("update_zone")
val UPDATE_SECTOR = DataExtra<Sector>("update_sector")
val UPDATE_IMAGES = DataExtra<Boolean>("update_images")

val QUIET_UPDATE = DataExtra<Boolean>("quiet_update")

val EXTRA_KMZ_FILE = DataExtra<String>("KMZFle")
val EXTRA_CENTER_CURRENT_LOCATION = DataExtra<Boolean>("CenterLocation")

/**
 * Used in [DynamicLinkHandler] for passing [LoadingActivity] the link that is wanted to be launched
 * once the data is loaded.
 * @author Arnau Mora
 * @since 20210521
 */
val EXTRA_LINK_PATH = DataExtra<String>("Link")

/**
 * If true, informs that the Activity wasn't launched from the "normal" navigation flow, it has been
 * launched after [DataClass.getIntent], for example.
 * @author Arnau Mora
 * @since 20210521
 */
val EXTRA_STATIC = DataExtra<Boolean>("Static")

/**
 * The User's UID for selecting which user's data to load.
 * @author Arnau Mora
 * @since 20210719
 */
val EXTRA_USER_UID = DataExtra<String>("UserUid")
