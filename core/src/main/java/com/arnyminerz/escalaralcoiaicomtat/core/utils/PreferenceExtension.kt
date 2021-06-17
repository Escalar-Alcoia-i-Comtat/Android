package com.arnyminerz.escalaralcoiaicomtat.core.utils

import androidx.preference.Preference

fun Preference?.disable() {
    if (this != null)
        isEnabled = false
}

fun Preference?.enable() {
    if (this != null)
        isEnabled = true
}

fun disable(vararg preferences: Preference?) {
    for (pref in preferences)
        pref.disable()
}

fun enable(vararg preferences: Preference?) {
    for (pref in preferences)
        pref.enable()
}
