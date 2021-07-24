package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import androidx.compose.ui.unit.Dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

abstract class DataClassViewModel<T : DataClass<*, *>>(application: App) :
    AndroidViewModel(application) {
    protected val firestore = Firebase.firestore
    protected val storage = Firebase.storage

    abstract val columnsPerRow: Int

    open val fixedHeight: Dp? = null

    abstract val items: LiveData<List<T>>
}