package com.arnyminerz.escalaralcoiaicomtat

import android.content.Context
import androidx.annotation.WorkerThread
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * The interface for accessing the data loader of the data module.
 * @author Arnau Mora
 * @since 20211224
 */
interface DataLoaderInterface {
    /**
     * Fetches the data from assets.
     * @author Arnau Mora
     * @since 20211224
     * @param context The [Context] that is requesting the data.
     * @return The built [JSONObject] that contains the data.
     * @throws IOException When there's an error while loading the data file.
     * @throws JSONException When there's an error while parsing the JSON file.
     */
    @WorkerThread
    @Throws(IOException::class, JSONException::class)
    suspend fun fetchData(context: Context): JSONObject
}