package com.arnyminerz.escalaralcoiaicomtat.core.utils

import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Makes a HTTP GET request using Volley.
 * @author Arnau Mora
 * @since 20220219
 * @param url The url to make the request to.
 * @param jsonRequest A [JSONObject] to post with the request. Null indicates no parameters will be posted along with request.
 * @return The GET result in JSON format.
 */
suspend fun getJson(url: String, jsonRequest: JSONObject? = null) =
    suspendCoroutine<JSONObject> { cont ->
        JsonObjectRequest(
            Request.Method.GET,
            url,
            jsonRequest,
            { cont.resume(it) },
            { cont.resumeWithException(it) }
        )
    }

class InputStreamVolleyRequest(
    post: Int,
    url: String,
    private val listener: Response.Listener<ByteArray>,
    errorListener: Response.ErrorListener,
    private val params: Map<String, String>,
    private val requestHeaders: Map<String, String>
) : Request<ByteArray>(post, url, errorListener) {
    var responseHeaders: Map<String, String>? = null
        private set

    init {
        setShouldCache(false)
    }

    override fun getParams(): Map<String, String> = params

    override fun getHeaders(): Map<String, String> = super.getHeaders()
        .toMutableMap()
        .apply {
            putAll(requestHeaders)
        }

    override fun deliverResponse(response: ByteArray?) = listener.onResponse(response)

    override fun parseNetworkResponse(response: NetworkResponse?): Response<ByteArray> =
        Response.success(response?.data, HttpHeaderParser.parseCacheHeaders(response)).also {
            responseHeaders = response?.headers
        }
}
