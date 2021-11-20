package com.arnyminerz.escalaralcoiaicomtat.core.utils.maps

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.view.ViewManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.edit
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.MapType
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.map.*
import com.arnyminerz.escalaralcoiaicomtat.core.shared.*
import com.arnyminerz.escalaralcoiaicomtat.core.utils.*
import com.arnyminerz.escalaralcoiaicomtat.core.view.hide
import com.arnyminerz.escalaralcoiaicomtat.core.view.show
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.storage.FirebaseStorage
import org.xml.sax.SAXParseException
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.InvalidObjectException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Initializes the MapHelper instance. This also prepares the Mapbox interface with the access token.
 * Note that this should be called before any map view inflation.
 * @author Arnau Mora
 * @since 20210421
 */
class MapHelper {

    companion object {
        /**
         * Gets the target intent to launch the correct activity from a [Marker].
         * @author Arnau Mora
         * @since 20210825
         * @param activity The [Activity] that is requesting the [Intent].
         * @return The target intent, or if not found, null.
         */
        @WorkerThread
        suspend fun Marker.getTarget(activity: Activity): Intent? {
            Timber.d("Getting marker's title...")
            val title = uiContext { getWindow().title }

            val app = activity.app

            return DataClass.getIntent(activity, app.searchSession, title)
        }

        fun getImageUrl(description: String?): String? {
            var result: String? = null
            if (description == null || description.isEmpty()) result = null
            else {
                if (description.startsWith("<img")) {
                    val linkPos = description.indexOf("https://")
                    val urlFirstPart = description.substring(linkPos) // This takes from the first "
                    result = urlFirstPart.substring(
                        0,
                        urlFirstPart.indexOf('"')
                    ) // This from the previous to the next
                }
            }

            return result
        }
    }

    var map: GoogleMap? = null

    private var mapView: MapView? = null
    private var mapFragment: SupportMapFragment? = null

    var locationComponent: LocationComponent? = null
        private set

    var export: MapExportModule? = null
        private set

    /**
     * Stores the last zoom amount the map has had.
     * @author Arnau Mora
     * @since 20210602
     */
    private var initialZoom: Float = DEFAULT_ZOOM

    /**
     * Stores the last position the map has had.
     * @author Arnau Mora
     * @since 20210602
     */
    private var initialPosition: LatLng = LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)

    private var allGesturesEnabled: Boolean = true

    /**
     * Stores all the markers that should be shown in the map when [display] is called.
     * @author Arnau Mora
     * @since 20210604
     */
    @Volatile
    internal var markers = listOf<GeoMarker>()

    /**
     * Stores all the geometries that should be shown in the map when [display] is called.
     * @author Arnau Mora
     * @since 20210604
     */
    @Volatile
    internal var geometries = listOf<GeoGeometry>()

    /**
     * Stores all the markers that have been added to the map, as references, for manipulating them
     * later.
     * @author Arnau Mora
     * @since 20210604
     */
    @Volatile
    private var commonMarkers = listOf<Marker>()

    /**
     * Stores all the polylines that have been added to the map, as references, for manipulating them
     * later.
     * @author Arnau Mora
     * @since 20210604
     */
    @Volatile
    private var polylines = listOf<Polyline>()

    /**
     * Stores all the polygons that have been added to the map, as references, for manipulating them
     * later.
     * @author Arnau Mora
     * @since 20210604
     */
    @Volatile
    private var polygons = listOf<Polygon>()

    private var loadedKmzFile: File? = null

    @get:Synchronized
    private val markerClickListeners = arrayListOf<Marker.() -> Boolean>()

    private var mapSetUp = false

    /**
     * Checks if the map has been completely loaded, so it can be used.
     * @author Arnau Mora
     * @since 20210604
     */
    val isLoaded: Boolean
        get() = map != null && mapSetUp && locationComponent != null

    fun onCreate(mapViewBundle: Bundle?) {
        if (isLoaded)
            mapView?.onCreate(mapViewBundle)
                ?: Timber.e("Could not call onStart() since mapView is null")
        Timber.d("onCreate()")
    }

    fun onStart() {
        if (isLoaded)
            mapView?.onStart() ?: Timber.e("Could not call onStart() since mapView is null")
        Timber.d("onStart()")
    }

    fun onResume() {
        if (isLoaded)
            mapView?.onResume() ?: Timber.e("Could not call onResume() since mapView is null")
        Timber.d("onResume()")
    }

    fun onPause() {
        if (isLoaded)
            mapView?.onPause() ?: Timber.e("Could not call onPause() since mapView is null")
        Timber.d("onPause()")
    }

    fun onStop() {
        if (isLoaded)
            mapView?.onStop() ?: Timber.e("Could not call onStop() since mapView is null")
        Timber.d("onStop()")
    }

    fun onLowMemory() {
        if (isLoaded)
            mapView?.onLowMemory() ?: Timber.e("Could not call onLowMemory() since mapView is null")
        Timber.d("onLowMemory()")
    }

    fun onDestroy() {
        locationComponent?.destroy()
        if (isLoaded)
            mapView?.onDestroy() ?: Timber.e("Could not call onDestroy() since mapView is null")
        Timber.d("onDestroy()")
    }

    fun withMapView(mapView: MapView): MapHelper {
        this.mapView = mapView
        return this
    }

    fun withMapFragment(activity: AppCompatActivity, @IdRes id: Int): MapHelper {
        mapFragment = activity.supportFragmentManager.findFragmentById(id) as? SupportMapFragment
        return this
    }

    fun withStartingPosition(startingPosition: LatLng?, zoom: Float = DEFAULT_ZOOM): MapHelper {
        if (startingPosition != null)
            initialPosition = startingPosition
        this.initialZoom = zoom
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

    private fun mapSetup(map: GoogleMap) {
        this.map = map

        map.setOnMarkerClickListener { marker ->
            Timber.d("Clicked marker!")
            var anyFalse = false
            for (list in markerClickListeners)
                if (!list(marker))
                    anyFalse = true
            !anyFalse
        }

        map.uiSettings.apply {
            this.isCompassEnabled = false
            setAllGesturesEnabled(allGesturesEnabled)
        }

        locationComponent = LocationComponent(this)
        export = MapExportModule(this)

        mapSetUp = true
        move(initialPosition, initialZoom, false)
    }

    /**
     * Initializes [mapView] and sets the desired [type]. Then runs [mapSetUp] for preparing all
     * the required variables for the rest of the functions of [MapHelper].
     * @author Arnau Mora
     * @param type The type to set to the map.
     * @throws IllegalStateException When prepared map and [isLoaded] is false.
     * @return A pair of the current [MapHelper] class, and a boolean, that will be true if the map
     * was loaded successfully, and false if there has been an exception.
     * @see MapHelper
     * @see MapView
     * @see GoogleMap
     * @see isLoaded
     */
    @Throws(IllegalStateException::class)
    @UiThread
    suspend fun loadMap(
        @MapType type: Int = GoogleMap.MAP_TYPE_SATELLITE
    ): Pair<MapHelper, Boolean> = suspendCoroutine { cont ->
        Timber.d("Loading map...")
        val mapReadyCallback = OnMapReadyCallback { map ->
            Timber.d("Setting map type...")
            map.mapType = type

            mapSetup(map)
            if (!isLoaded)
                throw IllegalStateException("There was an issue while initializing MapHelper.")
            cont.resume(this to true)
        }
        when {
            mapView != null -> mapView?.getMapAsync(mapReadyCallback)
            mapFragment != null -> mapFragment?.getMapAsync(mapReadyCallback)
            else -> {
                Timber.e("Could not load map since both mapView and mapFragment are null")
                cont.resume(this to false)
            }
        }
    }

    /**
     * Loads a KMZ file into the map.
     * @author Arnau Mora
     * @since 20210420
     * @param context The context to call from
     * @param kmzFile The file to load
     * @param addToMap If true, the loaded features will be added automatically to the map
     */
    @WorkerThread
    @Throws(SAXParseException::class)
    fun loadKMZ(
        context: Context,
        kmzFile: File,
        addToMap: Boolean = true
    ): MapFeatures? =
        try {
            Timber.v("Getting map features...")
            val features =
                com.arnyminerz.escalaralcoiaicomtat.core.data.map.loadKMZ(context, kmzFile)
            loadedKmzFile = kmzFile

            if (addToMap) {
                Timber.v("Adding features to the map...")
                add(features)
            }

            features
        } catch (_: FileNotFoundException) {
            Timber.w("Could not find KMZ file ($kmzFile). Will not load features")
            null
        }

    /**
     * Generates an intent for launching the MapsActivity.
     * @author Arnau Mora
     * @param context The context to launch from.
     * @param targetActivity The [Activity] to launch.
     */
    fun mapsActivityIntent(context: Context, targetActivity: Class<*>): Intent {
        sharedPreferences.edit {
            val loadedElements = markers.isNotEmpty() || geometries.isNotEmpty()
            if (!loadedElements)
                throw MapAnyDataToLoadException("Map doesn't have any loaded data.")

            Timber.d("Storing features in shared preferences......")
            val markersCount = markers.size
            if (markersCount > 0) {
                Timber.d("  Putting $markersCount markers...")
                putParcelableList(MAP_MARKERS_BUNDLE_EXTRA, markers)
            }
            val geometriesCount = geometries.size
            if (geometriesCount > 0) {
                Timber.d("  Putting $geometriesCount geometries...")
                putParcelableList(MAP_GEOMETRIES_BUNDLE_EXTRA, geometries)
            }
        }

        return Intent(context, targetActivity).apply {
            putExtra(EXTRA_KMZ_FILE, loadedKmzFile!!.path)
        }
    }

    /**
     * Moves the camera position
     * @param position The target position
     * @param zoom The target zoom
     * @param animate If the movement should be animated
     * @author Arnau Mora
     * @see LatLng
     * @throws MapNotInitializedException If the map has not been initialized
     * @return The instance
     */
    @Throws(MapNotInitializedException::class)
    fun move(position: LatLng, zoom: Float, animate: Boolean = true): MapHelper {
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        val update = CameraUpdateFactory.newLatLngZoom(position, zoom)

        if (animate)
            map?.animateCamera(update)
        else
            map?.moveCamera(update)

        return this
    }

    /**
     * Moves the camera position
     * @param position The target position
     * @param animate If the movement should be animated
     * @author Arnau Mora
     * @see LatLng
     * @throws MapNotInitializedException If the map has not been initialized
     * @return The instance
     */
    @Throws(MapNotInitializedException::class)
    fun move(position: LatLng, animate: Boolean = true): MapHelper {
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        val update = CameraUpdateFactory.newLatLng(position)

        if (animate)
            map?.animateCamera(update)
        else
            map?.moveCamera(update)

        return this
    }

    /**
     * Moves the camera position
     * @param zoom The target zoom
     * @param animate If the movement should be animated
     * @author Arnau Mora
     * @see LatLng
     * @throws MapNotInitializedException If the map has not been initialized
     * @return The instance
     */
    @Throws(MapNotInitializedException::class)
    fun move(zoom: Float, animate: Boolean = true): MapHelper {
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        val map = this.map
        if (map != null) {
            val cameraPosition = map.cameraPosition
            val update = CameraUpdateFactory.newCameraPosition(
                CameraPosition(
                    cameraPosition.target,
                    zoom,
                    cameraPosition.tilt,
                    cameraPosition.bearing
                )
            )

            if (animate)
                map.animateCamera(update)
            else
                map.moveCamera(update)
        }

        return this
    }

    /**
     * Moves the camera position.
     * @param bounds The bounds to move the camera to.
     * @param padding The padding to leave to the sides from the bounds.
     * @param animate If the movement should be animated.
     * @author Arnau Mora
     * @see LatLng
     * @throws MapNotInitializedException If the map has not been initialized
     * @return The instance
     */
    @Throws(MapNotInitializedException::class)
    fun move(bounds: LatLngBounds, padding: Int, animate: Boolean = true): MapHelper {
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        val update = CameraUpdateFactory.newLatLngBounds(bounds, padding)

        if (animate)
            map?.animateCamera(update)
        else
            map?.moveCamera(update)

        return this
    }

    /**
     * Adds a click listener for a symbol
     * @param call What to call on click
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @Throws(MapNotInitializedException::class)
    fun addMarkerClickListener(call: Marker.() -> Boolean) {
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        markerClickListeners.add(call)
    }

    /**
     * Adds the map features to the map
     * @param result The [MapFeatures] to add
     * @see MapFeatures
     * @see GeoGeometry
     * @see GeoMarker
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @Throws(MapNotInitializedException::class)
    fun add(result: MapFeatures) {
        Timber.v("Loading features...")
        with(result) {
            Timber.v("  Loading ${markers.size} markers...")
            addMarkers(*markers.toTypedArray())
            Timber.v("  Loading ${polygons.size} polygons...")
            addGeometries(polygons)
            Timber.v("  Loading ${polylines.size} polylines...")
            addGeometries(polylines)
        }
    }

    /**
     * Adds markers to the map
     * @param markers The markers to add
     * @see GeoMarker
     */
    fun addMarkers(vararg markers: GeoMarker) {
        for (marker in markers)
            addMarker(marker)
    }

    /**
     * Adds a marker to the map
     * @param marker The marker to add
     * @see GeoMarker
     */
    fun addMarker(marker: GeoMarker) {
        val copy = markers.toMutableList()
        copy.add(marker)
        markers = copy
    }


    /**
     * Adds markers to the map
     * @param markers The markers to add
     * @see Marker
     */
    fun addMarkers(vararg markers: Marker) {
        for (marker in markers)
            addMarker(marker)
    }

    /**
     * Adds a marker to the map
     * @param marker The marker to add
     * @see Marker
     */
    fun addMarker(marker: Marker) {
        val copy = commonMarkers.toMutableList()
        copy.add(marker)
        commonMarkers = copy
    }

    /**
     * Adds geometries to the map
     * @param geometries The geometries to add
     * @see GeoGeometry
     */
    fun addGeometries(geometries: Collection<GeoGeometry>) {
        for (geometry in geometries)
            addGeometry(geometry)
    }

    /**
     * Adds a geometry to the map
     * @param geometry The geometry to add
     * @see GeoGeometry
     */
    fun addGeometry(geometry: GeoGeometry) {
        val copy = geometries.toMutableList()
        copy.add(geometry)
        geometries = copy
    }

    /**
     * Adds polylines to the map
     * @param polylines The polylines to add
     * @see Polyline
     */
    fun addPolylines(polylines: Collection<Polyline>) {
        for (polyline in polylines)
            addPolyline(polyline)
    }

    /**
     * Adds a polyline to the map
     * @param polyline The polyline to add
     * @see Polyline
     */
    fun addPolyline(polyline: Polyline) {
        val copy = polylines.toMutableList()
        copy.add(polyline)
        polylines = copy
    }

    /**
     * Adds polygons to the map
     * @param polygons The polygons to add
     * @see Polygon
     */
    fun addPolygon(polygons: Collection<Polygon>) {
        for (polygon in polygons)
            addPolygon(polygon)
    }

    /**
     * Adds a polygon to the map
     * @param polygon The polyline to add
     * @see Polygon
     */
    fun addPolygon(polygon: Polygon) {
        val copy = polygons.toMutableList()
        copy.add(polygon)
        polygons = copy
    }

    /**
     * Adds a marker or geometry to the map. If the element type doesn't match any, anything will
     * be added.
     * @param element The element to add
     * @see GeoGeometry
     * @see GeoMarker
     */
    fun add(element: Parcelable) {
        if (element is GeoMarker)
            addMarker(element)
        else if (element is GeoGeometry)
            addGeometry(element)
    }

    /**
     * Clears all the symbols from the map
     * @author Arnau Mora
     * @see Marker
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @UiThread
    @Throws(MapNotInitializedException::class)
    fun clearSymbols() {
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")
        Timber.d("Clearing symbols from map...")
        for (marker in commonMarkers)
            marker.remove()
        val copy = commonMarkers.toMutableList()
        copy.clear()
        commonMarkers = copy
    }

    /**
     * Clears all the lines from the map
     * @author Arnau Mora
     * @since 20210602
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @UiThread
    @Throws(MapNotInitializedException::class)
    fun clearLines() {
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")
        Timber.d("Clearing lines from map...")
        for (line in polylines)
            line.remove()
        val copy = polylines.toMutableList()
        copy.clear()
        polylines = copy
    }

    /**
     * Clears all the polygons from the map
     * @author Arnau Mora
     * @since 20210602
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @UiThread
    @Throws(MapNotInitializedException::class)
    fun clearPolygons() {
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")
        Timber.d("Clearing polygons from map...")
        for (fill in polygons)
            fill.remove()
        val copy = polygons.toMutableList()
        copy.clear()
        polygons = copy
    }

    /**
     * Makes effective all the additions to the map through the add methods
     * @author Arnau Mora
     * @since 20210602
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @UiThread
    @Throws(MapNotInitializedException::class)
    fun display() {
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        Timber.d("Displaying map features...")
        Timber.d("Clearing old features...")
        clearSymbols()
        clearPolygons()
        clearLines()

        val geometries = geometries.addToMap(map!!)
        for (geometry in geometries) {
            geometry.first?.let { addPolyline(it) }
            geometry.second?.let { addPolygon(it) }
        }

        val symbols = markers.addToMap(this)
        addMarkers(*symbols.toTypedArray())
    }

    /**
     * Centers all the contents into the map window
     * @param padding Padding added to the bounds
     * @param animate If the movement should be animated
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @UiThread
    @Throws(MapNotInitializedException::class)
    fun center(padding: Int = 11, animate: Boolean = true, includeCurrentLocation: Boolean = true) {
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

        if (includeCurrentLocation)
            if (locationComponent?.lastKnownLocation != null) {
                val loc = locationComponent!!.lastKnownLocation
                Timber.d("Including current location ($loc)")
                points.add(loc!!)
            } else
                Timber.d("Could not include current location since it's null")

        if (points.isNotEmpty())
            if (points.size == 1)
                move(markers.first().position, DEFAULT_ZOOM)
            else {
                val boundsBuilder = LatLngBounds.Builder()
                boundsBuilder.includeAll(points)

                move(boundsBuilder.build(), padding, animate)
            }
    }

    /**
     * Creates a new symbol with the SymbolManager.
     * @author Arnau Mora
     * @since 20210319
     * @param options The symbol to add's options.
     * @param window The window data for the marker.
     * @throws MapNotInitializedException If the map has not been initialized.
     * @return The created symbol
     */
    @Throws(MapNotInitializedException::class)
    fun createMarker(
        options: MarkerOptions,
        window: MapObjectWindowData? = null
    ): Marker? {
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")
        return map!!.addMarker(options)?.also {
            if (window != null)
                it.tag = window
        }
    }

    /**
     * Shows an info card showing the contents of a marker
     * @author Arnau Mora
     * @since 20210315
     * @param activity The activity that is currently running
     * @param marker The marker to show the info for
     * @param rootView The activity's root view
     * @return The created window
     */
    fun infoCard(
        activity: Activity,
        storage: FirebaseStorage,
        marker: Marker,
        rootView: ViewGroup
    ): MarkerWindow = MarkerWindow(activity, marker, rootView, storage)

    /**
     * Changes the map view visibility
     * @author Arnau Mora
     * @since 20210310
     * @param visible If true, the map will be visible.
     * @return The MapHelper instance
     * @see View.visibility
     */
    @UiThread
    fun visibility(visible: Boolean): MapHelper {
        mapView?.visibility(visible)
        return this
    }

    /**
     * Hides the map's UI
     * @author Arnau Mora
     * @since 20210310
     * @see visibility
     */
    @UiThread
    fun hide() = visibility(false)

    /**
     * Shows the map's UI
     * @author Arnau Mora
     * @since 20210310
     * @see visibility
     */
    @UiThread
    fun show() = visibility(true)

    /**
     * Sets the click listener for when the user taps the map.
     * @author Arnau Mora
     * @since 20211120
     * @param listener The callback that will be executed when the map gets clicked.
     * @return The current instance of [MapHelper]
     */
    @UiThread
    fun setOnMapClickListener(listener: GoogleMap.OnMapClickListener?): MapHelper {
        map?.setOnMapClickListener(listener)
        return this
    }

    /**
     * Sets the click listener for when the user taps a marker in the map.
     * @author Arnau Mora
     * @since 20211120
     * @param listener The callback that will be executed when the marker gets clicked.
     * @return The current instance of [MapHelper]
     */
    @UiThread
    fun setOnMarkerClickListener(listener: GoogleMap.OnMarkerClickListener?): MapHelper {
        map?.setOnMarkerClickListener(listener)
        return this
    }

    /**
     * Enables or disables the map's compass.
     * @author Arnau Mora
     * @since 20211120
     * @param enabled Whether or not to enable the compass.
     */
    @UiThread
    fun setCompassEnabled(enabled: Boolean): MapHelper {
        map?.uiSettings?.isCompassEnabled = enabled
        return this
    }

    inner class MarkerWindow
    @UiThread constructor(
        private val activity: Activity,
        private val marker: Marker,
        private val rootView: ViewGroup,
        private val storage: FirebaseStorage
    ) {
        private var destroyed = false
        private var shown = false

        private var view: View =
            activity.layoutInflater.inflate(R.layout.dialog_map_marker, rootView, false)
        private var cardView: CardView = view.findViewById(R.id.mapInfoCardView)
        private var titleTextView: TextView = view.findViewById(R.id.map_info_textView)
        private var descriptionTextView: TextView = view.findViewById(R.id.mapDescTextView)
        private var imageView: ImageView = view.findViewById(R.id.mapInfoImageView)
        private var enterButton: FloatingActionButton = view.findViewById(R.id.fab_enter)
        private var mapButton: FloatingActionButton = view.findViewById(R.id.fab_maps)
        private var buttonsLayout: LinearLayout = view.findViewById(R.id.actions_layout)

        private val hideListeners = arrayListOf<() -> Unit>()

        /**
         * Shows the [MarkerWindow].
         * @author Arnau Mora
         * @since 20210416
         */
        @UiThread
        fun show() {
            val anim = AnimationUtils.loadAnimation(activity, R.anim.enter_bottom)
            anim.duration = MARKER_WINDOW_SHOW_DURATION
            cardView.show()
            cardView.startAnimation(anim)

            val window = try {
                marker.getWindow()
            } catch (e: InvalidObjectException) {
                Timber.e(e, "Could not get marker window data.")
                null
            }
            val title = window?.title
            val description = window?.message

            Timber.v("Marker title: $title")
            Timber.v("Marker description: $description")

            titleTextView.text = title

            val imageUrl = getImageUrl(description)

            if (imageUrl == null)
                descriptionTextView.text = description
            else if (!activity.isDestroyed)
            // TODO: Load image
                Timber.d("Loading \"$imageUrl\" into imageView...")
            else Timber.w("Will not load image since there is not an attached Activity.")

            visibility(imageView, imageUrl != null)
            visibility(descriptionTextView, imageUrl == null)

            enterButton.isEnabled = false

            val gmmIntentUri = marker.position.toUri(true, title)
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapButton.visibility(true)
            mapButton.setOnClickListener {
                activity.startActivity(mapIntent)
            }

            buttonsLayout.orientation =
                if (imageUrl != null) LinearLayout.VERTICAL
                else LinearLayout.HORIZONTAL

            rootView.addView(cardView, view.layoutParams)

            doAsync {
                // Info Window Data Class
                val activityIntent = marker.getTarget(activity)

                uiContext {
                    visibility(enterButton, activityIntent != null)
                    enterButton.isEnabled = true

                    if (activityIntent != null)
                        enterButton.setOnClickListener {
                            Timber.v("Launching intent...")
                            activity.startActivity(activityIntent)
                        }
                }
            }

            shown = true
            destroyed = false
        }

        /**
         * Hides the window
         * @author Arnau Mora
         * @since 20210315
         * @throws IllegalStateException If the method is called when the card has already been
         * destroyed, or has not been shown yet.
         */
        fun hide() = also {
            if (!shown)
                throw IllegalStateException("The card has already been destroyed")
            if (destroyed)
                throw IllegalStateException("The card has already been destroyed")

            Timber.v("Hiding MarkerWindow")
            val anim = AnimationUtils.loadAnimation(activity, R.anim.exit_bottom)
            anim.interpolator = AccelerateInterpolator()
            anim.duration = MARKER_WINDOW_HIDE_DURATION
            Handler(Looper.getMainLooper()).postDelayed({
                Timber.d("Finished animation")
                cardView.hide()
                (cardView.parent as ViewManager).removeView(cardView)
                destroyed = true
                shown = false
            }, MARKER_WINDOW_HIDE_DURATION)
            cardView.startAnimation(anim)

            for (list in hideListeners)
                list()
        }

        /**
         * Listens for when the window is hidden by the user.
         * @author Arnau Mora
         * @since 20210416
         */
        fun listenHide(block: () -> Unit): MarkerWindow =
            this.also {
                hideListeners.add(block)
            }
    }
}

class MapNotInitializedException(message: String) : Exception(message)
class MapAnyDataToLoadException(message: String) : Exception(message)
