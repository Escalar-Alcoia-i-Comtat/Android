package com.arnyminerz.escalaralcoiaicomtat.data

import com.arnyminerz.escalaralcoiaicomtat.data.info.version
import java.net.URL

class AppInfo {
    // TODO: Complete new version checking algorithm
    fun updateAvailable(): Boolean {
        val version = version()
        return false
    }
}