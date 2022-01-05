package com.arnyminerz.escalaralcoiaicomtat.core.loader

import com.bumptech.glide.load.Key
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.signature.ObjectKey
import com.google.firebase.storage.StorageReference
import java.io.InputStream

class StorageModelLoader : ModelLoader<StorageReference, InputStream> {
    override fun buildLoadData(
        model: StorageReference,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream> {
        val diskCacheKey: Key = ObjectKey(model.path)
        return ModelLoader.LoadData(diskCacheKey, StorageDataFetcher(model))
    }

    // Accept all type of storage references
    override fun handles(model: StorageReference): Boolean = true
}