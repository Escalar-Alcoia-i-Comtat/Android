package com.arnyminerz.escalaralcoiaicomtat.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.PopupMenu
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageAppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.data.map.GeoGeometry
import com.arnyminerz.escalaralcoiaicomtat.data.map.GeoMarker
import com.arnyminerz.escalaralcoiaicomtat.data.map.ICON_SIZE_MULTIPLIER
import com.arnyminerz.escalaralcoiaicomtat.data.map.MAP_LOAD_PADDING
import com.arnyminerz.escalaralcoiaicomtat.data.map.getWindow
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityMapsBinding
import com.arnyminerz.escalaralcoiaicomtat.device.vibrate
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.BottomPermissionAskerFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_CENTER_MARKER_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.generic.MapNotInitializedException
import com.arnyminerz.escalaralcoiaicomtat.generic.doAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.fileName
import com.arnyminerz.escalaralcoiaicomtat.generic.getExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.mime
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.generic.uiContext
import com.arnyminerz.escalaralcoiaicomtat.notification.DOWNLOAD_COMPLETE_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.notification.Notification
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_CENTER_CURRENT_LOCATION
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_ICON_SIZE_MULTIPLIER
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_KMZ_FILE
import com.arnyminerz.escalaralcoiaicomtat.shared.FOLDER_ACCESS_PERMISSION_REQUEST_CODE
import com.arnyminerz.escalaralcoiaicomtat.shared.INFO_VIBRATION
import com.arnyminerz.escalaralcoiaicomtat.shared.LOCATION_PERMISSION_REQUEST_CODE
import com.arnyminerz.escalaralcoiaicomtat.shared.MAP_GEOMETRIES_BUNDLE_EXTRA
import com.arnyminerz.escalaralcoiaicomtat.shared.MAP_MARKERS_BUNDLE_EXTRA
import com.arnyminerz.escalaralcoiaicomtat.shared.MIME_TYPE_GPX
import com.arnyminerz.escalaralcoiaicomtat.shared.MIME_TYPE_KMZ
import com.arnyminerz.escalaralcoiaicomtat.shared.PERMISSION_DIALOG_TAG
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import timber.log.Timber
import java.io.File

class MapsActivity : LanguageAppCompatActivity() {

    private var markers = arrayListOf<GeoMarker>()
    private var geometries = arrayListOf<GeoGeometry>()
    private var iconSizeMultiplier = ICON_SIZE_MULTIPLIER

    private lateinit var mapHelper: MapHelper
    private lateinit var firestore: FirebaseFirestore

    private var markerWindow: MapHelper.MarkerWindow? = null

    private var showingPolyline: GeoGeometry? = null

    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hi from march of 2021

        firestore = Firebase.firestore

        var kmzFile: File? = null
        var centerCurrentLocation = false
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
                intent.getExtra(EXTRA_ICON_SIZE_MULTIPLIER) ?: ICON_SIZE_MULTIPLIER

            kmzFile = intent.getExtra(EXTRA_KMZ_FILE)?.let { File(it) }
            centerCurrentLocation = intent.getExtra(EXTRA_CENTER_CURRENT_LOCATION, false)
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

        loadMap(savedInstanceState, kmzFile, centerCurrentLocation)
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
                    tryToShowCurrentLocation()
                else {
                    toast(R.string.toast_location_not_shown)
                    vibrate(this, INFO_VIBRATION)
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

    private fun loadMap(
        savedInstanceState: Bundle?,
        kmzFile: File?,
        centerCurrentLocation: Boolean
    ) {
        mapHelper = MapHelper(this)
        mapHelper.withMapView(binding.map)
        mapHelper.onCreate(savedInstanceState)
        mapHelper
            .withIconSizeMultiplier(iconSizeMultiplier)
            .loadMap(this) { _, map, _ ->
                Timber.v("Map loaded successfully")
                doAsync {
                    val kmlResult = kmzFile?.let { mapHelper.loadKMZ(this@MapsActivity, it) }

                    if (kmlResult != null)
                        mapHelper.add(kmlResult)

                    // Add the features from the intent
                    Timber.v(
                        "Got ${markers.size} markers and ${geometries.size} geometries from intent."
                    )
                    mapHelper.addMarkers(markers)
                    mapHelper.addGeometries(geometries)

                    uiContext {
                        mapHelper.display()
                        mapHelper.center(
                            MAP_LOAD_PADDING,
                            includeCurrentLocation = centerCurrentLocation
                        )
                    }
                }

                Timber.v("Loading current location")
                tryToShowCurrentLocation()

                map.uiSettings.apply {
                    isCompassEnabled = true
                    isDoubleTapGesturesEnabled = true
                }

                map.addOnMoveListener(object : MapboxMap.OnMoveListener {
                    override fun onMoveBegin(detector: MoveGestureDetector) {
                        if (mapHelper.lastKnownLocation != null)
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
                    if (SETTINGS_CENTER_MARKER_PREF.get())
                        mapHelper.move(latLng)
                    markerWindow?.hide()
                    val window = getWindow()
                    val title = window.title

                    if (title.isNotEmpty()) {
                        markerWindow = mapHelper.infoCard(
                            this@MapsActivity,
                            firestore,
                            this,
                            binding.root
                        ).also {
                            doAsync { it.show() }
                        }

                        true
                    } else
                        false
                }

                if (kmzFile != null)
                    binding.mapDownloadedImageView.visibility = View.VISIBLE

                binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_not_fixed_24)
            }
    }

    @SuppressLint("MissingPermission")
    private fun tryToShowCurrentLocation(): Boolean {
        var result = false
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
        } else {
            try {
                mapHelper.enableLocationComponent(this, cameraMode = CameraMode.NONE)

                binding.fabCurrentLocation.setOnClickListener {
                    val loc = mapHelper.lastKnownLocation
                    if (loc != null) {
                        Timber.d("Moving camera to current location ($loc)...")
                        mapHelper.track()
                        binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_fixed_24)
                    } else {
                        Timber.w("No known location!")
                        binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_off_24)
                    }
                }

                binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_off_24)

                result = true
            } catch (_: IllegalStateException) {
                Timber.d("GPS not enabled.")
                MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog)
                    .setTitle(R.string.dialog_gps_disabled_title)
                    .setMessage(R.string.dialog_gps_disabled_message)
                    .setPositiveButton(R.string.action_enable_location) { _, _ ->
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                    .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            } catch (_: MapNotInitializedException) {
                Timber.w("The map has not been initialized yet.")
            }
        }
        return result
    }
}
