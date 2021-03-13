package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.AREAS
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_PREVIEW_SCALE_PREF
import com.arnyminerz.escalaralcoiaicomtat.list.holder.AreaViewHolder
import com.arnyminerz.escalaralcoiaicomtat.view.ImageLoadParameters
import timber.log.Timber
import java.io.InvalidClassException

class AreaAdapter(
    private val context: Context,
    private var clickListener: ((viewHolder: AreaViewHolder, position: Int) -> Unit)? = null
) : RecyclerView.Adapter<AreaViewHolder>() {
    init {
        Timber.v("Initialized AreaAdapter with $itemCount areas.")
    }

    override fun getItemCount(): Int = AREAS.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AreaViewHolder(
            LayoutInflater.from(context).inflate(R.layout.list_item_area, parent, false)
        )

    override fun onBindViewHolder(holder: AreaViewHolder, position: Int) {
        if (AREAS.size < position)
            throw InvalidClassException("Current position is not a valid area")

        val area = AREAS.valueAt(position)
        Timber.v("Showing area #$position in RecyclerView")

        ViewCompat.setTransitionName(holder.titleTextView, area.transitionName)

        holder.titleTextView.text = area.displayName
        holder.imageView.setOnClickListener {
            Timber.v("Loading area ${area.displayName} with ${area.count()} zones")

            clickListener?.invoke(holder, position) ?: Timber.w("Any click listener was set!")
        }
        area.asyncLoadImage(
            context,
            holder.imageView,
            imageLoadParameters =
                ImageLoadParameters().withThumbnailSize(
                    SETTINGS_PREVIEW_SCALE_PREF.get(context.sharedPreferences)
                )
        )
    }
}
