package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R

class AreaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val imageView: ImageView = view.findViewById(R.id.area_imageView)
    val titleTextView: TextView = view.findViewById(R.id.title_textView)
}
