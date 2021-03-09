package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.content.Context
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.connection.web.download
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.*
import com.arnyminerz.escalaralcoiaicomtat.generic.onUiThread
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.storage.UnzipUtil
import com.arnyminerz.escalaralcoiaicomtat.storage.readBitmap
import com.arnyminerz.escalaralcoiaicomtat.storage.storeFile
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.Property
import org.w3c.dom.Document
import org.w3c.dom.Element
import timber.log.Timber
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

@ExperimentalUnsignedTypes
class KMLLoader(private val kmlAddress: String?, private val kmzFile: File?) {
    class LoadResult {
        val markers: ArrayList<GeoMarker> = arrayListOf()
        val polygons: ArrayList<GeoGeometry> = arrayListOf()
        val polylines: ArrayList<GeoGeometry> = arrayListOf()
    }

    /**
     * Loads the KML address or KMZ file. Should be called asyncronously.
     * @throws FileNotFoundException When the KMZ file could not be found
     * @throws NoInternetAccessException When no Internet access was detected
     * @return The LoadResult with the loaded data
     */
    @Throws(
        FileNotFoundException::class,
        NoInternetAccessException::class
    )
    fun load(
        context: Context,
        map: MapboxMap,
        mapStyle: Style,
        networkState: ConnectivityProvider.NetworkState
    ): LoadResult {
        val result = LoadResult()
        if (networkState.hasInternet) {
            val tempDir = kmlAddress?.let { addr -> File(context.cacheDir, addr.replace("/", "")) }
            val docKmlFile = File(tempDir, "doc.kml")
            var kmlDownloaded = false
            val stream =
                if (tempDir?.exists() == true && docKmlFile.exists()) {
                    Timber.v("The kml for ($kmlAddress) is already downloaded in cache. Loading from there.")
                    kmlDownloaded = true
                    null
                } else {
                    val stream = if (kmlAddress != null)
                        if (kmlAddress.endsWith("kmz")) null
                        else {
                            Timber.v("Downloading source KML ($kmlAddress)...")
                            download(kmlAddress)
                        } else null

                    if (tempDir != null)
                        if (tempDir.mkdirs())
                            Timber.d("Created tempDir")
                        else
                            Timber.e("Could not create tempDir!")

                    stream
                }
            val kmldbf: DocumentBuilderFactory =
                DocumentBuilderFactory.newInstance()
            val kmlDB: DocumentBuilder = kmldbf.newDocumentBuilder()
            val kmlDoc: Document? = when {
                stream != null -> {
                    Timber.d("Parsing stream...")
                    val doc = kmlDB.parse(stream)
                    Timber.d("Stream ready.")
                    doc
                }
                kmlDownloaded -> {
                    Timber.d("Parsing file contents ($docKmlFile)...")
                    val doc = kmlDB.parse(docKmlFile)
                    Timber.d("Contents loaded!")
                    doc
                }
                else -> null
            }

            if (kmzFile != null)
                Timber.v("Loading stored KML...")

            val hrefL = kmlDoc?.getElementsByTagName("href")
            val isKmlAddressValid = (hrefL != null && hrefL.length > 0) ||
                    kmlAddress?.endsWith("kmz") == true
            val doc =
                if (kmlAddress != null && isKmlAddressValid || kmzFile != null) {
                    Timber.v("The document needs to be loaded.")
                    val kmzUrl = if (hrefL != null && hrefL.length > 0) {
                        val href = hrefL.item(0) as Element
                        href.textContent?.replace("http://", "https://")
                            ?.replace("forcekml=1&", "")
                            ?.replace("<![CDATA[", "")
                            ?.replace("]]", "")
                    } else kmlAddress
                    val kmzStream = kmzUrl?.let {
                        download(
                            it
                        )
                    }
                    Timber.v("KMZ URL: $kmzUrl")

                    val targetKMZFile =
                        kmzFile ?: File(tempDir, "kmz.kmz")
                    if (kmzStream != null) {
                        if (!targetKMZFile.exists()) {
                            storeFile(targetKMZFile, kmzStream)
                            Timber.v("KMZ Stored. Decompressing...")
                        } else
                            Timber.d("KMZ already present")
                    } else
                        Timber.e("KMZ Stream is null")

                    val zipFile = ZipFile(targetKMZFile)
                    fun createDir(dir: File): Boolean {
                        if (!dir.exists() && !dir.mkdirs()) {
                            Timber.e("Cannot create dir $dir")
                            return false
                        }
                        return true
                    }

                    fun unzipEntry(zipFile: ZipFile, entry: ZipEntry, outputDir: File) {
                        if (entry.isDirectory) {
                            createDir(File(outputDir, entry.name))
                            return
                        }
                        val outputFile = File(outputDir, entry.name)
                        if (outputFile.parentFile != null && !outputFile.parentFile!!.exists())
                            if (!createDir(outputFile.parentFile!!))
                                return

                        val inputStream =
                            BufferedInputStream(zipFile.getInputStream(entry))
                        val outputStream =
                            BufferedOutputStream(FileOutputStream(outputFile))
                        outputStream.flush()
                        outputStream.close()
                        inputStream.close()
                    }
                    for (entry in zipFile.entries()) {
                        unzipEntry(zipFile, entry, tempDir!!)
                    }
                    val d = UnzipUtil(targetKMZFile, tempDir!!)
                    d.unzip()
                    Timber.v("Decompression complete!")

                    val kmlFile = File(tempDir, "doc.kml")
                    if (!kmlFile.exists()) {
                        Timber.e("KML file (${kmlFile.path}) doesn't exist!")
                        throw FileNotFoundException("KML file (${kmlFile.path}) doesn't exist!")
                    }

                    Timber.v("Parsing KML...")
                    val dbf: DocumentBuilderFactory =
                        DocumentBuilderFactory.newInstance()
                    val db: DocumentBuilder = dbf.newDocumentBuilder()
                    db.parse(kmlFile)
                } else {
                    Timber.v("The document doesn't need to be processed. Loading document...")
                    kmlDoc
                }

            val kml = doc?.getElementsByTagName("kml")
            val kmlElem = kml?.item(0) as Element?
            val kmlDocument = kmlElem?.getElementByTagName("Document")
            val folders = kmlElem?.getElementsByTagName("Folder")?.toElementList()

            Timber.d("Got ${folders?.size ?: -1} folders.")

            val addedPoints = arrayListOf<LatLng>()

            if (folders != null)
                for (folder in folders) {
                    for (placemark in folder.getElementsByTagName("Placemark")
                        .toElementList()) {
                        val styleUrl =
                            placemark.getElementByTagName("styleUrl")?.textContent
                        if (styleUrl != null)
                            Timber.d("Placemark has style: $styleUrl")
                        val styleId = styleUrl?.substring(1, styleUrl.length)
                        val style =
                            if (styleUrl != null && styleId != null && styleUrl.startsWith(
                                    "#"
                                )
                            ) kmlDocument!!.getElementByTagNameWithAttribute(
                                "Style", "id", styleId
                            )
                            else null
                        val styleMap =
                            if (styleUrl != null && styleId != null && styleUrl.startsWith(
                                    "#"
                                )
                            ) kmlDocument!!.getElementByTagNameWithAttribute(
                                "StyleMap", "id", styleId
                            )
                            else null
                        if (style == null && styleId != null && styleMap == null)
                            Timber.w(
                                "  Style not found!!${if (styleUrl.startsWith("#")) " It's an ID: $styleId" else ""}"
                            )
                        val styleMapNormal = styleMap?.let {
                            styleId?.let {
                                kmlDocument!!.getElementByTagNameWithAttribute(
                                    "Style", "id", "$styleId-normal"
                                )
                            }
                        }
                        if (styleMap != null && styleMapNormal == null)
                            Timber.w("  Normal style map not found!! Id: $styleId-normal")

                        val iconStyle = style?.getElementByTagName("IconStyle")
                            ?: styleMapNormal?.getElementByTagName("IconStyle")
                        val icon = iconStyle?.getElementByTagName("Icon")
                        val iconHref = icon?.getElementByTagName("href")
                        val iconImageFile = iconHref?.let {
                            val imageFile = File(
                                tempDir,
                                it.textContent
                            ); if (imageFile.exists()) imageFile else null
                        }
                        val iconBitmap = iconImageFile?.let {
                            if (it.exists()) {
                                Timber.v("  Got image file: ${iconImageFile.path}")
                                readBitmap(it)
                            } else {
                                Timber.w("  Image file doesn't exist.")
                                null
                            }
                        }

                        val polyStyle = styleMapNormal?.getElementByTagName("PolyStyle")
                        val polyColor =
                            polyStyle?.getElementByTagName("color")?.textContent

                        val lineStyle = styleMapNormal?.getElementByTagName("LineStyle")
                        val lineColor =
                            lineStyle?.getElementByTagName("color")?.textContent
                        val lineWidth =
                            lineStyle?.getElementByTagName("width")?.textContent

                        // Load Point
                        if (placemark.hasChildNode("Point")) {
                            val point = placemark.getElementByTagName("Point")
                            val coordinates = point?.getElementByTagName("coordinates")
                            val latLngD = coordinates?.textContent?.split(",")
                            val title =
                                placemark.getElementByTagName("name")?.textContent
                            val description =
                                placemark.getElementByTagName("description")?.textContent

                            if (latLngD == null) continue

                            val latLng = LatLng(latLngD[1].toDouble(), latLngD[0].toDouble())

                            context.onUiThread {
                                Timber.v("New Marker: $title")
                                val m = GeoMarker(
                                    latLng,
                                    windowData=title?.let { MapObjectWindowData(it, description) }
                                )
                                if (iconBitmap != null) {
                                    m.withImage(mapStyle, iconBitmap)
                                    Timber.v("Marker has image!")
                                }
                                result.markers.add(m)

                                addedPoints.add(latLng)
                            }
                        } else if (placemark.hasChildNode("Polygon")) { // Polygon
                            val polygon = placemark.getElementByTagName("Polygon")
                            val outerBoundaryIs =
                                polygon?.getElementByTagName("outerBoundaryIs")
                            val linearRing =
                                outerBoundaryIs?.getElementByTagName("LinearRing")
                            val coordinates =
                                linearRing?.getElementByTagName("coordinates")?.textContent
                            val coordItems = coordinates?.split("\n")
                            val title =
                                placemark.getElementByTagName("name")?.textContent
                            val description =
                                placemark.getElementByTagName("description")?.textContent
                            val polygonPoints = arrayListOf<LatLng>()

                            if (coordItems == null) continue

                            for (coordinate in coordItems) {
                                val latLngD = coordinate.split(",")
                                if (latLngD.size != 3) continue
                                val latLng =
                                    LatLng(latLngD[1].toDouble(), latLngD[0].toDouble())
                                polygonPoints.add(latLng)
                                addedPoints.add(latLng)
                            }
                            if (lineColor != null) {
                                Timber.d("  Stroke: #$lineColor")
                            }
                            if (lineWidth != null) {
                                Timber.d("  Stroke Width: #$lineWidth")
                            }
                            if (polyColor != null) {
                                Timber.d("  Fill Color: #$polyColor")
                            }

                            result.polygons.add(
                                GeoGeometry(
                                    GeoStyle(
                                        "#$polyColor",
                                        "#$lineColor",
                                        lineWidth?.toFloat(),
                                        Property.LINE_JOIN_ROUND
                                    ),
                                    polygonPoints,
                                    (if (title != null)
                                        MapObjectWindowData(title, description)
                                    else null),
                                    true
                                )
                            )
                        } else if (placemark.hasChildNode("LineString")) { // Polyline
                            val lineString = placemark.getElementByTagName("LineString")
                            val coordinates =
                                lineString?.getElementByTagName("coordinates")?.textContent
                            val coordItems = coordinates?.split("\n")
                            val title =
                                placemark.getElementByTagName("name")?.textContent
                            val description =
                                placemark.getElementByTagName("description")?.textContent
                            val polygonPoints = arrayListOf<LatLng>()

                            if (coordItems == null) continue

                            for (coordinate in coordItems) {
                                val latLngD = coordinate.split(",")
                                if (latLngD.size != 3) continue
                                val latLng =
                                    LatLng(latLngD[1].toDouble(), latLngD[0].toDouble())
                                polygonPoints.add(latLng)
                                addedPoints.add(latLng)
                            }

                            if (lineColor != null)
                                Timber.d("  Stroke: #$lineColor")
                            if (lineWidth != null)
                                Timber.d("  Stroke Width: #$lineWidth")

                            result.polylines.add(
                                GeoGeometry(
                                    GeoStyle(
                                        "#$polyColor",
                                        "#$lineColor",
                                        lineWidth?.toFloat(),
                                        Property.LINE_JOIN_ROUND
                                    ),
                                    polygonPoints,
                                    (if (title != null)
                                        MapObjectWindowData(title, description)
                                    else null),
                                    false
                                )
                            )
                        }
                    }
                }

            Timber.v("Centering map...")
            context.onUiThread {
                if (addedPoints.size > 1)
                    try {
                        newLatLngBounds(
                            addedPoints,
                            context.resources.getInteger(R.integer.marker_padding)
                        )?.let { bounds ->
                            map.moveCamera(bounds)
                        }
                    } catch (ex: NullPointerException) { // This sometimes throw when trying to get bounds
                        Timber.e(ex, "Could not find bounds:")
                    }
                else if (addedPoints.size > 0)
                    map.moveCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(addedPoints.first())
                                .zoom(16.0)
                                .build()
                        )
                    )
            }

            tempDir?.deleteRecursively()
            stream?.close()
        } else {
            Timber.v("Device doesn't have Internet connection to load the map")
            throw NoInternetAccessException()
        }

        return result
    }
}
