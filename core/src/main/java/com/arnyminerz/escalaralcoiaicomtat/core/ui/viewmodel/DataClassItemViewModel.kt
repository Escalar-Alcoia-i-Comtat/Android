package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch

class DataClassItemViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val storage = Firebase.storage

    var imageUrls by mutableStateOf<Map<String, Uri>>(mapOf())

    fun loadImage(dataClass: DataClass<*, *>) {
        @Suppress("BlockingMethodInNonBlockingContext")
        viewModelScope.launch {
            val storageUrl = dataClass.storageUrl(storage)
            val map = mutableMapOf<String, Uri>()
            map.putAll(imageUrls)
            map[dataClass.pin] = storageUrl
            imageUrls = map
        }
    }

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            if (modelClass.isAssignableFrom(DataClassItemViewModel::class.java))
                return DataClassItemViewModel(application) as T
            error("Unknown view model class: $modelClass")
        }
    }
}
