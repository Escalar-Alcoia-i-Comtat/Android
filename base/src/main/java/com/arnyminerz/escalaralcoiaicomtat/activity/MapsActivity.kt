package com.arnyminerz.escalaralcoiaicomtat.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.IntroActivity.Companion.hasLocationPermission
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerFragmentActivity
import com.arnyminerz.escalaralcoiaicomtat.connection.web.download
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.find
import com.arnyminerz.escalaralcoiaicomtat.data.map.GeoGeometry
import com.arnyminerz.escalaralcoiaicomtat.data.map.GeoMarker
import com.arnyminerz.escalaralcoiaicomtat.data.map.KMLLoader
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityMapsBinding
import com.arnyminerz.escalaralcoiaicomtat.device.vibrate
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.BottomPermissionAskerFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_CENTER_MARKER_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.*
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.*
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.OnMapReadyCallback
import com.google.android.libraries.maps.SupportMapFragment
import com.google.android.libraries.maps.model.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import timber.log.Timber
import java.io.File
import java.io.Serializable
import javax.xml.parsers.DocumentBuilderFactory


@ExperimentalUnsignedTypes
class MapsActivity : OnMapReadyCallback, NetworkChangeListenerFragmentActivity() {

    companion object {
        private const val PERMISSION_DIALOG_TAG = "PERM_TAG"

        private const val LOCATION_PERMISSION_REQUEST_CODE = 3 // This number was chosen by Dono
        private const val FOLDER_ACCESS_PERMISSION_REQUEST_CODE = 7

        const val KML_ADDRESS_BUNDLE_EXTRA = "KMLAddr"
        const val KMZ_FILE_BUNDLE_EXTRA = "KMZFle"
        const val MAP_DATA_BUNDLE_EXTRA = "MapDta"
        const val ZONE_NAME_BUNDLE_EXTRA = "ZneNm"
    }

    private var zoneName: String? = null
    private var kmlAddress: String? = null
    private var mapData: Serializable? = null
    private var kmzFile: File? = null
    private var googleMap: GoogleMap? = null
    private var markerWindow: MarkerWindow? = null
    private var markerName: String? = null

    private var showingPolyline: GeoGeometry? = null
    private var downloadGPXMarker: GeoMarker? = null

    private var movingCamera: Boolean = false

    private val polygons = arrayListOf<GeoGeometry>()
    private val polylines = arrayListOf<GeoGeometry>()
    private val markers = arrayListOf<GeoMarker>()

    private lateinit var binding: ActivityMapsBinding

    private fun allPoints(): ArrayList<LatLng> {
        val list = arrayListOf<LatLng>()
        for (p in polygons) list.addAll(p.points)
        for (p in polylines) list.addAll(p.points)
        return list
    }

    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (intent != null) {
            kmlAddress = intent.getStringExtra(KML_ADDRESS_BUNDLE_EXTRA)
            mapData = intent.getSerializableExtra(MAP_DATA_BUNDLE_EXTRA)
            zoneName = intent.getStringExtra(ZONE_NAME_BUNDLE_EXTRA)
            intent.getStringExtra(KMZ_FILE_BUNDLE_EXTRA)
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

        binding.dialogMapMarker.fabMaps.setOnClickListener {
            if (markerWindow == null || markerName == null) return@setOnClickListener

            val gmmIntentUri =
                Uri.parse("geo:${markerWindow!!.marker.position.latitude},${markerWindow!!.marker.position!!.longitude}?q=${markerWindow!!.marker.position!!.latitude},${markerWindow!!.marker.position!!.longitude}($markerName)")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            }
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this@MapsActivity.googleMap = googleMap

        fun innerLoad() {
            runOnUiThread {
                with(googleMap) {
                    Timber.v("Got googleMap. Setting type.")

                    visibility(binding.dialogMapMarker.mapInfoCardView, false)

                    mapType = GoogleMap.MAP_TYPE_SATELLITE

                    Timber.d("Got ${this@MapsActivity.polylines.size} polylines.")
                    Timber.d("Got ${this@MapsActivity.polygons.size} polygons.")
                    Timber.d("Got ${this@MapsActivity.markers.size} markers.")

                    for (polyline in this@MapsActivity.polylines)
                        polyline.addToMap(googleMap)
                    for (polygon in this@MapsActivity.polygons)
                        polygon.addToMap(googleMap)
                    for (marker in this@MapsActivity.markers)
                        marker.addToMap(googleMap)

                    Timber.v("Loading current location")
                    tryToShowCurrentLocation()

                    uiSettings.isCompassEnabled = true
                    uiSettings.isZoomControlsEnabled = false
                    uiSettings.isMapToolbarEnabled = false

                    setOnCameraMoveStartedListener { reason ->
                        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE)
                            if (lastKnownLocation != null)
                                binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_not_fixed_24)
                    }
                    setOnMapClickListener { position ->
                        showingPolyline = null

                        for (marker in this@MapsActivity.markers) {
                            if (position.distanceTo(marker.position.toLatLng()) < 15f) {
                                val windowData = marker.windowData
                                if (windowData.isNull()) continue

                                if (windowData!!.title == null) {
                                    Timber.w("Clicked on marker with no title!")
                                    return@setOnMapClickListener
                                }
                                binding.dialogMapMarker.mapInfoTextView.text = windowData.title
                                binding.dialogMapMarker.mapInfoTextView.text = windowData.message
                                markerName = windowData.title

                                showingPolyline = null

                                Timber.w("Showing info window")
                                visibility(binding.dialogMapMarker.mapInfoCardView, true)
                                val anim =
                                    AnimationUtils.loadAnimation(
                                        this@MapsActivity,
                                        R.anim.enter_bottom
                                    )
                                anim.duration = 500
                                anim.setAnimationListener(object : Animation.AnimationListener {
                                    override fun onAnimationRepeat(animation: Animation?) {}

                                    override fun onAnimationEnd(animation: Animation?) {
                                        visibility(binding.dialogMapMarker.mapInfoCardView, true)
                                    }

                                    override fun onAnimationStart(animation: Animation?) {
                                        visibility(binding.dialogMapMarker.mapInfoCardView, true)
                                    }
                                })
                                binding.dialogMapMarker.mapInfoCardView.startAnimation(anim)
                                return@setOnMapClickListener
                            }
                        }

                        Timber.v("There are ${this@MapsActivity.polygons}")
                        for (polygon in this@MapsActivity.polygons) {
                            if (polygon.windowData.title != null)
                                if (polygonContains(position, polygon.points)) {
                                    Timber.v("Point is contained somewhere!")
                                    val pos = computeCentroid(polygon.points)
                                    val textOverlay = GroundOverlayOptions()
                                        .image(
                                            BitmapDescriptorFactory.fromBitmap(
                                                textAsBitmap(
                                                    polygon.windowData.title!!,
                                                    2.0f,
                                                    Color.RED
                                                )
                                            )
                                        )
                                        .position(pos, 8600f)
                                    googleMap.addGroundOverlay(textOverlay)
                                }
                        }

                        markerWindow?.hide()
                        markerWindow = null
                    }

                    setOnInfoWindowClickListener { marker ->
                        val dataClass = MapHelper.getTarget(marker)
                        val scan = dataClass?.let { AREAS.find(it) }

                        Timber.v("Marker Title: ${marker.title}")
                        Timber.v("Clicked info window! Data Class: $dataClass")
                        scan?.launchActivity(this@MapsActivity)
                            ?: Timber.w("Won't launch activity since dataClass is null")
                    }

                    setOnMarkerClickListener { marker ->
                        if (SETTINGS_CENTER_MARKER_PREF.get(sharedPreferences))
                            googleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))

                        if (marker.title != null && marker.title.isNotEmpty()) {
                            markerWindow = MapHelper.infoCard(this@MapsActivity, marker, binding.dialogMapMarker)

                            true
                        } else
                            false
                    }

                    if (kmzFile != null)
                        binding.mapDownloadedImageView.visibility = View.VISIBLE

                    if (mapData != null) {
                        Timber.v("Got map data")
                        val bounds = LatLngBounds.Builder()
                        val items = mapData as ArrayList<*>
                        when {
                            items.size > 1 -> {
                                Timber.v("  Multiple points")
                                for (item in items)
                                    if (item is GeoMarker) {
                                        item.addToMap(googleMap)
                                        bounds.include(item.position.toLatLng())
                                    } else Timber.e("  Item is not GeoMarker")
                                googleMap.moveCamera(
                                    CameraUpdateFactory.newLatLngBounds(
                                        bounds.build(),
                                        50
                                    )
                                )
                            }
                            items.size > 0 -> items.first().let { item ->
                                Timber.v("  Only one point")
                                if (item is GeoMarker) {
                                    Timber.v("  Adding marker and moving camera")
                                    item.addToMap(googleMap)
                                    googleMap.moveCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            item.position.toLatLng(),
                                            15.0F
                                        )
                                    )
                                } else Timber.e("  Item is not MarkerOptions")
                            }
                            else -> Timber.e("  Could not get items")
                        }
                    }

                    binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_not_fixed_24)
                    binding.fabCurrentLocation.setOnLongClickListener {
                        allPoints().let {
                            if (it.size > 1)
                                googleMap.moveCamera(
                                    newLatLngBounds(
                                        it,
                                        resources.getInteger(R.integer.marker_padding)
                                    )
                                )
                        }
                        true
                    }

                    Timber.v("Moving Camera")
                    allPoints().let {
                        if (it.size > 1)
                            googleMap.moveCamera(
                                newLatLngBounds(
                                    it,
                                    resources.getInteger(R.integer.marker_padding)
                                )
                            )
                    }
                }
            }
        }
        if (kmlAddress != null || kmzFile != null)
            loadData(networkState) {
                innerLoad()
            }
        else innerLoad()
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
                    !(ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                            )
                ) {
                    googleMap?.isMyLocationEnabled = true
                } else {
                    toast(R.string.toast_location_not_shown)
                    vibrate(this, 20)
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
                        } catch (e: Exception) {
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
                            } catch (e: Exception) {
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
                            } catch (e: Exception) {
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
        if (!hasLocationPermission(this)) {
            googleMap?.isMyLocationEnabled = false

            binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_off_24)
            binding.fabCurrentLocation.setOnClickListener {
                tryToShowCurrentLocation()
            }

            val bottomNavigationDrawerFragment =
                BottomPermissionAskerFragment(
                    this@MapsActivity,
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
            googleMap?.isMyLocationEnabled = true
            googleMap?.uiSettings?.isMyLocationButtonEnabled = false

            binding.fabCurrentLocation.setOnClickListener {
                if (lastKnownLocation != null) {
                    val position = lastKnownLocation!!.toLatLng()
                    Timber.d("Moving camera to current location ($position)...")
                    movingCamera = true
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(
                                position,
                                17f
                            )
                        )
                    ) ?: Timber.e("GoogleMap is null!")
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
        try {
            if (hasLocationPermission(this)) {
                val locationResult = fusedLocationProviderClient?.lastLocation
                Timber.v("Adding complete listener")
                locationResult?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        Timber.d("Got new location: $lastKnownLocation")
                        binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_not_fixed_24)
                    } else {
                        Timber.d("Current location is null. Using defaults.")
                        Timber.e(task.exception, "Exception:")
                        binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_off_24)
                    }
                } ?: Timber.e("fusedLocationProviderClient is null")
                binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_off_24)
            }
        } catch (e: SecurityException) {
            Timber.e(e, "Exception:")
            binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_off_24)
        }
    }

    private fun loadData(
        networkState: ConnectivityProvider.NetworkState,
        finishedListener: () -> Unit
    ) {
        Timber.v("Found KML/KMZ to load")
        val loader = KMLLoader(kmlAddress, kmzFile)
        if (googleMap != null) {
            loader.load(this, googleMap!!, networkState, { result ->
                Timber.v("  Loaded KML!")
                markers.clear()
                polygons.clear()
                polylines.clear()

                markers.addAll(result.markers)
                polygons.addAll(result.polygons)
                polylines.addAll(result.polylines)

                finishedListener()
            }, { error ->
                Timber.e(error, "  Could not load!")
            })
        } else
            Timber.e("MapboxMap is null!")
    }
}