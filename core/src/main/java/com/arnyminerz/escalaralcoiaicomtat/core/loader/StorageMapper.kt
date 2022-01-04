package com.arnyminerz.escalaralcoiaicomtat.core.loader

import android.net.Uri
import coil.map.Mapper
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

class StorageMapper : Mapper<StorageReference, Uri> {
    override fun map(data: StorageReference): Uri =
        runBlocking { data.downloadUrl.await() }
}
