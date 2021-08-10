package com.arnyminerz.escalaralcoiaicomtat.worker.startup

import android.content.Context
import androidx.startup.Initializer
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import timber.log.Timber

class EscalarAlcoiaIComtatInitializer : Initializer<List<Area>> {
    override fun create(context: Context): List<Area> {
        Timber.v("Initializing EscalarAlcoiaIComtatInitializer")
        return listOf()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(TimberInitializer::class.java, NotificationChannelsInitializer::class.java)
}