package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.RequiredSafesData
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.SafeCountData
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.SafesData
import com.arnyminerz.escalaralcoiaicomtat.list.holder.PathEquipmentViewHolder
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import timber.log.Timber

enum class EquipmentAdapterType {
    FIXED, REQUIRED;

    @ColorRes
    fun getColor(): Int = when (this) {
        REQUIRED -> R.color.dialog_blue
        FIXED -> R.color.dialog_green
    }
}

@ExperimentalUnsignedTypes
class EquipmentAdapter(
    private val context: Context,
    private val safes: SafesData,
    private val equipmentType: EquipmentAdapterType
) : RecyclerView.Adapter<PathEquipmentViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PathEquipmentViewHolder =
        PathEquipmentViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.list_item_fixed_equipment, parent, false
            )
        )

    private val showableSafes = arrayListOf<SafeCountData>()
    override fun getItemCount(): Int {
        Timber.v("Calculating showable safes")
        showableSafes.clear()
        for (s in 0 until safes.count()) {
            val safe = safes[s]
            if(safe != null)
            if (safe.count > 0u) {
                showableSafes.add(safe)
                Timber.v("  Added $safe")
            }
        }
        Timber.v("There are ${showableSafes.size} showable safes")
        return showableSafes.size
    }

    override fun onBindViewHolder(holder: PathEquipmentViewHolder, position: Int) {
        val safe = showableSafes[position]

        if (safe.count > 0u || (safes is RequiredSafesData && (safes.any()))) {
            val color = ContextCompat.getColorStateList(context, equipmentType.getColor())

            holder.textView.text =
                if (equipmentType == EquipmentAdapterType.REQUIRED) context.getString(safe.displayName)
                else {
                    var str = context.getString(safe.displayName, safe.count)
                    if (safe.count == 1u)
                        str = str.replace(str.split(" ").first(), "")
                    str
                }
            if (color != null)
                holder.cardView.setCardBackgroundColor(color)
            holder.imageView.setImageResource(safe.image)
        } else {
            visibility(holder.layout, false)
        }
    }
}