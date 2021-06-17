package com.arnyminerz.escalaralcoiaicomtat.core.shared

import android.content.SharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.core.data.Cache
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area

val cache = Cache()

val AREAS = arrayListOf<Area>()

const val PREFERENCES_NAME = "EAICPreferences"
lateinit var sharedPreferences: SharedPreferences
