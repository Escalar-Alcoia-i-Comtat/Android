package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

data class DataClassMetadata(
    val objectId: String,
    val namespace: String,
    val parentNamespace: String?,
    val childNamespace: String?,
    val documentPath: String,
    val webURL: String?,
    val parentId: String?,
)
