package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.shared.context
import kotlinx.coroutines.launch

class DataClassItemViewModel(
    application: Application
) : AndroidViewModel(application) {
    val children by DataSingleton.getInstance(application).children

    fun <T : DataClass<*, *, *>, R : Comparable<R>> loadChildren(
        dataClass: T,
        sortBy: (DataClassImpl) -> R
    ) {
        viewModelScope.launch {
            DataSingleton.getInstance(getApplication()).apply {
                children.value = emptyList()
                children.value = dataClass.getChildren(context, sortBy)
            }
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
