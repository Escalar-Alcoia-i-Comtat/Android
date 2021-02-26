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
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.CompletedType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Path
import com.arnyminerz.escalaralcoiaicomtat.data.climb.enum.BlockingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.enum.Grade
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.exception.NotLoggedInException
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.*
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.LinePattern
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toStringLineJumping
import com.arnyminerz.escalaralcoiaicomtat.list.holder.SectorViewHolder
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.getColor
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.setTextColor
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber


@ExperimentalUnsignedTypes
class PathsAdapter(private val paths: ArrayList<Path>, private val activity: Activity) :
    RecyclerView.Adapter<SectorViewHolder>() {
    companion object {
        private const val smallCardHeight = 57f
    }

    private val toggled = arrayListOf<Boolean>()

    init {
        val pathsSize = paths.size
        try {
            if (pathsSize > 0) paths.sort()
        } catch (ex: NullPointerException) {
            Timber.e(ex, "Could not sort paths.")
        }
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
            smallCardHeight,
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

        Timber.v("Checking if completed...")
        val networkState = when (activity) {
            is SectorActivity -> activity.networkState
            is MainActivity -> activity.networkState
            else -> ConnectivityProvider.NetworkState.NOT_CONNECTED
        }
        if (!networkState.hasInternet)
            GlobalScope.launch {
                try {
                    val completed = path.isCompleted(networkState)!!

                    activity.runOnUiThread {
                        Timber.d("Completion: ${completed.type}")
                        holder.completionImageView.setBackgroundResource(
                            when (completed.type) {
                                CompletedType.FIRST -> R.drawable.circle_red
                                CompletedType.TOP_ROPE -> R.drawable.circle_yellow
                                CompletedType.LEAD -> R.drawable.circle_green
                            }
                        )
                        holder.idTextView.setTextColor(getColor(activity, R.color.bg_color))
                    }
                } catch (_: NoInternetAccessException) { // This shouldn't be thrown, but who knows
                    Timber.e("Could not check if the path is completed. No Internet.")
                } catch (_: NotLoggedInException) {
                    Timber.w("Could not check if the path is completed. User not logged in")
                } catch (error: Exception) {
                    Timber.e(error, "Could not check if the path is completed")
                    activity.runOnUiThread {
                        holder.completionImageView.setBackgroundResource(R.drawable.circle_transparent)
                        holder.idTextView.setTextColor(
                            getColor(
                                activity,
                                R.color.dark_background_color
                            )
                        )
                    }
                }

                try {
                    val blocked = path.isBlocked(networkState)

                    activity.runOnUiThread {
                        Timber.d("Binding ViewHolder for path $position: ${path.displayName}. Blocked: $blocked")

                        val anyBlocking = blocked != BlockingType.UNKNOWN
                        if (anyBlocking) {
                            setTextColor(holder.titleTextView, activity, R.color.grade_red)
                            holder.warningTextView.text =
                                activity.resources.getStringArray(R.array.path_warnings)[blocked.value]
                        }
                        visibility(holder.warningImageView, anyBlocking)
                        visibility(holder.warningCardView, anyBlocking)
                        visibility(holder.warningNameImageView, anyBlocking)
                    }
                } catch (_: NoInternetAccessException) {
                    Timber.e("Could not check if the path is blocked. No Internet.")
                } catch (error: Exception) {
                    Timber.e(error, "Could not check if path is blocked.")
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
                    path.grade().getSpannable(activity), //firstLineSpannable,
                    TextView.BufferType.SPANNABLE
                )

                holder.heightTextView.text = heightFull
            }

            val rotate = RotateAnimation(
                if (toggled) 0F else 180F,
                if (toggled) 180F else 0F,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
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
                    holder.safesChipGroup,
                    R.drawable.ic_icona_express,
                    chipType = ChipType.SAFE,
                    path = path
                )
            else
                addChip(
                    R.string.safe_strings_plural,
                    null,
                    holder.safesChipGroup,
                    R.drawable.ic_icona_express,
                    chipType = ChipType.SAFE,
                    path = path
                )

        val endings = path.endings
        if (endings.size == 1 && !endings[0].isUnknown()) {
            val ending = endings.first()
            val endingVal = ending.value

            addChip(
                activity.resources.getStringArray(R.array.path_endings)[endingVal],
                null,
                holder.safesChipGroup,
                ending.getImage(),
                chipType = ChipType.ENDING,
                path = path
            )
        } else if (endings.size > 1) {
            addChip(
                activity.resources.getString(R.string.path_ending_multiple),
                null,
                holder.safesChipGroup,
                null,
                chipType = ChipType.ENDING_MULTIPLE,
                path = path
            )
        } else
            addChip(
                activity.resources.getString(R.string.path_ending_none),
                null,
                holder.safesChipGroup,
                R.drawable.round_close_24,
                chipType = ChipType.ENDING,
                path = path
            )
    }

    private enum class ChipType {
        SAFE, ENDING, ENDING_MULTIPLE, REQUIRED
    }

    @ExperimentalUnsignedTypes
    private fun addChip(
        @StringRes string: Int?,
        count: UInt?,
        chipGroup: ChipGroup,
        @DrawableRes icon: Int? = null,
        chipType: ChipType,
        path: Path
    ) {
        addChip(
            if (string != null) activity.resources.getString(string) else null,
            count,
            chipGroup,
            icon,
            chipType,
            path
        )
    }

    @ExperimentalUnsignedTypes
    private fun addChip(
        string: String?,
        count: UInt?,
        chipGroup: ChipGroup,
        @DrawableRes icon: Int? = null,
        chipType: ChipType,
        path: Path
    ) {
        val chip = Chip(activity)
        val str = string ?: ""
        chip.text = if (string != null) if (count == null) str else String.format(
            str,
            count.toString()
        ) else ""

        chip.isClickable = true
        if (icon != null) {
            chip.chipIcon = ContextCompat.getDrawable(activity, icon)
        }
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
