package com.arnyminerz.escalaralcoiaicomtat.core.data.climb

import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl

interface DataRoot<T : DataClassImpl> {
    fun data(): T
}

fun <T : DataClassImpl, D : DataRoot<T>> Iterable<D>.toDataClassList(): List<T> = map { it.data() }
