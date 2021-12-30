package com.arnyminerz.escalaralcoiaicomtat.view.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App

class DataClassListViewModelFactory(
    private val app: App,
    private val areaId: String?,
    private val zoneId: String?,
    private val sectorId: String?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(DataClassListViewModel::class.java))
            return DataClassListViewModel(app, areaId, zoneId, sectorId) as T

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
