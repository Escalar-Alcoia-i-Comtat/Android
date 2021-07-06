package com.arnyminerz.escalaralcoiaicomtat.core.data.map

import android.content.Context
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.core.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.core.utils.MEGABYTE
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getElementByTagName
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getElementByTagNameWithAttribute
import com.arnyminerz.escalaralcoiaicomtat.core.utils.hasChildNode
import com.arnyminerz.escalaralcoiaicomtat.core.utils.includeAll
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.UnzipUtil
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.readBitmap
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.storeFile
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toElementList
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.web.download
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import org.w3c.dom.Element
import org.xml.sax.InputSource
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Loads the KML address or KMZ file. Should be called asyncronously.
 * @throws FileNotFoundException When the KMZ file could not be found
 * @throws NoInternetAccessException When no Internet access was detected
 * @throws IllegalStateException When both kmlAddress and kmzFile are null
 * @return The LoadResult with the loaded data
 */
@Throws(
    FileNotFoundException::class,
    NoInternetAccessException::class
)
@WorkerThread
suspend fun loadKML(
    context: Context,
    map: GoogleMap,
    kmlReference: StorageReference? = null,
    kmzFile: File? = null
): MapFeatures {
    if (kmlReference == null && kmzFile == null)
        throw IllegalStateException("Both kmlAddress and kmzFile are null")

    Timber.d("Instantiating LoadResult...")
    val result = MapFeatures()

    Timber.v("Loading KML $kmlReference...")
    val tempDir = kmlReference?.let { addr -> File(context.cacheDir, addr.name) }
    val docKmlFile = File(tempDir, "doc.kml")
    val bytes = if (tempDir?.exists() == true && docKmlFile.exists()) {
        Timber.v("The kml for ($kmlReference) is already downloaded in cache. Loading from there.")
        docKmlFile.inputStream().readBytes()
    } else kmlReference?.getBytes(30 * MEGABYTE)?.await()

    val kmldbf: DocumentBuilderFactory =
        DocumentBuilderFactory.newInstance()
    val kmlDB: DocumentBuilder = kmldbf.newDocumentBuilder()

    Timber.d("Parsing stream...")
    val inputSource = InputSource(bytes?.inputStream())
    val kmlDoc = kmlDB.parse(inputSource)
    Timber.d("Stream ready.")

    if (kmzFile != null)
        Timber.v("Loading stored KML...")

    val hrefL = kmlDoc?.getElementsByTagName("href")
    val isKmlAddressValid = (hrefL != null && hrefL.length > 0) ||
            kmlReference?.path?.endsWith("kmz") == true
    val doc =
        if (kmlReference != null && isKmlAddressValid || kmzFile != null) {
            Timber.v("The document needs to be loaded.")
            val kmzUrl = if (hrefL != null && hrefL.length > 0) {
                val href = hrefL.item(0) as Element
                href.textContent?.replace("http://", "https://")
                    ?.replace("forcekml=1&", "")
                    ?.replace("<![CDATA[", "")
                    ?.replace("]]", "")
            } else kmlReference?.path
            val kmzStream = kmzUrl?.let {
                download(it)
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
                else
                    Timber.w("  Style ID: $styleId")

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

                    if (latLngD != null) {
                        val latLng = LatLng(latLngD[1].toDouble(), latLngD[0].toDouble())

                        Timber.v("New Marker: $title")
                        val m = GeoMarker(
                            latLng,
                            windowData = title?.let {
                                MapObjectWindowData(
                                    it,
                                    description
                                )
                            },
                            icon = ICON_WAYPOINT_ESCALADOR_BLANC.toGeoIcon(context)!!
                        )
                        if (iconBitmap != null) {
                            m.withImage(iconBitmap, styleId)
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

                    if (coordItems != null) {
                        for (coordinate in coordItems) {
                            val latLngD = coordinate.split(",")
                            if (latLngD.size != MAP_LOADER_LATLNG_SIZE) continue
                            val latLng =
                                LatLng(latLngD[1].toDouble(), latLngD[0].toDouble())
                            polygonPoints.add(latLng)
                            addedPoints.add(latLng)
                        }
                        if (lineColor != null)
                            Timber.d("  Stroke: #$lineColor")
                        if (lineWidth != null)
                            Timber.d("  Stroke Width: #$lineWidth")
                        if (polyColor != null)
                            Timber.d("  Fill Color: #$polyColor")

                        result.polygons.add(
                            GeoGeometry(
                                GeoStyle(
                                    "#$polyColor",
                                    "#$lineColor",
                                    lineWidth?.toFloat(),
                                    JointType.ROUND
                                ),
                                polygonPoints,
                                (if (title != null)
                                    MapObjectWindowData(title, description)
                                else null),
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
                        if (latLngD.size != MAP_LOADER_LATLNG_SIZE) continue
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
                                JointType.ROUND
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
    uiContext {
        val update = when {
            addedPoints.size > 1 -> CameraUpdateFactory.newLatLngBounds(
                LatLngBounds.builder().includeAll(addedPoints).build(), 10
            )
            addedPoints.size > 0 -> CameraUpdateFactory.newLatLng(addedPoints.first())
            else -> null
        }
        if (update != null)
            map.moveCamera(update)
    }

    tempDir?.deleteRecursively()

    return result
}
