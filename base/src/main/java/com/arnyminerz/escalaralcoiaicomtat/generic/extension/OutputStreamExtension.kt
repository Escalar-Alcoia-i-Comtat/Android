package com.arnyminerz.escalaralcoiaicomtat.generic.extension

import java.io.OutputStream

fun OutputStream.write(s: String) {
    write(s.toByteArray())
}
