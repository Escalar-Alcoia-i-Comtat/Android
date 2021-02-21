package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R

class ArtifoEndingPitchViewHolder(view: View): RecyclerView.ViewHolder(view) {
    val pitchTextView: TextView = view.findViewById(R.id.pitch_textView)
    val pitchRappelImageView: ImageView = view.findViewById(R.id.pitchRappel_imageView)
    val pitchEndingImageView: ImageView = view.findViewById(R.id.pitchEnding_imageView)
    val pitchIdTextView: TextView = view.findViewById(R.id.pitchId_textView)
}