package com.arnyminerz.lib.app_intro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

interface Value<T : Any?> {
    val value: State<T>
        @Composable
        get

    suspend fun setValue(value: T)
}
