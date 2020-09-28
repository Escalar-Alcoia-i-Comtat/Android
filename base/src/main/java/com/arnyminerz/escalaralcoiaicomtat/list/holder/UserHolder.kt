package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.android.synthetic.main.list_item_user.view.*

class UserHolder(view: View): RecyclerView.ViewHolder(view){
    val imageView: ImageView = view.user_imageView
    val textView: TextView = view.username_textView
    val addFriendButton: MaterialButton = view.add_friend_button
    val addingProgressBar: ProgressBar = view.adding_progressBar
}