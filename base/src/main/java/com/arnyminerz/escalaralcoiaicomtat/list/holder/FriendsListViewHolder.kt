package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_item_friend.view.*

class FriendsListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val imageButton: ImageButton = view.friend_imageButton
    val textView: TextView = view.friend_textView
}