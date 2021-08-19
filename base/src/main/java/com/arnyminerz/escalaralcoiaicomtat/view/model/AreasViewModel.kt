package com.arnyminerz.escalaralcoiaicomtat.view.model

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.paging.DataClassPagingSource

class AreasViewModel(app: App) : AndroidViewModel(app) {
    val flow = Pager(PagingConfig(pageSize = 10)) {
        DataClassPagingSource(app.searchSession)
    }.flow.cachedIn(viewModelScope)
}
