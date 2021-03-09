package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R

class PathEquipmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val layout: LinearLayout = view.findViewById(R.id.fixedEquipment_layout)
    val imageView: ImageView = view.findViewById(R.id.fixedEquipment_imageView)
    val textView: TextView = view.findViewById(R.id.fixedEquipment_textView)
    val cardView: CardView = view.findViewById(R.id.fixedEquipment_cardView)
}
