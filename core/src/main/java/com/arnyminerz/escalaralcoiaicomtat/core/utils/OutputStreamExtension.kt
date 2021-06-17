package com.arnyminerz.escalaralcoiaicomtat.core.utils

import java.io.OutputStream

fun OutputStream.write(s: String) {
    write(s.toByteArray())
}
