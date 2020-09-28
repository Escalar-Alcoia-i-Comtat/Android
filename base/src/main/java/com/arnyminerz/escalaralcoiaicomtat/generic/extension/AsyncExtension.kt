package com.arnyminerz.escalaralcoiaicomtat.generic.extension

import kotlinx.coroutines.flow.FlowCollector
import java.util.*

suspend fun <T> FlowCollector<T>.emitAll(items: ArrayList<T>) {
    for (item in items)
        emit(item)
}