package com.arnyminerz.escalaralcoiaicomtat.list.completions.holder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R

class CommentsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val profileImageView: ImageView = view.findViewById(R.id.profileImage_imageView)

    val profileNameTextView: TextView = view.findViewById(R.id.profileName_textView)
    val gradeTextView: TextView = view.findViewById(R.id.grade_textView)
    val commentTextView: TextView = view.findViewById(R.id.comment_textView)
    val likesTextView: TextView = view.findViewById(R.id.likes_textView)
    val deleteTextView: TextView = view.findViewById(R.id.delete_textView)
}
