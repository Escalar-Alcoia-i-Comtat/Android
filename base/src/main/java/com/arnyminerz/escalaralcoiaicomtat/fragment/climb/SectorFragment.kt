package com.arnyminerz.escalaralcoiaicomtat.fragment.climb

import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.SectorActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentSectorBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.generic.getDisplaySize
import com.arnyminerz.escalaralcoiaicomtat.generic.runOnUiThread
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.PathsAdapter
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.shared.ARGUMENT_AREA_ID
import com.arnyminerz.escalaralcoiaicomtat.shared.ARGUMENT_SECTOR_INDEX
import com.arnyminerz.escalaralcoiaicomtat.shared.ARGUMENT_ZONE_ID
import com.arnyminerz.escalaralcoiaicomtat.shared.CROSSFADE_DURATION
import com.arnyminerz.escalaralcoiaicomtat.shared.SECTOR_THUMBNAIL_SIZE
import com.arnyminerz.escalaralcoiaicomtat.view.ImageLoadParameters
import com.arnyminerz.escalaralcoiaicomtat.view.show
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestOptions
import timber.log.Timber
import java.util.concurrent.CompletableFuture.runAsync

class SectorFragment : NetworkChangeListenerFragment() {
    private lateinit var areaId: String
    private lateinit var zoneId: String
    private var sectorIndex: Int = -1
    private lateinit var sector: Sector

    private var loaded = false
    private var isDownloaded = false
    private var maximized = false
    private var notMaximizedImageHeight = 0

    private var _binding: FragmentSectorBinding? = null
    private val binding get() = _binding!!

    private val sectorActivity: SectorActivity?
        get() = (activity as? SectorActivity?)

    @UiThread
    private fun refreshMaximizeStatus() {
        binding.sizeChangeFab.setImageResource(
            if (maximized) R.drawable.round_flip_to_front_24
            else R.drawable.round_flip_to_back_24
        )

        sectorActivity?.userInputEnabled(!maximized)
    }

    @UiThread
    fun minimize() {
        maximized = false
        if (_binding != null)
            refreshMaximizeStatus()
    }

    /**
     * Load's the sector's image
     * @author Arnau Mora
     * @since 20210323
     */
    @UiThread
    private fun loadImage() {
        sector.asyncLoadImage(
            requireContext(),
            binding.sectorImageView,
            binding.sectorProgressBar,
            ImageLoadParameters<Bitmap>().apply {
                withTransitionOptions(BitmapTransitionOptions.withCrossFade(CROSSFADE_DURATION))
                withThumbnailSize(SECTOR_THUMBNAIL_SIZE)
                withResultImageScale(1f)
                withRequestOptions(
                    RequestOptions().apply {
                        centerInside()
                        format(DecodeFormat.PREFER_RGB_565)
                    }
                )
                setShowPlaceholder(false)
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        areaId = requireArguments().getString(ARGUMENT_AREA_ID)!!
        zoneId = requireArguments().getString(ARGUMENT_ZONE_ID)!!
        sectorIndex = requireArguments().getInt(ARGUMENT_SECTOR_INDEX, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        if (isResumed)
            runAsync {
                load()
            }
    }

    /**
     * Loads the sector data.
     * @author Arnau Mora
     * @since 20210314
     */
    @WorkerThread
    fun load() {
        if (!this::zoneId.isInitialized)
            return Timber.w("Could not load since class is not initialized")

        if (loaded && this::sector.isInitialized) {
            runOnUiThread {
                (this as? SectorActivity?)?.updateTitle(sector.displayName, isDownloaded)
                loadImage()
            }
            return
        }

        Timber.d("Loading sector #$sectorIndex of $areaId/$zoneId")
        runOnUiThread {
            (this as? SectorActivity?)?.setLoading(true)
        }
        sector = AREAS[areaId]!![zoneId][sectorIndex]

        isDownloaded = sector.downloadStatus(requireContext()).isDownloaded()
        binding.sectorTextView.text = sector.displayName

        val size = getDisplaySize(requireActivity())
        notMaximizedImageHeight = size.second / 2
        binding.sectorImageViewLayout.layoutParams.height = notMaximizedImageHeight
        binding.sectorImageViewLayout.requestLayout()

        if (activity != null) {
            Timber.v("Loading paths...")
            val children = sector.getChildren(sectorActivity!!.firestore)
            Timber.v("Finished loading children sectors")

            runOnUiThread {
                Timber.v("Finished loading paths, performing UI updates")
                (this as? SectorActivity?)?.updateTitle(sector.displayName, isDownloaded)
                loadImage()

                binding.sizeChangeFab.setOnClickListener {
                    maximized = !maximized

                    (binding.sectorImageViewLayout.layoutParams as ViewGroup.MarginLayoutParams).apply {
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
                    binding.sectorImageViewLayout.requestLayout()

                    refreshMaximizeStatus()
                }
                binding.dataScrollView.show()
                refreshMaximizeStatus()

                // Load Paths
                binding.pathsRecyclerView.layoutManager = LinearLayoutManager(this)
                binding.pathsRecyclerView.layoutAnimation =
                    AnimationUtils.loadLayoutAnimation(
                        this,
                        R.anim.item_enter_left_animator
                    )
                binding.pathsRecyclerView.adapter = PathsAdapter(children, requireActivity())
                binding.pathsRecyclerView.show()

                // Load info bar
                sector.sunTime.appendChip(this, binding.sunChip)
                sector.kidsAptChip(this, binding.kidsAptChip)
                sector.walkingTimeView(this, binding.walkingTimeTextView)

                // Load chart
                sector.loadChart(this, binding.sectorBarChart, children)

                (this as? SectorActivity?)?.setLoading(false)
            }
        } else
            Timber.e("Could not start loading sectors since context is null")

        loaded = true
    }
}
