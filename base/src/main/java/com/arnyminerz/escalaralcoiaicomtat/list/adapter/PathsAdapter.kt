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
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.SectorActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Path
import com.arnyminerz.escalaralcoiaicomtat.data.climb.types.BlockingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.types.Grade
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.*
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.LinePattern
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toStringLineJumping
import com.arnyminerz.escalaralcoiaicomtat.generic.runAsync
import com.arnyminerz.escalaralcoiaicomtat.list.holder.SectorViewHolder
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.setTextColor
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import timber.log.Timber

const val ROTATION_A = 0f
const val ROTATION_B = 180f
const val ROTATION_PIVOT_X = 0.5f
const val ROTATION_PIVOT_Y = 0.5f

const val SMALL_CARD_HEIGHT = 57f

@ExperimentalUnsignedTypes
class PathsAdapter(private val paths: ArrayList<Path>, private val activity: Activity) :
    RecyclerView.Adapter<SectorViewHolder>() {
    private val toggled = arrayListOf<Boolean>()

    init {
        val pathsSize = paths.size
        if (pathsSize > 0) paths.sort()
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

    private fun updateToggleStatus(cardView: CardView, toggled: Boolean) {
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
            holder.cardView.hide()
            return
        }
        val path = paths[position]

        holder.warningImageView.hide()
        holder.warningCardView.hide()

        val hasInfo = path.hasInfo()
        visibility(holder.infoImageButton, hasInfo)
        if (hasInfo)
            holder.infoImageButton.setOnClickListener {
                val dialog = DescriptionDialog.create(activity, path)
                if (dialog != null)
                    dialog.show()
                else
                    Timber.e("Could not create dialog")
            }

        Timber.v("Getting network state...")
        val networkState = when (activity) {
            is SectorActivity -> activity.networkState
            is MainActivity -> activity.networkState
            else -> {
                Timber.w("Could not fetch network state. Set to NOT_CONNECTED")
                ConnectivityProvider.NetworkState.NOT_CONNECTED
            }
        }
        if (!networkState.hasInternet)
            runAsync {
                try {
                    Timber.v("Checking if blocked...")
                    val blocked = path.isBlocked(networkState)

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
                } catch (_: NoInternetAccessException) {
                    Timber.w("Could not check if the path is blocked. No Internet.")
                }
            }

        holder.titleTextView.text = path.displayName
        holder.difficultyTextView.setText(
            path.grade().getSpannable(activity),
            TextView.BufferType.SPANNABLE
        )

        holder.difficultyTextView.maxLines = 1

        val heightFull =
            if (path.heights.size > 0)
                String.format(
                    activity.getString(R.string.sector_height),
                    path.heights[0]
                ) else null
        val heightOther =
            if (path.heights.size > 1) path.heights.toStringLineJumping(
                1,
                LinePattern(activity, R.string.sector_height)
            ) else null
        holder.heightTextView.text = heightFull

        holder.idTextView.text = path.sketchId.toString()
        holder.toggleImageButton.setOnClickListener { toggleImageButton ->
            this@PathsAdapter.toggled[position] =
                !this@PathsAdapter.toggled[position]
            val cardView = holder.cardView
            val toggled = this@PathsAdapter.toggled[position]
            Timber.d("Toggling card. Now it's $toggled")

            TransitionManager.beginDelayedTransition(
                cardView, TransitionSet().addTransition(ChangeBounds())
            )

            if (toggled) {
                holder.titleTextView.ellipsize = null
                holder.titleTextView.isSingleLine = false

                holder.difficultyTextView.isSingleLine = false
                if (path.grades.size > 1)
                    holder.difficultyTextView.setText(
                        Grade.GradesList().addAllHere(
                            path.grades.subList(
                                1,
                                path.grades.size
                            ) // Remove first line
                        ).getSpannable(activity),
                        TextView.BufferType.SPANNABLE
                    )
                else
                    holder.difficultyTextView.setText(
                        path.grade().getSpannable(activity),
                        TextView.BufferType.SPANNABLE
                    )

                if (heightOther != null)
                    holder.heightTextView.text = heightOther
                else
                    holder.heightTextView.text = heightFull
            } else {
                holder.titleTextView.ellipsize = TextUtils.TruncateAt.END
                holder.titleTextView.isSingleLine = true

                holder.difficultyTextView.isSingleLine = true
                holder.difficultyTextView.setText(
                    path.grade().getSpannable(activity),
                    TextView.BufferType.SPANNABLE
                )

                holder.heightTextView.text = heightFull
            }

            val rotate = RotateAnimation(
                if (toggled) ROTATION_A else ROTATION_B,
                if (toggled) ROTATION_B else ROTATION_A,
                Animation.RELATIVE_TO_SELF,
                ROTATION_PIVOT_X,
                Animation.RELATIVE_TO_SELF,
                ROTATION_PIVOT_Y
            )
            rotate.duration = 300
            rotate.interpolator = LinearInterpolator()
            rotate.isFillEnabled = true
            rotate.fillAfter = true

            toggleImageButton.startAnimation(rotate)

            updateToggleStatus(cardView, toggled)
        }

        holder.toggleImageButton.rotation = if (toggled[position]) 180F else 0F
        updateToggleStatus(holder.cardView, toggled[position])

        holder.safesChipGroup.removeAllViews()

        if (path.fixedSafesData.sum() > 0u)
            if (!path.hasSafeCount())
                addChip(
                    R.string.safe_strings,
                    path.fixedSafesData.stringCount,
                    ChipData(
                        holder.safesChipGroup,
                        ChipType.SAFE,
                        R.drawable.ic_icona_express
                    ),
                    path = path
                )
            else
                addChip(
                    R.string.safe_strings_plural,
                    null,
                    ChipData(
                        holder.safesChipGroup,
                        ChipType.SAFE,
                        R.drawable.ic_icona_express
                    ),
                    path = path
                )

        val endings = path.endings
        if (endings.size == 1 && !endings[0].isUnknown()) {
            val ending = endings.first()
            val endingVal = ending.index

            addChip(
                activity.resources.getStringArray(R.array.path_endings)[endingVal],
                null,
                ChipData(
                    holder.safesChipGroup,
                    ChipType.ENDING,
                    ending.getImage()
                ),
                path = path
            )
        } else if (endings.size > 1) {
            addChip(
                activity.resources.getString(R.string.path_ending_multiple),
                null,
                ChipData(
                    holder.safesChipGroup,
                    ChipType.ENDING_MULTIPLE
                ),
                path = path
            )
        } else
            addChip(
                activity.resources.getString(R.string.path_ending_none),
                null,
                ChipData(
                    holder.safesChipGroup,
                    ChipType.ENDING,
                    R.drawable.round_close_24
                ),
                path = path
            )
    }

    private enum class ChipType {
        SAFE, ENDING, ENDING_MULTIPLE, REQUIRED
    }

    private data class ChipData(
        val chipGroup: ChipGroup,
        val chipType: ChipType,
        @DrawableRes val icon: Int? = null
    )

    @ExperimentalUnsignedTypes
    private fun addChip(
        @StringRes string: Int?,
        count: UInt?,
        chipData: ChipData,
        path: Path
    ) {
        addChip(
            if (string != null) activity.resources.getString(string) else null,
            count,
            chipData,
            path
        )
    }

    @ExperimentalUnsignedTypes
    private fun addChip(
        string: String?,
        count: UInt?,
        chipData: ChipData,
        path: Path
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
            if (chipType == ChipType.ENDING_MULTIPLE) {
                ArtifoPathEndingDialog(activity, path.endings, path.pitches).show()

                return@setOnClickListener
            }
            if (chipType == ChipType.SAFE) {
                PathEquipmentDialog(activity, path.fixedSafesData, path.requiredSafesData).show()

                return@setOnClickListener
            }

            MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.path_chip_safe))
                .setMessage(
                    when (chipType) {
                        ChipType.SAFE -> activity.getString(R.string.path_chip_safe)
                        ChipType.REQUIRED -> activity.getString(R.string.path_chip_required)
                        ChipType.ENDING -> activity.getString(R.string.path_chip_ending)
                        ChipType.ENDING_MULTIPLE -> ""
                    }
                )
                .setPositiveButton(R.string.action_ok, null)
                .show()
        }
        chipGroup.addView(chip)
    }
}
