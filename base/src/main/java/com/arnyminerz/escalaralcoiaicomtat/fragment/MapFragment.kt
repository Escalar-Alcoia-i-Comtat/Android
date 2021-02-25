package com.arnyminerz.escalaralcoiaicomtat.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.AREAS
import com.arnyminerz.escalaralcoiaicomtat.activity.IntroActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.async.ResultListener
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.find
import com.arnyminerz.escalaralcoiaicomtat.data.map.GeoGeometry
import com.arnyminerz.escalaralcoiaicomtat.data.map.GeoMarker
import com.arnyminerz.escalaralcoiaicomtat.data.map.MapFeatures
import com.arnyminerz.escalaralcoiaicomtat.data.map.addToMap
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentMapBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_CENTER_MARKER_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.bounds
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.LatLng
import org.jetbrains.anko.toast
import timber.log.Timber

@ExperimentalUnsignedTypes
class MapFragment : NetworkChangeListenerFragment() {
    private lateinit var mapHelper: MapHelper

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val areas = arrayListOf<Area>()
    private var googleMap: GoogleMap? = null

    fun setAreas(areas: ArrayList<Area>) =
        this.areas.addAll(areas)

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()

        mapHelper = MapHelper()
            .withStartingPosition(LatLng(38.7216704, -0.4799751), 12.5f)

        if (IntroActivity.hasLocationPermission(requireContext()))
            googleMap?.isMyLocationEnabled = true
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()

        mapHelper.loadMap(
            childFragmentManager.findFragmentById(R.id.page_mapView)!!,
            { _, googleMap ->
                this@MapFragment.googleMap = googleMap

                if (context != null)
                    try {
                        if (IntroActivity.hasLocationPermission(requireContext()))
                            googleMap.isMyLocationEnabled = true
                    } catch (ex: IllegalStateException) {
                        Timber.w("Tried to check location permission without being attached to a context.")
                    }

                var counter = 0
                val max = 3
                val markers = arrayListOf<GeoMarker>()
                val polygons = arrayListOf<GeoGeometry>()
                val polylines = arrayListOf<GeoGeometry>()

                for (area in areas)
                    mapHelper
                        .loadKML(requireActivity(), area.kmlAddress, networkState)
                        .listen(object : ResultListener<MapFeatures> {
                            override fun onCompleted(result: MapFeatures) {
                                if (context == null || !isResumed) return

                                for (marker in result.markers) {
                                    marker.windowData?.message = ""
                                    markers.add(marker)
                                }
                                polygons.addAll(result.polygons)
                                polylines.addAll(result.polylines)

                                counter++
                                if (counter >= max) {
                                    markers.addToMap(googleMap)
                                    polygons.addToMap(googleMap)
                                    polylines.addToMap(googleMap)

                                    val positions = arrayListOf<LatLng>()

                                    for (marker in markers)
                                        positions.add(marker.position.toLatLng())
                                    for (marker in polygons)
                                        positions.addAll(marker.points)
                                    for (marker in polylines)
                                        positions.addAll(marker.points)

                                    if (positions.size > 1)
                                        googleMap.moveCamera(
                                            CameraUpdateFactory.newLatLngBounds(
                                                positions.bounds(),
                                                30
                                            )
                                        )
                                    else if (positions.size > 0)
                                        googleMap.moveCamera(
                                            CameraUpdateFactory.newLatLng(positions.first())
                                        )
                                }
                            }

                            override fun onFailure(error: Exception?) {
                                Timber.e(error, "Could not load KML")
                                requireContext().toast(R.string.toast_error_internal)
                            }
                        })

                googleMap.setOnMarkerClickListener { marker ->
                    if (SETTINGS_CENTER_MARKER_PREF.get(sharedPreferences))
                        googleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))

                    false
                }

                googleMap.setOnInfoWindowClickListener { marker ->
                    val dataClass = MapHelper.getTarget(marker)
                    val scan = dataClass?.let { AREAS.find(it) }
                    scan?.launchActivity(requireContext())
                        ?: Timber.w("Won't launch activity since dataClass is null")

                    Timber.e(
                        "Could not find any valid zone with name \"%s\".",
                        marker.title
                    )
                }
            },
            {
                Timber.e(it, "Could not load map:")
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)
        val hasInternet = state.hasInternet

        if (isResumed) {
            visibility(binding.pageMapView, hasInternet)
            visibility(binding.mapsNoInternetCardView.noInternetCardView, !hasInternet)
        }
    }
}