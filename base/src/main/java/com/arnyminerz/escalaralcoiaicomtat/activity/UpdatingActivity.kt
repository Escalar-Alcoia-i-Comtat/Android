package com.arnyminerz.escalaralcoiaicomtat.activity

import android.os.Bundle
import androidx.annotation.WorkerThread
import androidx.work.WorkInfo
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ERROR_VIBRATE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.QUIET_UPDATE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.UPDATE_AREA
import com.arnyminerz.escalaralcoiaicomtat.core.shared.UPDATE_IMAGES
import com.arnyminerz.escalaralcoiaicomtat.core.shared.UPDATE_SECTOR
import com.arnyminerz.escalaralcoiaicomtat.core.shared.UPDATE_ZONE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityUpdatingBinding
import com.arnyminerz.escalaralcoiaicomtat.device.vibrate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import timber.log.Timber
import java.io.IOException

class UpdatingActivity : NetworkChangeListenerActivity() {
    private var updateArea: Area? = null // Sets the area id to update (re-download images)
    private var updateZone: Zone? = null // Sets the zone id to update (re-download images)
    private var updateSector: Sector? = null // Sets the sector id to update (re-download images)
    private var updateDownloads: Boolean? =
        null // If this is true, all the downloaded images will be downloaded again

    /**
     * If true, app won't be launched when finished updating.
     */
    private var quietUpdate: Boolean = false

    private lateinit var binding: ActivityUpdatingBinding

    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdatingBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (intent == null || intent.extras == null) {
            Timber.e("Intent doesn't have data")
            onBackPressed()
            return
        }

        storage = Firebase.storage

        updateArea = intent.getExtra(UPDATE_AREA)
        updateZone = intent.getExtra(UPDATE_ZONE)
        updateSector = intent.getExtra(UPDATE_SECTOR)
        updateDownloads = intent.getExtra(UPDATE_IMAGES)
        quietUpdate = intent.getExtra(QUIET_UPDATE) ?: false
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        val hasInternet = state.hasInternet
        val wifiConnected = state.wifiConnected

        if (!hasInternet) {
            binding.progressBar.isIndeterminate = true
            binding.progressTextView.setText(R.string.update_progress_internet_waiting)
            return
        }

        if (updateDownloads == true && !wifiConnected)
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_high_data_usage_title)
                .setMessage(R.string.dialog_high_data_usage_message)
                .setPositiveButton(R.string.action_continue) { dialog, _ ->
                    dialog.dismiss()
                    runActions()
                }
                .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                    dialog.dismiss()
                    onBackPressed()
                }
                .setCancelable(false)
                .show()
        else runActions()
    }

    private fun runActions() {
        if (updateArea != null || updateZone != null || updateSector != null) { // Cache should be re-downloaded
            Timber.v("Has to update - updateArea:$updateArea updateZone:$updateZone updateSector:$updateSector")

            Timber.v("Downloading newest data...")
            binding.progressTextView.setText(R.string.update_progress_downloading_new_cache)
            binding.progressBar.isIndeterminate = true
            try {
                doAsync {
                    if (updateArea != null) // Update area
                        iterateUpdate(updateArea!!)
                    if (updateZone != null) // Update zone
                        iterateUpdate(updateZone!!)
                    if (updateSector != null) // Update sector
                        iterateUpdate(updateSector!!)
                }
            } catch (e: IOException) {
                Timber.w(e, "Could not iterate update.")
                vibrate(this, ERROR_VIBRATE)
                visibility(binding.progressBar, false, setGone = false)
                binding.updatingTextView.setText(R.string.update_progress_error)
                binding.progressTextView.setText(R.string.toast_error_internal)
            }
            return
        }
        if (updateDownloads == true) {
            Timber.v("Updating downloads...")
            binding.progressTextView.setText(R.string.update_progress_downloading_new_cache)
            binding.progressBar.isIndeterminate = true

            doAsync {
                val app = application as App
                val areas = app.getAreas()
                for (area in areas) {
                    if (area.downloadStatus(app, storage).isDownloaded())
                        iterateUpdate(area)
                    else for (zone in area.getChildren(app))
                        if (zone.downloadStatus(app, storage).isDownloaded())
                            iterateUpdate(zone)
                        else for (sector in zone.getChildren(app))
                            if (sector.downloadStatus(app, storage)
                                    .isDownloaded()
                            )
                                iterateUpdate(sector)
                }
            }
        }

        onBackPressed()
    }

    // Download complete
    private val updatesToDo = 0 +
            (if (updateArea != null) 1 else 0) +
            (if (updateZone != null) 1 else 0) +
            (if (updateSector != null) 1 else 0)
    private var updatesCounter = 0

    @WorkerThread
    private suspend fun iterateUpdate(dataClass: DataClass<*, *>) {
        Timber.v("Deleting area #$updateArea...")
        dataClass.delete(app)

        Timber.v("Downloading area #$updateArea...")
        // TODO: The map won't be downloaded again
        val result = dataClass.download(this)
        uiContext {
            result.observe(this) { workInfo ->
                val state = workInfo.state
                val data = workInfo.outputData
                Timber.v("Current download status: ${workInfo.state}")
                if (state == WorkInfo.State.FAILED) {
                    vibrate(this, ERROR_VIBRATE)
                    visibility(binding.progressBar, false, setGone = false)
                    binding.updatingTextView.setText(R.string.update_progress_error)
                    Timber.w("Download failed! Error: ${data.getString("error")}")
                } else if (state == WorkInfo.State.SUCCEEDED) {
                    updatesCounter++
                    if (updatesCounter >= updatesToDo)
                        if (quietUpdate)
                            Timber.v("Finished downloading everything, quiet mode enabled, won't launch anything...")
                        else {
                            Timber.v("Finished downloading everything, launching MainActivity...")
                            launch(MainActivity::class.java)
                        }
                }
            }
        }
    }
}
