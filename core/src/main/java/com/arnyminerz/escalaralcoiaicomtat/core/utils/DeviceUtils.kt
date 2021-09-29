package com.arnyminerz.escalaralcoiaicomtat.core.utils

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Checks if the device is compatible with MD5 file hashing.
 * @author Arnau Mora
 * @since 20210929
 * @return true if the device supports MD5 hashing, false otherwise.
 */
fun md5Compatible(): Boolean =
    try {
        MessageDigest.getInstance("MD5")
        true
    } catch (e: NoSuchAlgorithmException) {
        false
    }
