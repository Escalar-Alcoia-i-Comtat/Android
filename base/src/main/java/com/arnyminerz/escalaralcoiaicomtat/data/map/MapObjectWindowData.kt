package com.arnyminerz.escalaralcoiaicomtat.data.map

import com.arnyminerz.escalaralcoiaicomtat.generic.generateUUID
import java.io.Serializable

data class MapObjectWindowData(
    var title: String?,
    var message: String?,
    val clickAction: Unit?
): Serializable {
    private var layerUUID: String = generateUUID()

    @Suppress("unused")
    fun getLayerUUID(): String = layerUUID
}