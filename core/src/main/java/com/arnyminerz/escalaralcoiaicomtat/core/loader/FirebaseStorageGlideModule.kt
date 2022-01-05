package com.arnyminerz.escalaralcoiaicomtat.core.loader

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.google.firebase.storage.StorageReference
import java.io.InputStream

/**
 * Allows [StorageReference]s to be used with glide.
 * @author Arnau Mora
 * @since 20220105
 */
@GlideModule
class FirebaseStorageGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.prepend(
            StorageReference::class.java,
            InputStream::class.java,
            StorageModelLoaderFactory()
        )
    }
}
