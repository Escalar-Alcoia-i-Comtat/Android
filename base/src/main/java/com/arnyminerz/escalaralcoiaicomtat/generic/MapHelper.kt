package com.arnyminerz.escalaralcoiaicomtat.generic

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.*
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.getIntent
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

class MapHelper(private val mapView: MapView) {
    companion object {
        @ExperimentalUnsignedTypes
        fun getTarget(context: Context, marker: Symbol): Intent? {
            Timber.d("Getting marker's title...")
            val title = marker.getWindow().title
            Timber.v("Searching in ${AREAS.size} cached areas...")
            return getIntent(context, title)
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

    private var startingPosition: LatLng = LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
    private var startingZoom: Double = DEFAULT_ZOOM
    private var markerSizeMultiplier: Float = ICON_SIZE_MULTIPLIER
    private var allGesturesEnabled: Boolean = true

    private val markers = arrayListOf<GeoMarker>()
    private val geometries = arrayListOf<GeoGeometry>()
    private val symbols = arrayListOf<Symbol>()
    private val lines = arrayListOf<Line>()
    private val fills = arrayListOf<Fill>()

    private val symbolClickListeners = arrayListOf<Symbol.() -> Boolean>()

    val isLoaded: Boolean
        get() = symbolManager != null && fillManager != null && lineManager != null &&
                map != null && style != null && style!!.isFullyLoaded

    fun onCreate(savedInstanceState: Bundle?) = mapView.onCreate(savedInstanceState)

    fun onStart() = mapView.onStart()
    fun onResume() = mapView.onResume()
    fun onPause() = mapView.onPause()
    fun onStop() = mapView.onStop()
    fun onSaveInstanceState(outState: Bundle) = mapView.onSaveInstanceState(outState)
    fun onLowMemory() = mapView.onLowMemory()
    fun onDestroy() = mapView.onDestroy()

    fun withStartingPosition(startingPosition: LatLng?, zoom: Double = DEFAULT_ZOOM): MapHelper {
        if (startingPosition != null)
            this.startingPosition = startingPosition
        this.startingZoom = zoom
        return this
    }

    fun withIconSizeMultiplier(multiplier: Float): MapHelper {
        this.markerSizeMultiplier = multiplier
        return this
    }

    /**
     * Updates the controllable status of the map
     * @author Arnau Mora
     * @param controllable If gestures over the map should be enabled
     */
    fun withControllable(controllable: Boolean): MapHelper {
        allGesturesEnabled = controllable
        if (map != null)
            map?.uiSettings?.setAllGesturesEnabled(allGesturesEnabled)
        return this
    }

    private fun mapSetup(context: Context, map: MapboxMap, style: Style) {
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
        loadDefaultIcons(context)

        map.uiSettings.apply {
            isCompassEnabled = false
            setAllGesturesEnabled(allGesturesEnabled)
        }

        move(startingPosition, startingZoom, false)
    }

    /**
     * Loads the icons defined in ICONS into the map
     * @param context The context to call from
     * @see ICONS
     * @author Arnau Mora
     * @throws MapNotInitializedException When the map has not been initialized yet
     */
    @Throws(MapNotInitializedException::class)
    private fun loadDefaultIcons(context: Context) {
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        Timber.d("Loading default icons...")
        for (icon in ICONS) {
            val drawable = ResourcesCompat.getDrawable(
                context.resources,
                icon.icon,
                context.theme
            )
            if (drawable == null) {
                Timber.d("Icon ${icon.name} doesn't have a valid drawable.")
                continue
            }
            style!!.addImage(icon.name, drawable)
        }
    }

    /**
     * Initializes the map
     * @author Arnau Mora
     * @param context The context to call from
     * @param style A Mapbox map style to set
     * @param callback What to call when the map gets loaded
     * @see MapHelper
     * @see MapView
     * @see MapboxMap
     * @see Style
     */
    fun loadMap(
        context: Context,
        style: String = Style.SATELLITE,
        callback: MapHelper.(mapView: MapView, map: MapboxMap, style: Style) -> Unit
    ): MapHelper {
        Timber.d("Loading map...")
        mapView.getMapAsync { map ->
            Timber.d("Setting map style...")
            map.setStyle(style) { style ->
                mapSetup(context, map, style)
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
    @Throws(FileNotFoundException::class, NoInternetAccessException::class, MapNotInitializedException::class)
    @ExperimentalUnsignedTypes
    fun loadKML(
        activity: FragmentActivity,
        kmlAddress: String?,
        networkState: ConnectivityProvider.NetworkState,
        addToMap: Boolean = true
    ): MapFeatures {
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        val loader = KMLLoader(kmlAddress, null)
        val result = loader.load(activity, map!!, style!!, networkState)
        if (addToMap)
            activity.runOnUiThread {
                Timber.v("Loading features...")
                with(result) {
                    Timber.v("  Loading ${markers.size} markers...")
                    addMarkers(markers)
                    Timber.v("  Loading ${polygons.size} polygons...")
                    addGeometries(polygons)
                    Timber.v("  Loading ${polylines.size} polylines...")
                    addGeometries(polylines)

                    display(activity)
                    center()
                }
            }
        loadedKMLAddress = kmlAddress
        return MapFeatures(result.markers, result.polylines, result.polygons)
    }

    /**
     * Starts the MapsActivity through the specified context.
     * @author Arnau Mora
     * @param context The context to launch from
     * @param overrideLoadedValues If true, the loader markers and geometries will be ignored, and
     * the KML address will be passed to MapsActivity.
     * @throws MapAnyDataToLoadException When no data has been loaded
     * @see MapsActivity
     */
    @Throws(MapAnyDataToLoadException::class)
    @ExperimentalUnsignedTypes
    fun showMapsActivity(context: Context, overrideLoadedValues: Boolean = false) {
        val loadedElements = markers.isNotEmpty() || geometries.isNotEmpty()

        if (loadedKMLAddress == null && !loadedElements)
            throw MapAnyDataToLoadException("Map doesn't have any loaded data. You may run loadKML, for example.")

        context.startActivity(
            Intent(context, MapsActivity::class.java).apply {
                if (loadedElements && !overrideLoadedValues) {
                    Timber.v("Passing to MapsActivity with parcelable list.")
                    Timber.d("  Putting ${markers.size} markers...")
                    putExtra(MAP_MARKERS_BUNDLE_EXTRA, markers.toTypedArray())
                    Timber.d("  Putting ${geometries.size} geometries...")
                    putExtra(MAP_GEOMETRIES_BUNDLE_EXTRA, geometries.toTypedArray())
                } else {
                    Timber.d("Passing to MapsActivity with kml address ($loadedKMLAddress).")
                    putExtra(KML_ADDRESS_BUNDLE_EXTRA, loadedKMLAddress!!)
                }
                putExtra(ICON_SIZE_MULTIPLIER_BUNDLE_EXTRA, markerSizeMultiplier)
            }
        )
    }

    /**
     * Moves the camera position
     * @param position The target position
     * @param zoom The target zoomo
     * @param animate If the movement should be animated
     * @author Arnau Mora
     * @see LatLng
     * @throws MapNotInitializedException If the map has not been initialized
     * @return The instance
     */
    @Throws(MapNotInitializedException::class)
    fun move(position: LatLng, zoom: Double = DEFAULT_ZOOM, animate: Boolean = true): MapHelper {
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        return move(
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
     * @see CameraUpdate
     * @see CameraUpdateFactory
     * @throws MapNotInitializedException If the map has not been initialized
     * @return The instance
     */
    @Throws(MapNotInitializedException::class)
    fun move(update: CameraUpdate, animate: Boolean = true): MapHelper {
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        if (animate)
            map?.animateCamera(update)
        else
            map?.moveCamera(update)
        return this
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
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @SuppressLint("MissingPermission")
    @Throws(MissingPermissionException::class, MapNotInitializedException::class)
    fun enableLocationComponent(
        context: Context,
        cameraMode: Int = CameraMode.TRACKING,
        renderMode: Int = RenderMode.COMPASS
    ) {
        if (!isLoaded)
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
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        symbolClickListeners.add(call)
    }

    /**
     * Adds markers to the map
     * @param markers The markers to add
     * @see GeoMarker
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @Throws(MapNotInitializedException::class)
    fun addMarkers(markers: Collection<GeoMarker>) {
        for (marker in markers)
            addMarker(marker)
    }

    /**
     * Adds a marker to the map
     * @param marker The marker to add
     * @see GeoMarker
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @Throws(MapNotInitializedException::class)
    fun addMarker(marker: GeoMarker) {
        marker.iconSizeMultiplier = markerSizeMultiplier
        markers.add(marker)
    }

    /**
     * Adds geometries to the map
     * @param geometries The geometries to add
     * @see GeoGeometry
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @Throws(MapNotInitializedException::class)
    fun addGeometries(geometries: Collection<GeoGeometry>) {
        for (geometry in geometries)
            addGeometry(geometry)
    }

    /**
     * Adds a geometry to the map
     * @param geometry The geometry to add
     * @see GeoGeometry
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @Throws(MapNotInitializedException::class)
    fun addGeometry(geometry: GeoGeometry) {
        geometries.add(geometry)
    }

    /**
     * Adds a marker or geometry to the map. If the element type doesn't match any, anything will
     * be added.
     * @param element The element to add
     * @see GeoGeometry
     * @see GeoMarker
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @Throws(MapNotInitializedException::class)
    fun add(element: Parcelable) {
        if (element is GeoMarker)
            addMarker(element)
        else if (element is GeoGeometry)
            addGeometry(element)
    }

    /**
     * Clears all the symbols from the map
     * @author Arnau Mora
     * @see SymbolManager
     * @see Symbol
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @Throws(MapNotInitializedException::class)
    fun clearSymbols() {
        if (!isLoaded)
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
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @Throws(MapNotInitializedException::class)
    fun clearLines() {
        if (!isLoaded)
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
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @Throws(MapNotInitializedException::class)
    fun clearFills() {
        if (!isLoaded)
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
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        Timber.d("Displaying map features...")
        Timber.d("Clearing old features...")
        clearSymbols()
        clearFills()
        clearLines()

        val geometries = geometries.addToMap(fillManager!!, lineManager!!)
        for ((line, fill) in geometries) {
            lines.add(line)
            fill?.let { fills.add(it) }
        }

        val symbols = markers.addToMap(context, style!!, symbolManager!!)
        this.symbols.addAll(symbols)
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

        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        Timber.d("Centering map in features...")
        val points = arrayListOf<LatLng>()
        for (marker in markers)
            points.add(marker.position)
        for (geometry in geometries)
            points.addAll(geometry.points)

        if (markers.size == 1)
            move(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(markers.first().position).build()
                )
            )
        else {
            val boundsBuilder = LatLngBounds.Builder()
            for (marker in markers)
                boundsBuilder.include(marker.position)

            move(
                CameraUpdateFactory.newLatLngBounds(
                    boundsBuilder.build(),
                    padding
                ), animate
            )
        }
    }

    @ExperimentalUnsignedTypes
    fun infoCard(
        context: Context,
        marker: Symbol,
        binding: DialogMapMarkerBinding
    ): MarkerWindow {
        val latLng = marker.latLng

        val anim = AnimationUtils.loadAnimation(context, R.anim.enter_bottom)
        anim.duration = MARKER_WINDOW_SHOW_DURATION
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
        binding.fabMaps.visibility(true)
        binding.fabMaps.setOnClickListener {
            context.startActivity(mapIntent)
        }

        if (activityIntent != null)
            binding.fabEnter.setOnClickListener {
                Timber.v("Launching intent...")
                context.startActivity(activityIntent)
            }

        binding.actionsLayout.orientation =
            if (imageUrl != null) LinearLayout.VERTICAL
            else LinearLayout.HORIZONTAL

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
    anim.duration = MARKER_WINDOW_HIDE_DURATION
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
