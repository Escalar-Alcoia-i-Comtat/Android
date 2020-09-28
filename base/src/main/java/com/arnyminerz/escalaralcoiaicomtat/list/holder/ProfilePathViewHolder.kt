package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_item_profile_path.view.*

class ProfilePathViewHolder (view: View) : RecyclerView.ViewHolder(view){
    val textView: TextView = view.textView

    val gradeTextView: TextView = view.grade_textView

    val attemptsTextView: TextView = view.attempts_textView
    val hangsTextView: TextView = view.hangs_textView

    val shareButton: ImageButton = view.share_button
}