package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import org.json.JSONObject

@Suppress("PropertyName")
abstract class DataClassCompanion<D : DataClass<*, *, *>> {
    abstract val NAMESPACE: Namespace

    abstract val IMAGE_QUALITY: Int

    abstract val CONSTRUCTOR: (data: JSONObject, objectId: String, childrenCount: Long) -> D

    abstract val SAMPLE: D
}