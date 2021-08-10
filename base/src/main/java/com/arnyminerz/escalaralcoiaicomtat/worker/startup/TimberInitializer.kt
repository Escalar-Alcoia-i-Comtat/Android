package com.arnyminerz.escalaralcoiaicomtat.worker.startup

import android.content.Context
import androidx.startup.Initializer
import com.arnyminerz.escalaralcoiaicomtat.core.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.core.CrashReportingTree
import timber.log.Timber

class TimberInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else CrashReportingTree())
        Timber.v("Planted Timber.")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}