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
        Timber.e(e, "Could not parse JSON date. JSON: $this")
        defaultValue
    } catch (e: InvalidDataTypeException) {
        Timber.e(e, "Could not parse JSON date. JSON: $this")
        defaultValue
    } catch (e: InvalidObjectTypeException) {
        Timber.e(e, "Could not parse JSON date. JSON: $this")
        defaultValue
    } catch (e: ParseException) {
        Timber.e(e, "Could not parse JSON date. JSON: $this")
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
        has(key) && get(key).toString().uppercase() != "NULL"
    } catch (e: JSONException) {
        false
    }

/**
 * Gets the String stored at [key] if it's not null, and valid.
 * @param key The key to get.
 * @return The value stored at [key] or null if invalid or null.
 */
fun JSONObject.getStringOrNull(key: String): String? =
    if (hasValid(key))
        getString(key).takeIf { it.isNotBlank() }
    else null
