package com.arnyminerz.escalaralcoiaicomtat.core.utils

import androidx.compose.runtime.Composable

/**
 * Calls [call] only if the class is true.
 * @author Arnau Mora
 * @since 20210819
 * @param call What to run if the class is true.
 */
fun <R> Boolean.then(call: () -> R): R? =
    if (this) call() else null

/**
 * Calls [call] only if the class is true. Used for composable calls
 * @author Arnau Mora
 * @since 20220329
 * @param call What to run if the class is true.
 */
@Composable
fun <R> Boolean.thenComp(call: @Composable () -> R): R? =
    if (this) call() else null
