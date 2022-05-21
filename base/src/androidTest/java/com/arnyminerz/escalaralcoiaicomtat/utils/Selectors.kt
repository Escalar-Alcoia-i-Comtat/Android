package com.arnyminerz.escalaralcoiaicomtat.utils

import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector

const val defaultSelectorTimeout: Long = 5_000

/**
 * Gets an [UiObject] from the [DeviceProvider.device], and makes sure it exists. Timeouts at
 * [timeout] ms.
 * @author Arnau Mora
 * @since 20220521
 * @param description The description to search for.
 * @param timeout The amount of time to wait until the object exists.
 * @throws AssertionError If the object doesn't exist.
 */
@Throws(AssertionError::class)
fun DeviceProvider.descriptionSelector(
    description: String,
    timeout: Long = defaultSelectorTimeout,
): UiObject =
    device
        .findObject(
            UiSelector()
                .description(description)
        )
        .assertExists(timeout)

/**
 * Gets an [UiObject] from the [DeviceProvider.device], and makes sure it exists. Timeouts at
 * [timeout] ms.
 * @author Arnau Mora
 * @since 20220521
 * @param text The text to search for.
 * @param timeout The amount of time to wait until the object exists.
 * @throws AssertionError If the object doesn't exist.
 */
@Throws(AssertionError::class)
fun DeviceProvider.textSelector(
    text: String,
    timeout: Long = defaultSelectorTimeout,
): UiObject =
    device
        .findObject(
            UiSelector()
                .text(text)
        )
        .assertExists(timeout)

/**
 * Gets an [UiObject] from the [DeviceProvider.device], and makes sure it exists. Timeouts at
 * [timeout] ms.
 * @author Arnau Mora
 * @since 20220521
 * @param regex The regex text to search for.
 * @param timeout The amount of time to wait until the object exists.
 * @throws AssertionError If the object doesn't exist.
 */
@Throws(AssertionError::class)
fun DeviceProvider.regexTextSelector(
    regex: String,
    timeout: Long = defaultSelectorTimeout,
): UiObject =
    device
        .findObject(
            UiSelector()
                .textMatches(regex)
        )
        .assertExists(timeout)

/**
 * Gets an [UiObject] from the [DeviceProvider.device], and makes sure it exists. Timeouts at
 * [timeout] ms.
 * @author Arnau Mora
 * @since 20220521
 * @param className The class name to search for.
 * @param timeout The amount of time to wait until the object exists.
 * @throws AssertionError If the object doesn't exist.
 */
@Throws(AssertionError::class)
fun DeviceProvider.classNameSelector(
    className: Class<*>,
    timeout: Long = defaultSelectorTimeout,
): UiObject =
    device
        .findObject(
            UiSelector()
                .className(className)
        )
        .assertExists(timeout)
