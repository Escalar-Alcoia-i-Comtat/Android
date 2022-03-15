package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone

/**
 * Contains a list of the namespaces of the downloadable DataClasses.
 * @author Arnau Mora
 * @since 20220304
 */
val DOWNLOADABLE_NAMESPACES = listOf(Zone.NAMESPACE, Sector.NAMESPACE)

/**
 * Fetches the namespace and object id from a pin.
 * @author Arnau Mora
 * @since 20220304
 * @param pin The pin to decode, has de format <letter>/<objectId>
 * @return A pair of Strings, the first is the decoded namespace, and the second the id.
 * @throws NoSuchElementException When the namespace could not be decoded from [pin].
 */
@Throws(NoSuchElementException::class)
fun decodePin(pin: String): Pair<Namespace, @ObjectId String> {
    val separator = pin.indexOf('_')
    val namespaceLetter = pin.substring(0, separator)[0]
    val objectId = pin.substring(separator + 1)
    val namespace = Namespace.find(namespaceLetter)
        ?: throw NoSuchElementException("Could not find a namespace starting with \"$namespaceLetter\"")
    return namespace to objectId
}