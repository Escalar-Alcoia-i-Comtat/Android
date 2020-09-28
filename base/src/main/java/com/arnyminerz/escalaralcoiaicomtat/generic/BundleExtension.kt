package com.arnyminerz.escalaralcoiaicomtat.generic

import android.os.Bundle
import com.arnyminerz.escalaralcoiaicomtat.exception.UnexpectedTypeException
import java.io.Serializable

/**
 * Gets and casts a serializable stored in the bundle.
 * @param key The key of the object
 * @throws UnexpectedTypeException If the object found in the key is not a serializable or the T type
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
inline fun <reified T: Serializable> Bundle.getSerializable(key: String?): T {
    val obj = get(key)

    if (obj !is Serializable)
        throw UnexpectedTypeException("The specified key is not a serializable. It's ${obj?.javaClass?.name}")

    if (obj !is T)
        throw UnexpectedTypeException("The specified key is not T. It's ${obj.javaClass.name}")

    return obj
}