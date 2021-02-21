package com.arnyminerz.escalaralcoiaicomtat.fragment.climb

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Sector
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentSectorBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.generic.getDisplaySize
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.PathsAdapter
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.ImageLoadParameters
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestOptions
import timber.log.Timber


@ExperimentalUnsignedTypes
class SectorFragment(private val sector: Sector, private val viewPager: ViewPager2) :
    NetworkChangeListenerFragment() {
    private var maximized = false

    private var notMaximizedImageHeight = 0

    private var _binding: FragmentSectorBinding? = null
    private val binding get() = _binding!!

    private fun refreshMaximizeStatus() {
        binding.sizeChangeFab.setImageResource(if (maximized) R.drawable.round_flip_to_front_24 else R.drawable.round_flip_to_back_24)

        viewPager.isUserInputEnabled = !maximized
    }

    fun minimize() {
        maximized = false
        refreshMaximizeStatus()
    }

    private fun loadImage() {
        sector.asyncLoadImage(
            requireContext(),
            binding.sectorImageView,
            binding.sectorProgressBar,
            ImageLoadParameters().apply {
                withTransitionOptions(BitmapTransitionOptions.withCrossFade(50))
                withThumbnailSize(0.1f)
                withRequestOptions(
                    with(RequestOptions()) {
                        //centerCrop()
                        fitCenter()
                        format(DecodeFormat.PREFER_RGB_565)
                        this
                    }
                )
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSectorBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.sectorTextView.text = sector.displayName

        val size = getDisplaySize(requireActivity())
        notMaximizedImageHeight = size.second / 2
        binding.sectorImageViewLayout.layoutParams.height = notMaximizedImageHeight
        binding.sectorImageViewLayout.requestLayout()

        loadImage()

        if (context != null) {
            val context = requireContext()
            Timber.v("Loading paths...")

            // Load Paths
            binding.pathsRecyclerView.layoutManager = LinearLayoutManager(context)
            binding.pathsRecyclerView.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(context, R.anim.item_enter_left_animator)
            binding.pathsRecyclerView.adapter = PathsAdapter(sector.children, requireActivity())
            context.visibility(binding.pathsRecyclerView, true)

            // Load info bar
            sector.sunTime.appendChip(context, binding.sunChip)
            sector.kidsAptChip(context, binding.kidsAptChip)
            sector.walkingTimeView(context, binding.walkingTimeTextView)

            // Load chart
            sector.loadChart(context, binding.sectorBarChart)
        } else {
            Timber.e("Could not start loading sectors since context is null")
        }

        binding.sizeChangeFab.setOnClickListener {
            maximized = !maximized

            (binding.sectorImageViewLayout.layoutParams as ViewGroup.MarginLayoutParams).apply {
                val tv = TypedValue()
                requireContext().theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)
                val actionBarHeight = resources.getDimensionPixelSize(tv.resourceId)
                setMargins(0, if (maximized) actionBarHeight else 0, 0, 0)
                height =
                    if (maximized) LinearLayout.LayoutParams.MATCH_PARENT else notMaximizedImageHeight
            }
            binding.sectorImageViewLayout.requestLayout()

            refreshMaximizeStatus()
        }
        refreshMaximizeStatus()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)

        if (isResumed && view != null)
            loadImage()
    }
}