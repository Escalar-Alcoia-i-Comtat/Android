package com.arnyminerz.escalaralcoiaicomtat.view.model

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.paging.DataClassPagingSource

class DataClassListViewModel(app: App, areaId: String?, zoneId: String?, sectorId: String?) :
    AndroidViewModel(app) {
    val flow = Pager(PagingConfig(pageSize = 10)) {
        DataClassPagingSource(app.searchSession, areaId, zoneId, sectorId)
    }.flow.cachedIn(viewModelScope)
}
