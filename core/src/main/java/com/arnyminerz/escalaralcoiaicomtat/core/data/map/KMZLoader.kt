package com.arnyminerz.escalaralcoiaicomtat.core.data.map

import android.content.Context
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getElementByTagName
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getElementByTagNameWithAttribute
import com.arnyminerz.escalaralcoiaicomtat.core.utils.hasChildNode
import com.arnyminerz.escalaralcoiaicomtat.core.utils.mkdirsIfNotExists
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.UnzipUtil
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.readBitmap
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toElementList
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import org.w3c.dom.Element
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
 * @author Arnau Mora
 * @since 20210416
 * @param context The context to load from
 * @param kmzFile The file to load
 * @throws FileNotFoundException When the KMZ file could not be found
 * @return The MapFeatures with the loaded data
 */
@Throws(FileNotFoundException::class)
@WorkerThread
fun loadKMZ(
    context: Context,
    kmzFile: File
): MapFeatures {
    if (!kmzFile.exists())
        throw FileNotFoundException("The KMZ file (${kmzFile.path}) doesn't exist.")

    Timber.d("Instantiating LoadResult...")
    val result = MapFeatures()

    Timber.v("Loading KMZ (${kmzFile.path})...")
    val tempName = kmzFile.name.replace(" ", "_").lowercase()
    val tempDir = File(context.cacheDir, tempName)

    // If tempDir already exists, but it's not a dir, delete it
    if (tempDir.exists() && !tempDir.isDirectory)
        tempDir.delete()

    // If temp dir doesn't exist, create it
    if (!tempDir.exists())
        tempDir.mkdirs()

    // Decompress the kmz file
    val doc = ZipFile(kmzFile).let { zipFile ->
        fun unzipEntry(zipFile: ZipFile, entry: ZipEntry, outputDir: File) {
            if (entry.isDirectory) {
                File(outputDir, entry.name).mkdirsIfNotExists()
                return
            }
            val outputFile = File(outputDir, entry.name)
            if (outputFile.parentFile != null && !outputFile.parentFile!!.exists())
                if (!outputFile.parentFile!!.mkdirsIfNotExists())
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
            unzipEntry(zipFile, entry, tempDir)
        }
        val d = UnzipUtil(kmzFile, tempDir)
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
        val doc = db.parse(kmlFile)
        doc
    }

    val kml = doc.getElementsByTagName("kml")
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

    return result
}
