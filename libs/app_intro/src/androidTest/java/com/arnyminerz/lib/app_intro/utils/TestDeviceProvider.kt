package com.arnyminerz.lib.app_intro.utils

import androidx.test.uiautomator.UiDevice

abstract class TestDeviceProvider {
    internal lateinit var device: UiDevice
}
