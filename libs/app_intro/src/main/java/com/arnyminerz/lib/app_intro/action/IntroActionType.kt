package com.arnyminerz.lib.app_intro.action

/**
 * Stores the types of actions that are available to be used in the intro page.
 * @author Arnau Mora
 * @since 20220130
 * @param R The return type of the callback when the input is used.
 */
@Suppress("unused")
sealed class IntroActionType<R> {
    /**
     * Displays a checkbox that can be ticked. When its value changes, a callback with type
     * [Boolean] is sent.
     * @author Arnau Mora
     * @since 20220130
     */
    object CHECKBOX : IntroActionType<Boolean>()

    /**
     * Displays a switch that can be switched on or off. When its value changes, a callback with
     * [Boolean] type is sent.
     * @author Arnau Mora
     * @since 20220130
     */
    object SWITCH : IntroActionType<Boolean>()

    /**
     * Displays a button that can be pressed. When it's tapped, a callback with no type will be
     * sent.
     * @author Arnau Mora
     * @since 20220130
     */
    object BUTTON : IntroActionType<Any?>()

    /**
     * When no action is wanted to be displayed.
     * @author Arnau Mora
     * @since 20220130
     */
    object NONE : IntroActionType<Any?>()
}
