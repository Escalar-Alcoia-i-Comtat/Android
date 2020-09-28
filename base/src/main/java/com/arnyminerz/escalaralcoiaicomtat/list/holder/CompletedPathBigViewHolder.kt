package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_item_completed_path_big.view.*

class CompletedPathBigViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val pathNameTextView: TextView = view.title_textView
    val pathGradeTextView: TextView = view.grade_textView
    val enterImageButton: ImageButton = view.enter_imageButton
}