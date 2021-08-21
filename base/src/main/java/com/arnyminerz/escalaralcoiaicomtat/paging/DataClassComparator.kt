package com.arnyminerz.escalaralcoiaicomtat.paging

import androidx.recyclerview.widget.DiffUtil
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl

class DataClassComparator : DiffUtil.ItemCallback<DataClassImpl>() {
    override fun areItemsTheSame(oldItem: DataClassImpl, newItem: DataClassImpl): Boolean =
        oldItem.documentPath == newItem.documentPath

    override fun areContentsTheSame(oldItem: DataClassImpl, newItem: DataClassImpl): Boolean =
        oldItem.documentPath == newItem.documentPath
}
