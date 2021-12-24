package com.arnyminerz.escalaralcoiaicomtat.worker.startup

import android.content.Context
import android.os.Build
import androidx.startup.Initializer
import com.arnyminerz.escalaralcoiaicomtat.createNotificationChannels
import timber.log.Timber

class NotificationChannelsInitializer : Initializer<Boolean> {
    override fun create(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Timber.i("Creating notification channels...")
            context.createNotificationChannels()
            true
        } else {
            Timber.w("Won't create notification channels since SDK level is lower than O.")
            false
        }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(TimberInitializer::class.java)
}
