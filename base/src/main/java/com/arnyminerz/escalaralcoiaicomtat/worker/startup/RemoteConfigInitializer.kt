package com.arnyminerz.escalaralcoiaicomtat.worker.startup

import android.content.Context
import androidx.startup.Initializer
import com.arnyminerz.escalaralcoiaicomtat.core.shared.APP_UPDATE_MAX_TIME_DAYS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.APP_UPDATE_MAX_TIME_DAYS_KEY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENABLE_AUTHENTICATION
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENABLE_AUTHENTICATION_KEY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PROFILE_IMAGE_SIZE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PROFILE_IMAGE_SIZE_KEY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REMOTE_CONFIG_DEFAULTS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REMOTE_CONFIG_MIN_FETCH_INTERVAL
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SHOW_NON_DOWNLOADED
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SHOW_NON_DOWNLOADED_KEY
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import timber.log.Timber

class RemoteConfigInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Timber.v("Getting remote configuration...")
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = REMOTE_CONFIG_MIN_FETCH_INTERVAL
        }
        remoteConfig
            .setConfigSettingsAsync(configSettings)
            .addOnSuccessListener {
                remoteConfig
                    .setDefaultsAsync(REMOTE_CONFIG_DEFAULTS)
                    .addOnSuccessListener {
                        remoteConfig
                            .fetchAndActivate()
                            .addOnSuccessListener {
                                APP_UPDATE_MAX_TIME_DAYS =
                                    remoteConfig.getLong(APP_UPDATE_MAX_TIME_DAYS_KEY)
                                SHOW_NON_DOWNLOADED =
                                    remoteConfig.getBoolean(SHOW_NON_DOWNLOADED_KEY)
                                ENABLE_AUTHENTICATION =
                                    remoteConfig.getBoolean(ENABLE_AUTHENTICATION_KEY)
                                PROFILE_IMAGE_SIZE = remoteConfig.getLong(PROFILE_IMAGE_SIZE_KEY)

                                Timber.v("APP_UPDATE_MAX_TIME_DAYS: $APP_UPDATE_MAX_TIME_DAYS")
                                Timber.v("SHOW_NON_DOWNLOADED: $SHOW_NON_DOWNLOADED")
                                Timber.v("ENABLE_AUTHENTICATION: $ENABLE_AUTHENTICATION")
                                Timber.v("PROFILE_IMAGE_SIZE: $PROFILE_IMAGE_SIZE")
                            }
                            .addOnFailureListener { e ->
                                Timber.e(e, "Could not get remote config.")
                            }
                    }
                    .addOnFailureListener { e ->
                        Timber.e(e, "Could not get remote config.")
                    }
            }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(TimberInitializer::class.java)
}
