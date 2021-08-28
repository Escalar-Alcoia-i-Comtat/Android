package com.arnyminerz.escalaralcoiaicomtat.core.shared

import com.arnyminerz.escalaralcoiaicomtat.core.data.preference.PreferenceData

private const val NEARBY_DISTANCE_DEFAULT = 1000
private const val MARKER_SIZE_DEFAULT = 3
private const val PREVIEW_SCALE_DEFAULT = .5f

val SETTINGS_ERROR_REPORTING_PREF = PreferenceData("error_reporting", true)
val SETTINGS_ALERT_PREF = PreferenceData("alert_pref", true)
val SETTINGS_LANGUAGE_PREF = PreferenceData("lang_pref", "en")
val SETTINGS_NEARBY_DISTANCE_PREF = PreferenceData("nearby_distance", NEARBY_DISTANCE_DEFAULT)
val SETTINGS_CENTER_MARKER_PREF = PreferenceData("center_marker", true)
val SETTINGS_MOBILE_DOWNLOAD_PREF = PreferenceData("mobile_download", true)
val SETTINGS_ROAMING_DOWNLOAD_PREF = PreferenceData("roaming_download", false)
val AUTOMATIC_DOWNLOADS_UPDATE_PREF = PreferenceData("automatic_downloads_update", false)
val DOWNLOADS_QUALITY_PREF = PreferenceData("downloads_quality", DOWNLOAD_QUALITY_DEFAULT)
val PREF_DISABLE_NEARBY = PreferenceData("NearbyZonesDisable", false)
val PREF_SHOWN_INTRO = PreferenceData("ShownIntro", false)
val PREF_WAITING_EMAIL_CONFIRMATION = PreferenceData("WaitingEmail", false)
val PREF_WARN_BATTERY = PreferenceData("WarnBatteryOptimization", true)
val PREF_INDEXED_SEARCH = PreferenceData("SearchIndexed", false)
