package com.arnyminerz.escalaralcoiaicomtat.generic

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.os.TransactionTooLargeException
import android.view.View
import android.view.ViewGroup
import android.view.ViewManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.cardview.widget.CardView
import androidx.collection.arrayMapOf
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MapsActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClass.Companion.getIntent
import com.arnyminerz.escalaralcoiaicomtat.data.map.DEFAULT_LATITUDE
import com.arnyminerz.escalaralcoiaicomtat.data.map.DEFAULT_LONGITUDE
import com.arnyminerz.escalaralcoiaicomtat.data.map.DEFAULT_ZOOM
import com.arnyminerz.escalaralcoiaicomtat.data.map.GeoGeometry
import com.arnyminerz.escalaralcoiaicomtat.data.map.GeoMarker
import com.arnyminerz.escalaralcoiaicomtat.data.map.MARKER_WINDOW_HIDE_DURATION
import com.arnyminerz.escalaralcoiaicomtat.data.map.MARKER_WINDOW_SHOW_DURATION
import com.arnyminerz.escalaralcoiaicomtat.data.map.MapFeatures
import com.arnyminerz.escalaralcoiaicomtat.data.map.MapObjectWindowData
import com.arnyminerz.escalaralcoiaicomtat.data.map.addToMap
import com.arnyminerz.escalaralcoiaicomtat.data.map.getWindow
import com.arnyminerz.escalaralcoiaicomtat.exception.CouldNotCompressImageException
import com.arnyminerz.escalaralcoiaicomtat.exception.CouldNotCreateDirException
import com.arnyminerz.escalaralcoiaicomtat.exception.CouldNotOpenStreamException
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.includeAll
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toUri
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.write
import com.arnyminerz.escalaralcoiaicomtat.generic.maps.LocationComponent
import com.arnyminerz.escalaralcoiaicomtat.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_KMZ_FILE
import com.arnyminerz.escalaralcoiaicomtat.shared.MAP_GEOMETRIES_BUNDLE_EXTRA
import com.arnyminerz.escalaralcoiaicomtat.shared.MAP_MARKERS_BUNDLE_EXTRA
import com.arnyminerz.escalaralcoiaicomtat.storage.zipFile
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.show
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.perf.FirebasePerformance
import idroid.android.mapskit.factory.Maps
import idroid.android.mapskit.model.CommonMarker
import idroid.android.mapskit.model.CommonMarkerOptions
import idroid.android.mapskit.model.CommonPolygon
import idroid.android.mapskit.model.CommonPolyline
import idroid.android.mapskit.ui.HuaweiGoogleMapView
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException

class MapHelper
/**
 * Initializes the MapHelper instance. This also prepares the Mapbox interface with the access token.
 * Note that this should be called before any map view inflation.
 * @author Arnau Mora
 * @since 20210421
 */
{

    companion object {
        suspend fun getTarget(
            activity: Activity,
            marker: CommonMarker,
            firestore: FirebaseFirestore
        ): Intent? {
            Timber.d("Getting marker's title...")
            val title = marker.getWindow().title
            Timber.v("Searching in ${AREAS.size} cached areas...")
            return getIntent(activity, title, firestore)
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

    var map: Maps? = null

    private var mapView: HuaweiGoogleMapView? = null

    var locationComponent: LocationComponent? = null
        private set

    /**
     * Stores the last zoom amount the map has had.
     * @author Arnau Mora
     * @since 20210602
     */
    private var lastZoom: Float = DEFAULT_ZOOM

    /**
     * Stores the last position the map has had.
     * @author Arnau Mora
     * @since 20210602
     */
    private var lastPosition: LatLng = LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)

    private var allGesturesEnabled: Boolean = true

    private val markers = arrayListOf<GeoMarker>()
    private val geometries = arrayListOf<GeoGeometry>()
    private val commonMarkers = arrayListOf<CommonMarker>()
    private val polylines = arrayListOf<CommonPolyline>()
    private val polygons = arrayListOf<CommonPolygon>()

    private var loadedKmzFile: File? = null

    private val markerClickListeners = arrayListOf<CommonMarker.() -> Boolean>()

    private var mapSetUp = false
    val isLoaded: Boolean
        get() = map != null && mapSetUp && locationComponent != null

    fun onCreate(mapViewBundle: Bundle) {
        if (mapView == null)
            Timber.e("Could not call onStart() since mapView is null")
        else
            mapView!!.onCreate(mapViewBundle)
        Timber.d("onCreate()")
    }

    fun onStart() {
        if (mapView == null)
            Timber.e("Could not call onStart() since mapView is null")
        else
            mapView!!.onStart()
        Timber.d("onStart()")
    }

    fun onResume() {
        if (mapView == null)
            Timber.e("Could not call onResume() since mapView is null")
        else
            mapView!!.onResume()
        Timber.d("onResume()")
    }

    fun onPause() {
        if (mapView == null)
            Timber.e("Could not call onPause() since mapView is null")
        else
            mapView!!.onPause()
        Timber.d("onPause()")
    }

    fun onStop() {
        if (mapView == null)
            Timber.e("Could not call onStop() since mapView is null")
        else
            mapView!!.onStop()
        Timber.d("onStop()")
    }

    fun onLowMemory() {
        if (mapView == null)
            Timber.e("Could not call onLowMemory() since mapView is null")
        else
            mapView!!.onLowMemory()
        Timber.d("onLowMemory()")
    }

    fun onDestroy() {
        locationComponent?.destroy()
        if (mapView == null)
            Timber.e("Could not call onDestroy() since mapView is null")
        else
            mapView!!.onDestroy()
        Timber.d("onDestroy()")
    }

    fun withMapView(mapView: HuaweiGoogleMapView): MapHelper {
        this.mapView = mapView
        return this
    }

    fun withStartingPosition(startingPosition: LatLng?, zoom: Float = DEFAULT_ZOOM): MapHelper {
        if (startingPosition != null)
            lastPosition = startingPosition
        this.lastZoom = zoom
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
            map?.setAllGesturesEnabled(allGesturesEnabled)
        return this
    }

    private fun mapSetup(map: Maps) {
        this.map = map

        map.setOnMarkerClickListener(object : Maps.OnMapMarkerClickListener {
            override fun onMarkerClick(marker: CommonMarker): Boolean {
                Timber.d("Clicked marker!")
                var anyFalse = false
                for (list in markerClickListeners)
                    if (!list(marker))
                        anyFalse = true
                return !anyFalse
            }
        })

        map.setCompassEnabled(false)
        map.setAllGesturesEnabled(allGesturesEnabled)

        locationComponent = LocationComponent(this)

        mapSetUp = true
        move(lastPosition, lastZoom, false)
    }

    /**
     * Initializes [mapView] and sets the desired [type]. Then runs [mapSetUp] for preparing all
     * the required variables for the rest of the functions of [MapHelper].
     * @author Arnau Mora
     * @param context The context to call from.
     * @param type The [Maps.Type] to set to the map.
     * @param callback What to call when the map gets loaded
     * @throws IllegalStateException When prepared map and [isLoaded] is false.
     * @see MapHelper
     * @see HuaweiGoogleMapView
     * @see Maps.Type
     * @see Maps
     * @see isLoaded
     */
    @Throws(IllegalStateException::class)
    fun loadMap(
        type: Maps.Type = Maps.Type.SATALLITE,
        callback: (mapView: HuaweiGoogleMapView, map: Maps) -> Unit
    ): MapHelper {
        Timber.d("Loading map...")
        mapView?.getMapAsync(object : Maps.OnMapReadyListener {
            override fun onMapReady(map: Maps) {
                Timber.d("Setting map type...")
                map.setMapType(type)

                mapSetup(map)
                if (!isLoaded)
                    throw IllegalStateException("There was an issue while initializing MapHelper.")
                callback(mapView!!, map)
            }
        }) ?: Timber.e("Could not call loadMap() since mapView is null")

        return this
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
    fun loadKMZ(
        context: Context,
        kmzFile: File,
        addToMap: Boolean = true
    ): MapFeatures? =
        try {
            Timber.v("Getting map features...")
            val features = com.arnyminerz.escalaralcoiaicomtat.data.map.loadKMZ(context, kmzFile)
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
     * If the loaded features fit inside the [Intent]'s extras, they will be passed with this method,
     * otherwise, [TransactionTooLargeException] will be thrown, unless the data has been loaded
     * through [loadKMZ].
     * @author Arnau Mora
     * @param context The context to launch from
     * @param overrideLoadedValues If true, the loader markers and geometries will be ignored, and
     * the KML address will be passed to MapsActivity.
     * @throws MapAnyDataToLoadException When no data has been loaded
     * @throws TransactionTooLargeException When there's too much data on the map to transfer
     * @see MapsActivity
     */
    @Throws(
        MapAnyDataToLoadException::class,
        TransactionTooLargeException::class
    )
    fun mapsActivityIntent(context: Context, overrideLoadedValues: Boolean = false): Intent {
        val loadedElements = markers.isNotEmpty() || geometries.isNotEmpty()
        if (!loadedElements)
            throw MapAnyDataToLoadException("Map doesn't have any loaded data.")

        Timber.d("Preparing MapsActivity intent...")
        val elementsIntent = Intent(context, MapsActivity::class.java).apply {
            Timber.v("Passing to MapsActivity with parcelable list.")
            val markersCount = markers.size
            if (markersCount > 0) {
                Timber.d("  Putting $markersCount markers...")
                putParcelableArrayListExtra(MAP_MARKERS_BUNDLE_EXTRA, markers)
            }
            val geometriesCount = geometries.size
            if (geometriesCount > 0) {
                Timber.d("  Putting $geometriesCount geometries...")
                putParcelableArrayListExtra(MAP_GEOMETRIES_BUNDLE_EXTRA, geometries)
            }
        }
        val elementsIntentSize = elementsIntent.getSize()
        val size = humanReadableByteCountBin(elementsIntentSize.toLong())
        Timber.d("Elements Intent size: $size")
        // The size check ensures that TransactionTooLargeException is not thrown
        return if (loadedElements && !overrideLoadedValues && elementsIntentSize < MBYTE / 2)
            elementsIntent
        else if (loadedKmzFile != null)
            Intent(context, MapsActivity::class.java).apply {
                putExtra(EXTRA_KMZ_FILE, loadedKmzFile!!.path)
            }
        else
            throw TransactionTooLargeException("There are too many items in the map. Size: $elementsIntentSize")
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
    fun move(position: LatLng? = null, zoom: Float? = null, animate: Boolean = true): MapHelper {
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        if (animate) {
            if (position != null && zoom != null) {
                map?.animateCamera(position, zoom)
                lastZoom = zoom
                lastPosition = position
            } else if (position != null && zoom == null) {
                map?.animateCamera(position, lastZoom)
                lastPosition = position
            } else if (position == null && zoom != null) {
                map?.animateCamera(zoom)
                lastZoom = zoom
            }
        } else {
            if (position != null && zoom != null) {
                map?.moveCamera(position, zoom)
                lastZoom = zoom
                lastPosition = position
            } else if (position != null && zoom == null) {
                map?.moveCamera(position, lastZoom)
                lastPosition = position
            } else if (position == null && zoom != null) {
                map?.moveCamera(lastPosition, zoom)
                lastZoom = zoom
            }
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

        if (animate)
            map?.animateCamera(bounds, padding)
        else
            map?.moveCamera(bounds.center, lastZoom)

        return this
    }

    /**
     * Adds a click listener for a symbol
     * @param call What to call on click
     * @throws MapNotInitializedException If the map has not been initialized
     */
    @Throws(MapNotInitializedException::class)
    fun addMarkerClickListener(call: CommonMarker.() -> Boolean) {
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
            addMarkers(markers)
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
    fun addMarkers(markers: Collection<GeoMarker>) {
        for (marker in markers)
            addMarker(marker)
    }

    /**
     * Adds a marker to the map
     * @param marker The marker to add
     * @see GeoMarker
     */
    fun addMarker(marker: GeoMarker) {
        markers.add(marker)
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
        geometries.add(geometry)
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
     * @see CommonMarker
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
        commonMarkers.clear()
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
        polylines.clear()
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
        polygons.clear()
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
            geometry.first?.let { polylines.add(it) }
            geometry.second?.let { polygons.add(it) }
        }

        val symbols = markers.addToMap(this)
        this.commonMarkers.addAll(symbols)
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
        options: CommonMarkerOptions,
        window: MapObjectWindowData? = null
    ): CommonMarker {
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")
        return map!!.addMarker(options).also {
            if (window != null)
                it.setMarkerTag(window)
        }
    }

    /**
     * Stores the map's features into a GPX file
     * @author Arnau Mora
     * @since 20210318
     * @param context The context to run from
     * @param uri The uri to store at
     * @param title The title of the GPX
     *
     * @throws FileNotFoundException If the uri could not be openned
     */
    @Throws(FileNotFoundException::class)
    fun storeGPX(context: Context, uri: Uri, title: String = "Escalar Alcoià i Comtat") {
        val trace = FirebasePerformance.getInstance().newTrace("store_gpx").apply {
            putAttribute("uri", uri.toString())
            uri.fileName(context)?.let { putAttribute("file_name", it) }
        }
        trace.start()

        val contentResolver = context.contentResolver
        val stream = contentResolver.openOutputStream(uri) ?: throw CouldNotOpenStreamException()
        val description = context.getString(R.string.attr_gpx)

        Timber.v("  Storing GPX data...")
        stream.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>")
        stream.write("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" creator=\"EscalarAlcoiaIComtat-App\" version=\"1.1\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">")
        stream.write("<metadata>")
        stream.write("<link href=\"https://escalaralcoiaicomtat.centrexcursionistalcoi.org/\">")
        stream.write("<text>Escalar Alcoià i Comtat</text>")
        stream.write("</link>")
        stream.write("<name><![CDATA[ $title ]]></name>")
        stream.write("<desc><![CDATA[ $description ]]></desc>")
        stream.write("</metadata>")
        stream.write("<trk>")
        stream.write("<name><![CDATA[ $title ]]></name>")
        stream.write("<desc><![CDATA[ $description ]]></desc>")
        for (geometry in geometries) {
            stream.write("<trkseg>")
            for ((p, point) in geometry.points.withIndex()) {
                stream.write("<trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">")
                stream.write("<ele>0</ele>")
                stream.write("<name>$p</name>")
                stream.write("</trkpt>")
            }
            stream.write("</trkseg>")
        }
        stream.write("</trk>")
        for (marker in markers) {
            val pos = marker.position
            val lat = pos.latitude
            val lon = pos.longitude
            val window = marker.windowData
            stream.write("<wpt lat=\"$lat\" lon=\"$lon\">")
            if (window != null) {
                stream.write("<name>${window.title}</name>")
                if (window.message != null) {
                    val message = window.message!!
                        .replace("<br>", "<br/>")
                    stream.write("<desc>$message</desc>")
                }
            }
            stream.write("</wpt>")
        }
        stream.write("</gpx>")

        trace.stop()
    }

    /**
     * Stores the map's features into a KMZ file
     * @author Arnau Mora
     * @since 20210318
     * @param context The context to run from
     * @param uri The uri to store at
     * @param name The name of the document
     * @param description The description of the document
     * @param imageCompressionQuality The compression quality for the icons
     *
     * @throws FileNotFoundException If the uri could not be openned
     * @throws CouldNotOpenStreamException If the uri's stream could not be openned
     * @throws CouldNotCreateDirException If there was an error creating a dir
     * @throws
     */
    @Throws(
        FileNotFoundException::class,
        CouldNotOpenStreamException::class,
        CouldNotCreateDirException::class
    )
    fun storeKMZ(
        context: Context,
        uri: Uri,
        name: String? = null,
        description: String? = null,
        imageCompressionQuality: Int = 100
    ) {
        val trace = FirebasePerformance.getInstance().newTrace("store_kmz").apply {
            putAttribute("uri", uri.toString())
            uri.fileName(context)?.let { putAttribute("file_name", it) }
        }
        trace.start()

        val contentResolver = context.contentResolver
        val stream = contentResolver.openOutputStream(uri) ?: throw CouldNotOpenStreamException()
        Timber.v("Storing KMZ...")
        Timber.d("Creating temp dir...")
        val dir = File(context.cacheDir, "maphelper_" + generateUUID())
        if (!dir.mkdirs())
            throw CouldNotCreateDirException("There was an error while creating the temp dir ($dir)")
        val kmlFile = File(dir, "doc.kml")
        val imagesDir = File(dir, "images")
        if (!imagesDir.mkdirs())
            throw CouldNotCreateDirException("There was an error while creating the images dir ($imagesDir)")

        val icons = arrayMapOf<String, String>()
        val placemarksBuilder = StringBuilder()
        for (marker in markers) {
            val icon = marker.icon
            val window = marker.windowData
            val position = marker.position
            val id = generateUUID()

            val iconId: String = marker.icon.name
            Timber.d("Storing icon image for $iconId")
            val iconFileName = "$iconId.png"
            val iconFile = File(imagesDir, iconFileName)
            if (!iconFile.exists()) {
                val iconFileOutputStream = iconFile.outputStream()
                if (!icon.icon.compress(
                        Bitmap.CompressFormat.PNG,
                        imageCompressionQuality,
                        iconFileOutputStream
                    )
                )
                    throw CouldNotCompressImageException("The marker's icon could not be compressed")
                if (!icons.containsKey(iconId))
                    icons[iconId] = iconFileName
            }

            val title = window?.title ?: id
            val message = window?.message ?: id
            val lat = position.latitude
            val lon = position.longitude
            placemarksBuilder.append(
                "<Placemark>" +
                        "<name><![CDATA[$title]]></name>" +
                        "<description><![CDATA[$message]]></description>" +
                        "<styleUrl>#$iconId</styleUrl>" +
                        "<Point>" +
                        "<coordinates>" +
                        "$lon,$lat" +
                        "</coordinates>" +
                        "</Point>" +
                        "</Placemark>"
            )
        }

        val stylesBuilder = StringBuilder()
        val linesBuilder = StringBuilder()
        val polygonBuilder = StringBuilder()
        for (geometry in geometries) {
            val id = generateUUID()
            val window = geometry.windowData

            stylesBuilder.append(
                "<Style id=\"$id\">" +
                        "<LineStyle>" +
                        "<color>${geometry.style.strokeColor?.replace("#", "")}</color>" +
                        "<width>${geometry.style.lineWidth}</width>" +
                        "</LineStyle>" +
                        "<PolyStyle>" +
                        "<color>${geometry.style.strokeColor?.replace("#", "")}</color>" +
                        "</PolyStyle>" +
                        "</Style>"
            )

            if (geometry.closedShape) {
                // This is a polygon
                polygonBuilder.append("<Placemark>")
                if (window != null) {
                    polygonBuilder.append("<name><![CDATA[${window.title}]]></name>")
                    polygonBuilder.append("<description><![CDATA[${window.message}]]></description>")
                }
                polygonBuilder.append(
                    "<styleUrl>#$id</styleUrl>" +
                            "<Polygon>" +
                            "<altitudeMode>absolute</altitudeMode>" +
                            "<outerBoundaryIs>" +
                            "<LinearRing>" +
                            "<coordinates>"
                )
                for (point in geometry.points)
                    polygonBuilder.appendLine("${point.longitude},${point.latitude}")
                polygonBuilder.append(
                    "</coordinates>" +
                            "</LinearRing>" +
                            "</outerBoundaryIs>" +
                            "</Polygon>"
                )
                polygonBuilder.append("</Placemark>")
            } else {
                // This is a line
                linesBuilder.append("<Placemark>")
                if (window != null) {
                    linesBuilder.append("<name><![CDATA[${window.title}]]></name>")
                    linesBuilder.append("<description><![CDATA[${window.message}]]></description>")
                }
                linesBuilder.append(
                    "<styleUrl>#$id</styleUrl>" +
                            "<LineString>" +
                            "<altitudeMode>absolute</altitudeMode>" +
                            "<coordinates>"
                )
                for (point in geometry.points)
                    linesBuilder.appendLine("${point.longitude},${point.latitude}")
                linesBuilder.append(
                    "</coordinates>" +
                            "</LineString>"
                )
                linesBuilder.append("</Placemark>")
            }
        }

        kmlFile.outputStream().apply {
            Timber.d("Writing output stream...")
            write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">")
            write("<Document>")
            write(
                if (name != null)
                    "<name>$name</name>"
                else
                    "<name/>"
            )
            write(
                if (description != null)
                    "<description>$description</description>"
                else
                    "<description/>"
            )

            Timber.d("Generating styles...")
            for (id in icons.keys) {
                val fileName = icons[id]
                write("<Style id=\"$id\">")
                write("<IconStyle>")
                write("<scale>1.0</scale>")
                write("<Icon><href>images/$fileName</href></Icon>")
                write("</IconStyle>")
                write("</Style>")
            }
            write(stylesBuilder.toString())

            Timber.d("Generating folder...")
            write("<Folder>")
            write(
                if (name != null)
                    "<name>$name</name>"
                else
                    "<name/>"
            )
            write(placemarksBuilder.toString())
            write(linesBuilder.toString())
            write(polygonBuilder.toString())
            write("</Folder>")

            write("</Document>")
            write("</kml>")
        }
        Timber.d("Compressing KMZ...")
        zipFile(dir, stream, false)
        Timber.d("Complete!")

        trace.stop()
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
        firestore: FirebaseFirestore,
        marker: CommonMarker,
        rootView: ViewGroup
    ): MarkerWindow = MarkerWindow(activity, marker, rootView, firestore)

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

    inner class MarkerWindow
    @UiThread constructor(
        private val activity: Activity,
        private val marker: CommonMarker,
        private val rootView: ViewGroup,
        private val firestore: FirebaseFirestore
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
        suspend fun show() {
            uiContext {
                val anim = AnimationUtils.loadAnimation(activity, R.anim.enter_bottom)
                anim.duration = MARKER_WINDOW_SHOW_DURATION
                cardView.show()
                cardView.startAnimation(anim)
            }

            val window = marker.getWindow()
            val title = window.title
            val description = window.message
            val activityIntent = getTarget(activity, marker, firestore) // Info Window Data Class

            Timber.v("Marker title: $title")
            Timber.v("Marker description: $description")

            uiContext {
                titleTextView.text = title
            }

            val imageUrl = getImageUrl(description)

            uiContext {
                if (imageUrl == null)
                    descriptionTextView.text = description
                else if (!activity.isDestroyed)
                    Glide.with(activity)
                        .load(imageUrl)
                        .into(imageView)
                else Timber.w("Will not load image since there is not an attached Activity.")

                visibility(enterButton, activityIntent != null)
                visibility(imageView, imageUrl != null)
                visibility(descriptionTextView, imageUrl == null)

                val gmmIntentUri = marker.position.toUri(true, title)
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapButton.visibility(true)
                mapButton.setOnClickListener {
                    activity.startActivity(mapIntent)
                }

                if (activityIntent != null)
                    enterButton.setOnClickListener {
                        Timber.v("Launching intent...")
                        activity.startActivity(activityIntent)
                    }

                buttonsLayout.orientation =
                    if (imageUrl != null) LinearLayout.VERTICAL
                    else LinearLayout.HORIZONTAL

                rootView.addView(cardView, view.layoutParams)
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
