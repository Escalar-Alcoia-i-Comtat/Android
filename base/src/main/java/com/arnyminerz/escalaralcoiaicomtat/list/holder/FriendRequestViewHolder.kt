package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_item_friend_request.view.*

class FriendRequestViewHolder(view: View): RecyclerView.ViewHolder(view) {
    val textView: TextView = view.user_textView
    val acceptImageButton: ImageButton = view.confirm_imageButton
    val denyImageButton: ImageButton = view.deny_imageButton
    val progressBar: ProgressBar = view.friend_request_progressBar
}