package com.arnyminerz.escalaralcoiaicomtat.generic

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.cardview.widget.CardView
import androidx.collection.arrayMapOf
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.*
import com.arnyminerz.escalaralcoiaicomtat.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.getIntent
import com.arnyminerz.escalaralcoiaicomtat.data.map.*
import com.arnyminerz.escalaralcoiaicomtat.exception.*
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toUri
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.write
import com.arnyminerz.escalaralcoiaicomtat.storage.zipFile
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.show
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
import java.io.File
import java.io.FileNotFoundException

class MapHelper(private val mapView: MapView) {
    companion object {
        fun getTarget(context: Context, marker: Symbol): Intent? {
            Timber.d("Getting marker's title...")
            val title = marker.getWindow().title
            Timber.v("Searching in ${AREAS.size} cached areas...")
            return getIntent(context, title)
        }

        fun getImageUrl(description: String?): String? {
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

    private var mapSetUp = false
    val isLoaded: Boolean
        get() = symbolManager != null && fillManager != null && lineManager != null &&
                map != null && style != null && style!!.isFullyLoaded && mapSetUp

    val kmlAddress: String?
        get() = loadedKMLAddress

    fun onCreate(savedInstanceState: Bundle?) = mapView.onCreate(savedInstanceState)

    fun onStart() {
        mapView.onStart()
        Timber.d("onStart()")
    }

    fun onResume() {
        mapView.onResume()
        Timber.d("onResume()")
    }

    fun onPause() {
        mapView.onPause()
        Timber.d("onPause()")
    }

    fun onStop() {
        mapView.onStop()
        Timber.d("onStop()")
    }

    fun onSaveInstanceState(outState: Bundle) {
        mapView.onSaveInstanceState(outState)
        Timber.d("onSaveInstanceState(outState)")
    }

    fun onLowMemory() {
        mapView.onLowMemory()
        Timber.d("onLowMemory()")
    }

    fun onDestroy() {
        mapView.onDestroy()
        Timber.d("onDestroy()")
    }

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
        fillManager = FillManager(mapView, map, style)
        lineManager = LineManager(mapView, map, style)
        symbolManager = SymbolManager(mapView, map, style)

        Timber.d("Configuring SymbolManager...")
        symbolManager!!.apply {
            iconAllowOverlap = true
            addClickListener {
                Timber.d("Clicked symbol!")
                var anyFalse = false
                for (list in symbolClickListeners)
                    if (!list(it))
                        anyFalse = true
                !anyFalse
            }
        }
        loadDefaultIcons(context)

        map.uiSettings.apply {
            isCompassEnabled = false
            setAllGesturesEnabled(allGesturesEnabled)
        }

        mapSetUp = true
        move(startingPosition, startingZoom, false)
    }

    /**
     * Loads the icons defined in ICONS into the map
     * @param context The context to call from
     * @see ICONS
     * @author Arnau Mora
     */
    private fun loadDefaultIcons(context: Context) {
        Timber.d("Loading default icons...")
        for (icon in ICONS) {
            val drawable = ResourcesCompat.getDrawable(
                context.resources,
                icon.drawable,
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
        callback: (mapView: MapView, map: MapboxMap, style: Style) -> Unit
    ): MapHelper {
        Timber.d("Loading map...")
        mapView.getMapAsync { map ->
            Timber.d("Setting map style...")
            map.setStyle(style) { style ->
                mapSetup(context, map, style)
                callback(mapView, map, style)
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
    @Throws(
        FileNotFoundException::class,
        NoInternetAccessException::class,
        MapNotInitializedException::class
    )
    @WorkerThread
    fun loadKML(
        activity: FragmentActivity,
        kmlAddress: String?,
        addToMap: Boolean = true
    ): MapFeatures {
        if (addToMap && !isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")
        if (!appNetworkState.hasInternet)
            throw NoInternetAccessException()

        Timber.v("Loading KML $kmlAddress...")
        val result = loadKML(activity, map!!, kmlAddress = kmlAddress)
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
     * Generates an intent for launching the MapsActivity
     * @author Arnau Mora
     * @param context The context to launch from
     * @param overrideLoadedValues If true, the loader markers and geometries will be ignored, and
     * the KML address will be passed to MapsActivity.
     * @throws MapAnyDataToLoadException When no data has been loaded
     * @see MapsActivity
     */
    @Throws(MapAnyDataToLoadException::class)
    fun mapsActivityIntent(context: Context, overrideLoadedValues: Boolean = false): Intent {
        val loadedElements = markers.isNotEmpty() || geometries.isNotEmpty()

        if (loadedKMLAddress == null && !loadedElements)
            throw MapAnyDataToLoadException("Map doesn't have any loaded data. You may run loadKML, for example.")

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
            putExtra(ICON_SIZE_MULTIPLIER_BUNDLE_EXTRA, markerSizeMultiplier)
        }
        val elementsIntentSize = elementsIntent.getSize()
        val size = humanReadableByteCountBin(elementsIntentSize.toLong())
        Timber.d("Elements Intent size: $size")
        // The size check ensures that TransactionTooLargeException is not thrown
        return if (loadedElements && !overrideLoadedValues && elementsIntentSize < MBYTE / 2)
            elementsIntent
        else
            Intent(context, MapsActivity::class.java).apply {
                Timber.d("Passing to MapsActivity with kml address ($loadedKMLAddress).")
                putExtra(KML_ADDRESS_BUNDLE_EXTRA, loadedKMLAddress!!)
                putExtra(ICON_SIZE_MULTIPLIER_BUNDLE_EXTRA, markerSizeMultiplier)
            }
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
    fun move(position: LatLng? = null, zoom: Double? = null, animate: Boolean = true): MapHelper {
        if (!isLoaded)
            throw MapNotInitializedException("Map not initialized. Please run loadMap before this")

        return move(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().apply {
                    position?.let { target(it) }
                    zoom?.let { zoom(it) }
                }.build()
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
            map?.easeCamera(update)
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
    @UiThread
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
    @UiThread
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
    @UiThread
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
    @UiThread
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
    @UiThread
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
                    stream.write("<desc>${message}</desc>")
                }
            }
            stream.write("</wpt>")
        }
        stream.write("</gpx>")
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
        val contentResolver = context.contentResolver
        val stream = contentResolver.openOutputStream(uri) ?: throw CouldNotOpenStreamException()
        Timber.v("Storing KMZ...")
        Timber.d("Creating temp dir...")
        val dir = File.createTempFile("maphelper_", null, context.cacheDir)
        if (!dir.mkdirs())
            throw CouldNotCreateDirException("There was an error while creating the temp dir")
        val kmlFile = File(dir, "doc.kml")
        val imagesDir = File(dir, "images")
        val icons = arrayMapOf<String, String>()
        val placemarksBuilder = StringBuilder()
        for (marker in markers) {
            val icon = marker.icon
            val window = marker.windowData
            val position = marker.position
            val id = generateUUID()
            var iconId: String? = null
            if (icon != null) {
                Timber.d("Storing icon image for ${marker.id}")
                iconId = marker.id
                val iconFileName = "$iconId.png"
                val iconFile = File(imagesDir, iconFileName)
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
                        "<name>$title</name>" +
                        "<description><![CDATA[$message]]></description>" +
                        "<styleUrl>#$iconId</styleUrl>" +
                        "<Point>" +
                        "<coordinates>" +
                        "$lat,$lon" +
                        "</coordinates>" +
                        "</Point>" +
                        "</Placemark>"
            )
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

            Timber.d("Generating folder...")
            write("<Folder>")
            write(
                if (name != null)
                    "<name>$name</name>"
                else
                    "<name/>"
            )
            write(placemarksBuilder.toString())
            write("</Folder>")

            write("</Document>")
            write("</kml>")
        }
        Timber.d("Compressing KMZ...")
        zipFile(dir, stream)
        Timber.d("Complete!")
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
        marker: Symbol,
        rootView: ViewGroup
    ): MarkerWindow =
        MarkerWindow(activity, marker, rootView)

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
        mapView.visibility(visible)
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
        marker: Symbol,
        rootView: ViewGroup
    ) {
        private var destroyed = false

        private var view: View =
            activity.layoutInflater.inflate(R.layout.dialog_map_marker, rootView, false)
        private var cardView: CardView = view.findViewById(R.id.mapInfoCardView)
        private var titleTextView: TextView = view.findViewById(R.id.map_info_textView)
        private var descriptionTextView: TextView = view.findViewById(R.id.mapDescTextView)
        private var imageView: ImageView = view.findViewById(R.id.mapInfoImageView)
        private var enterButton: FloatingActionButton = view.findViewById(R.id.fab_enter)
        private var mapButton: FloatingActionButton = view.findViewById(R.id.fab_maps)
        private var buttonsLayout: LinearLayout = view.findViewById(R.id.actions_layout)

        init {
            val anim = AnimationUtils.loadAnimation(activity, R.anim.enter_bottom)
            anim.duration = MARKER_WINDOW_SHOW_DURATION
            cardView.show()
            cardView.startAnimation(anim)

            val window = marker.getWindow()
            val title = window.title
            val description = window.message
            val activityIntent = getTarget(activity, marker) // Info Window Data Class

            Timber.v("Marker title: $title")
            Timber.v("Marker description: $description")

            titleTextView.text = title

            val imageUrl = getImageUrl(description)
            if (imageUrl == null)
                descriptionTextView.text = description
            else
                Glide.with(activity)
                    .load(imageUrl)
                    .into(imageView)

            visibility(enterButton, activityIntent != null)
            visibility(imageView, imageUrl != null)
            visibility(descriptionTextView, imageUrl == null)

            val gmmIntentUri = marker.latLng.toUri(true, title)
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

        /**
         * Hides the window
         * @author Arnau Mora
         * @since 20210315
         * @throws IllegalStateException If the method is called when the card has already been destroyed
         */
        fun hide() {
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
            }, MARKER_WINDOW_HIDE_DURATION)
            cardView.startAnimation(anim)
        }
    }
}

class MapNotInitializedException(message: String) : Exception(message)
class MapAnyDataToLoadException(message: String) : Exception(message)
