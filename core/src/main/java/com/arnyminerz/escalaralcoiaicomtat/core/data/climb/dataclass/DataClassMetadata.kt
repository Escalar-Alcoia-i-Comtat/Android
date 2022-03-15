package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace

data class DataClassMetadata(
    val objectId: String,
    val namespace: Namespace,
    val webURL: String?,
    val parentId: String?,
)
