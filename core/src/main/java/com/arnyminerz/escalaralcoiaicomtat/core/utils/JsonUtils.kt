package com.arnyminerz.escalaralcoiaicomtat.core.utils

import com.arnyminerz.escalaralcoiaicomtat.core.exception.InvalidDataTypeException
import com.arnyminerz.escalaralcoiaicomtat.core.exception.InvalidObjectTypeException
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * Converts a date with the ISO format to a [Date] object.
 * @author Arnau Mora
 * @since 20220223
 * @param key The key of the object to get.
 * @return The parsed [Date] object.
 */
fun JSONObject.getDate(key: String, defaultValue: Date? = null): Date? =
    try {
        val date = getString(key)
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        format.parse(date) ?: defaultValue
    } catch (e: JSONException) {
        defaultValue
    } catch (e: InvalidDataTypeException) {
        defaultValue
    } catch (e: InvalidObjectTypeException) {
        defaultValue
    } catch (e: ParseException) {
        Timber.e(e, "Could not parse data.")
        defaultValue
    }

/**
 * Gets the value at [key] and gets casted to [T], if could not be casted, or not existing returns
 * [defaultValue].
 * @author Arnau Mora
 * @since 20220330
 * @param key The key to get.
 * @param defaultValue The value to return if the object does not contain [key] or it's invalid.
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any?> JSONObject.getValue(key: String, defaultValue: T): T =
    try {
        if (hasValid(key))
            get(key) as? T ?: defaultValue
        else
            defaultValue
    } catch (e: JSONException) {
        defaultValue
    }

/**
 * Checks if the [JSONObject] has a child key with a valid value (excluding "NULL").
 * @author Arnau Mora
 * @since 20220330
 * @return True if [JSONObject] has [key] and it's not null.
 */
fun JSONObject.hasValid(key: String): Boolean =
    try {
        has(key) && getString(key).uppercase() != "NULL"
    } catch (e: JSONException) {
        false
    }
