package com.arnyminerz.escalaralcoiaicomtat.fragment.climb

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.MapsActivity
import com.arnyminerz.escalaralcoiaicomtat.core.maps.NearbyZonesModule
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PREF_DISABLE_NEARBY
import com.arnyminerz.escalaralcoiaicomtat.core.utils.maps.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentViewAreasBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.AreaAdapter
import com.arnyminerz.escalaralcoiaicomtat.list.holder.AreaViewHolder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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
     * What will get called when an area is clicked.
     * @author Arnau Mora
     * @since 20210617
     */
    private var areaClickListener: ((viewHolder: AreaViewHolder, position: Int) -> Unit)? = null

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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (PREF_DISABLE_NEARBY.get())
            Timber.i("Nearby Zones is disabled, won't load")
        else {
            Timber.v("Initializing Nearby Zones...")
            nearbyZones = NearbyZonesModule(this, MapsActivity::class.java, binding.nearbyZonesCard)

            Timber.v("Initializing MapHelper")
            nearbyZones?.onCreate(savedInstanceState)

            nearbyZones?.initializeMap()
        }

        Timber.v("Initializing search engine...")
        val searchManager =
            requireContext().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        binding.searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
        binding.searchView.setIconifiedByDefault(false)

        Timber.v("Refreshing areas...")
        Timber.d("Initializing area adapter for AreasViewFragment...")
        binding.areasRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        if (justAttached)
            binding.areasRecyclerView.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(
                    requireContext(),
                    R.anim.item_fall_animator
                )
        binding.areasRecyclerView.adapter =
            AreaAdapter(requireActivity() as MainActivity, areaClickListener)
    }

    override fun onStart() {
        super.onStart()
        nearbyZones?.onStart()
    }

    override fun onResume() {
        super.onResume()

        nearbyZones?.initializeMap()
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

    /**
     * Update the area click listener to the desired one.
     * @author Arnau Mora
     * @since 20210617
     * @param areaClickListener This will get called whenever an area is selected by the user.
     */
    fun setItemClickListener(areaClickListener: ((viewHolder: AreaViewHolder, position: Int) -> Unit)?) {
        this.areaClickListener = areaClickListener
    }
}
