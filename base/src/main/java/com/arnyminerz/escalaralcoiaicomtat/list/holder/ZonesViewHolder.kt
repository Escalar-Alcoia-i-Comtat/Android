package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_item_area.view.title_textView
import kotlinx.android.synthetic.main.list_item_zone.view.*
import kotlinx.android.synthetic.main.list_item_zone.view.download_imageButton

class ZonesViewHolder (view: View) : RecyclerView.ViewHolder(view){
    val imageView: ImageView = view.zone_imageView
    val titleTextView : TextView = view.title_textView
    val mapImageButton: ImageButton = view.map_imageButton
    val downloadImageButton : ImageButton = view.download_imageButton
    val progressBar : ProgressBar = view.zone_download_progressBar
}