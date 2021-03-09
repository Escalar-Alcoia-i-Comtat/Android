package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R

class ZonesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val imageView: ImageView = view.findViewById(R.id.zone_imageView)
    val titleTextView: TextView = view.findViewById(R.id.title_textView)
    val mapImageButton: ImageButton = view.findViewById(R.id.map_imageButton)
    val downloadImageButton: ImageButton = view.findViewById(R.id.download_imageButton)
    val progressBar: ProgressBar = view.findViewById(R.id.zone_download_progressBar)
}
