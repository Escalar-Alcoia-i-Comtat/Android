package com.arnyminerz.escalaralcoiaicomtat.core.loader

import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.google.firebase.storage.StorageReference
import java.io.InputStream

class StorageModelLoaderFactory : ModelLoaderFactory<StorageReference, InputStream> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<StorageReference, InputStream> =
        StorageModelLoader()

    override fun teardown() {
        // Nothing is needed
    }
}