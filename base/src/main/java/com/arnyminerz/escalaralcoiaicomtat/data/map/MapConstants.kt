package com.arnyminerz.escalaralcoiaicomtat.data.map

import com.arnyminerz.escalaralcoiaicomtat.R

const val LOCATION_UPDATE_MIN_TIME: Long = 10 * 1000 // 10 seconds
const val LOCATION_UPDATE_MIN_DIST: Float = 1.5f // 1.5 meters

val ICON_WAYPOINT_ESCALADOR_BLANC =
    GeoIconConstant("ic_waypoint_escalador_blanc", R.drawable.ic_waypoint_escalador_blanc)

val ICONS = listOf(ICON_WAYPOINT_ESCALADOR_BLANC)

const val MARKER_WINDOW_HIDE_DURATION: Long = 500
const val MARKER_WINDOW_SHOW_DURATION: Long = 500

const val DEFAULT_LATITUDE = 38.7284401
const val DEFAULT_LONGITUDE = -0.43821
const val DEFAULT_ZOOM = 12.0
