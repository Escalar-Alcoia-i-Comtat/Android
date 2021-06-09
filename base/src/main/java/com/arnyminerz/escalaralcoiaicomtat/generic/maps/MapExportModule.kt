package com.arnyminerz.escalaralcoiaicomtat.generic.maps

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.collection.arrayMapOf
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.exception.CouldNotCompressImageException
import com.arnyminerz.escalaralcoiaicomtat.exception.CouldNotCreateDirException
import com.arnyminerz.escalaralcoiaicomtat.exception.CouldNotOpenStreamException
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.write
import com.arnyminerz.escalaralcoiaicomtat.generic.fileName
import com.arnyminerz.escalaralcoiaicomtat.generic.generateUUID
import com.arnyminerz.escalaralcoiaicomtat.storage.zipFile
import com.google.firebase.perf.FirebasePerformance
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException

class MapExportModule(private val mapHelper: MapHelper) {

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
        for (geometry in mapHelper.geometries) {
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
        for (marker in mapHelper.markers) {
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
        for (marker in mapHelper.markers) {
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
        for (geometry in mapHelper.geometries) {
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
}
