package com.arnyminerz.escalaralcoiaicomtat.core.data.map

import com.arnyminerz.escalaralcoiaicomtat.core.R

const val LOCATION_UPDATE_MIN_TIME: Long = 10 * 1000 // 10 seconds
const val LOCATION_UPDATE_MIN_DIST: Float = 1.5f // 1.5 meters

val ICON_WAYPOINT_ESCALADOR_BLANC =
    GeoIconConstant("ic_waypoint_escalador_blanc", R.drawable.ic_waypoint_escalador_blanc)

val ICONS = listOf(ICON_WAYPOINT_ESCALADOR_BLANC)

const val MARKER_WINDOW_HIDE_DURATION: Long = 500
const val MARKER_WINDOW_SHOW_DURATION: Long = 500

const val DEFAULT_LATITUDE = 38.7284401
const val DEFAULT_LONGITUDE = -0.43821
const val DEFAULT_ZOOM = 12.0f

/**
 * The size of the images from the map.
 * @author Arnau Mora
 * @since 20210604
 * @see GeoIcon
 */
const val MARKER_SIZE = 56
const val SMALL_ICON_SIZE_MULTIPLIER = .35f
const val LINE_WIDTH_MULTIPLIER = 1f
const val MAP_LOAD_PADDING = 50

const val WINDOW_DATA_KEY = "window_data"

const val MAP_LOADER_LATLNG_SIZE = 3
