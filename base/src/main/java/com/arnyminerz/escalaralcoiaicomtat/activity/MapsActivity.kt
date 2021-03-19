package com.arnyminerz.escalaralcoiaicomtat.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageAppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.data.map.*
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityMapsBinding
import com.arnyminerz.escalaralcoiaicomtat.device.vibrate
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.BottomPermissionAskerFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_CENTER_MARKER_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.*
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toLatLng
import com.arnyminerz.escalaralcoiaicomtat.notification.DOWNLOAD_COMPLETE_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.notification.Notification
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.parse.ParseAnalytics
import timber.log.Timber
import java.io.File

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
val CENTER_CURRENT_LOCATION_EXTRA = IntentExtra<Boolean>("CenterLocation")

const val MIME_TYPE_KML = "application/vnd.google-earth.kml+xml"
const val MIME_TYPE_KMZ = "application/vnd.google-earth.kmz"
const val MIME_TYPE_GPX = "application/gpx+xml"

class MapsActivity : LanguageAppCompatActivity() {

    private var zoneName: String? = null
    private var markers = arrayListOf<GeoMarker>()
    private var geometries = arrayListOf<GeoGeometry>()
    private var iconSizeMultiplier = ICON_SIZE_MULTIPLIER

    private lateinit var mapHelper: MapHelper

    private var markerWindow: MapHelper.MarkerWindow? = null
    private var markerName: String? = null

    private var showingPolyline: GeoGeometry? = null

    private var movingCamera: Boolean = false

    private lateinit var binding: ActivityMapsBinding

    private var locationManager: LocationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        ParseAnalytics.trackAppOpenedInBackground(intent)

        // Hi from march of 2021
        Timber.v("Getting Mapbox instance...")
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        var kmlAddress: String? = null
        var kmzFile: File? = null
        var centerCurrentLocation: Boolean = false
        if (intent != null) {
            Timber.d("Getting markers list...")
            val markersList =
                intent.getParcelableArrayListExtra<GeoMarker>(MAP_MARKERS_BUNDLE_EXTRA)
            markersList?.let { markers.addAll(it) }
            Timber.d("Getting geometries list...")
            val geometriesList =
                intent.getParcelableArrayListExtra<GeoGeometry>(MAP_GEOMETRIES_BUNDLE_EXTRA)
            geometriesList?.let { geometries.addAll(it) }
            Timber.d("Got ${markers.size} markers and ${geometries.size} geometries.")

            iconSizeMultiplier =
                intent.getExtra(ICON_SIZE_MULTIPLIER_BUNDLE_EXTRA) ?: ICON_SIZE_MULTIPLIER

            kmlAddress = intent.getExtra(KML_ADDRESS_BUNDLE_EXTRA)
            zoneName = intent.getExtra(ZONE_NAME_BUNDLE_EXTRA)
            intent.getExtra(KMZ_FILE_BUNDLE_EXTRA)
                .let { path -> if (path != null) kmzFile = File(path) }
            centerCurrentLocation = intent.getExtra(CENTER_CURRENT_LOCATION_EXTRA, false)
        } else
            Timber.w("Intent is null")

        binding.floatingActionButton.setOnClickListener {
            onBackPressed()
        }

        binding.mapDownloadedImageView.visibility = View.GONE

        binding.fabDownload.setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.menuInflater.inflate(R.menu.menu_map_download, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                var mime: String? = null
                var extension: String? = null
                when (item.itemId) {
                    R.id.export_kmz -> {
                        mime = MIME_TYPE_KMZ
                        extension = "kmz"
                    }
                    R.id.export_gpx -> {
                        mime = MIME_TYPE_GPX
                        extension = "gpx"
                    }
                    else -> Timber.w("Unkown item clicked")
                }
                if (mime != null) {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        // Filter to only show results that can be "opened", such as
                        // a file (as opposed to a list of contacts or timezones).
                        addCategory(Intent.CATEGORY_OPENABLE)

                        // Create a file with the requested MIME type.
                        type = mime
                        putExtra(
                            Intent.EXTRA_TITLE,
                            "*.$extension"
                        )
                    }
                    startActivityForResult(intent, FOLDER_ACCESS_PERMISSION_REQUEST_CODE)
                    true
                } else
                    false
            }
            popup.show()
        }

        mapHelper = MapHelper(binding.map)
        mapHelper.onCreate(savedInstanceState)
        mapHelper
            .withIconSizeMultiplier(iconSizeMultiplier)
            .loadMap(this) { _, map, _ ->
                runAsync {
                    if (kmlAddress != null) {
                        Timber.v("Loading KML...")
                        mapHelper.loadKML(this, kmlAddress)
                    }

                    runOnUiThread {
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
                                mapHelper.move(latLng)
                            markerWindow?.hide()
                            val window = getWindow()
                            val title = window.title

                            if (title.isNotEmpty()) {
                                markerWindow = mapHelper.infoCard(
                                    this@MapsActivity,
                                    this,
                                    view
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
                        mapHelper.center(
                            MAP_LOAD_PADDING,
                            includeCurrentLocation = centerCurrentLocation
                        )

                        binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_not_fixed_24)
                    }
                }
            }
    }

    override fun onStart() {
        super.onStart()
        mapHelper.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapHelper.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapHelper.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapHelper.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapHelper.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapHelper.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapHelper.onDestroy()
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
                val mime = uri.mime(this)
                Timber.i("Uri: $uri. File name: ${uri.fileName(this)}. Mime: $mime")

                when (mime) {
                    MIME_TYPE_GPX -> {
                        mapHelper.storeGPX(this, uri)
                        val notificationBuilder = Notification.Builder(this)
                            .withChannelId(DOWNLOAD_COMPLETE_CHANNEL_ID)
                            .withTitle(R.string.notification_gpx_stored_title)
                            .withText(R.string.notification_gpx_stored_message)
                            .withIcon(R.drawable.ic_notifications)
                            .withIntent(
                                PendingIntent.getActivity(
                                    this,
                                    0,
                                    Intent().apply {
                                        action = Intent.ACTION_VIEW
                                        setDataAndType(uri, mime)
                                    },
                                    PendingIntent.FLAG_IMMUTABLE
                                )
                            )
                        Timber.d("Notification title: ${notificationBuilder.title}")
                        val notification = notificationBuilder.build()
                        notification.show()
                        toast(R.string.toast_stored_gpx)
                    }
                    MIME_TYPE_KMZ -> {
                        mapHelper.storeKMZ(this, uri)
                        val notificationBuilder = Notification.Builder(this)
                            .withChannelId(DOWNLOAD_COMPLETE_CHANNEL_ID)
                            .withTitle(R.string.notification_kmz_stored_title)
                            .withText(R.string.notification_kmz_stored_message)
                            .withIcon(R.drawable.ic_notifications)
                            .withIntent(
                                PendingIntent.getActivity(
                                    this,
                                    0,
                                    Intent().apply {
                                        action = Intent.ACTION_VIEW
                                        setDataAndType(uri, mime)
                                    },
                                    PendingIntent.FLAG_IMMUTABLE
                                )
                            )
                        Timber.d("Notification title: ${notificationBuilder.title}")
                        val notification = notificationBuilder.build()
                        notification.show()
                        toast(R.string.toast_stored_kmz)
                    }
                    else -> Timber.w("Got unkown mime: $mime")
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
                    Timber.w("No known location!")
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
}
