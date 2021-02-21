package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R

class CompletedPathBigViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val pathNameTextView: TextView = view.findViewById(R.id.title_textView)
    val pathGradeTextView: TextView = view.findViewById(R.id.grade_textView)
    val enterImageButton: ImageButton = view.findViewById(R.id.enter_imageButton)
}