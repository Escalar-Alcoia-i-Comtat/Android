package com.arnyminerz.escalaralcoiaicomtat.generic.extension

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

fun NodeList.toList(): ArrayList<Node> {
    val list = arrayListOf<Node>()
    for (i in 0 until length)
        list.add(item(i))
    return list
}
fun NodeList.toElementList(): ArrayList<Element> {
    val list = arrayListOf<Element>()
    for (element in toList())
        list.add(element as Element)
    return list
}