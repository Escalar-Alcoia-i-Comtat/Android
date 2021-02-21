package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R

class SectorsViewHolder(view: View): RecyclerView.ViewHolder(view) {
    val imageView: ImageView = view.findViewById(R.id.sector_imageView)
    val titleTextView: TextView = view.findViewById(R.id.title_textView)
    val downloadImageButton: ImageButton = view.findViewById(R.id.download_imageButton)
    val downloadProgressBar: ProgressBar = view.findViewById(R.id.sector_download_progressBar)
    val cardView: CardView = view.findViewById(R.id.sector_cardView)
}