package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

abstract class DataClassViewModel<T : DataClass<*, *>> : ViewModel() {
    protected val firestore = Firebase.firestore
    protected val storage = Firebase.storage

    abstract val items: LiveData<List<T>>
}