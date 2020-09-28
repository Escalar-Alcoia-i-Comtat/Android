package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_item_artifo_ending.view.*

class ArtifoEndingPitchViewHolder(view: View): RecyclerView.ViewHolder(view) {
    val pitchTextView: TextView = view.pitch_textView
    val pitchRappelImageView: ImageView = view.pitchRappel_imageView
    val pitchEndingImageView: ImageView = view.pitchEnding_imageView
    val pitchIdTextView: TextView = view.pitchId_textView
}