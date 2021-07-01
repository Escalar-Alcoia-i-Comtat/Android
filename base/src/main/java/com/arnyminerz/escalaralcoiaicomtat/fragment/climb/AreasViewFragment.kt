package com.arnyminerz.escalaralcoiaicomtat.fragment.climb

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
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentViewAreasBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.AreaAdapter
import com.arnyminerz.escalaralcoiaicomtat.list.holder.AreaViewHolder
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
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
    private var justAttached = false

    private lateinit var firestore: FirebaseFirestore

    private lateinit var nearbyZones: NearbyZonesModule

    private var areaClickListener: ((viewHolder: AreaViewHolder, position: Int) -> Unit)? = null

    private var _binding: FragmentViewAreasBinding? = null
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

        nearbyZones = NearbyZonesModule(this, MapsActivity::class.java, binding.nearbyZonesCard)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.v("Initializing MapHelper")
        nearbyZones.onCreate(savedInstanceState)

        nearbyZones.initializeMap()

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
        nearbyZones.onStart()
    }

    override fun onResume() {
        super.onResume()

        nearbyZones.initializeMap()
        justAttached = false
        nearbyZones.onResume()
    }

    override fun onPause() {
        super.onPause()
        nearbyZones.onPause()
    }

    override fun onStop() {
        super.onStop()
        nearbyZones.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        nearbyZones.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        nearbyZones.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        if (!isResumed) return

        visibility(binding.areasNoInternetCardView.noInternetCardView, !state.hasInternet)
    }

    fun setItemClickListener(areaClickListener: ((viewHolder: AreaViewHolder, position: Int) -> Unit)?) {
        this.areaClickListener = areaClickListener
    }
}
