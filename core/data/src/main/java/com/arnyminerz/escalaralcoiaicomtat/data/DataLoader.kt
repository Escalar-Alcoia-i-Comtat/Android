package com.arnyminerz.escalaralcoiaicomtat.data

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.StringBuilder

/**
 * Fetches the data from assets.
 * @author Arnau Mora
 * @since 20211224
 * @param context The [Context] that is requesting the data.
 * @return The built [JSONObject] that contains the data.
 * @throws IOException When there's an error while loading the data file.
 * @throws JSONException When there's an error while parsing the JSON file.
 */
@Throws(IOException::class, JSONException::class)
fun fetchData(context: Context): JSONObject {
    val assetManager = context.assets
    val dataStream = assetManager.open("data.json")
    val streamReader = BufferedReader(InputStreamReader(dataStream))
    val jsonBuilder = StringBuilder()
    var inputString = streamReader.readLine()
    while (inputString != null) {
        jsonBuilder.append(inputString)
        inputString = streamReader.readLine()
    }
    return JSONObject(jsonBuilder.toString())
}
