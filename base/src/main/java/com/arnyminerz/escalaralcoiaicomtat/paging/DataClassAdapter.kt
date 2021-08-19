package com.arnyminerz.escalaralcoiaicomtat.paging

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.databinding.ListItemDwDataclassBinding

class DataClassAdapter(diffCallback: DiffUtil.ItemCallback<DataClassImpl>) :
    PagingDataAdapter<DataClassImpl, DataClassAdapter.DataClassViewHolder>(diffCallback) {
    class DataClassViewHolder(val binding: ListItemDwDataclassBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataClassViewHolder {
        val binding =
            ListItemDwDataclassBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DataClassViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DataClassViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {

        }
    }
}
