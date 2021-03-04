package com.arnyminerz.escalaralcoiaicomtat.generic

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.FragmentActivity
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.*
import com.arnyminerz.escalaralcoiaicomtat.activity.MapsActivity.Companion.KML_ADDRESS_BUNDLE_EXTRA
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.AreaActivity
import com.arnyminerz.escalaralcoiaicomtat.data.map.*
import com.arnyminerz.escalaralcoiaicomtat.databinding.DialogMapMarkerBinding
import com.arnyminerz.escalaralcoiaicomtat.exception.MissingPermissionException
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toUri
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.show
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.bumptech.glide.Glide
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.*
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.Serializable

class MapHelper(private val mapView: MapView) {
    companion object {
        @ExperimentalUnsignedTypes
        fun getTarget(context: Context, marker: Symbol): Intent? {
            Timber.d("Getting marker's title...")
            val title = marker.getWindow().title
            Timber.v("Searching in ${AREAS.size} cached areas...")
            for (area in AREAS)
                if (area.displayName.equals(title, true))
                    return Intent(context, AreaActivity::class.java).apply {
                        putExtra(EXTRA_AREA, area.id)
                    }
                else if (area.isNotEmpty())
                    for (zone in area)
                        if (zone.displayName.equals(title, true))
                            return Intent(context, AreaActivity::class.java).apply {
                                putExtra(EXTRA_AREA, area.id)
                                putExtra(EXTRA_ZONE, zone.id)
                            }
                        else if (zone.isNotEmpty())
                            for (sector in zone)
                                if (sector.displayName.equals(title, true))
                                    return Intent(context, AreaActivity::class.java).apply {
                                        putExtra(EXTRA_AREA, area.id)
                                        putExtra(EXTRA_ZONE, zone.id)
                                        putExtra(EXTRA_SECTOR, sector.id)
                                    }

            Timber.w("Could not find targeted data class")
            return null
        }

        private fun getImageUrl(description: String?): String? {
            if (description == null || description.isEmpty()) return null

            if (description.startsWith("<img")) {
                val linkPos = description.indexOf("https://")
                val urlFirstPart = description.substring(linkPos) // This takes from the first "
                return urlFirstPart.substring(
                    0,
                    urlFirstPart.indexOf('"')
                ) // This from the previous to the next
            }

            return null
        }
    }

    private var map: MapboxMap? = null
    var style: Style? = null
        private set
    private var symbolManager: SymbolManager? = null
    private var fillManager: FillManager? = null
    private var lineManager: LineManager? = null

    private var loadedKMLAddress: String? = null

    private var startingPosition: LatLng = LatLng(-52.6885, -70.1395)
    private var startingZoom: Double = 2.0

    private val markers = arrayListOf<GeoMarker>()
    private val geometries = arrayListOf<GeoGeometry>()
    private val symbols = arrayListOf<Symbol>()
    private val lines = arrayListOf<Line>()
    private val fills = arrayListOf<Fill>()

    private val symbolClickListeners = arrayListOf<Symbol.() -> Boolean>()

    fun onCreate(savedInstanceState: Bundle?) = mapView.onCreate(savedInstanceState)

    fun onStart() = mapView.onStart()
    fun onResume() = mapView.onResume()
    fun onPause() = mapView.onPause()
    fun onStop() = mapView.onStop()
    fun onSaveInstanceState(outState: Bundle) = mapView.onSaveInstanceState(outState)
    fun onLowMemory() = mapView.onLowMemory()
    fun onDestroy() = mapView.onDestroy()

    fun withStartingPosition(startingPosition: LatLng?, zoom: Double = 2.0): MapHelper {
        if (startingPosition != null)
            this.startingPosition = startingPosition
        this.startingZoom = zoom
        return this
    }

    private fun mapSetup(map: MapboxMap, style: Style) {
        this.map = map
        this.style = style

        Timber.d("Loading managers...")
        symbolManager = SymbolManager(mapView, map, style)
        fillManager = FillManager(mapView, map, style)
        lineManager = LineManager(mapView, map, style)

        Timber.d("Configuring SymbolManager...")
        symbolManager!!.iconAllowOverlap = true
        symbolManager!!.addClickListener {
            Timber.d("Clicked symbol!")
            var anyFalse = false
            for (list in symbolClickListeners)
                if (!list(it))
                    anyFalse = true
            !anyFalse
        }

        map.uiSettings.apply {
            isCompassEnabled = false
            setAllGesturesEnabled(false)
        }

        map.moveCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(startingPosition)
                    .zoom(startingZoom)
                    .build()
            )
        )
    }

    fun loadMap(
        callback: MapHelper.(mapView: MapView, map: MapboxMap, style: Style) -> Unit
    ): MapHelper {
        Timber.d("Loading map...")
        mapView.getMapAsync { map ->
            Timber.d("Setting map style...")
            map.setStyle(Style.SATELLITE) { style ->
                mapSetup(map, style)
                callback(this, mapView, map, style)
            }
        }

        return this
    }

    /**
     * Loads the KML address. Should be called asyncronously.
     * @throws FileNotFoundException When the KMZ file could not be found
     * @throws NoInternetAccessException When no Internet access was detected
     * @throws MapNotInitializedException If this function is called before loadMap
     * @see loadMap
     * @see MapFeatures
     * @author Arnau Mora
     * @return A MapFeatures object with all the loaded data
     */
    @Throws(FileNotFoundException::class, NoInternetAccessException::class)
    @ExperimentalUnsignedTypes
    fun loadKML(
        activity: FragmentActivity,
        kmlAddress: String?,
        networkState: ConnectivityProvider.NetworkState,
        addToMap: Boolean = true
    ): MapFeatures {
        if (map == null || style == null || symbolManager == null || fillManager == null || lineManager == null)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        val loader = KMLLoader(kmlAddress, null)
        val result = loader.load(activity, map!!, style!!, networkState)
        if (addToMap)
            activity.runOnUiThread {
                Timber.v("Loading features...")
                with(result) {
                    Timber.v("  Loading ${markers.size} markers...")
                    add(*markers.toTypedArray())
                    Timber.v("  Loading ${polygons.size} polygons...")
                    add(*polygons.toTypedArray())
                    Timber.v("  Loading ${polylines.size} polylines...")
                    add(*polylines.toTypedArray())

                    display(activity)
                    center()
                }
            }
        loadedKMLAddress = kmlAddress
        return MapFeatures(result.markers, result.polylines, result.polygons)
    }

    @ExperimentalUnsignedTypes
    fun showMapsActivity(activity: FragmentActivity) {
        if (loadedKMLAddress == null) throw MapAnyDataToLoadException("Map doesn't have any loaded data. You may run loadKML, for example.")

        Timber.v("Launching MapsActivity from KML \"$loadedKMLAddress\"")
        activity.startActivity(
            Intent(activity, MapsActivity::class.java)
                .putExtra(
                    KML_ADDRESS_BUNDLE_EXTRA,
                    loadedKMLAddress!!
                )
        )
    }

    /**
     * Moves the camera position
     * @param position The target position
     * @param zoom The target zoomo
     * @param animate If the movement should be animated
     * @author Arnau Mora
     */
    fun move(position: LatLng, zoom: Double, animate: Boolean = true) {
        if (map == null)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        move(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(position)
                    .zoom(zoom)
                    .build()
            ),
            animate
        )
    }

    /**
     * Moves the camera position
     * @param update The movement to make
     * @param animate If the movement should be animated
     * @author Arnau Mora
     */
    private fun move(update: CameraUpdate, animate: Boolean = true) {
        if (map == null)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        if (animate)
            map?.animateCamera(update)
        else
            map?.moveCamera(update)
    }

    /**
     * Enables the current location pointer. Requires the location permission to be granted
     * @param context The context to call from
     * @param cameraMode The camera mode to set
     * @param renderMode The pointer render mode to set
     * @author Arnau Mora
     * @see CameraMode
     * @see RenderMode
     * @see PermissionsManager
     * @throws MissingPermissionException If the location permission is not granted
     */
    @SuppressLint("MissingPermission")
    @Throws(MissingPermissionException::class)
    fun enableLocationComponent(
        context: Context,
        cameraMode: Int = CameraMode.TRACKING,
        renderMode: Int = RenderMode.COMPASS
    ) {
        if (map == null || style == null || !style!!.isFullyLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        if (!PermissionsManager.areLocationPermissionsGranted(context))
            throw MissingPermissionException("Location permission not granted")

        map!!.locationComponent.apply {
            activateLocationComponent(
                LocationComponentActivationOptions.builder(context, style!!).build()
            )
            isLocationComponentEnabled = true
            this.cameraMode = cameraMode
            this.renderMode = renderMode
        }
    }

    /**
     * Adds a click listener for a symbol
     * @param call What to call on click
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @Throws(MapNotInitializedException::class)
    fun addSymbolClickListener(call: Symbol.() -> Boolean) {
        if (map == null || style == null)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        symbolClickListeners.add(call)
    }

    /**
     * Adds a marker to the map
     * @param markers The markers to add
     * @see GeoMarker
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @Throws(MapNotInitializedException::class)
    fun add(vararg markers: GeoMarker) {
        for (marker in markers)
            this.markers.add(marker)
    }

    /**
     * Adds geometries to the map
     * @param geometries The geometries to add
     * @see GeoGeometry
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @Throws(MapNotInitializedException::class)
    fun add(vararg geometries: GeoGeometry) {
        for (geometry in geometries)
            this.geometries.add(geometry)
    }

    /**
     * Clears all the symbols from the map
     * @author Arnau Mora
     * @see SymbolManager
     * @see Symbol
     */
    fun clearSymbols() {
        if (symbolManager == null)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")
        Timber.d("Clearing symbols from map...")
        symbolManager!!.delete(symbols)
        symbols.clear()
    }

    /**
     * Clears all the lines from the map
     * @author Arnau Mora
     * @see LineManager
     * @see Line
     */
    fun clearLines() {
        if (lineManager == null)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")
        Timber.d("Clearing lines from map...")
        lineManager!!.delete(lines)
        lines.clear()
    }

    /**
     * Clears all the lines from the map
     * @author Arnau Mora
     * @see LineManager
     * @see Line
     */
    fun clearFills() {
        if (fillManager == null)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")
        Timber.d("Clearing fills from map...")
        fillManager!!.delete(fills)
        fills.clear()
    }

    /**
     * Makes effective all the additions to the map through the add methods
     * @param context The context to call from
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @ExperimentalUnsignedTypes
    @Throws(MapNotInitializedException::class)
    fun display(context: Context) {
        if (symbolManager == null || fillManager == null || lineManager == null)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        Timber.d("Displaying map features...")
        Timber.d("Clearing old features...")
        clearSymbols()
        clearFills()
        clearLines()

        val symbols = markers.addToMap(context, symbolManager!!)
        this.symbols.addAll(symbols)

        val geometries = geometries.addToMap(fillManager!!, lineManager!!)
        for (geometry in geometries){
            lines.add(geometry.first)
            geometry.second?.let { fills.add(it) }
        }
    }

    /**
     * Centers all the contents into the map window
     * @param padding Padding added to the bounds
     * @param animate If the movement should be animated
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @Throws(MapNotInitializedException::class)
    fun center(padding: Int = 11, animate: Boolean = true) {
        if (markers.isEmpty())
            return

        if (symbolManager == null || fillManager == null || lineManager == null)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        Timber.d("Centering map in features...")
        val points = arrayListOf<LatLng>()
        for (marker in markers)
            points.add(marker.position.toLatLng())
        for (geometry in geometries)
            points.addAll(geometry.points)

        if (markers.size == 1)
            move(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(markers.first().position.toLatLng()).build()
                )
            )
        else {
            val boundsBuilder = LatLngBounds.Builder()
            for (marker in markers)
                boundsBuilder.include(marker.position.toLatLng())

            move(
                CameraUpdateFactory.newLatLngBounds(
                    boundsBuilder.build(),
                    padding
                ), animate
            )
        }
    }

    @ExperimentalUnsignedTypes
    fun mapsActivityIntent(context: Context): Intent =
        Intent(context, MapsActivity::class.java).apply {
            val mapData = arrayListOf<Serializable>()
            for (zm in markers)
                zm.let { zoneMarker ->
                    Timber.d("  Adding position [${zoneMarker.position.latitude}, ${zoneMarker.position.longitude}]")
                    mapData.add(zoneMarker as Serializable)
                }
            putExtra(MapsActivity.MAP_DATA_BUNDLE_EXTRA, mapData)
        }

    @ExperimentalUnsignedTypes
    fun infoCard(
        context: Context,
        marker: Symbol,
        binding: DialogMapMarkerBinding
    ): MarkerWindow {
        val latLng = marker.latLng

        val anim =
            AnimationUtils.loadAnimation(context, R.anim.enter_bottom)
        anim.duration = 500
        binding.mapInfoCardView.show()
        binding.mapInfoCardView.startAnimation(anim)

        val window = marker.getWindow()
        val title = window.title
        val description = window.message
        val activityIntent = getTarget(context, marker) // Info Window Data Class

        Timber.v("Marker title: $title")
        Timber.v("Marker description: $description")

        binding.mapInfoTextView.text = title

        val imageUrl = getImageUrl(description)
        if (imageUrl == null)
            binding.mapDescTextView.text = description
        else
            Glide.with(context)
                .load(imageUrl)
                .into(binding.mapInfoImageView)

        visibility(binding.fabEnter, activityIntent != null)
        visibility(binding.mapInfoImageView, imageUrl != null)
        visibility(binding.mapDescTextView, imageUrl == null)

        val gmmIntentUri = latLng.toUri(true, title)
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        val mapsAvailable = mapIntent.resolveActivity(context.packageManager) != null
        binding.fabMaps.visibility(mapsAvailable)
        if (mapsAvailable)
            binding.fabMaps.setOnClickListener {
                context.startActivity(mapIntent)
            }

        if (activityIntent != null)
            binding.fabEnter.setOnClickListener {
                Timber.v("Launching intent...")
                context.startActivity(activityIntent)
            }

        return MarkerWindow(context, marker, binding)
    }
}

class MapNotInitializedException(message: String) : Exception(message)
class MapAnyDataToLoadException(message: String) : Exception(message)
data class MarkerWindow(
    val context: Context,
    val marker: Symbol,
    val binding: DialogMapMarkerBinding
)

fun MarkerWindow.hide() {
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