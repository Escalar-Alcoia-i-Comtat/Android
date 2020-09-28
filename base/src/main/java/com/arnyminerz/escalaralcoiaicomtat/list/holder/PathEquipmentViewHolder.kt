package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_item_fixed_equipment.view.*

class PathEquipmentViewHolder(view: View) : RecyclerView.ViewHolder(view){
    val layout: LinearLayout = view.fixedEquipment_layout
    val imageView: ImageView = view.fixedEquipment_imageView
    val textView: TextView = view.fixedEquipment_textView
    val cardView: CardView = view.fixedEquipment_cardView
}