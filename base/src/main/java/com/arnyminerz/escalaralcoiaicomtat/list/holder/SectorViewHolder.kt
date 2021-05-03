package com.arnyminerz.escalaralcoiaicomtat.list.holder

import android.app.Activity
import android.text.SpannableString
import android.text.TextUtils
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.BlockingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.view.setTextColor
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup

class SectorViewHolder(private val activity: Activity, view: View) : RecyclerView.ViewHolder(view) {
    val cardView: CardView = view.findViewById(R.id.cardView)
    val expandedLayout: ConstraintLayout = view.findViewById(R.id.expanded_layout)

    val idTextView: TextView = view.findViewById(R.id.id_textView)
    val titleTextView: TextView = view.findViewById(R.id.name_textView)
    val difficultyTextView: TextView = view.findViewById(R.id.difficulty_textView)
    val heightTextView: TextView = view.findViewById(R.id.height_textView)

    val warningTextView: TextView = view.findViewById(R.id.alert_textView)
    val warningCardView: CardView = view.findViewById(R.id.alert_cardView)
    val warningNameImageView: ImageView = view.findViewById(R.id.alert_name_imageView)

    val toggleImageButton: ImageButton = view.findViewById(R.id.toggle_imageButton)
    val infoImageButton: ImageButton = view.findViewById(R.id.info_imageButton)
    val commentsImageButton: ImageButton = view.findViewById(R.id.comments_imageButton)

    val safesChipGroup: ChipGroup = view.findViewById(R.id.safesChipGroup)

    val markCompletedButton: MaterialButton = view.findViewById(R.id.markCompleted_button)

    /**
     * Updates the toggle status of the [CardView]: Changes the card's size according to [toggled].
     * @author Arnau Mora
     * @since 20210406
     * @param toggled If the card should be toggled or not. If true, the card will be large, and more
     * info will be shown.
     * @param hasInfo If true, the [Path] has a description
     * @param pathSpannables The first element should be pathSpannable, the second one, toggledPathSpannable.
     * @param heights The first element should be the full height, the second one, the other cases'
     * height.
     */
    @UiThread
    fun updateCardToggleStatus(
        toggled: Boolean,
        hasInfo: Boolean,
        pathSpannables: Pair<SpannableString, SpannableString>,
        heights: Pair<String?, String?>
    ) {
        visibility(expandedLayout, toggled)
        visibility(infoImageButton, hasInfo)
        if (toggled) {
            titleTextView.ellipsize = null
            titleTextView.isSingleLine = false

            difficultyTextView.isSingleLine = false
            difficultyTextView.setText(
                pathSpannables.second,
                TextView.BufferType.SPANNABLE
            )

            heightTextView.text = heights.second ?: heights.first ?: ""
        } else {
            titleTextView.ellipsize = TextUtils.TruncateAt.END
            titleTextView.isSingleLine = true

            difficultyTextView.isSingleLine = true
            difficultyTextView.setText(
                pathSpannables.first,
                TextView.BufferType.SPANNABLE
            )

            heightTextView.text = heights.first ?: ""
        }
    }

    /**
     * Updates the blocking status of the view holder.
     * @author Arnau Mora
     * @since 20210427
     * @param status The blocking status to set
     */
    @UiThread
    fun updateBlockedStatus(status: BlockingType) {
        val anyBlocking = status != BlockingType.UNKNOWN
        if (anyBlocking) {
            setTextColor(titleTextView, activity, R.color.path_blocked_text_color)
            setTextColor(idTextView, activity, R.color.path_blocked_text_color)
            warningCardView.setCardBackgroundColor(
                activity.resources.getColor(R.color.path_blocked_card_color, activity.theme)
            )
            warningTextView.text =
                activity.resources.getStringArray(R.array.path_warnings)[status.index]
        }
        visibility(warningCardView, anyBlocking)
        visibility(warningNameImageView, anyBlocking)
    }
}
