package com.arnyminerz.escalaralcoiaicomtat.utils

import androidx.test.uiautomator.UiDevice

abstract class DeviceProvider {
    internal lateinit var device: UiDevice
}