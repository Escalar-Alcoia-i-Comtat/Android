package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_item_sector_image.view.*

class SectorsViewHolder(view: View): RecyclerView.ViewHolder(view) {
    val imageView: ImageView = view.sector_imageView
    val titleTextView: TextView = view.title_textView
    val downloadImageButton: ImageButton = view.download_imageButton
    val downloadProgressBar: ProgressBar = view.sector_download_progressBar
    val cardView: CardView = view.sector_cardView
}