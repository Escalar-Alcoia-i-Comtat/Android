package com.arnyminerz.escalaralcoiaicomtat.generic

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.AREAS
import com.arnyminerz.escalaralcoiaicomtat.activity.MapsActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.MapsActivity.Companion.KML_ADDRESS_BUNDLE_EXTRA
import com.arnyminerz.escalaralcoiaicomtat.async.LoadResult
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.find
import com.arnyminerz.escalaralcoiaicomtat.data.map.KMLLoader
import com.arnyminerz.escalaralcoiaicomtat.data.map.MapFeatures
import com.arnyminerz.escalaralcoiaicomtat.databinding.DialogMapMarkerBinding
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toUri
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.bumptech.glide.Glide
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.SupportMapFragment
import com.google.android.libraries.maps.model.CameraPosition
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.Marker
import timber.log.Timber

class MapHelper {
    companion object {
        @ExperimentalUnsignedTypes
        fun getTarget(marker: Marker): DataClass<*, *>? {
            Timber.v("Searching in ${AREAS.size} cached areas...")
            for (area in AREAS)
                if (area.displayName.equals(marker.title, true))
                    return area
                else if (area.isNotEmpty())
                    for (zone in area)
                        if (zone.displayName.equals(marker.title, true))
                            return zone
                        else if (zone.isNotEmpty())
                            for (sector in zone)
                                if (sector.displayName.equals(marker.title, true))
                                    return sector

            Timber.w("Could not find targeted data class")
            return null
        }

        private fun getImageUrl(description: String?): String? {
            if (description == null || description.isEmpty()) return null

            if (description.startsWith("<img")) {
                val urlFirstPart =
                    description.substring(description.indexOf('"') + 1) // This takes from the first "
                return urlFirstPart.substring(urlFirstPart.indexOf('"') - 1) // This from the previous to the next
            }

            return null
        }

        @ExperimentalUnsignedTypes
        fun infoCard(context: Context, marker: Marker, binding: DialogMapMarkerBinding): MarkerWindow {
            val latLng = marker.position

            val anim =
                AnimationUtils.loadAnimation(context, R.anim.enter_bottom)
            anim.duration = 500
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    binding.mapInfoCardView.visibility = View.VISIBLE
                }

                override fun onAnimationStart(animation: Animation?) {
                    binding.mapInfoCardView.visibility = View.VISIBLE
                }
            })
            binding.mapInfoCardView.startAnimation(anim)

            val title = marker.title
            val description = marker.snippet
            val iwdc = getTarget(marker) // Info Window Data Class
            val dcSearch = iwdc?.let { AREAS.find(it) }

            binding.mapInfoTextView.text = title

            val imageUrl = getImageUrl(description)
            if (imageUrl == null)
                binding.mapDescTextView.text = description
            else
                Glide.with(context)
                    .load(imageUrl)
                    .into(binding.mapInfoImageView)

            visibility(binding.fabEnter, iwdc != null && dcSearch?.isEmpty() == false)
            visibility(binding.mapInfoImageView, imageUrl != null)
            visibility(binding.mapDescTextView, imageUrl == null)

            val gmmIntentUri = latLng!!.toUri(true, title)
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                .setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                binding.fabMaps.show()
                binding.fabMaps.setOnClickListener {
                    context.startActivity(mapIntent)
                }
            } else binding.fabMaps.hide()

            if (iwdc != null && dcSearch?.isEmpty() == false)
                binding.fabEnter.setOnClickListener {
                    Timber.v("Searching for info window ${iwdc.namespace}:${iwdc.id}")
                    if (!dcSearch.launchActivity(context))
                        context.toast(R.string.toast_error_internal)
                }

            return MarkerWindow(context, marker, binding)
        }
    }

    private var googleMap: GoogleMap? = null
    private var supportMapFragment: SupportMapFragment? = null

    private var loadedKMLAddress: String? = null

    private var startingPosition: LatLng = LatLng(-52.6885, -70.1395)
    private var startingZoom: Float = 2f

    fun withStartingPosition(startingPosition: LatLng?, zoom: Float = 2f): MapHelper {
        if (startingPosition.isNotNull())
            this.startingPosition = startingPosition!!
        this.startingZoom = zoom
        return this
    }

    fun loadMap(
        fragment: Fragment,
        callback: (supportMapFragment: SupportMapFragment, googleMap: GoogleMap) -> Unit,
        onError: ((exception: Exception) -> Unit)?
    ): MapHelper {
        val smf = fragment as? SupportMapFragment

        smf?.getMapAsync { googleMap ->
            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            this.googleMap = googleMap
            this.supportMapFragment = smf

            googleMap.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(startingPosition, startingZoom)
                )
            )

            callback(smf, googleMap)
        } ?: onError?.invoke(NullPointerException("Could not find support map fragment"))

        return this
    }

    @ExperimentalUnsignedTypes
    fun loadKML(
        activity: FragmentActivity,
        kmlAddress: String?,
        networkState: ConnectivityProvider.NetworkState,
        addToMap: Boolean = true
    ): LoadResult<MapFeatures> {
        val listener = LoadResult<MapFeatures>(activity)

        runAsync {
            if (googleMap.isNull() || supportMapFragment.isNull())
                return@runAsync listener.onFailure(MapNotInitializedException("Map not initialized. Please run loadMap before this"))

            val loader = KMLLoader(kmlAddress, null)
            loader.load(activity, googleMap!!, networkState, { result ->
                Timber.v("Loaded kml. Loading map features")

                if (addToMap) {
                    for (marker in result.markers)
                        marker.addToMap(googleMap!!)
                    for (polygon in result.polygons)
                        polygon.addToMap(googleMap!!)
                    for (polyline in result.polylines)
                        polyline.addToMap(googleMap!!)
                }

                loadedKMLAddress = kmlAddress

                val mapFeatures = MapFeatures(result.markers, result.polylines, result.polygons)
                listener.onCompleted(mapFeatures)
            }, { error ->
                listener.onFailure(error)
            })
        }

        return listener
    }

    @ExperimentalUnsignedTypes
    fun showMapsActivity(activity: FragmentActivity) {
        if (loadedKMLAddress.isNull()) throw MapAnyDataToLoadException("Map doesn't have any loaded data. You may run loadKML, for example.")

        Timber.v("Launching MapsActivity from KML \"$loadedKMLAddress\"")
        activity.startActivity(
            Intent(activity, MapsActivity::class.java)
                .putExtra(
                    KML_ADDRESS_BUNDLE_EXTRA,
                    loadedKMLAddress!!
                )
        )
    }
}

class MapNotInitializedException(message: String) : Exception(message)
class MapAnyDataToLoadException(message: String) : Exception(message)
data class MarkerWindow(val context: Context, val marker: Marker, val binding: DialogMapMarkerBinding)

fun MarkerWindow.hide(){
    val anim = AnimationUtils.loadAnimation(context, R.anim.exit_bottom)
    anim.duration = 500
    anim.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation?) {}

        override fun onAnimationEnd(animation: Animation?) {
            binding.mapInfoCardView.visibility = View.GONE
        }

        override fun onAnimationStart(animation: Animation?) {
            binding.mapInfoCardView.visibility = View.VISIBLE
        }
    })
    binding.mapInfoCardView.startAnimation(anim)
}