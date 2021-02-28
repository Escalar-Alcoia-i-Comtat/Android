package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.content.Context
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.connection.web.download
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_MARKER_SIZE_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.*
import com.arnyminerz.escalaralcoiaicomtat.generic.onUiThread
import com.arnyminerz.escalaralcoiaicomtat.generic.runAsync
import com.arnyminerz.escalaralcoiaicomtat.location.serializable
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.storage.UnzipUtil
import com.arnyminerz.escalaralcoiaicomtat.storage.readBitmap
import com.arnyminerz.escalaralcoiaicomtat.storage.storeFile
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.CameraPosition
import com.google.android.libraries.maps.model.JointType
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.RoundCap
import kotlinx.coroutines.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
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

    fun load(
        context: Context,
        googleMap: GoogleMap,
        networkState: ConnectivityProvider.NetworkState,
        finishedListener: ((result: LoadResult) -> Unit)?,
        errorListener: ((error: Exception) -> Unit)?
    ) {
        val loadResult = LoadResult()
        with(context) {
            runAsync {
                if (networkState.hasInternet) {
                    Timber.v(
                        if (kmlAddress != null) "Downloading source KML ($kmlAddress)..." else if (kmzFile != null) "Loading stored KML..." else "WTF am I loading?"
                    )
                    val stream =
                        if (kmlAddress != null)
                            if (kmlAddress.endsWith("kmz")) null
                            else download(kmlAddress)
                        else null

                    val tempDirParent = File(cacheDir, "temp")
                    val tempDir = if (kmlAddress != null) File(
                        tempDirParent,
                        kmlAddress.replace("/", "")
                    ) else null
                    if (kmlAddress != null) {
                        if (tempDir!!.exists())
                            if (tempDir.deleteRecursively())
                                Timber.d("Deleted old tempDir")
                            else
                                Timber.e("Could not delete tempDir!")
                        if (tempDir.mkdirs())
                            Timber.d("Created tempDir")
                        else
                            Timber.e("Could not create tempDir!")
                    }

                    val kmlDoc: Document? = if (stream != null) {
                        val kmldbf: DocumentBuilderFactory =
                            DocumentBuilderFactory.newInstance()
                        val kmlDB: DocumentBuilder = kmldbf.newDocumentBuilder()
                        kmlDB.parse(stream)
                    } else null

                    //Timber.d("Source KML: ${kmlDoc?.toReadableString()}")

                    val hrefL = kmlDoc?.getElementsByTagName("href")
                    val doc =
                        if (kmlAddress != null &&
                            ((hrefL != null && hrefL.length > 0) || kmlAddress.endsWith(
                                "kmz"
                            )) || kmzFile != null
                        ) {
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
                                return@runAsync
                            }

                            Timber.v("Parsing KML...")
                            val dbf: DocumentBuilderFactory =
                                DocumentBuilderFactory.newInstance()
                            val db: DocumentBuilder = dbf.newDocumentBuilder()
                            db.parse(kmlFile)
                        } else
                            kmlDoc

                    val kml = doc?.getElementsByTagName("kml")
                    val kmlElem = kml?.item(0) as Element?
                    val kmlDocument = kmlElem?.getElementByTagName("Document")
                    val folders = kmlElem?.getElementsByTagName("Folder")?.toElementList()

                    //Timber.d("KML: ${doc?.toReadableString() ?: "No KML"}")
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

                                    val latLng =
                                        LatLng(latLngD[1].toDouble(), latLngD[0].toDouble())

                                    context.onUiThread {
                                        Timber.v("New Marker: $title")
                                        val m = GeoMarker(
                                            latLng.serializable(),
                                            (30 * SETTINGS_MARKER_SIZE_PREF.get(sharedPreferences).toFloat()).toInt(),
                                            MapObjectWindowData(title, description, null)
                                        )
                                        if (iconBitmap != null) {
                                            m.withImage(iconBitmap)
                                            Timber.v("Marker has image!")
                                        }
                                        loadResult.markers.add(m)

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

                                    onUiThread {
                                        loadResult.polygons.add(
                                            GeoGeometry(
                                                GeoStyle(
                                                    "#$polyColor",
                                                    "#$lineColor",
                                                    lineWidth?.toFloat(),
                                                    RoundCap(),
                                                    JointType.ROUND
                                                ),
                                                polygonPoints,
                                                MapObjectWindowData(title, description, null),
                                                true
                                            )
                                        )
                                    }
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

                                    loadResult.polylines.add(
                                        GeoGeometry(
                                            GeoStyle(
                                                "#$polyColor",
                                                "#$lineColor",
                                                lineWidth?.toFloat(),
                                                RoundCap(),
                                                JointType.ROUND
                                            ),
                                            polygonPoints,
                                            MapObjectWindowData(title, description, null),
                                            false
                                        )
                                    )
                                }
                            }
                        }

                    Timber.v("Centering map...")
                    onUiThread {
                        if (addedPoints.size > 1)
                            try {
                                googleMap.moveCamera(
                                    newLatLngBounds(
                                        addedPoints,
                                        resources.getInteger(R.integer.marker_padding)
                                    )
                                )
                            } catch (ex: NullPointerException) { // This sometimes throw when trying to get bounds
                                Timber.e(ex, "Could not find bounds:")
                            }
                        else if (addedPoints.size > 0)
                            googleMap.moveCamera(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.fromLatLngZoom(addedPoints.first(), 16f)
                                )
                            )
                    }

                    tempDir?.deleteRecursively()
                    stream?.close()
                } else {
                    Timber.v("Device doesn't have Internet connection to load the map")
                    errorListener?.invoke(NoInternetAccessException())
                }

                finishedListener?.let { context.onUiThread { it(loadResult) } }
            }
        }
    }
}