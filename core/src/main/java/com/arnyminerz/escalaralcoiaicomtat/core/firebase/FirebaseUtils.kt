package com.arnyminerz.escalaralcoiaicomtat.core.firebase

import androidx.annotation.UiThread
import com.arnyminerz.escalaralcoiaicomtat.core.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_ERROR_REPORTING_PREF
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import timber.log.Timber

/**
 * Initializes the user-set data collection policy.
 * If debugging, data collection will always be disabled.
 * @author Arnau Mora
 * @since 20210617
 * @see SETTINGS_ERROR_REPORTING_PREF
 */
@UiThread
fun dataCollectionSetUp() {
    val enableErrorReporting = SETTINGS_ERROR_REPORTING_PREF.get()

    Firebase.crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG && enableErrorReporting)
    Timber.v("Set Crashlytics collection enabled to $enableErrorReporting")

    Firebase.analytics.setAnalyticsCollectionEnabled(!BuildConfig.DEBUG && enableErrorReporting)
    Timber.v("Set Analytics collection enabled to $enableErrorReporting")

    Firebase.performance.isPerformanceCollectionEnabled = enableErrorReporting
    Timber.v("Set Performance collection enabled to $enableErrorReporting")
}