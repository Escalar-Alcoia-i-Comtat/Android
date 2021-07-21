package com.arnyminerz.escalaralcoiaicomtat.instant.shared

import android.app.Application
import android.content.Context
import android.util.Log
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PREFERENCES_NAME
import com.arnyminerz.escalaralcoiaicomtat.core.shared.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.instant.BuildConfig
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import timber.log.Timber

private const val CRASHLYTICS_KEY_PRIORITY = "priority"
private const val CRASHLYTICS_KEY_TAG = "tag"

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else CrashReportingTree())
        Timber.v("Planted Timber.")

        sharedPreferences =
            applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    private inner class CrashReportingTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            val crashlytics = Firebase.crashlytics

            crashlytics.setCustomKey(CRASHLYTICS_KEY_PRIORITY, priority)
            if (tag != null)
                crashlytics.setCustomKey(CRASHLYTICS_KEY_TAG, tag)

            if (priority == Log.DEBUG || priority == Log.VERBOSE || priority == Log.INFO || priority == Log.WARN)
                crashlytics.log(message)
            else if (t != null)
                crashlytics.recordException(t)
            else
                crashlytics.log(message)
        }
    }
}