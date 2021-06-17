package com.arnyminerz.escalaralcoiaicomtat.core.data.preference

import android.content.SharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.core.shared.sharedPreferences

// Must only be String, Boolean, Int, Float and Long
/**
 * Initializes a [PreferenceData] instance.
 * @author Arnau Mora
 * @since 20210321
 * @param T The type of the variable to store. Only can be String, Boolean, Int, Float or Long.
 * @param key The shared preferences key of the value to store.
 * @param default The default value to return if there's no stored value in shared preferences.
 * @see SharedPreferences
 * @see sharedPreferences
 */
class PreferenceData<T : Any> constructor(val key: String, val default: T) {
    /**
     * Gets the [PreferenceData]'s value
     * @author Arnau Mora
     * @since 20210321
     * @return The stored value in preferences
     */
    @Suppress("UNCHECKED_CAST")
    fun get(): T =
        (sharedPreferences.all?.get(key) as? T?) ?: default

    /**
     * Stores a value into the [PreferenceData]
     * @author Arnau Mora
     * @since 20210321
     * @param value The value to store
     * @throws DataTypeNonStorable If the specified type at [T] is not storable.
     */
    @Throws(DataTypeNonStorable::class)
    fun put(value: T?) =
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

    /**
     * Checks if there's an stored value in the [PreferenceData]
     * @author Arnau Mora
     * @since 20210321
     * @return True if the value is stored, false otherwise
     */
    fun isSet(): Boolean =
        sharedPreferences.all?.get(key) == null
}

/**
 * Stores a value into a [PreferenceData]
 * @author Arnau Mora
 * @since 20210321
 * @param data The preference storage
 * @see PreferenceData
 * @see sharedPreferences
 */
fun <T : Any> T?.store(
    data: PreferenceData<T>
) = data.put(this)

class DataTypeNonStorable : Exception("The given data type cannot be stored in settings")
