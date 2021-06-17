package com.arnyminerz.escalaralcoiaicomtat.core.utils

import org.w3c.dom.Element

fun Element.hasChildNode(nodeName: String): Boolean =
    getElementsByTagName(nodeName).length > 0

fun Element.getElementByTagName(tagName: String) : Element? =
    getElementsByTagName(tagName)?.item(0) as Element?

fun Element.getElementByTagNameWithAttribute(tagName: String, attribute: String, value: String) : Element? {
    for(element in getElementsByTagName(tagName).toElementList())
        if (element.hasAttribute(attribute) && element.getAttribute(attribute) == value)
            return element

    return null
}
