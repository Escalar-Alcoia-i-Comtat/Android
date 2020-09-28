package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import kotlinx.android.synthetic.main.list_item_path.view.*

class SectorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val cardView: CardView = view.cardView

    val completionImageView : ImageView = view.completion_imageView
    val idTextView: TextView = view.id_textView
    val titleTextView: TextView = view.name_textView
    val difficultyTextView: TextView = view.difficulty_textView
    val heightTextView: TextView = view.height_textView

    val warningImageView: ImageView = view.alert_imageView
    val warningTextView : TextView = view.alert_textView
    val warningCardView : CardView = view.alert_cardView
    val warningNameImageView: ImageView = view.alert_name_imageView

    val toggleImageButton: ImageButton = view.toggle_imageButton
    val infoImageButton: ImageButton = view.info_imageButton

    val safesChipGroup: ChipGroup = view.safesChipGroup

    val markCompletedButton : MaterialButton = view.mark_completed_button
}