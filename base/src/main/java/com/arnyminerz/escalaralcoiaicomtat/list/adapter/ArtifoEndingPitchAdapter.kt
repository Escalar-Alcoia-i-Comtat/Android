package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.EndingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.Pitch
import com.arnyminerz.escalaralcoiaicomtat.list.holder.ArtifoEndingPitchViewHolder
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import timber.log.Timber

class ArtifoEndingPitchAdapter(
    private val context: Context,
    private val endings: List<EndingType>,
    private val pitches: List<Pitch>
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

        if (pitch == null) Timber.w("There's no pitch for index $position!")

        holder.pitchIdTextView.text =
            context.getString(R.string.path_pitch_id, (position + 1).toString())
        holder.pitchTextView.text =
            pitch?.getDisplayText(context) ?: context.getString(ending.displayName)
        holder.pitchEndingImageView.setImageResource(ending.getImage())
        try {
            holder.pitchRappelImageView.setImageResource(pitch!!.getRappelImage())
        } catch (_: NullPointerException) {
            holder.pitchRappelLayout.hide()
        } catch (_: IllegalStateException) {
            holder.pitchRappelLayout.hide()
        }
    }
}
