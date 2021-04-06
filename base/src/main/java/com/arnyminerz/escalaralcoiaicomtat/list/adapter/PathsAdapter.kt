package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.app.Activity
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.BlockingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.EndingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.Grade
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.Path
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.Pitch
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.safes.FixedSafesData
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.safes.RequiredSafesData
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.ArtifoPathEndingDialog
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.DescriptionDialog
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.PathEquipmentDialog
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.LinePattern
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toStringLineJumping
import com.arnyminerz.escalaralcoiaicomtat.list.holder.SectorViewHolder
import com.arnyminerz.escalaralcoiaicomtat.view.setTextColor
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import timber.log.Timber
import java.util.concurrent.CompletableFuture.runAsync

const val ROTATION_A = 0f
const val ROTATION_B = 180f
const val ROTATION_PIVOT_X = 0.5f
const val ROTATION_PIVOT_Y = 0.5f

const val ANIMATION_DURATION = 300L

const val SMALL_CARD_HEIGHT = 73f

class PathsAdapter(private val paths: List<Path>, private val activity: Activity) :
    RecyclerView.Adapter<SectorViewHolder>() {
    private val toggled = arrayListOf<Boolean>()

    init {
        val pathsSize = paths.size
        // if (pathsSize > 0) paths.sort()
        Timber.d("Created with %d paths", pathsSize)
        (0 until pathsSize).forEach { _ -> toggled.add(false) }
    }

    override fun getItemCount(): Int = paths.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectorViewHolder {
        return SectorViewHolder(
            LayoutInflater.from(activity).inflate(
                R.layout.list_item_path, parent, false
            )
        )
    }

    /**
     * Updates the toggle status of the [CardView]: Changes the card's size according to [toggled].
     * @author Arnau Mora
     * @since 20210406
     * @param cardView The [CardView] to update.
     * @param toggled If the card should be toggled or not. If true, the card will be large, otherwise
     * it will be smaller ([SMALL_CARD_HEIGHT]).
     * @see SMALL_CARD_HEIGHT
     */
    @UiThread
    private fun updateCardToggleStatus(cardView: CardView, toggled: Boolean) {
        val params = cardView.layoutParams
        params.height = if (!toggled) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            SMALL_CARD_HEIGHT,
            activity.resources.displayMetrics
        ).toInt() else ViewGroup.LayoutParams.WRAP_CONTENT
        cardView.layoutParams = params
    }

    @ExperimentalUnsignedTypes
    override fun onBindViewHolder(holder: SectorViewHolder, position: Int) {
        if (position >= paths.size) {
            Timber.e("Position $position is out of the paths' bounds: ${paths.size}. Hiding the card.")
            visibility(holder.cardView, false)
            return
        }
        val path = paths[position]

        Timber.d("Loading path data")
        runAsync {
            val hasInfo = path.hasInfo()
            val pathSpannable = path.grade().getSpannable(activity)

            // This determines if there are more than 1 line, so that means that the path has multiple
            //   pitches, and they should be shown when the arrow is tapped.
            val shouldShowHeight = path.heights.isNotEmpty() && path.heights[0] > 0
            val heightFull =
                if (shouldShowHeight)
                    String.format(
                        activity.getString(R.string.sector_height),
                        path.heights[0]
                    ) else null
            val heightOther =
                if (shouldShowHeight && path.heights.size > 1)
                    path.heights.toStringLineJumping(
                        1,
                        LinePattern(activity, R.string.sector_height)
                    ) else null

            activity.runOnUiThread {
                val cardView = holder.cardView
                val titleTextView = holder.titleTextView
                val difficultyTextView = holder.difficultyTextView
                val infoImageButton = holder.infoImageButton
                val heightTextView = holder.heightTextView

                visibility(holder.warningImageView, false)
                visibility(holder.warningCardView, false)

                if (hasInfo)
                    infoImageButton.setOnClickListener {
                        DescriptionDialog.create(activity, path)?.show()
                            ?: Timber.e("Could not create dialog")
                    }
                else visibility(infoImageButton, false)

                titleTextView.text = path.displayName
                difficultyTextView.setText(
                    pathSpannable,
                    TextView.BufferType.SPANNABLE
                )
                difficultyTextView.maxLines = 1

                heightTextView.text = heightFull
                visibility(holder.heightTextView, shouldShowHeight)

                holder.idTextView.text = path.sketchId.toString()

                holder.toggleImageButton.rotation =
                    if (toggled[position]) ROTATION_B else ROTATION_A
                updateCardToggleStatus(holder.cardView, toggled[position])

                addChips(
                    path.endings,
                    path.pitches,
                    path.fixedSafesData,
                    path.requiredSafesData,
                    holder.safesChipGroup
                )

                holder.toggleImageButton.setOnClickListener { toggleImageButton ->
                    // Switch the toggled status
                    val toggled = !this@PathsAdapter.toggled[position]
                    this@PathsAdapter.toggled[position] = toggled

                    Timber.d("Toggling card. Now it's $toggled")

                    TransitionManager.beginDelayedTransition(
                        cardView, TransitionSet().addTransition(ChangeBounds())
                    )

                    if (toggled) {
                        titleTextView.ellipsize = null
                        titleTextView.isSingleLine = false

                        difficultyTextView.isSingleLine = false
                        difficultyTextView.setText(
                            (if (path.grades.size > 1)
                                Grade.GradesList().addAllHere(
                                    path.grades.subList(
                                        1,
                                        path.grades.size
                                    ) // Remove first line
                                ).getSpannable(activity)
                            else
                                path.grade().getSpannable(activity)),
                            TextView.BufferType.SPANNABLE
                        )

                        heightTextView.text = heightOther ?: heightFull
                    } else {
                        titleTextView.ellipsize = TextUtils.TruncateAt.END
                        titleTextView.isSingleLine = true

                        difficultyTextView.isSingleLine = true
                        difficultyTextView.setText(
                            path.grade().getSpannable(activity),
                            TextView.BufferType.SPANNABLE
                        )

                        heightTextView.text = heightFull
                    }

                    toggleImageButton.startAnimation(
                        RotateAnimation(
                            if (toggled) ROTATION_A else ROTATION_B,
                            if (toggled) ROTATION_B else ROTATION_A,
                            Animation.RELATIVE_TO_SELF,
                            ROTATION_PIVOT_X,
                            Animation.RELATIVE_TO_SELF,
                            ROTATION_PIVOT_Y
                        ).apply {
                            duration = ANIMATION_DURATION
                            interpolator = LinearInterpolator()
                            isFillEnabled = true
                            fillAfter = true
                        }
                    )

                    updateCardToggleStatus(cardView, toggled)
                }
            }

            Timber.v("Checking if blocked...")
            val blocked = path.isBlocked()
            Timber.d("Path ${path.objectId} block status: $blocked")

            activity.runOnUiThread {
                Timber.d("Binding ViewHolder for path $position: ${path.displayName}. Blocked: $blocked")

                val anyBlocking = blocked != BlockingType.UNKNOWN
                if (anyBlocking) {
                    setTextColor(holder.titleTextView, activity, R.color.grade_red)
                    holder.warningTextView.text =
                        activity.resources.getStringArray(R.array.path_warnings)[blocked.index]
                }
                visibility(holder.warningImageView, anyBlocking)
                visibility(holder.warningCardView, anyBlocking)
                visibility(holder.warningNameImageView, anyBlocking)
            }
        }
    }

    private enum class ChipType {
        SAFE, ENDING, ENDING_MULTIPLE, REQUIRED
    }

    private data class ChipData(
        val chipGroup: ChipGroup,
        val chipType: ChipType,
        @DrawableRes val icon: Int? = null
    )

    /**
     * Gets a [String] from the resources of [activity].
     * @author Arnau Mora
     * @since 20210406
     * @param stringRes The string resource to get
     * @return The string value from the resources with key [stringRes].
     */
    fun getString(@StringRes stringRes: Int) = activity.resources.getString(stringRes)

    /**
     * Adds all the chips to the chip group.
     * @author Arnau Mora
     * @since 20210406
     */
    @UiThread
    private fun addChips(
        endings: List<EndingType>,
        pitches: List<Pitch>,
        fixedSafesData: FixedSafesData,
        requiredSafesData: RequiredSafesData,
        safesChipGroup: ChipGroup
    ) {
        safesChipGroup.removeAllViews()

        if (fixedSafesData.sum() > 0)
            if (!fixedSafesData.hasSafeCount())
                addChip(
                    getString(R.string.safe_strings),
                    fixedSafesData.stringCount,
                    ChipData(
                        safesChipGroup,
                        ChipType.SAFE,
                        R.drawable.ic_icona_express
                    ),
                    endings,
                    pitches,
                    fixedSafesData,
                    requiredSafesData
                )
            else
                addChip(
                    getString(R.string.safe_strings_plural),
                    null,
                    ChipData(
                        safesChipGroup,
                        ChipType.SAFE,
                        R.drawable.ic_icona_express
                    ),
                    endings,
                    pitches,
                    fixedSafesData,
                    requiredSafesData
                )

        if (endings.size == 1 && !endings[0].isUnknown()) {
            val ending = endings.first()
            val endingVal = ending.index

            addChip(
                activity.resources.getStringArray(R.array.path_endings)[endingVal],
                null,
                ChipData(
                    safesChipGroup,
                    ChipType.ENDING,
                    ending.getImage()
                ),
                endings,
                pitches,
                fixedSafesData,
                requiredSafesData
            )
        } else if (endings.size > 1)
            addChip(
                getString(R.string.path_ending_multiple),
                null,
                ChipData(
                    safesChipGroup,
                    ChipType.ENDING_MULTIPLE
                ),
                endings,
                pitches,
                fixedSafesData,
                requiredSafesData
            )
        else
            addChip(
                getString(R.string.path_ending_none),
                null,
                ChipData(
                    safesChipGroup,
                    ChipType.ENDING,
                    R.drawable.round_close_24
                ),
                endings,
                pitches,
                fixedSafesData,
                requiredSafesData
            )
    }

    private fun addChip(
        string: String?,
        count: Int?,
        chipData: ChipData,
        endings: List<EndingType>,
        pitches: List<Pitch>,
        fixedSafesData: FixedSafesData,
        requiredSafesData: RequiredSafesData
    ) {
        val chip = Chip(activity)
        val icon = chipData.icon
        val chipType = chipData.chipType
        val chipGroup = chipData.chipGroup

        chip.text = string?.let {
            if (count == null) it
            else String.format(it, count.toString())
        } ?: ""

        chip.isClickable = true
        if (icon != null)
            chip.chipIcon = ContextCompat.getDrawable(activity, icon)
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener { chip.performClick() }
        chip.closeIcon = ContextCompat.getDrawable(activity, R.drawable.round_launch_24)

        chip.setOnClickListener {
            when (chipType) {
                ChipType.ENDING_MULTIPLE -> ArtifoPathEndingDialog(
                    activity,
                    endings,
                    pitches
                )
                ChipType.SAFE -> PathEquipmentDialog(
                    activity,
                    fixedSafesData,
                    requiredSafesData
                )
                else -> MaterialAlertDialogBuilder(
                    activity,
                    R.style.ThemeOverlay_App_MaterialAlertDialog
                )
                    .setTitle(activity.getString(R.string.path_chip_safe))
                    .setMessage(
                        activity.getString(
                            if (chipType == ChipType.REQUIRED)
                                R.string.path_chip_required
                            else
                            // We can assume this can only be ENDING, since SAFE and
                            //   ENDING_MULTIPLE can't happen since they are catched before
                                R.string.path_chip_ending
                        )
                    )
                    .setPositiveButton(R.string.action_ok, null)
                    .create()
            }.show()
        }
        chipGroup.addView(chip)
    }
}
