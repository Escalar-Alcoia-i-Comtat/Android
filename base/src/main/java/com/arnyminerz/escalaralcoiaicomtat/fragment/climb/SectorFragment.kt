package com.arnyminerz.escalaralcoiaicomtat.fragment.climb

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.SectorActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.appendChip
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ARGUMENT_SECTOR_ID
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_PATH_DOCUMENT
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getDisplaySize
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.ImageLoadParameters
import com.arnyminerz.escalaralcoiaicomtat.core.view.show
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentSectorBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.PathsAdapter
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import timber.log.Timber

/**
 * A fragment that displays the contents of a Sector.
 * @author Arnau Mora
 * @since 20211006
 */
@ExperimentalBadgeUtils
class SectorFragment private constructor() : NetworkChangeListenerFragment() {
    companion object {
        /**
         * Creates a new [SectorFragment] instance with the specified arguments.
         * @author Arnau Mora
         * @since 20210820
         * @param sectorId The id of the Sector to display.
         */
        fun newInstance(sectorId: String): SectorFragment {
            return SectorFragment().apply {
                arguments = Bundle().apply {
                    putString(ARGUMENT_SECTOR_ID, sectorId)
                }
            }
        }
    }

    /**
     * The id of the sector that is being displayed.
     * @author Arnau Mora
     * @since 20211006
     */
    private lateinit var sectorId: String

    /**
     * The data of the sector that is being displayed.
     * @author Arnau Mora
     * @since 20211006
     */
    private lateinit var sector: Sector

    /**
     * Will be true when the contents are being loaded.
     * @author Arnau Mora
     * @since 20211006
     */
    private var loading = false

    /**
     * Will be true once the contents have been loaded.
     * @author Arnau Mora
     * @since 20211006
     */
    private var loaded = false

    /**
     * Will be true once the sector's image has been loaded.
     * @author Arnau Mora
     * @since 20211006
     */
    private var imageLoaded = false

    /**
     * Stores temporally if the sector is downloaded.
     * @author Arnau Mora
     * @since 20211006
     */
    private var isDownloaded = false

    /**
     * True if the sector's image is maximized. False otherwise.
     * @author Arnau Mora
     * @since 20211006
     */
    private var maximized = false

    /**
     * The height that the image should have when it's not maximized. It gets calculated from a
     * proportions with the size of the device.
     * @author Arnau Mora
     * @since 20211006
     */
    private var notMaximizedImageHeight = 0

    /**
     * The view binding of the layout of the fragment.
     * @author Arnau Mora
     * @since 20211006
     */
    internal var binding: FragmentSectorBinding? = null

    /**
     * A reference to the [FirebaseFirestore] instance.
     * @author Arnau Mora
     * @since 20211006
     */
    private lateinit var firestore: FirebaseFirestore

    /**
     * A reference of the [FirebaseStorage] instance.
     * @author Arnau Mora
     * @since 20211006
     */
    private lateinit var storage: FirebaseStorage

    /**
     * An automatic cast of [getActivity] to [SectorActivity].
     * Is null if not attached to an activity, or if parent Activity is null.
     * @author Arnau Mora
     * @since 20211006
     */
    private val sectorActivity: SectorActivity?
        get() = (activity as? SectorActivity?)

    /**
     * The intent request for marking a path as complete.
     * @author Arnau Mora
     * @since 20211006
     */
    val markAsCompleteRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data

            Timber.v("Marked path. Getting document.")
            val pathDocument = data?.getExtra(EXTRA_PATH_DOCUMENT)
            if (pathDocument != null) {
                Timber.v("The marked path's document is \"$pathDocument\".")
                firestore.document(pathDocument)
                    .get()
                    .addOnSuccessListener { pathData ->
                        Timber.v("Processing path data...")
                        val path = Path(pathData)
                        doAsync {
                            Timber.v("Getting adapter...")
                            val adapter = binding?.pathsRecyclerView?.adapter as PathsAdapter?
                            if (adapter != null) {
                                Timber.v("Getting view holder...")
                                val holder = adapter.viewHolders[path.objectId]
                                if (holder != null) {
                                    Timber.v("Loading completed path data...")
                                    adapter.loadCompletedPathData(
                                        Firebase.auth.currentUser,
                                        path,
                                        holder.commentsImageButton
                                    )
                                } else Timber.w("Could not find view holder")
                            } else Timber.w("Could not fetch adapter.")
                        }
                    }
                    .addOnFailureListener { e ->
                        Timber.e(e, "Could not get path data to refresh comments")
                    }
            } else Timber.w("Could not get the path's document.")
        }

    /**
     * Refreshes the UI to match the state of maximization.
     * @author Arnau Mora
     * @since 20211006
     */
    @UiThread
    private fun refreshMaximizeStatus() {
        binding?.sizeChangeFab?.setImageResource(
            if (maximized) R.drawable.round_flip_to_front_24
            else R.drawable.round_flip_to_back_24
        )

        sectorActivity?.userInputEnabled(!maximized)
    }

    /**
     * Minimizes the sector's image view and updates the UI.
     * @author Arnau Mora
     * @since 20211006
     */
    @UiThread
    fun minimize() {
        maximized = false
        if (binding != null)
            refreshMaximizeStatus()
    }

    /**
     * Load's the sector's image
     * @author Arnau Mora
     * @since 20210323
     */
    @UiThread
    private fun loadImage() {
        if (imageLoaded) return
        val iv = binding?.sectorImageView ?: return
        // TODO: Add error handler
        sector.loadImage(
            requireActivity(),
            storage,
            iv,
            binding?.sectorProgressBar,
            ImageLoadParameters().apply {
                withResultImageScale(1f)
                setShowPlaceholder(false)
            })
        Timber.v("Finished loading image")
        imageLoaded = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSectorBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sectorId = requireArguments().getString(ARGUMENT_SECTOR_ID)!!

        firestore = Firebase.firestore
        storage = Firebase.storage
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override suspend fun onStateChangeAsync(state: ConnectivityProvider.NetworkState) {
        if (isResumed)
            load()
    }

    /**
     * Loads the sector data.
     * @author Arnau Mora
     * @since 20210314
     */
    @WorkerThread
    suspend fun load() {
        var error = false
        val sectorActivity = this.sectorActivity
        if (!this::sectorId.isInitialized) {
            Timber.w("Could not load since class is not initialized")
            error = true
        }
        if (loaded && imageLoaded) {
            Timber.i("Will not load again.")
            error = true
        }
        if (loading) {
            Timber.i("Already loading.")
            error = true
        }
        if (sectorActivity == null) {
            Timber.w("Activity is null")
            error = true
        }
        if (error)
            return
        loading = true

        uiContext {
            binding?.sectorProgressBar?.visibility(true)
        }

        if (loaded && this::sector.isInitialized)
            uiContext {
                sectorActivity?.updateTitle(sector.displayName, isDownloaded)
                loadImage()
            }
        else {
            val app = requireActivity().application as App
            Timber.d("Loading sector S/$sectorId")
            sector = app.getSector(sectorId) ?: run {
                Timber.e("Could not get sector S/$sectorId")
                uiContext { toast(R.string.toast_error_not_found) }
                activity?.onBackPressed()
                return
            }

            uiContext {
                binding?.sectorTextView?.text = sector.displayName
            }

            isDownloaded = sector.downloadStatus(app, app.searchSession).downloaded

            if (activity != null && activity?.isDestroyed == false) {
                val size = activity?.let { getDisplaySize(it).second } ?: 0
                notMaximizedImageHeight = size / 2

                uiContext {
                    binding?.sectorImageViewLayout?.layoutParams?.height = notMaximizedImageHeight
                    binding?.sectorImageViewLayout?.requestLayout()
                }

                Timber.v("Loading paths...")
                val paths = sector.getChildren(app.searchSession).sortedBy { it.sketchId }
                Timber.v("Finished loading children sectors")

                uiContext {
                    Timber.v("Loading sector fragment")
                    loadImage()

                    Timber.v("Finished loading paths, performing UI updates")
                    (this as? SectorActivity?)?.updateTitle(sector.displayName, isDownloaded)

                    binding?.sizeChangeFab?.setOnClickListener {
                        maximized = !maximized

                        (binding?.sectorImageViewLayout?.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                            val tv = TypedValue()
                            requireContext().theme.resolveAttribute(
                                android.R.attr.actionBarSize,
                                tv,
                                true
                            )
                            val actionBarHeight = resources.getDimensionPixelSize(tv.resourceId)
                            setMargins(0, if (maximized) actionBarHeight else 0, 0, 0)
                            height =
                                if (maximized) LinearLayout.LayoutParams.MATCH_PARENT else notMaximizedImageHeight
                        }
                        binding?.sectorImageViewLayout?.requestLayout()

                        refreshMaximizeStatus()
                    }
                    binding?.dataScrollView?.show()
                    refreshMaximizeStatus()

                    // Load Paths
                    binding?.pathsRecyclerView?.layoutManager =
                        LinearLayoutManager(requireContext())
                    binding?.pathsRecyclerView?.layoutAnimation =
                        AnimationUtils.loadLayoutAnimation(
                            requireContext(),
                            R.anim.item_enter_left_animator
                        )
                    binding?.pathsRecyclerView?.adapter = PathsAdapter(paths, markAsCompleteRequest)
                    binding?.pathsRecyclerView?.show()

                    // Load info bar
                    binding?.sunChip?.let {
                        appendChip(requireContext(), sector.sunTime, it)
                    }
                    binding?.kidsAptChip?.let {
                        sector.kidsAptChip(requireContext(), it)
                    }
                    binding?.walkingTimeTextView?.let {
                        sector.walkingTimeView(requireContext(), it)
                    }

                    // Load chart
                    binding?.sectorBarChart?.let {
                        sector.loadChart(requireActivity(), it, paths)
                    }
                }
            } else
                Timber.e("Could not start loading sectors since context is null. Activity destroyed: ${activity?.isDestroyed}")
        }

        uiContext {
            binding?.sectorProgressBar?.visibility(false)
        }

        loaded = true
        loading = false
    }
}
