package com.arnyminerz.escalaralcoiaicomtat.list.completions.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.profile.CommentsActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.MarkedDataInt
import com.arnyminerz.escalaralcoiaicomtat.list.completions.holder.CommentsViewHolder

/**
 * The RecyclerView adapter for showing the comments that people have published into a Path.
 * This is meant to be used inside [CommentsActivity].
 * @author Arnau Mora
 * @since 2021051
 * @param activity The activity that contains the RecyclerView to use.
 * @param items The [MarkedDataInt] elements to show.
 */
class CommentsAdapter(
    private val activity: CommentsActivity,
    private val items: List<MarkedDataInt>
) : RecyclerView.Adapter<CommentsViewHolder>() {
    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentsViewHolder =
        CommentsViewHolder(
            LayoutInflater.from(activity).inflate(R.layout.list_item_comment, parent, false)
        )

    override fun onBindViewHolder(holder: CommentsViewHolder, position: Int) {
        TODO("Not yet implemented")
    }
}
