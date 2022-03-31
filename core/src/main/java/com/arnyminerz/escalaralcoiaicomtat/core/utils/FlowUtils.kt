package com.arnyminerz.escalaralcoiaicomtat.core.utils

/**
 * Runs the block at [block] with the UI context.
 * @author Arnau Mora
 * @since 20220331
 * @param block The code to run.
 */
suspend fun <T, R> T.uiLet(block: suspend (T) -> R): R =
    uiContext { this@uiLet.let { t -> block.invoke(t) } }
