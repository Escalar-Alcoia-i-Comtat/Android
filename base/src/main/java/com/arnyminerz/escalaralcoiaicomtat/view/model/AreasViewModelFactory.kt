package com.arnyminerz.escalaralcoiaicomtat.view.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App

class AreasViewModelFactory(private val app: App) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AreasViewModel::class.java))
            return AreasViewModel(app) as T

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
