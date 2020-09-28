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
import com.arnyminerz.escalaralcoiaicomtat.activity.ANALYTICS_EVENT_KEY_SECTORS_NO_CONTEXT
import com.arnyminerz.escalaralcoiaicomtat.activity.ANALYTICS_EVENT_NAME_ERRORS
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity.Companion.analytics
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Sector
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.generic.getDisplaySize
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.PathsAdapter
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.ImageLoadParameters
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.analytics.ktx.logEvent
import kotlinx.android.synthetic.main.fragment_sector.view.*
import timber.log.Timber


@ExperimentalUnsignedTypes
class SectorFragment(private val sector: Sector, private val viewPager: ViewPager2) :
    NetworkChangeListenerFragment() {
    private var maximized = false

    private var notMaximizedImageHeight = 0

    private fun refreshMaximizeStatus(vw: View? = view) {
        vw?.size_change_fab?.setImageResource(if (maximized) R.drawable.round_flip_to_front_24 else R.drawable.round_flip_to_back_24)

        viewPager.isUserInputEnabled = !maximized
    }

    fun minimize() {
        maximized = false
        refreshMaximizeStatus()
    }

    fun loadImage(view: View) {
        sector.asyncLoadImage(
            requireContext(),
            view.sector_imageView,
            view.sector_progressBar,
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
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sector, container, false)

        view.sector_textView.text = sector.displayName

        val size = getDisplaySize(requireActivity())
        notMaximizedImageHeight = size.second / 2
        view.sector_imageView_layout.layoutParams.height = notMaximizedImageHeight
        view.sector_imageView_layout.requestLayout()

        loadImage(view)

        if (context != null) {
            val context = requireContext()
            Timber.v("Loading paths...")

            // Load Paths
            view.paths_recyclerView.layoutManager = LinearLayoutManager(context)
            view.paths_recyclerView.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(context, R.anim.item_enter_left_animator)
            view.paths_recyclerView.adapter = PathsAdapter(sector.children, requireActivity())
            context.visibility(view.paths_recyclerView, true)

            // Load info bar
            sector.sunTime.appendChip(context, view.sun_chip)
            sector.kidsAptChip(context, view.kidsApt_chip)
            sector.walkingTimeView(context, view.walkingTime_textView)

            // Load chart
            sector.loadChart(context, view.sector_bar_chart)
        } else {
            Timber.e("Could not start loading sectors since context is null")
            analytics.logEvent(ANALYTICS_EVENT_NAME_ERRORS) {
                param(ANALYTICS_EVENT_KEY_SECTORS_NO_CONTEXT, "true")
            }
        }

        view.size_change_fab.setOnClickListener {
            maximized = !maximized

            (view.sector_imageView_layout.layoutParams as ViewGroup.MarginLayoutParams).apply {
                val tv = TypedValue()
                requireContext().theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)
                val actionBarHeight = resources.getDimensionPixelSize(tv.resourceId)
                setMargins(0, if (maximized) actionBarHeight else 0, 0, 0)
                height =
                    if (maximized) LinearLayout.LayoutParams.MATCH_PARENT else notMaximizedImageHeight
            }
            view.sector_imageView_layout.requestLayout()

            refreshMaximizeStatus(view)
        }
        refreshMaximizeStatus(view)

        return view
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)

        if (isResumed && view != null)
            loadImage(requireView())
    }
}