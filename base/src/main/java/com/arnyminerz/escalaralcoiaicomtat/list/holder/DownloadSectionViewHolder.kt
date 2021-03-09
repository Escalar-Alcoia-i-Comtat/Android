package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.google.android.material.chip.Chip

class DownloadSectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val titleTextView: TextView = view.findViewById(R.id.downloadSection_title_textView)
    val deleteButton: ImageButton = view.findViewById(R.id.downloadSection_delete_imageButton)
    val toggleButton: ImageButton = view.findViewById(R.id.downloadSection_toggle_imageButton)
    val viewButton: ImageButton = view.findViewById(R.id.downloadSection_view_imageButton)
    val recyclerView: RecyclerView = view.findViewById(R.id.downloadSection_recyclerView)
    val sizeChip: Chip = view.findViewById(R.id.size_chip)
    val cardView: CardView = view.findViewById(R.id.cardView)

    val progressBar: ProgressBar = view.findViewById(R.id.downloadSection_progressBar)

    val downloadButton: ImageButton = view.findViewById(R.id.downloadSection_download_imageButton)
    val downloadProgressBar: ProgressBar = view.findViewById(R.id.downloadSection_download_progressBar)
}
