package com.arnyminerz.escalaralcoiaicomtat.activity.climb

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.annotation.DimenRes
import androidx.annotation.UiThread
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MapsActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.map.DEFAULT_LATITUDE
import com.arnyminerz.escalaralcoiaicomtat.core.data.map.DEFAULT_LONGITUDE
import com.arnyminerz.escalaralcoiaicomtat.core.data.map.DEFAULT_ZOOM
import com.arnyminerz.escalaralcoiaicomtat.core.exception.AlreadyLoadingException
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.shared.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.core.shared.exception_handler.handleStorageException
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.maps.MapAnyDataToLoadException
import com.arnyminerz.escalaralcoiaicomtat.core.utils.maps.MapHelper
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.hide
import com.arnyminerz.escalaralcoiaicomtat.core.view.show
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.databinding.LayoutListBinding
import com.arnyminerz.escalaralcoiaicomtat.paging.DataClassAdapter
import com.arnyminerz.escalaralcoiaicomtat.paging.DataClassComparator
import com.arnyminerz.escalaralcoiaicomtat.view.model.DataClassListViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.ktx.storage
import timber.log.Timber
import java.io.FileNotFoundException

/**
 * @author Arnau Mora
 * @since 20210815
 * @param T The [DataClass] type that is being displayed.
 * @param C The children type of [T].
 * @param B Should match [T].
 * @param itemsPerRow The amount of items that should be on each row of the list.
 * @param itemHeight The height of the items of the list.
 */
abstract class DataClassListActivity<C : DataClass<*, *>, B : DataClassImpl, T : DataClass<C, B>>(
    private val itemsPerRow: Int,
    @DimenRes private val itemHeight: Int
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
    internal lateinit var dataClass: T

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

    /**
     * Stores if the Activity's content has just been attached.
     * @author Arnau Mora
     * @since 20210815
     */
    internal var justAttached = false

    /**
     * Stores if the content has been loaded completely.
     * @author Arnau Mora
     * @since 20210815
     */
    internal var loaded = false

    /**
     * Stores the current position of the list for recovering it just in case it's necessary.
     * @author Arnau Mora
     * @since 20210815
     */
    internal var position = 0

    /**
     * Stores the name for showing transitions in the title.
     * @author Arnau Mora
     * @since 20210815
     */
    internal var transitionName: String? = null

    /**
     * Tells if the [dataClass] has been initialized.
     * @author Arnau Mora
     * @since 20210815
     */
    internal val viewModelInitialized: Boolean
        get() = this::viewModel.isInitialized

    /**
     * Stores whether or not the map has been loaded.
     * @author Arnau Mora
     * @since 20210818
     */
    private var mapLoaded: Boolean = false

    /**
     * Stores the [DataClassListActivity]'s ViewModel.
     */
    lateinit var viewModel: DataClassListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firestore = Firebase.firestore
        storage = Firebase.storage
        mapHelper = MapHelper()

        binding = LayoutListBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        mapHelper = mapHelper.withMapFragment(this, R.id.map)
        mapHelper.onCreate(savedInstanceState)

        binding.backImageButton.setOnClickListener { onBackPressed() }
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
        if (this::mapHelper.isInitialized && mapHelper.isLoaded && mapLoaded) {
            binding.loadingIndicator.hide()
            binding.mapProgressBarCard.hide()
        }
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
        binding.mapProgressBarCard.show()
        visibility(binding.mapProgressBar, false)
        binding.mapProgressBar.isIndeterminate = true
        visibility(binding.mapProgressBar, true)

        updateList()
        updateIcon()
        loadMap()
    }

    /**
     * This will get called when the user wants to navigate.
     * @author Arnau Mora
     * @since 20210815
     * @param transitionName The transition name of the clicked item
     * @param objectId The ID of the object that should be launched.
     * @return The [Intent] that should be launched with the data required for the target [Activity].
     */
    abstract fun intentExtra(transitionName: String?, objectId: String): Intent

    /**
     * Updates the status icon at the actionbar.
     * @author Arnau Mora
     * @since 20210604
     */
    @UiThread
    private fun updateIcon() {
        val i = binding.statusImageView
        binding.statusImageView.hide(setGone = false)
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
                dataClass.downloadStatus(app, app.searchSession, storage)
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

    /**
     * Loads the map.
     * @author Arnau Mora
     * @since 20210818
     */
    private fun loadMap() {
        val hasInternet = appNetworkState.hasInternet
        val initializationCheck = this::dataClass.isInitialized && this::mapHelper.isInitialized
        if (initializationCheck && !mapHelper.isLoaded && hasInternet) {
            Timber.v("Loading map...")
            mapHelper
                .show()
                .withStartingPosition(LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE), DEFAULT_ZOOM)
                .withControllable(false)
                .loadMap { map ->
                    try {
                        doAsync {
                            Timber.v("Getting KMZ file...")
                            val kmzFile = dataClass.kmzFile(
                                this@DataClassListActivity,
                                storage,
                                false
                            ) {
                                binding.mapProgressBar.progress = it.percentage()
                                binding.mapProgressBar.max = 100
                            }
                            uiContext {
                                visibility(binding.mapProgressBar, false)
                                binding.mapProgressBar.isIndeterminate = true
                                visibility(binding.mapProgressBar, true)
                            }
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
                                binding.mapLayout.show()
                                binding.mapProgressBarCard.hide()

                                map.setOnMapClickListener {
                                    try {
                                        val intent = mapHelper.mapsActivityIntent(
                                            this@DataClassListActivity,
                                            MapsActivity::class.java
                                        )
                                        Timber.v("Starting MapsActivity...")
                                        startActivity(intent)
                                    } catch (_: MapAnyDataToLoadException) {
                                        Timber.w("Clicked on map and any data has been loaded")
                                    }
                                }
                            }
                            mapLoaded = true
                        }
                    } catch (_: FileNotFoundException) {
                        Timber.w("KMZ file not found")
                        binding.map.hide()
                        binding.mapLayout.hide()
                        binding.mapProgressBarCard.hide()
                    } catch (e: IllegalStateException) {
                        Firebase.crashlytics.recordException(e)
                        Timber.w("The DataClass ($dataClass) does not contain a KMZ address")
                        toast(R.string.toast_error_no_kmz)
                        binding.map.hide()
                        binding.mapLayout.hide()
                        binding.mapProgressBarCard.hide()
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
            binding.mapProgressBarCard.hide()
            binding.mapLayout.hide()
            if (this::mapHelper.isInitialized)
                mapHelper.hide()
        } else
            binding.mapProgressBarCard.hide()
    }

    private fun updateList() {
        if (!loaded && viewModelInitialized) {
            binding.titleTextView.text = dataClass.displayName
            binding.titleTextView.transitionName = transitionName

            try {
                if (mapLoaded)
                    binding.mapProgressBarCard.hide()
                binding.recyclerView.layoutManager = if (itemsPerRow > 1)
                    GridLayoutManager(this, itemsPerRow)
                else
                    LinearLayoutManager(this)
                if (justAttached)
                    binding.recyclerView.layoutAnimation =
                        AnimationUtils.loadLayoutAnimation(
                            this,
                            R.anim.item_enter_left_animator
                        )
                val adapter = DataClassAdapter(
                    itemsPerRow,
                    false,
                    itemHeight,
                    { binding, position, item ->
                        this.binding.loadingIndicator.show()

                        Timber.v("Clicked item $position")
                        val trn = ViewCompat.getTransitionName(binding.titleTextView)
                        Timber.v("Transition name: $trn")
                        val intent = intentExtra(trn, item.objectId)
                        val options = if (trn != null)
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                this, binding.titleTextView, trn
                            )
                        else null

                        startActivity(intent, options?.toBundle())
                    },
                    DataClassComparator()
                )
                binding.recyclerView.adapter = adapter
                binding.recyclerView.scrollToPosition(position)

                loaded = true
            } catch (_: AlreadyLoadingException) {
                // Ignore an already loading exception. The content will be loaded from somewhere else
                Timber.v(
                    "An AlreadyLoadingException has been thrown while loading the zones in ZoneActivity."
                ) // Let's just warn the debugger this is controlled
            }
        } else
            Timber.w("DataClass not initialized!")
    }
}
