package com.arnyminerz.escalaralcoiaicomtat.core.network

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.Volley

/**
 * Used for accessing the Volley singleton for making network requests.
 * @author Arnau Mora
 * @since 20220222
 * @param context The context of the Application.
 * @see <a href="https://developer.android.com/training/volley/requestqueue#singleton">Android Developers</a>
 */
class VolleySingleton(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: VolleySingleton? = null

        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: VolleySingleton(context)
                    .also { INSTANCE = it }
            }
    }

    val imageLoader: ImageLoader by lazy {
        ImageLoader(requestQueue, object : ImageLoader.ImageCache {
            private val cache = LruCache<String, Bitmap>(20)

            override fun getBitmap(url: String?): Bitmap? = cache.get(url)

            override fun putBitmap(url: String?, bitmap: Bitmap?) {
                cache.put(url, bitmap)
            }
        })
    }

    val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(context.applicationContext)
    }

    fun <T> addToRequestQueue(req: Request<T>) = requestQueue.add(req)
}