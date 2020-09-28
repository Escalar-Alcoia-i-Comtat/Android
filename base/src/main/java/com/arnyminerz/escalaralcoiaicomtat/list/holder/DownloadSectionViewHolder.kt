package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.download_section_item.view.*

class DownloadSectionViewHolder (view: View) : RecyclerView.ViewHolder(view){
    val titleTextView: TextView = view.downloadSection_title_textView
    val deleteButton: ImageButton = view.downloadSection_delete_imageButton
    val toggleButton: ImageButton = view.downloadSection_toggle_imageButton
    val viewButton: ImageButton = view.downloadSection_view_imageButton
    val recyclerView: RecyclerView = view.downloadSection_recyclerView
    val sizeChip: Chip = view.size_chip
    val cardView: CardView = view.cardView

    val progressBar: ProgressBar = view.downloadSection_progressBar

    val downloadButton: ImageButton = view.downloadSection_download_imageButton
    val downloadProgressBar: ProgressBar = view.downloadSection_download_progressBar
}