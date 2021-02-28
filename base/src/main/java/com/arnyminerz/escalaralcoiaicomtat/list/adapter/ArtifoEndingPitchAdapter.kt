package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Pitch
import com.arnyminerz.escalaralcoiaicomtat.data.climb.types.EndingType
import com.arnyminerz.escalaralcoiaicomtat.generic.isNull
import com.arnyminerz.escalaralcoiaicomtat.list.holder.ArtifoEndingPitchViewHolder
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import timber.log.Timber

class ArtifoEndingPitchAdapter(
    private val context: Context,
    private val endings: MutableList<EndingType>,
    private val pitches: MutableList<Pitch>
) : RecyclerView.Adapter<ArtifoEndingPitchViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtifoEndingPitchViewHolder =
        ArtifoEndingPitchViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.list_item_artifo_ending, parent, false
            )
        )

    override fun getItemCount(): Int = endings.size

    override fun onBindViewHolder(holder: ArtifoEndingPitchViewHolder, position: Int) {
        val pitch = if (pitches.size > position) pitches[position] else null
        val ending = endings[position]

        if (pitch.isNull()) Timber.e("Pitch is null for index $position!")

        holder.pitchIdTextView.text =
            context.getString(R.string.path_pitch_id, (position + 1).toString())
        holder.pitchTextView.text =
            pitch?.getDisplayText(context) ?: context.getString(ending.displayName)
        holder.pitchEndingImageView.setImageResource(ending.getImage())
        if (pitch != null)
            holder.pitchRappelImageView.setImageResource(pitch.getRappelImage())
        else visibility(holder.pitchRappelImageView, false, setGone = false)
    }
}