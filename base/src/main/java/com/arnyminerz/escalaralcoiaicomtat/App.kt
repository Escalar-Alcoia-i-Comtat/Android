package com.arnyminerz.escalaralcoiaicomtat

import android.app.Application
import com.parse.Parse
import com.parse.Parse.Configuration

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Parse.initialize(
            Configuration.Builder(this)
                .applicationId(BuildConfig.PARSE_APPLICATION_ID) // if defined
                .clientKey(BuildConfig.PARSE_KEY)
                .server(BuildConfig.PARSE_SERVER)
                .build()
        )
    }
}
