package com.arnyminerz.escalaralcoiaicomtat.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerFragmentActivity
import com.arnyminerz.escalaralcoiaicomtat.connection.web.download
import com.arnyminerz.escalaralcoiaicomtat.data.map.*
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityMapsBinding
import com.arnyminerz.escalaralcoiaicomtat.device.vibrate
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.BottomPermissionAskerFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_CENTER_MARKER_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.*
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toLatLng
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.write
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import org.w3c.dom.Document
import org.w3c.dom.Element
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory

private const val CURRENT_LOCATION_DEFAULT_ZOOM = 17.0
private const val MAP_LOAD_PADDING = 50
private const val VIBRATION: Long = 20

private const val PERMISSION_DIALOG_TAG = "PERM_TAG"

private const val LOCATION_PERMISSION_REQUEST_CODE = 3 // This number was chosen by Dono
private const val FOLDER_ACCESS_PERMISSION_REQUEST_CODE = 7

val KML_ADDRESS_BUNDLE_EXTRA = IntentExtra<String>("KMLAddr")
val KMZ_FILE_BUNDLE_EXTRA = IntentExtra<String>("KMZFle")
const val MAP_MARKERS_BUNDLE_EXTRA = "Markers"
const val MAP_GEOMETRIES_BUNDLE_EXTRA = "Geometries"
val ICON_SIZE_MULTIPLIER_BUNDLE_EXTRA = IntentExtra<Float>("IconSize")
val ZONE_NAME_BUNDLE_EXTRA = IntentExtra<String>("ZneNm")

@ExperimentalUnsignedTypes
class MapsActivity : NetworkChangeListenerFragmentActivity() {

    private var zoneName: String? = null
    private var kmlAddress: String? = null
    private var markers = arrayListOf<GeoMarker>()
    private var geometries = arrayListOf<GeoGeometry>()
    private var kmzFile: File? = null
    private var iconSizeMultiplier = ICON_SIZE_MULTIPLIER

    private lateinit var mapHelper: MapHelper

    private var markerWindow: MarkerWindow? = null
    private var markerName: String? = null

    private var showingPolyline: GeoGeometry? = null
    private var downloadGPXMarker: GeoMarker? = null

    private var movingCamera: Boolean = false

    private lateinit var binding: ActivityMapsBinding

    private var locationManager: LocationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Hi from march of 2021
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        if (intent != null) {
            val markersList = intent.getParcelableArrayExtra(MAP_MARKERS_BUNDLE_EXTRA)
            if (markersList != null)
                for (m in markersList) {
                    val marker = m as GeoMarker?
                    if (marker != null)
                        markers.add(marker)
                }
            val geometriesList = intent.getParcelableArrayExtra(MAP_GEOMETRIES_BUNDLE_EXTRA)
            geometriesList?.let {
                for (g in it)
                    if (g is GeoGeometry)
                        geometries.add(g)
            }
            Timber.d("Got ${markers.size} markers and ${geometries.size} geometries.")

            iconSizeMultiplier =
                intent.getExtra(ICON_SIZE_MULTIPLIER_BUNDLE_EXTRA) ?: ICON_SIZE_MULTIPLIER

            kmlAddress = intent.getExtra(KML_ADDRESS_BUNDLE_EXTRA)
            zoneName = intent.getExtra(ZONE_NAME_BUNDLE_EXTRA)
            intent.getExtra(KMZ_FILE_BUNDLE_EXTRA)
                .let { path -> if (path != null) kmzFile = File(path) }
        }

        binding.floatingActionButton.setOnClickListener {
            onBackPressed()
        }

        binding.mapDownloadedImageView.visibility = View.GONE

        if (kmlAddress != null)
            binding.fabDownload.setOnClickListener {
                if (kmlAddress == null) return@setOnClickListener

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    // Filter to only show results that can be "opened", such as
                    // a file (as opposed to a list of contacts or timezones).
                    addCategory(Intent.CATEGORY_OPENABLE)

                    // Create a file with the requested MIME type.
                    type = "application/vnd.google-earth.kmz"
                    putExtra(
                        Intent.EXTRA_TITLE,
                        (kmlAddress!!.split("/").last().split(".")[0]).replace("%20", " ")
                    )
                }
                startActivityForResult(intent, FOLDER_ACCESS_PERMISSION_REQUEST_CODE)
            }
        if (kmzFile != null) {
            binding.fabDownload.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.content_save_move
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                binding.fabDownload.tooltipText = getString(R.string.action_store)
            binding.fabDownload.setOnClickListener {
                if (kmzFile == null) return@setOnClickListener

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    // Filter to only show results that can be "opened", such as
                    // a file (as opposed to a list of contacts or timezones).
                    addCategory(Intent.CATEGORY_OPENABLE)

                    // Create a file with the requested MIME type.
                    type = "application/vnd.google-earth.kmz"
                    putExtra(
                        Intent.EXTRA_TITLE,
                        zoneName ?: kmzFile!!.path.split("/").last().split(".").first()
                    )
                }
                startActivityForResult(intent, FOLDER_ACCESS_PERMISSION_REQUEST_CODE)
            }
        }
        visibility(binding.fabDownload, kmlAddress != null || kmzFile != null)

        mapHelper = MapHelper(binding.map)
        mapHelper.onCreate(savedInstanceState)
        mapHelper
            .withIconSizeMultiplier(iconSizeMultiplier)
            .loadMap(this) { _, map, _ ->
                runAsync {
                    if (kmlAddress != null || kmzFile != null)
                        loadData(networkState)

                    runOnUiThread {
                        visibility(binding.dialogMapMarker.mapInfoCardView, false)

                        Timber.v("Loading current location")
                        tryToShowCurrentLocation()

                        map.uiSettings.apply {
                            isCompassEnabled = true
                            isDoubleTapGesturesEnabled = true
                        }

                        map.addOnMoveListener(object : MapboxMap.OnMoveListener {
                            override fun onMoveBegin(detector: MoveGestureDetector) {
                                if (lastKnownLocation != null)
                                    binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_not_fixed_24)
                            }

                            override fun onMove(detector: MoveGestureDetector) {}
                            override fun onMoveEnd(detector: MoveGestureDetector) {}
                        })
                        map.addOnMapClickListener {
                            showingPolyline = null

                            markerWindow?.hide()
                            markerWindow = null

                            true
                        }

                        mapHelper.addSymbolClickListener {
                            if (SETTINGS_CENTER_MARKER_PREF.get(sharedPreferences))
                                map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                            val window = getWindow()
                            val title = window.title

                            if (title.isNotEmpty()) {
                                markerWindow = mapHelper.infoCard(
                                    this@MapsActivity,
                                    this,
                                    binding.dialogMapMarker
                                )

                                true
                            } else
                                false
                        }

                        if (kmzFile != null)
                            binding.mapDownloadedImageView.visibility = View.VISIBLE

                        Timber.v(
                            "Got ${markers.size} markers and ${geometries.size} geometries from intent."
                        )

                        mapHelper.addMarkers(markers)
                        mapHelper.addGeometries(geometries)

                        mapHelper.display(this@MapsActivity)
                        mapHelper.center(MAP_LOAD_PADDING)

                        binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_not_fixed_24)
                    }
                }
            }
    }

    override fun onStart() {
        super.onStart()
        binding.map.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.map.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.map.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.map.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.map.onDestroy()
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) &&
                    PermissionsManager.areLocationPermissionsGranted(this)
                )
                    mapHelper.enableLocationComponent(this, cameraMode = CameraMode.NONE)
                else {
                    toast(R.string.toast_location_not_shown)
                    vibrate(this, VIBRATION)
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FOLDER_ACCESS_PERMISSION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            data?.data?.also { uri ->
                Timber.i("Uri: $uri")

                when {
                    downloadGPXMarker != null -> {
                        toast(R.string.toast_storing_gpx)
                        val stream = contentResolver.openOutputStream(uri)
                        if (stream == null) {
                            toast(R.string.toast_error_internal)
                            return
                        }
                        try {
                            Timber.v("  Storing GPX data...")
                            stream.write("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" creator=\"EscalarAlcoiaIComtat-App\" version=\"1.1\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">")
                            stream.write("<metadata>")
                            stream.write("<name><![CDATA[ ${downloadGPXMarker?.windowData?.title} ]]></name>")
                            stream.write("<desc><![CDATA[ ${downloadGPXMarker?.windowData?.message} ]]></desc>")
                            stream.write("</metadata>")
                            stream.write("<trk>")
                            stream.write("<name><![CDATA[ ${downloadGPXMarker?.windowData?.title} ]]></name>")
                            stream.write("<desc><![CDATA[ ${downloadGPXMarker?.windowData?.message} ]]></desc>")
                            stream.write("<trkseg>")
                            for ((c, point) in showingPolyline!!.points.withIndex()) {
                                stream.write("<trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">")
                                stream.write("<ele>0</ele>")
                                stream.write("<name>$c</name>")
                                stream.write("</trkpt>")
                            }
                            stream.write("</trkseg>")
                            stream.write("</trk>")
                            stream.write("</gpx>")

                            runOnUiThread { toast(R.string.toast_stored_gpx) }
                        } catch (e: IOException) {
                            Timber.e(e, "Could not store GPX: ")
                            runOnUiThread {
                                toast(R.string.toast_error_internal)
                            }
                        } finally {
                            stream.close()
                            downloadGPXMarker = null
                        }
                    }
                    kmlAddress != null -> {
                        toast(R.string.toast_downloading)
                        val stream = contentResolver.openOutputStream(uri)
                        if (stream == null) {
                            toast(R.string.toast_error_internal)
                            return
                        }
                        runAsync {
                            try {
                                val kmlStream = download(kmlAddress!!)

                                fun storeKMZ(href: String) {
                                    val kmzStream = download(href)
                                    stream.write(kmzStream.readBytes())
                                    Timber.v("Stored KMZ!")
                                    runOnUiThread {
                                        toast(R.string.toast_download_complete)
                                    }
                                }

                                if (kmlAddress!!.endsWith("kmz")) {
                                    Timber.d("Address is KMZ, downloading...")
                                    storeKMZ(kmlAddress!!)
                                } else if (kmlAddress!!.endsWith("kml")) {
                                    Timber.d("Address is KML, getting address...")
                                    val kmlDoc: Document? =
                                        DocumentBuilderFactory.newInstance().newDocumentBuilder()
                                            .parse(kmlStream)
                                    val hrefL = kmlDoc?.getElementsByTagName("href")
                                    if (hrefL != null) {
                                        val href = (hrefL.item(0) as Element)
                                            .textContent?.replace("http://", "https://")
                                            ?.replace("forcekml=1&", "")
                                            ?.replace("<![CDATA[", "")
                                            ?.replace("]]", "")
                                        if (href != null) {
                                            Timber.d("Address loaded. Downloading...")
                                            storeKMZ(href)
                                        } else Timber.v("KMZ Address href is null")
                                    } else {
                                        Timber.e("Could not find KMZ Address")
                                    }
                                } else
                                    Timber.e("Unknown kml type")

                                runOnUiThread { toast(R.string.toast_download_complete) }
                            } catch (e: IOException) {
                                Timber.e(e, "Could not store GPX: ")
                                runOnUiThread { toast(R.string.toast_error_internal) }
                            } finally {
                                stream.close()
                            }
                        }
                    }
                    kmzFile != null -> {
                        toast(R.string.toast_downloading)
                        val stream = contentResolver.openOutputStream(uri)
                        if (stream == null) {
                            toast(R.string.toast_error_internal)
                            return
                        }
                        runAsync {
                            try {
                                val kmzStream = kmzFile!!.inputStream()

                                stream.write(kmzStream.readBytes())

                                runOnUiThread { toast(R.string.toast_stored_kmz) }
                            } catch (e: IOException) {
                                Timber.e(e, "Could not store GPX:")
                                runOnUiThread { toast(R.string.toast_error_internal) }
                            } finally {
                                stream.close()
                            }
                        }
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    @SuppressLint("MissingPermission")
    private fun tryToShowCurrentLocation(): Boolean {
        if (!PermissionsManager.areLocationPermissionsGranted(this)) {
            binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_off_24)
            binding.fabCurrentLocation.setOnClickListener {
                tryToShowCurrentLocation()
            }

            val bottomNavigationDrawerFragment =
                BottomPermissionAskerFragment(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ), LOCATION_PERMISSION_REQUEST_CODE,
                    getString(R.string.dialog_permission_my_location_message)
                )
            bottomNavigationDrawerFragment.show(
                supportFragmentManager,
                PERMISSION_DIALOG_TAG
            )
            return false
        } else {
            if (locationManager == null)
                locationManager =
                    applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager

            mapHelper.enableLocationComponent(this, cameraMode = CameraMode.NONE)

            binding.fabCurrentLocation.setOnClickListener {
                if (lastKnownLocation != null) {
                    val position = lastKnownLocation!!.toLatLng()
                    Timber.d("Moving camera to current location ($position)...")
                    movingCamera = true
                    mapHelper.move(position, CURRENT_LOCATION_DEFAULT_ZOOM)
                    binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_fixed_24)
                } else {
                    Timber.e("No known location!")
                    binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_off_24)
                }
            }

            binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_off_24)

            getDeviceLocation()

            return true
        }
    }

    private var lastKnownLocation: Location? = null

    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        if (locationManager == null)
            return Timber.w("Location Manager not initialized")

        try {
            if (PermissionsManager.areLocationPermissionsGranted(this)) {
                val locationResult =
                    locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: return Timber.w("Could not get last known location")
                Timber.v("Adding complete listener")
                lastKnownLocation = locationResult
                Timber.d("Got new location: $lastKnownLocation")
                binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_not_fixed_24)
            }
        } catch (e: SecurityException) {
            Timber.e(e, "Exception:")
            binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_off_24)
        }
    }

    private fun loadData(networkState: ConnectivityProvider.NetworkState): MapFeatures {
        Timber.v("Loading KML...")
        return mapHelper.loadKML(this, kmlAddress, networkState)
    }
}
