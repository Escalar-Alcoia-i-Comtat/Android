package com.arnyminerz.escalaralcoiaicomtat.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageAppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.map.GeoGeometry
import com.arnyminerz.escalaralcoiaicomtat.core.data.map.GeoMarker
import com.arnyminerz.escalaralcoiaicomtat.core.data.map.MAP_LOAD_PADDING
import com.arnyminerz.escalaralcoiaicomtat.core.data.map.getWindow
import com.arnyminerz.escalaralcoiaicomtat.core.notification.DOWNLOAD_COMPLETE_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_CENTER_CURRENT_LOCATION
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_KMZ_FILE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.INFO_VIBRATION
import com.arnyminerz.escalaralcoiaicomtat.core.shared.LOCATION_PERMISSION_REQUEST_CODE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.MAP_GEOMETRIES_BUNDLE_EXTRA
import com.arnyminerz.escalaralcoiaicomtat.core.shared.MAP_MARKERS_BUNDLE_EXTRA
import com.arnyminerz.escalaralcoiaicomtat.core.shared.MIME_TYPE_GPX
import com.arnyminerz.escalaralcoiaicomtat.core.shared.MIME_TYPE_KMZ
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PERMISSION_DIALOG_TAG
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_CENTER_MARKER_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.shared.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.fileName
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getParcelableList
import com.arnyminerz.escalaralcoiaicomtat.core.utils.isLocationPermissionGranted
import com.arnyminerz.escalaralcoiaicomtat.core.utils.maps.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.core.utils.maps.MapNotInitializedException
import com.arnyminerz.escalaralcoiaicomtat.core.utils.maps.TrackMode
import com.arnyminerz.escalaralcoiaicomtat.core.utils.mime
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityMapsBinding
import com.arnyminerz.escalaralcoiaicomtat.device.vibrate
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.BottomPermissionAskerFragment
import com.arnyminerz.escalaralcoiaicomtat.notification.Notification
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import timber.log.Timber
import java.io.File

class MapsActivity : LanguageAppCompatActivity() {

    private var markers = arrayListOf<GeoMarker>()
    private var geometries = arrayListOf<GeoGeometry>()

    private lateinit var mapHelper: MapHelper
    private lateinit var firestore: FirebaseFirestore

    private var markerWindow: MapHelper.MarkerWindow? = null

    private var showingPolyline: GeoGeometry? = null

    private lateinit var binding: ActivityMapsBinding

    val accessFolderRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val resultCode = result.resultCode

            if (resultCode != Activity.RESULT_OK) {
                Timber.w("Result code was not OK: $resultCode")
                return@registerForActivityResult
            }

            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            data?.data?.also { uri ->
                val mime = uri.mime(this)
                Timber.i("Uri: $uri. File name: ${uri.fileName(this)}. Mime: $mime")

                when (mime) {
                    MIME_TYPE_GPX -> {
                        mapHelper.export?.storeGPX(this, uri)
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
                        mapHelper.export?.storeKMZ(this, uri)
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
                sharedPreferences.getParcelableList<GeoMarker>(MAP_MARKERS_BUNDLE_EXTRA, listOf())
            markers.addAll(markersList)

            Timber.d("Getting geometries list...")
            val geometriesList = sharedPreferences.getParcelableList<GeoGeometry>(
                MAP_GEOMETRIES_BUNDLE_EXTRA,
                listOf()
            )
            geometries.addAll(geometriesList)

            Timber.d("Got ${markers.size} markers and ${geometries.size} geometries.")

            if (markersList.isEmpty() && geometriesList.isEmpty())
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
                    accessFolderRequest.launch(intent)
                    true
                } else
                    false
            }
            popup.show()
        }

        try {
            loadMap(savedInstanceState, kmzFile, centerCurrentLocation)
        } catch (e: IllegalStateException) {
            Timber.e(e, "Could not load map. Exitting activity")
            toast(R.string.toast_error_map_load)
            finish()
            return
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
                if ((grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) &&
                    isLocationPermissionGranted()
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

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    /**
     * Prepares the map configuration, initializes it, and loads the features.
     * @author Arnau Mora
     * @since 20210527
     * @param savedInstanceState The [Activity]'s saved instance state bundle.
     * @param kmzFile The KMZ file to load the features from.
     * @param centerCurrentLocation If the map should be centered in the current location.
     * @throws IllegalStateException When there was an unknown exception while initializing the map.
     * @see markers
     * @see geometries
     * @see MAP_LOAD_PADDING
     * @see mapHelper
     */
    @UiThread
    @Throws(IllegalStateException::class)
    private fun loadMap(
        savedInstanceState: Bundle?,
        kmzFile: File?,
        centerCurrentLocation: Boolean
    ) {
        mapHelper = MapHelper()
            .withMapFragment(this, R.id.map)
        mapHelper.onCreate(savedInstanceState)
        mapHelper = mapHelper
            .loadMap { map ->
                Timber.v("Map loaded successfully")
                doAsync {
                    val kmlResult = kmzFile?.let { loadKMZ(this@MapsActivity, it) }

                    if (kmlResult != null)
                        add(kmlResult)

                    // Add the features from the intent
                    Timber.v(
                        "Got ${markers.size} markers and ${geometries.size} geometries from intent."
                    )
                    addMarkers(markers)
                    addGeometries(geometries)

                    uiContext {
                        display()
                        center(MAP_LOAD_PADDING, includeCurrentLocation = centerCurrentLocation)
                    }
                }

                Timber.v("Loading current location")
                tryToShowCurrentLocation()

                map.uiSettings.isCompassEnabled = true

                map.setOnCameraMoveListener {
                    if (mapHelper.locationComponent?.lastKnownLocation != null)
                        binding.fabCurrentLocation.setImageResource(R.drawable.round_gps_not_fixed_24)
                }
                map.setOnMapClickListener {
                    showingPolyline = null

                    try {
                        markerWindow?.hide()
                    } catch (_: IllegalStateException) {
                    }
                    markerWindow = null
                }

                mapHelper.addMarkerClickListener {
                    if (SETTINGS_CENTER_MARKER_PREF.get())
                        mapHelper.move(position)
                    markerWindow?.hide()
                    val window = this.getWindow()
                    val title = window.title

                    if (title.isNotEmpty()) {
                        markerWindow = mapHelper.infoCard(
                            this@MapsActivity,
                            firestore,
                            this,
                            binding.root
                        ).also {
                            it.show()
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
        if (!this.isLocationPermissionGranted()) {
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
                try {
                    mapHelper.locationComponent?.enable(this)
                } catch (_: IllegalStateException) {
                    Timber.w("The GPS is disabled.")
                    toast(R.string.toast_error_gps_disabled)
                }

                binding.fabCurrentLocation.setOnClickListener {
                    val locationComponent = mapHelper.locationComponent
                    val loc = locationComponent?.lastKnownLocation
                    if (loc != null) {
                        Timber.d("Moving camera to current location ($loc)...")
                        locationComponent.trackMode = TrackMode.ANIMATED
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
