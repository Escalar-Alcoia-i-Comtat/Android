package com.arnyminerz.escalaralcoiaicomtat.core.utils

import com.arnyminerz.escalaralcoiaicomtat.core.exception.InvalidDataTypeException
import com.arnyminerz.escalaralcoiaicomtat.core.exception.InvalidObjectTypeException
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


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
