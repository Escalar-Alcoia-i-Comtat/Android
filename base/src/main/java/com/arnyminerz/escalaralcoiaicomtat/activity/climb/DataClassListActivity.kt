package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.os.Build
import android.os.Bundle
import androidx.annotation.UiThread
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.map.DEFAULT_LATITUDE
import com.arnyminerz.escalaralcoiaicomtat.data.map.DEFAULT_LONGITUDE
import com.arnyminerz.escalaralcoiaicomtat.data.map.DEFAULT_ZOOM
import com.arnyminerz.escalaralcoiaicomtat.databinding.LayoutListBinding
import com.arnyminerz.escalaralcoiaicomtat.generic.doAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.maps.MapAnyDataToLoadException
import com.arnyminerz.escalaralcoiaicomtat.generic.maps.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.generic.uiContext
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.shared.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.shared.exception_handler.handleStorageException
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.show
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.ktx.storage
import idroid.android.mapskit.factory.Maps
import timber.log.Timber
import java.io.FileNotFoundException

abstract class DataClassListActivity<T : DataClass<*, *>>(
    private val overrideLoadedMapData: Boolean = false,
) : NetworkChangeListenerActivity() {
    /**
     * The ViewBinding object for the Activity's view.
     * @author Arnau Mora
     * @since 20210604
     */
    protected lateinit var binding: LayoutListBinding

    /**
     * The [T] object that stores the data to show to the user.
     * @author Arnau Mora
     * @since 20210604
     */
    protected lateinit var dataClass: T

    /**
     * The [MapHelper] instance for doing map-stuff.
     * @author Arnau Mora
     * @since 20210604
     */
    private lateinit var mapHelper: MapHelper

    /**
     * The Firestore reference for getting data from the server.
     * @author Arnau Mora
     * @since 20210604
     */
    lateinit var firestore: FirebaseFirestore

    /**
     * The Firebase Storage reference for getting images and KMZs from the server.
     * @author Arnau Mora
     * @since 20210604
     */
    lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firestore = Firebase.firestore
        storage = Firebase.storage
        mapHelper = MapHelper()

        binding = LayoutListBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        mapHelper = mapHelper.withMapView(binding.map)
        mapHelper.onCreate(savedInstanceState ?: Bundle.EMPTY)

        binding.statusImageView.setOnClickListener { it.performLongClick() }
        updateIcon()
    }

    override fun onStart() {
        super.onStart()
        mapHelper.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapHelper.onResume()
        if (this::mapHelper.isInitialized && mapHelper.isLoaded)
            binding.loadingIndicator.hide()
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

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        val hasInternet = state.hasInternet
        visibility(binding.noInternetCard.noInternetCardView, !hasInternet)

        updateIcon()

        val initializationCheck = this::dataClass.isInitialized && this::mapHelper.isInitialized
        if (initializationCheck && !mapHelper.isLoaded && hasInternet) {
            Timber.v("Loading map...")
            mapHelper
                .show()
                .withStartingPosition(LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE), DEFAULT_ZOOM)
                .withControllable(false)
                .loadMap { _, map ->
                    try {
                        doAsync {
                            Timber.v("Getting KMZ file...")
                            val kmzFile =
                                dataClass.kmzFile(this@DataClassListActivity, storage, false)
                            Timber.v("Getting map features...")
                            val features = mapHelper.loadKMZ(this@DataClassListActivity, kmzFile)

                            if (features != null) {
                                Timber.v("Adding map features...")
                                mapHelper.add(features)
                            }

                            uiContext {
                                mapHelper.display()
                                mapHelper.center(animate = false)
                                binding.map.show()

                                map.setOnMapClickListener(object : Maps.MapClickListener {
                                    override fun onMapClick(point: LatLng) {
                                        try {
                                            val intent = mapHelper.mapsActivityIntent(
                                                this@DataClassListActivity,
                                                overrideLoadedMapData
                                            )
                                            Timber.v("Starting MapsActivity...")
                                            startActivity(intent)
                                        } catch (_: MapAnyDataToLoadException) {
                                            Timber.w("Clicked on map and any data has been loaded")
                                        }
                                    }
                                })
                            }
                        }
                    } catch (_: FileNotFoundException) {
                        Timber.w("KMZ file not found")
                        binding.map.hide()
                    } catch (e: IllegalStateException) {
                        Firebase.crashlytics.recordException(e)
                        Timber.w("The DataClass ($dataClass) does not contain a KMZ address")
                        toast(R.string.toast_error_no_kmz)
                        binding.map.hide()
                    } catch (e: StorageException) {
                        Firebase.crashlytics.recordException(e)
                        val handler = handleStorageException(e)
                        if (handler != null) {
                            Timber.e(e, handler.second)
                            toast(handler.first)
                        }
                    } finally {
                        binding.loadingIndicator.hide()
                    }
                }
        } else if (!hasInternet) {
            binding.loadingIndicator.hide()
            if (this::mapHelper.isInitialized)
                mapHelper.hide()
        }
    }

    /**
     * Updates the status icon at the actionbar.
     * @author Arnau Mora
     * @since 20210604
     */
    @UiThread
    private fun updateIcon() {
        val i = binding.statusImageView
        binding.statusImageView.hide(setGone = false)
        val activity = this
        val dataClassInitialized = this::dataClass.isInitialized
        doAsync {
            if (!appNetworkState.hasInternet)
                uiContext {
                    i.setImageResource(R.drawable.ic_round_signal_cellular_off_24)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        i.tooltipText = getString(R.string.status_no_internet)
                    i.show()
                }

            Timber.v("Updating icon, getting download status...")
            val downloadStatus = if (dataClassInitialized)
                dataClass.downloadStatus(activity, firestore)
            else null
            Timber.v("Got download status: $downloadStatus")

            uiContext {
                if (downloadStatus?.isDownloaded() == true) {
                    i.setImageResource(R.drawable.cloud_check)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        i.tooltipText = getString(R.string.status_downloaded)
                    i.show()
                } else if (!appNetworkState.hasInternet) {
                    i.setImageResource(R.drawable.ic_round_signal_cellular_off_24)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        i.tooltipText = getString(R.string.status_no_internet)
                    i.show()
                } else
                    i.hide(setGone = false)
            }
        }
    }
}
