package com.arnyminerz.escalaralcoiaicomtat.activity

import android.content.Intent
import android.os.Bundle
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Zone
import com.arnyminerz.escalaralcoiaicomtat.data.climb.types.DownloadStatus
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityUpdatingBinding
import com.arnyminerz.escalaralcoiaicomtat.device.vibrate
import com.arnyminerz.escalaralcoiaicomtat.generic.IntentExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.getExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.runAsync
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import timber.log.Timber
import java.io.IOException

private const val ERROR_VIBRATE: Long = 500

@ExperimentalUnsignedTypes
val UPDATE_AREA = IntentExtra<Area>("update_area")

@ExperimentalUnsignedTypes
val UPDATE_ZONE = IntentExtra<Zone>("update_zone")

@ExperimentalUnsignedTypes
val UPDATE_SECTOR = IntentExtra<Sector>("update_sector")

val UPDATE_IMAGES = IntentExtra<Boolean>("update_images")

val QUIET_UPDATE = IntentExtra<Boolean>("quiet_update")

@ExperimentalUnsignedTypes
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
                runAsync {
                    if (updateArea != null) // Update area
                        iterateUpdate(updateArea!!)
                    if (updateZone != null) // Update zone
                        iterateUpdate(updateZone!!)
                    if (updateSector != null) // Update sector
                        iterateUpdate(updateSector!!)
                }
            } catch (e: IOException) {
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

            for (area in AREAS.values) {
                if (area.isDownloaded(this) == DownloadStatus.DOWNLOADED)
                    iterateUpdate(area)
                else for (zone in area)
                    if (zone.isDownloaded(this) == DownloadStatus.DOWNLOADED)
                        iterateUpdate(zone)
                    else for (sector in zone)
                        if (sector.isDownloaded(this) == DownloadStatus.DOWNLOADED)
                            iterateUpdate(sector)
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
    private fun iterateUpdate(dataClass: DataClass<*, *>) {
        Timber.v("Deleting area #$updateArea...")
        dataClass.delete(this@UpdatingActivity)

        Timber.v("Downloading area #$updateArea...")
        dataClass.download(this@UpdatingActivity, true, { /* Start */ }, {
            // Finish
            updatesCounter++
            if (updatesCounter >= updatesToDo)
                if (quietUpdate) {
                    Timber.v("Finished downloading everything, quiet mode enabled, won't launch anything...")
                } else {
                    Timber.v("Finished downloading everything, launching MainActivity...")
                    startActivity(Intent(this, MainActivity::class.java))
                }
        }, { progress, max ->
            // Progress
            runOnUiThread {
                binding.progressBar.isIndeterminate = false
                binding.progressBar.max = max
                binding.progressBar.progress = progress
            }
        }, {
            // Error
            runOnUiThread {
                vibrate(this@UpdatingActivity, ERROR_VIBRATE)
                visibility(binding.progressBar, false, setGone = false)
                binding.updatingTextView.setText(R.string.update_progress_error)
            }
        })
    }
}
