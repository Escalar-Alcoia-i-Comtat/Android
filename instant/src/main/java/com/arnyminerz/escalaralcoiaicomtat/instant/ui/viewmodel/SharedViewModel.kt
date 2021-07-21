package com.arnyminerz.escalaralcoiaicomtat.instant.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    val areaId = MutableLiveData<String>()

    val zoneId = MutableLiveData<String>()

    val sectorId = MutableLiveData<String>()

    fun setAreaId(areaId: String) {
        this.areaId.value = areaId
    }

    fun setZoneId(zoneId: String) {
        this.zoneId.value = zoneId
    }

    fun setSectorId(sectorId: String) {
        this.sectorId.value = sectorId
    }
}