package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone

/**
 * Fetches the namespace and object id from a pin.
 * @author Arnau Mora
 * @since 20220304
 * @param pin The pin to decode, has de format <letter>/<objectId>
 * @return A pair of Strings, the first is the decoded namespace, and the second the id.
 */
fun decodePin(pin: String): Pair<@Namespace String, @ObjectId String> {
    val separator = pin.indexOf('_')
    val namespaceLetter = pin.substring(0, separator)
    val objectId = pin.substring(separator + 1)
    val namespace = if (Area.NAMESPACE.startsWith(namespaceLetter))
        Area.NAMESPACE
    else if (Zone.NAMESPACE.startsWith(namespaceLetter))
        Zone.NAMESPACE
    else if (Sector.NAMESPACE.startsWith(namespaceLetter))
        Sector.NAMESPACE
    else if (Path.NAMESPACE.startsWith(namespaceLetter))
        Path.NAMESPACE
    else ""
    return namespace to objectId
}