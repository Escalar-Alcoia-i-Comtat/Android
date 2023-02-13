package com.arnyminerz.escalaralcoiaicomtat.worker.startup

import android.content.Context
import androidx.startup.Initializer
import timber.log.Timber

class TimberInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Timber.plant(Timber.DebugTree())
        Timber.v("Planted Timber.")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}