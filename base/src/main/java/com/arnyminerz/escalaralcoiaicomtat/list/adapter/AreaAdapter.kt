package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_PREVIEW_SCALE_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.view.ImageLoadParameters
import com.arnyminerz.escalaralcoiaicomtat.list.holder.AreaViewHolder
import timber.log.Timber
import java.io.InvalidClassException

class AreaAdapter(
    private val activity: MainActivity,
    private val areas: List<Area>,
    private var clickListener: ((viewHolder: AreaViewHolder, position: Int) -> Unit)? = null
) : RecyclerView.Adapter<AreaViewHolder>() {
    init {
        Timber.v("Initialized AreaAdapter with $itemCount areas.")
    }

    override fun getItemCount(): Int = areas.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AreaViewHolder(
            LayoutInflater.from(activity).inflate(R.layout.list_item_area, parent, false)
        )

    override fun onBindViewHolder(holder: AreaViewHolder, position: Int) {
        if (areas.size < position)
            throw InvalidClassException("Current position is not a valid area")

        val area = areas[position]
        Timber.v("Showing area #$position in RecyclerView")

        ViewCompat.setTransitionName(holder.titleTextView, area.pin)

        holder.titleTextView.text = area.displayName
        holder.imageView.setOnClickListener {
            clickListener?.invoke(holder, position) ?: Timber.w("Any click listener was set!")
        }
        area.loadImage(
            activity,
            activity.storage,
            holder.imageView,
            null,
            imageLoadParameters =
            ImageLoadParameters()
                .withResultImageScale(SETTINGS_PREVIEW_SCALE_PREF.get())
                .withScaleType(ImageView.ScaleType.CENTER_CROP)
        )
    }
}
