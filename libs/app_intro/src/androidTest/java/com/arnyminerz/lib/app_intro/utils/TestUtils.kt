package com.arnyminerz.lib.app_intro.utils

import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector

/**
 * Tries to find an object in [TestDeviceProvider.device] whose text is equal to [text].
 * @author Arnau Mora
 * @since 20220520
 * @param text The text to search for.
 */
fun TestDeviceProvider.textSelector(text: String): UiObject =
    device
        .findObject(
            UiSelector()
                .text(text)
        )

/**
 * Tries to find an object in [TestDeviceProvider.device] whose class name is equal to [className].
 * @author Arnau Mora
 * @since 20220520
 * @param className The class name to search for.
 */
fun TestDeviceProvider.classNameSelector(className: Class<*>): UiObject =
    device
        .findObject(
            UiSelector()
                .className(className)
        )

/**
 * Tries to find an object in [TestDeviceProvider.device] whose description is equal to [description].
 * @author Arnau Mora
 * @since 20220520
 * @param description The description to search for.
 */
fun TestDeviceProvider.descriptionSelector(description: String): UiObject =
    device
        .findObject(
            UiSelector()
                .description(description)
        )

/**
 * Serves as an alias for [descriptionSelector] but using a [StringRes] as input.
 * @author Arnau Mora
 * @since 20220520
 * @param descriptionRes The description to search for.
 */
fun TestDeviceProvider.descriptionSelector(@StringRes descriptionRes: Int): UiObject =
    descriptionSelector(
        InstrumentationRegistry
            .getInstrumentation()
            .context
            .getString(descriptionRes)
    )

/**
 * Makes sure that an [UiObject] exists. If it doesn't exist after [timeout] ms, [AssertionError] is
 * thrown, otherwise [this] is returned.
 * @author Arnau Mora
 * @since 20220520
 * @param timeout The amount of time to wait for the existence in milliseconds.
 */
fun UiObject.assertExists(timeout: Long): UiObject {
    assert(
        waitForExists(timeout)
    )
    return this
}
