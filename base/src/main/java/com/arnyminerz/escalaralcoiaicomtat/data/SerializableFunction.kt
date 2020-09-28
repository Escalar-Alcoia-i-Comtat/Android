package com.arnyminerz.escalaralcoiaicomtat.data

import java.io.Serializable

interface SerializableFunction : Serializable {
    fun caller()

    operator fun invoke() {
        caller()
    }
}