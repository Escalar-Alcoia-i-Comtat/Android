package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.*
import com.arnyminerz.escalaralcoiaicomtat.core.view.hide
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.databinding.ListItemFixedEquipmentBinding
import com.arnyminerz.escalaralcoiaicomtat.list.holder.PathEquipmentViewHolder
import timber.log.Timber

class EquipmentAdapter <D: SafesData>
/**
 * The constructor of the [EquipmentAdapter] class.
 * @author Arnau Mora
 * @since 20210916
 * @param context The context where the adapter is placed at.
 * @param safes The [SafesData] that provides the required information to display.
 */
constructor(
    private val context: Context,
    private val safes: D
) : RecyclerView.Adapter<PathEquipmentViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PathEquipmentViewHolder =
        PathEquipmentViewHolder(
            ListItemFixedEquipmentBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )

    private val showableSafes = arrayListOf<SafeCountData>()
    override fun getItemCount(): Int {
        Timber.v("Calculating showable safes")
        showableSafes.clear()
        for (safe in safes)
            if (safe.count > 0) {
                showableSafes.add(safe)
                Timber.v("  Added $safe")
            }
        Timber.v("There are ${showableSafes.size} showable safes")
        return showableSafes.size
    }

    override fun onBindViewHolder(holder: PathEquipmentViewHolder, position: Int) {
        val binding = holder.binding
        val safe = showableSafes[position]

        if (safe.count > 0 || (safes is RequiredSafesData && (safes.any()))) {
            val color = (safes as? RequiredSafesData)
                ?.let { ContextCompat.getColorStateList(context, safes.color) }

            binding.fixedEquipmentTextView.text =
                if (safes is RequiredSafesData) context.getString(safe.displayName)
                else {
                    var str = context.getString(safe.displayName, safe.count)
                    if (safe.count == 1L)
                        str = str.replace(str.split(" ").first(), "")
                    str
                }
            if (color != null)
                binding.fixedEquipmentCardView.setCardBackgroundColor(color)
            binding.fixedEquipmentImageView.setImageResource(safe.image)
        } else
            binding.fixedEquipmentLayout.hide()
    }
}
