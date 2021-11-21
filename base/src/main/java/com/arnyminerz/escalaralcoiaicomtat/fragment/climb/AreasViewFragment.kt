package com.arnyminerz.escalaralcoiaicomtat.fragment.climb

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MapsActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.AreaActivity
import com.arnyminerz.escalaralcoiaicomtat.core.maps.NearbyZonesModule
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_AREA_TRANSITION_NAME
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.maps.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentViewAreasBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.paging.AreaAdapter
import com.arnyminerz.escalaralcoiaicomtat.view.model.AreasViewModel
import com.arnyminerz.escalaralcoiaicomtat.view.model.AreasViewModelFactory
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Shows the user all the areas in a list, as well as the nearby zones panel.
 * @author Arnau Mora
 * @since 20210617
 */
class AreasViewFragment : NetworkChangeListenerFragment() {
    /**
     * If the fragment has just been attached. This will tell whether or not to animate the items
     * in the recycler view.
     * @author Arnau Mora
     * @since 20210617
     */
    private var justAttached = false

    /**
     * The [FirebaseFirestore] instance reference for requesting data to the server.
     * @author Arnau Mora
     * @since 20210617
     */
    private lateinit var firestore: FirebaseFirestore

    /**
     * The [FirebaseAnalytics] instance reference for analyzing the user actions.
     * @author Arnau Mora
     * @since 20210826
     */
    private lateinit var analytics: FirebaseAnalytics

    /**
     * The nearby zones module for showing the user the zones that are nearby, if enabled.
     * @author Arnau Mora
     * @since 20210617
     */
    var nearbyZones: NearbyZonesModule? = null

    /**
     * The [MapHelper] instance for making map-related actions easily.
     * @author Arnau Mora
     * @since 20210617
     */
    val mapHelper: MapHelper?
        get() = nearbyZones?.mapHelper

    /**
     * The View Binding of the Fragment, for accessing the views.
     * @author Arnau Mora
     * @since 20210617
     */
    private var _binding: FragmentViewAreasBinding? = null

    /**
     * A non-null reference of the View Binding of the fragment.
     * @author Arnau Mora
     * @since 20210617
     * @see _binding
     */
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        justAttached = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewAreasBinding.inflate(inflater, container, false)

        firestore = Firebase.firestore
        analytics = Firebase.analytics

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as App
        val viewModel by viewModels<AreasViewModel> { AreasViewModelFactory(app) }

        Timber.v("Initializing Nearby Zones...")
        nearbyZones = NearbyZonesModule(this, MapsActivity::class.java, binding.nearbyZonesCard)

        Timber.v("Initializing MapHelper")
        nearbyZones?.onCreate(savedInstanceState)
        nearbyZones?.updateNearbyZones()

        Timber.v("Initializing search engine...")
        val searchManager =
            requireContext().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        binding.searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
        binding.searchView.setIconifiedByDefault(false)

        doAsync {
            Timber.v("Refreshing areas...")
            uiContext {
                Timber.d("Initializing area adapter for AreasViewFragment...")
                binding.areasRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                if (justAttached)
                    binding.areasRecyclerView.layoutAnimation =
                        AnimationUtils.loadLayoutAnimation(
                            requireContext(),
                            R.anim.item_fall_animator
                        )
                val adapter = AreaAdapter { holder, position, area ->
                    Timber.v("Clicked item %s", position)
                    analytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
                        param(FirebaseAnalytics.Param.ITEM_ID, area.objectId)
                        param(FirebaseAnalytics.Param.ITEM_LIST_ID, area.documentPath)
                        param(FirebaseAnalytics.Param.ITEM_CATEGORY, area.namespace)
                        param(FirebaseAnalytics.Param.CONTENT_TYPE, area.namespace)
                        param(FirebaseAnalytics.Param.ITEM_NAME, area.displayName)
                    }

                    val transition = ViewCompat.getTransitionName(holder.titleTextView)
                    val optionsBundle = transition?.let { transitionName ->
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                            requireActivity(),
                            holder.titleTextView,
                            transitionName
                        ).toBundle()
                    } ?: Bundle()

                    requireActivity().launch(AreaActivity::class.java, optionsBundle) {
                        putExtra(EXTRA_AREA, area.objectId)
                        if (transition != null)
                            putExtra(EXTRA_AREA_TRANSITION_NAME, transition)
                    }
                }
                binding.areasRecyclerView.adapter = adapter

                viewLifecycleOwner.lifecycleScope.launch {
                    Timber.v("Collecting flow from ViewModel...")
                    viewModel.flow.collectLatest { pagingData ->
                        Timber.v("Submitting paging data...")
                        adapter.submitData(pagingData)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        nearbyZones?.onStart()
    }

    override fun onResume() {
        super.onResume()

        nearbyZones?.updateNearbyZones()
        justAttached = false
        nearbyZones?.onResume()
    }

    override fun onPause() {
        super.onPause()
        nearbyZones?.onPause()
    }

    override fun onStop() {
        super.onStop()
        nearbyZones?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        nearbyZones?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        nearbyZones?.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        if (!isResumed) return

        visibility(binding.areasNoInternetCardView.noInternetCardView, !state.hasInternet)
    }
}
