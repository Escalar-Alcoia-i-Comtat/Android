package com.arnyminerz.lib.app_intro.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.arnyminerz.lib.app_intro.Value

/**
 * Sets the data for an action for an intro page.
 * @author Arnau Mora
 * @since 20220130
 */
data class IntroAction<in R : Any?>(
    val text: String,
    val value: Value<@UnsafeVariance R>,
    val type: IntroActionType<@UnsafeVariance R>,
    val enabled: Boolean = true,
) {
    companion object {
        /**
         * The default value for [IntroAction] that will display no actions.
         * @author Arnau Mora
         * @since 20220130
         */
        val None = IntroAction(
            "",
            object : Value<Any?> {
                override val value: State<Any?>
                    @Composable
                    get() = remember { mutableStateOf(null) }

                override suspend fun setValue(value: Any?) {}
            },
            IntroActionType.NONE,
        )
    }
}
