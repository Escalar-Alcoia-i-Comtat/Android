package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.SectorActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.profile.MarkCompletedActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.BlockingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.EndingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.Grade
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.Pitch
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.safes.FixedSafesData
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.safes.RequiredSafesData
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.ArtifoPathEndingDialog
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.DescriptionDialog
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.PathEquipmentDialog
import com.arnyminerz.escalaralcoiaicomtat.generic.doAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.LinePattern
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toStringLineJumping
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.uiContext
import com.arnyminerz.escalaralcoiaicomtat.list.holder.SectorViewHolder
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_PATH
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_SECTOR_INDEX
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import timber.log.Timber

const val ROTATION_A = 0f
const val ROTATION_B = 180f
const val ROTATION_PIVOT_X = 0.5f
const val ROTATION_PIVOT_Y = 0.5f

const val ANIMATION_DURATION = 300L

/**
 * Specifies the [RecyclerView]'s adapter for the paths list.
 * @author Arnau Mora
 * @since 20210427
 * @param paths The paths list
 * @param activity the activity that is loading the recycler view
 * @see SectorViewHolder
 */
class PathsAdapter(private val paths: List<Path>, private val activity: SectorActivity) :
    RecyclerView.Adapter<SectorViewHolder>() {
    /**
     * Specifies the toggled status of all the paths.
     * @author Arnau Mora
     * @since 20210427
     */
    private val toggled = arrayListOf<Boolean>()

    /**
     * Specifies the blocking status of all the paths.
     * @author Arnau Mora
     * @since 20210427
     * @see BlockingType
     */
    private val blockStatuses = arrayListOf<BlockingType>()

    /**
     * Stores the Firestore instance.
     * @author Arnau Mora
     * @since 20210427
     */
    private val firestore = Firebase.firestore

    init {
        // Initialize [toggled] and [blockStatuses] with the default values.
        val pathsSize = paths.size
        Timber.d("Created with %d paths", pathsSize)
        (0 until pathsSize).forEach { _ ->
            toggled.add(false)
            blockStatuses.add(BlockingType.UNKNOWN)
        }
    }

    override fun getItemCount(): Int = paths.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectorViewHolder =
        SectorViewHolder(
            activity,
            LayoutInflater.from(activity).inflate(
                R.layout.list_item_path, parent, false
            )
        )

    override fun onBindViewHolder(holder: SectorViewHolder, position: Int) {
        if (position >= paths.size) {
            Timber.e("Position $position is out of the paths' bounds: ${paths.size}. Hiding the card.")
            visibility(holder.cardView, false)
            return
        }
        val path = paths[position]

        Timber.d("Loading path data")
        doAsync {
            loadPathData(path, position, holder)
        }
    }

    /**
     * Loads the [Path]'s data, and shows it through the UI.
     * @author Arnau Mora
     * @since 20210427
     * @param path The [Path]'s data to load.
     * @param position The index of the path element
     * @param holder The view holder to update
     */
    @WorkerThread
    private suspend fun loadPathData(path: Path, position: Int, holder: SectorViewHolder) {
        // First load all the required data
        val toggled = this@PathsAdapter.toggled[position]
        val blockStatus = blockStatuses[position]
        val hasInfo = path.hasInfo()
        val pathSpannable = path.grade().getSpannable(activity)
        val toggledPathSpannable =
            if (path.grades.size > 1)
                Grade.GradesList(
                    path.grades.subList(
                        1,
                        path.grades.size
                    ) // Remove first line
                ).getSpannable(activity)
            else
                pathSpannable

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

        val descriptionDialog = DescriptionDialog.create(activity, path)

        val chips = createChips(
            path.endings,
            path.pitches,
            path.fixedSafesData,
            path.requiredSafesData
        )

        // Now, with all the data loaded, update the UI
        uiContext {
            val cardView = holder.cardView
            val titleTextView = holder.titleTextView
            val infoImageButton = holder.infoImageButton
            val heightTextView = holder.heightTextView
            val safesChipGroup = holder.safesChipGroup
            val warningCardView = holder.warningCardView

            visibility(holder.warningNameImageView, false)
            visibility(warningCardView, false)

            if (hasInfo)
                infoImageButton.setOnClickListener {
                    descriptionDialog?.show()
                        ?: Timber.e("Could not create dialog")
                }
            else visibility(infoImageButton, false)

            titleTextView.text = path.displayName
            heightTextView.text = heightFull
            visibility(holder.heightTextView, shouldShowHeight)

            holder.idTextView.text = path.sketchId.toString()

            holder.toggleImageButton.rotation =
                if (toggled) ROTATION_B else ROTATION_A
            holder.updateCardToggleStatus(
                toggled,
                hasInfo,
                blockStatus,
                pathSpannable to toggledPathSpannable,
                heightFull to heightOther
            )

            safesChipGroup.removeAllViews()
            for (chip in chips)
                safesChipGroup.addView(chip)

            holder.toggleImageButton.setOnClickListener { toggleImageButton ->
                // Switch the toggled status
                val newToggled = !this@PathsAdapter.toggled[position]
                this@PathsAdapter.toggled[position] = newToggled

                Timber.d("Toggling card. Now it's $newToggled")
                val newBlockStatus = blockStatuses[position]

                TransitionManager.beginDelayedTransition(
                    cardView, TransitionSet().addTransition(ChangeBounds())
                )

                holder.updateCardToggleStatus(
                    newToggled,
                    hasInfo,
                    newBlockStatus,
                    pathSpannable to toggledPathSpannable,
                    heightFull to heightOther
                )

                val fromRotation = if (toggled) ROTATION_A else ROTATION_B
                val toRotation = if (toggled) ROTATION_B else ROTATION_A
                toggleImageButton.startAnimation(
                    RotateAnimation(
                        fromRotation,
                        toRotation,
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
            }
            holder.markCompletedButton.setOnClickListener {
                activity.startActivity(
                    Intent(activity, MarkCompletedActivity::class.java).apply {
                        putExtra(EXTRA_AREA, activity.areaId)
                        putExtra(EXTRA_ZONE, activity.zoneId)
                        putExtra(EXTRA_SECTOR_INDEX, activity.currentPage)
                        putExtra(EXTRA_PATH, path.objectId)
                    }
                )
            }
        }

        Timber.v("Checking if blocked...")
        val blocked = path.isBlocked(firestore)
        Timber.d("Path ${path.objectId} block status: $blocked")
        blockStatuses[position] = blocked

        uiContext {
            Timber.d("Binding ViewHolder for path $position: ${path.displayName}. Blocked: $blocked")
            holder.updateBlockedStatus(blocked)
        }
    }

    internal enum class ChipType {
        SAFE, ENDING, ENDING_MULTIPLE, REQUIRED
    }

    internal data class ChipData(
        val chipType: ChipType,
        @DrawableRes val icon: Int? = null,
        val endings: List<EndingType>,
        val pitches: List<Pitch>,
        val fixedSafesData: FixedSafesData,
        val requiredSafesData: RequiredSafesData
    ) {
        /**
         * Creates a new chip with the provided parameters.
         * @author Arnau Mora
         * @since 20210427
         * @param string The text of the chip
         * @param count The count of the chip to add
         */
        fun createChip(
            activity: Activity,
            string: String?,
            count: Long?
        ): Chip {
            val chip = Chip(activity)
            val icon = icon
            val chipType = chipType
            val endings = endings
            val pitches = pitches
            val fixedSafesData = fixedSafesData
            val requiredSafesData = requiredSafesData

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
            return chip
        }
    }

    /**
     * Gets a [String] from the resources of [activity].
     * @author Arnau Mora
     * @since 20210406
     * @param stringRes The string resource to get
     * @return The string value from the resources with key [stringRes].
     */
    fun getString(@StringRes stringRes: Int) = activity.resources.getString(stringRes)

    /**
     * Creates all the [Chip]s that should be shown in the [ChipGroup].
     * @author Arnau Mora
     * @since 20210406
     */
    @UiThread
    private fun createChips(
        endings: List<EndingType>,
        pitches: List<Pitch>,
        fixedSafesData: FixedSafesData,
        requiredSafesData: RequiredSafesData
    ): List<Chip> = with(arrayListOf<Chip>()) {
        if (fixedSafesData.sum() > 0)
            add(
                if (!fixedSafesData.hasSafeCount())
                    ChipData(
                        ChipType.SAFE,
                        R.drawable.ic_icona_express,
                        endings,
                        pitches,
                        fixedSafesData,
                        requiredSafesData
                    ).createChip(
                        activity,
                        getString(R.string.safe_strings),
                        fixedSafesData.stringCount
                    )
                else
                    ChipData(
                        ChipType.SAFE,
                        R.drawable.ic_icona_express,
                        endings,
                        pitches,
                        fixedSafesData,
                        requiredSafesData
                    ).createChip(
                        activity,
                        getString(R.string.safe_strings_plural),
                        null,
                    )
            )

        if (endings.size == 1 && !endings[0].isUnknown()) {
            val ending = endings.first()
            val endingVal = ending.index

            add(
                ChipData(
                    ChipType.ENDING,
                    ending.getImage(),
                    endings,
                    pitches,
                    fixedSafesData,
                    requiredSafesData
                ).createChip(
                    activity,
                    activity.resources.getStringArray(R.array.path_endings)[endingVal],
                    null
                )
            )
        } else if (endings.size > 1)
            add(
                ChipData(
                    ChipType.ENDING_MULTIPLE,
                    null,
                    endings,
                    pitches,
                    fixedSafesData,
                    requiredSafesData
                ).createChip(
                    activity,
                    getString(R.string.path_ending_multiple),
                    null
                )
            )
        else
            add(
                ChipData(
                    ChipType.ENDING,
                    R.drawable.round_close_24,
                    endings,
                    pitches,
                    fixedSafesData,
                    requiredSafesData
                ).createChip(
                    activity,
                    getString(R.string.path_ending_none),
                    null
                )
            )

        this
    }
}
