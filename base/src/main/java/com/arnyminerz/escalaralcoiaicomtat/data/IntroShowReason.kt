package com.arnyminerz.escalaralcoiaicomtat.data

enum class IntroShowReason(val msg: String) {
    OK("Intro should not be shown"),
    STORAGE_PERMISSION("Storage permission missing"),
    PREF_FALSE("Intro not shown")
}
