package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R

class ProfilePathViewHolder (view: View) : RecyclerView.ViewHolder(view){
    val textView: TextView = view.findViewById(R.id.textView)

    val gradeTextView: TextView = view.findViewById(R.id.grade_textView)

    val attemptsTextView: TextView = view.findViewById(R.id.attempts_textView)
    val hangsTextView: TextView = view.findViewById(R.id.hangs_textView)

    val shareButton: ImageButton = view.findViewById(R.id.share_button)
}