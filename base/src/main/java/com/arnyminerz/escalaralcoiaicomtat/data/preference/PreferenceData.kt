package com.arnyminerz.escalaralcoiaicomtat.data.preference

import android.content.SharedPreferences

@Suppress("unused", "MemberVisibilityCanBePrivate")
// May only be String, Boolean, Int, Float and Long
class PreferenceData<T : Any> constructor(val key: String, val default: T) {
    private val d = default

    fun get(sharedPreferences: SharedPreferences?): T =
        (sharedPreferences?.all?.getOrDefault(key, default) as? T?) ?: default

    fun put(sharedPreferences: SharedPreferences, value: Any?) {
        with(sharedPreferences.edit()) {
            when (value) {
                is String -> putString(key, value)
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Float -> putFloat(key, value)
                is Long -> putLong(key, value)
                else -> throw DataTypeNonStorable()
            }
            apply()
        }
    }

    fun isSet(sharedPreferences: SharedPreferences): Boolean =
        sharedPreferences.all?.get(key) == null
}

fun <T : Any> T?.store(
    sharedPreferences: SharedPreferences?,
    data: PreferenceData<T>
): Boolean {
    return if (sharedPreferences != null) {
        data.put(sharedPreferences, this)
        true
    } else false
}

class DataTypeNonStorable : Exception("The given data type cannot be stored in settings")