package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.collection.arrayMapOf
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.profile.CommentsActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.profile.MarkCompletedActivity
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.EndingType
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Pitch
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.storage.MarkedDataInt
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.getSpannable
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.image
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.index
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.isUnknown
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.FixedSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.RequiredSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ENABLE_AUTHENTICATION
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_PATH
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_PATH_DOCUMENT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.INFO_VIBRATION
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.utils.LinePattern
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toStringLineJumping
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.databinding.ListItemPathBinding
import com.arnyminerz.escalaralcoiaicomtat.device.vibrate
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.ArtifoPathEndingDialog
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.DescriptionDialog
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.PathEquipmentDialog
import com.arnyminerz.escalaralcoiaicomtat.list.holder.SectorViewHolder
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
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
 * @param markAsCompleteRequestHandler The request handler when it's asked to mark a path as complete
 * @see SectorViewHolder
 */
@ExperimentalBadgeUtils
class PathsAdapter(
    private val paths: List<Path>,
    private val markAsCompleteRequestHandler: ActivityResultLauncher<Intent>
) : RecyclerView.Adapter<SectorViewHolder>() {
    /**
     * The current application context.
     * Gets initialized in [onCreateViewHolder].
     * @author Arnau Mora
     * @since 20210824
     */
    private lateinit var context: Context

    /**
     * The current [Activity] instance.
     * Gets initialized in [onCreateViewHolder].
     * @author Arnau Mora
     * @since 20210824
     */
    private lateinit var activity: Activity

    /**
     * The current [App] instance.
     * Gets initialized in [onCreateViewHolder].
     * @author Arnau Mora
     * @since 20210824
     */
    private lateinit var app: App

    /**
     * Specifies the toggled status of all the paths.
     * @author Arnau Mora
     * @since 20210427
     */
    private val toggled = arrayListOf<Boolean>()

    /**
     * Stores the badges loaded for each path. The key stores the path id, and the value the badge set.
     * @author Arnau Mora
     * @since 20210430
     */
    private val badges = arrayMapOf<String, BadgeDrawable>()

    /**
     * Stores the view holders of all the elements once they get created.
     * @author Arnau Mora
     * @since 20210430
     */
    internal val viewHolders = arrayMapOf<String, SectorViewHolder>()

    /**
     * Stores the Firestore instance.
     * @author Arnau Mora
     * @since 20210427
     */
    private val firestore = Firebase.firestore

    /**
     * Stores the Firebase Auth instance.
     * @author Arnau Mora
     * @since 20210430
     */
    private val auth = Firebase.auth

    /**
     * Stores the currently logged in user
     * @author Arnau Mora
     * @since 20210430
     */
    private val user = auth.currentUser

    /**
     * The listeners for when new completions are added.
     * @author Arnau Mora
     * @since 20210719
     */
    private var listenerRegistrations = arrayListOf<ListenerRegistration>()

    init {
        // Initialize [toggled] and [blockStatuses] with the default values.
        val pathsSize = paths.size
        Timber.d("Created with %d paths", pathsSize)
        (0 until pathsSize).forEach { _ ->
            toggled.add(false)
        }
    }

    override fun getItemCount(): Int = paths.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectorViewHolder {
        context = parent.context
        activity = context as Activity
        app = activity.app
        val inflater = LayoutInflater.from(context)
        val view = ListItemPathBinding.inflate(inflater, parent, false)
        return SectorViewHolder(activity, view.root)
    }

    override fun onBindViewHolder(holder: SectorViewHolder, position: Int) {
        if (position >= paths.size) {
            Timber.e("Position $position is out of the paths' bounds: ${paths.size}. Hiding the card.")
            visibility(holder.cardView, false)
            return
        }
        val path = paths[position]
        viewHolders[path.objectId] = holder

        val auth = Firebase.auth
        val user = auth.currentUser
        val loggedIn = user != null
        Timber.v("Updating Mark completed button visibility: $loggedIn")
        holder.markCompletedButton.visibility(loggedIn && ENABLE_AUTHENTICATION)

        Timber.d("Loading path data")
        doAsync {
            loadPathData(path, position, holder)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        Timber.v("Removing all listeners...")
        for (listenerRegistration in listenerRegistrations)
            listenerRegistration.remove()
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
        val hasInfo = path.hasInfo()
        val pathSpannable = path.grade().getSpannable(activity)
        val gradesCount = path.grades.size
        val toggledPathSpannable =
            if (gradesCount > 1)
                path.grades.subList(1, gradesCount) // Remove first line
                    .getSpannable(activity)
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

        val chips: List<Chip> = try {
            createChips(
                path.endings,
                path.pitches,
                path.fixedSafesData,
                path.requiredSafesData
            )
        } catch (e: ArrayIndexOutOfBoundsException) {
            Timber.e(e, "Could not create chips for Path $path.")
            emptyList()
        }

        // Now, with all the data loaded, update the UI
        uiContext {
            val cardView = holder.cardView
            val titleTextView = holder.titleTextView
            val builtByTextView = holder.builtByTextView
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

            val rebuilders = path.rebuilders
            val builtBy = path.builtBy
            val hasBuiltBy = builtBy?.ifBlank { null } != null
            val hasRebuilder = rebuilders.isNotEmpty()
            builtByTextView.text = when {
                hasBuiltBy -> builtBy
                hasRebuilder -> rebuilders[0]
                else -> ""
            }
            builtByTextView.visibility(hasBuiltBy || hasRebuilder)
            Timber.v("$path > Rebuilt by: ${path.rebuiltBy}. Rebuilders: $rebuilders")
            Timber.v("$path > Has built by: $hasBuiltBy. Has rebuilder: $hasRebuilder")

            titleTextView.text = path.displayName
            heightTextView.text = heightFull
            visibility(holder.heightTextView, shouldShowHeight)

            holder.idTextView.text = path.sketchId.toString()

            holder.toggleImageButton.rotation =
                if (toggled) ROTATION_B else ROTATION_A
            holder.updateCardToggleStatus(
                toggled,
                hasInfo,
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

                TransitionManager.beginDelayedTransition(
                    cardView, TransitionSet().addTransition(ChangeBounds())
                )

                holder.updateCardToggleStatus(
                    newToggled,
                    hasInfo,
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
                markAsCompleteRequestHandler.launch(
                    Intent(activity, MarkCompletedActivity::class.java).apply {
                        putExtra(EXTRA_PATH, path.objectId)
                    }
                )
            }
        }

        Timber.v("${path.objectId} > Checking if blocked...")
        val blocked = path.getBlockStatus(app.searchSession, firestore)
        Timber.d("${path.objectId} > Block status: $blocked")

        uiContext {
            Timber.d("Binding ViewHolder for path $position: ${path.displayName}. Blocked: $blocked")
            holder.updateBlockedStatus(blocked)
        }

        loadCompletedPathData(user, path, holder.commentsImageButton)
    }

    /**
     * Loads the completed path data, which contains the user's completions, and all the comments
     * people have posted in the path.
     * @author Arnau Mora
     * @since 20210430
     * @param user The currently logged in user
     */
    suspend fun loadCompletedPathData(
        user: FirebaseUser?,
        path: Path,
        commentsImageButton: ImageButton
    ) {
        if (!ENABLE_AUTHENTICATION)
            return uiContext {
                Timber.i("Won't load completions since ENABLE_AUTHENTICATION is false.")
                visibility(commentsImageButton, false)
            }

        val completions = arrayListOf<MarkedDataInt>()

        Timber.v("${path.objectId} > Adding completions listener...")
        val registration = path.observeCompletions(firestore, activity) {
            completions.add(it)
            processCompletions(path, user, commentsImageButton, completions)
        }
        listenerRegistrations.add(registration)
    }

    /**
     * Once completions get loaded, this processes them, and shows them to the user.
     * @author Arnau Mora
     * @since 20210719
     * @param path The [Path] that contains the completions.
     * @param user The currently logged in user, or null if not logged in.
     * @param commentsImageButton The [ImageButton] that shows the user the amount of completions,
     * and offers the possibility to view them.
     * @param completions The loaded completions to get processed.
     */
    @UiThread
    fun processCompletions(
        path: Path,
        user: FirebaseUser?,
        commentsImageButton: ImageButton,
        completions: ArrayList<MarkedDataInt>
    ) {
        Timber.v("${path.objectId} > Got completions. ${completions.size} elements...")
        val comments = arrayListOf<String>()
        val notes = arrayListOf<String>()
        for (completion in completions) {
            if (completion.comment != null)
                comments.add(completion.comment!!)
            if (completion.notes != null)
                if (user != null)
                    if (completion.user.uid == user.uid)
                        notes.add(completion.notes!!)
                    else Timber.v("${path.objectId} > Got note which the user (${user.uid}) is not the author of.")
                else Timber.v("${path.objectId} > User not logged in")
            /*if (completion is MarkedCompletedData) {

            } else if (completion is MarkedProjectData) {

            }*/
        }

        Timber.v("${path.objectId} > Got ${comments.size} comments and ${notes.size} notes.")
        val containsKey = synchronized(badges) { badges.containsKey(path.objectId) }
        if (containsKey) {
            Timber.v("${path.objectId} > ettaching old badge...")
            val badgeObject = synchronized(badges) { badges[path.objectId] }
            BadgeUtils.detachBadgeDrawable(badgeObject, commentsImageButton)
        }
        Timber.v("${path.objectId} > Creating comments badge...")
        val badge = BadgeDrawable.create(activity)

        val commentsCount = comments.size
        val notesCount = notes.size
        badge.number = commentsCount + notesCount

        badge.isVisible = true
        Timber.v("${path.objectId} > Storing badge...")
        synchronized(badges) {
            badges[path.objectId] = badge
        }
        Timber.v("${path.objectId} > Attaching badge...")
        BadgeUtils.attachBadgeDrawable(badge, commentsImageButton)

        commentsImageButton.setOnClickListener {
            if (comments.isNotEmpty() || notes.isNotEmpty())
                activity.launch(CommentsActivity::class.java) {
                    putExtra(EXTRA_PATH_DOCUMENT, path.documentPath)
                }
            else
                vibrate(activity, INFO_VIBRATION)
        }
    }

    internal enum class ChipType {
        SAFE, ENDING, ENDING_MULTIPLE, REQUIRED
    }

    internal data class ChipData(
        val chipType: ChipType,
        @DrawableRes val icon: Int? = null,
        val endings: List<@EndingType String>,
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
    private fun createChips(
        endings: List<@EndingType String>,
        pitches: List<Pitch>,
        fixedSafesData: FixedSafesData,
        requiredSafesData: RequiredSafesData
    ): List<Chip> = with(arrayListOf<Chip>()) {
        if (fixedSafesData.sum() > 0)
            add(
                if (!fixedSafesData.hasSafeCount() || fixedSafesData.stringCount > 0)
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

        if (endings.size == 1 && !endings[0].isUnknown) {
            val ending = endings.first()
            Timber.v("Adding chip \"$ending\"")
            val endingVal = ending.index

            add(
                ChipData(
                    ChipType.ENDING,
                    ending.image,
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
