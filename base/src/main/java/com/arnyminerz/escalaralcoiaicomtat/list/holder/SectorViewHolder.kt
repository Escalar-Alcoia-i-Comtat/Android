package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.google.android.material.chip.ChipGroup

class SectorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val cardView: CardView = view.findViewById(R.id.cardView)
    val expandedLayout: ConstraintLayout = view.findViewById(R.id.expanded_layout)

    val idTextView: TextView = view.findViewById(R.id.id_textView)
    val titleTextView: TextView = view.findViewById(R.id.name_textView)
    val difficultyTextView: TextView = view.findViewById(R.id.difficulty_textView)
    val heightTextView: TextView = view.findViewById(R.id.height_textView)

    val warningTextView: TextView = view.findViewById(R.id.alert_textView)
    val warningCardView: CardView = view.findViewById(R.id.alert_cardView)
    val warningNameImageView: ImageView = view.findViewById(R.id.alert_name_imageView)

    val toggleImageButton: ImageButton = view.findViewById(R.id.toggle_imageButton)
    val infoImageButton: ImageButton = view.findViewById(R.id.info_imageButton)

    val safesChipGroup: ChipGroup = view.findViewById(R.id.safesChipGroup)
}
