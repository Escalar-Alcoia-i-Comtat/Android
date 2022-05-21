package com.arnyminerz.escalaralcoiaicomtat.utils

import androidx.test.uiautomator.UiObject

/**
 * Makes sure the object exists, with [timeout] ms.
 * @author Arnau Mora
 * @since 20220521
 * @param timeout The amount of time in milliseconds to wait until the object gets considered
 * un-existing.
 * @throws AssertionError When the object does not exist.
 */
@Throws(AssertionError::class)
fun UiObject.assertExists(timeout: Long): UiObject {
    assert(waitForExists(timeout)) { "Object doesn't exist." }
    return this
}
