package com.arnyminerz.escalaralcoiaicomtat.core.utils

import com.arnyminerz.escalaralcoiaicomtat.core.exception.InvalidDataTypeException
import com.arnyminerz.escalaralcoiaicomtat.core.exception.InvalidObjectTypeException
import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonParseException
import org.json.JSONObject
import java.util.*

/**
 * Parses a JSON-based Firebase Firestore object to a Java Date object.
 * The JSON must follow the format:
 * <code>
 * {
 *   "__datatype__": "timestamp",
 *   "value": {
 *     "_seconds": XXX,
 *     "_nanoseconds": XXX
 *   }
 * }
 * </code>
 * @author Arnau Mora
 * @since 20211224
 * @return The parsed [Date] object.
 * @throws InvalidObjectTypeException When the JSONObject does not follow the format.
 * @throws InvalidDataTypeException When the data type specified in "__datatype__" is not "timestamp".
 */
@Throws(InvalidObjectTypeException::class, InvalidDataTypeException::class)
private fun JSONObject.parseTimestamp(): Date {
    if (!has("__datatype__") || !has("value"))
        throw InvalidObjectTypeException("The JSONObject is not a valid Firebase object type.")
    if (getString("__datatype__").equals("timestamp", ignoreCase = true))
        throw InvalidDataTypeException("The Firebase object's type is not of timestamp.")
    val value = getJSONObject("value")
    val seconds = value.getLong("_seconds")
    return Date(seconds)
}

/**
 * Gets the JSON-based Firebase Firestore object to a Java Date object.
 * The JSON must follow the format:
 * <code>
 * {
 *   "__datatype__": "timestamp",
 *   "value": {
 *     "_seconds": XXX,
 *     "_nanoseconds": XXX
 *   }
 * }
 * </code>
 * @author Arnau Mora
 * @since 20211224
 * @param key The key of the object to get.
 * @return The parsed [Date] object.
 * @throws InvalidObjectTypeException When the JSONObject does not follow the format.
 * @throws InvalidDataTypeException When the data type specified in "__datatype__" is not "timestamp".
 */
@Throws(InvalidObjectTypeException::class, InvalidDataTypeException::class)
fun JSONObject.getDate(key: String, defaultValue: Date? = null): Date? =
    try {
        getJSONObject(key).parseTimestamp()
    } catch (e: JsonParseException) {
        defaultValue
    }

/**
 * Parses a JSON-based Firebase Firestore object to a LatLng object.
 * The JSON must follow the format:
 * <code>
 * {
 *   "__datatype__": "geopoint",
 *   "value": {
 *     "_latitude": XXX,
 *     "_longitude": XXX
 *   }
 * }
 * </code>
 * @author Arnau Mora
 * @since 20211224
 * @return The parsed [LatLng] object.
 * @throws InvalidObjectTypeException When the JSONObject does not follow the format.
 * @throws InvalidDataTypeException When the data type specified in "__datatype__" is not "geopoint".
 */
@Throws(InvalidObjectTypeException::class, InvalidDataTypeException::class)
private fun JSONObject.parseGeoPoint(): LatLng {
    if (!has("__datatype__") || !has("value"))
        throw InvalidObjectTypeException("The JSONObject is not a valid Firebase object type.")
    if (getString("__datatype__").equals("geopoint", ignoreCase = true))
        throw InvalidDataTypeException("The Firebase object's type is not of geopoint.")
    val value = getJSONObject("value")
    val latitude = value.getDouble("_latitude")
    val longitude = value.getDouble("_longitude")
    return LatLng(latitude, longitude)
}

/**
 * Gets the JSON-based Firebase Firestore object to a LatLng object.
 * The JSON must follow the format:
 * <code>
 * {
 *   "__datatype__": "geopoint",
 *   "value": {
 *     "_latitude": XXX,
 *     "_longitude": XXX
 *   }
 * }
 * </code>
 * @author Arnau Mora
 * @since 20211224
 * @param key The key of the object to get.
 * @return The parsed [LatLng] object.
 * @throws InvalidObjectTypeException When the JSONObject does not follow the format.
 * @throws InvalidDataTypeException When the data type specified in "__datatype__" is not "geopoint".
 */
@Throws(InvalidObjectTypeException::class, InvalidDataTypeException::class)
fun JSONObject.getLatLng(key: String, defaultValue: LatLng? = null): LatLng? =
    try {
        getJSONObject(key).parseGeoPoint()
    } catch (e: JsonParseException) {
        defaultValue
    }
