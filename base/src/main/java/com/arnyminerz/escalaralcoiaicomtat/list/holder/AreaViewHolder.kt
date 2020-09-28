package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_item_area.view.*

class AreaViewHolder (view: View) : RecyclerView.ViewHolder(view){
    val imageView: ImageView = view.area_imageView
    val titleTextView : TextView = view.title_textView
}