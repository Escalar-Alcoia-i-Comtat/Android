package com.arnyminerz.escalaralcoiaicomtat.shared

/**
 * The value for [APP_TYPE_PROP] when the app is installed (non-instant).
 * @author Arnau Mora
 * @since 20210730
 */
const val STATUS_INSTALLED = "installed"

/**
 * The value for [APP_TYPE_PROP] when the app is instant.
 * @author Arnau Mora
 * @since 20210730
 */
const val STATUS_INSTANT = "instant"

/**
 * The key attribute for analytics and crashlytics for specifying if the app is installed or instant.
 * @author Arnau Mora
 * @since 20210730
 * @see STATUS_INSTALLED
 * @see STATUS_INSTANT
 */
const val APP_TYPE_PROP = "app_type"
