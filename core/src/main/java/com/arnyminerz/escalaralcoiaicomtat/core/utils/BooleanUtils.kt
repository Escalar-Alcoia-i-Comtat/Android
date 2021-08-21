package com.arnyminerz.escalaralcoiaicomtat.core.utils

/**
 * Calls [call] only if the class is true.
 * @author Arnau Mora
 * @since 20210819
 * @param call What to run if the class is true.
 */
fun <R> Boolean.then(call: () -> R): R? =
    if (this) call() else null
