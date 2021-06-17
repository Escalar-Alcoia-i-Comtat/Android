package com.arnyminerz.escalaralcoiaicomtat.core.worker

import androidx.collection.arrayMapOf
import androidx.work.Data
import androidx.work.ListenableWorker

fun dataOf(vararg pairs: Pair<String, Any>): Data {
    val map = arrayMapOf<String, Any>()
    for ((key, value) in pairs)
        map[key] = value
    return dataOf(map)
}

fun dataOf(data: Map<String, Any>): Data {
    val builder = Data.Builder()
    builder.putAll(data)
    return builder.build()
}

fun failure(error: String) =
    ListenableWorker.Result.failure(dataOf(
        "error" to error
    ))
