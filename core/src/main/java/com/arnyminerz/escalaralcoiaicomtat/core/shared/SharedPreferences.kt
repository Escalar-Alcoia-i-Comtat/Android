package com.arnyminerz.escalaralcoiaicomtat.core.shared

import com.arnyminerz.escalaralcoiaicomtat.core.data.preference.PreferenceData

private const val NEARBY_DISTANCE_DEFAULT = 1000
private const val MARKER_SIZE_DEFAULT = 3
private const val PREVIEW_SCALE_DEFAULT = .5f

@Deprecated("Use PreferencesModule")
val SETTINGS_ERROR_REPORTING_PREF = PreferenceData("error_reporting", true)

@Deprecated("Use PreferencesModule")
val SETTINGS_ALERT_PREF = PreferenceData("alert_pref", true)

@Deprecated("Use PreferencesModule")
val SETTINGS_LANGUAGE_PREF = PreferenceData("lang_pref", "en")

@Deprecated("Use PreferencesModule")
val SETTINGS_NEARBY_DISTANCE_PREF = PreferenceData("nearby_distance", NEARBY_DISTANCE_DEFAULT)

@Deprecated("Use PreferencesModule")
val SETTINGS_CENTER_MARKER_PREF = PreferenceData("center_marker", true)

@Deprecated("Use PreferencesModule")
val SETTINGS_MOBILE_DOWNLOAD_PREF = PreferenceData("mobile_download", true)

@Deprecated("Use PreferencesModule")
val SETTINGS_ROAMING_DOWNLOAD_PREF = PreferenceData("roaming_download", false)

@Deprecated("Use PreferencesModule")
val DOWNLOADS_QUALITY_PREF = PreferenceData("downloads_quality", DOWNLOAD_QUALITY_DEFAULT)

@Deprecated("Use PreferencesModule")
val PREF_DISABLE_NEARBY = PreferenceData("NearbyZonesDisable", false)

@Deprecated("Use PreferencesModule")
val PREF_WAITING_EMAIL_CONFIRMATION = PreferenceData("WaitingEmail", false)

@Deprecated("Use PreferencesModule")
val PREF_WARN_BATTERY = PreferenceData("WarnBatteryOptimization", true)

@Deprecated("Use PreferencesModule")
val PREF_INDEXED_SEARCH = PreferenceData("SearchIndexed", false)

@Deprecated("Use PreferencesModule")
val PREF_DATA_VERSION = PreferenceData("DataVersion", "")

@Deprecated("Use PreferencesModule")
val PREF_DATA_DATE = PreferenceData("DataDate", -1L)

@Deprecated("Use PreferencesModule")
val PREF_SHOWN_MD5_WARNING = PreferenceData("ShownMd5Warning", false)
