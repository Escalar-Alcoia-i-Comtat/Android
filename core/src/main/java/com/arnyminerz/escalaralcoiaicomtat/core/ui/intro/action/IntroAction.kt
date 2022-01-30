package com.arnyminerz.escalaralcoiaicomtat.core.ui.intro.action

/**
 * Sets the data for an action for an intro page.
 * @author Arnau Mora
 * @since 20220130
 */
data class IntroAction<R>(
    val text: String,
    val type: IntroActionType<R>,
    val callback: (value: R) -> Unit,
    val enabled: Boolean = true,
)
