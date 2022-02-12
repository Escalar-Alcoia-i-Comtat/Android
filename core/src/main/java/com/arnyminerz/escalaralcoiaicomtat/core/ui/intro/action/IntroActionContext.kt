package com.arnyminerz.escalaralcoiaicomtat.core.ui.intro.action

/**
 * Used in [IntroAction] for allowing to do some modifications over the action.
 * @author Arnau Mora
 * @since 20220130
 */
abstract class IntroActionContext<T> {
    /**
     * Updates the state of the action.
     * @author Arnau Mora
     * @since 20220130
     * @param state The new state to set.
     */
    abstract fun setState(state: T)
}
