package com.arnyminerz.escalaralcoiaicomtat.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.content.FileProvider
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Zone
import com.arnyminerz.escalaralcoiaicomtat.device.vibrate
import com.arnyminerz.escalaralcoiaicomtat.fragment.intro.DownloadAreasIntroFragment
import com.arnyminerz.escalaralcoiaicomtat.generic.IntentExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.deleteIfExists
import com.arnyminerz.escalaralcoiaicomtat.generic.getExtra
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_updating.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import timber.log.Timber
import java.io.File

val DOWNLOAD_APK = IntentExtra<String>("apkUrl")

val UPDATE_AREA = IntentExtra<Int>("update_area")
val UPDATE_ZONE = IntentExtra<Int>("update_zone")
val UPDATE_SECTOR = IntentExtra<Int>("update_sector")

val UPDATE_CACHE = IntentExtra<Boolean>("update_cache")
val UPDATE_IMAGES = IntentExtra<Boolean>("update_images")

val QUIET_UPDATE = IntentExtra<Boolean>("quiet_update")

@ExperimentalUnsignedTypes
class UpdatingActivity : NetworkChangeListenerActivity() {
    private var updateArea: Int? = null // Sets the area id to update (re-download images)
    private var updateZone: Int? = null // Sets the zone id to update (re-download images)
    private var updateSector: Int? = null // Sets the sector id to update (re-download images)
    private var updateCache: Boolean? =
        null // If this is true, the cache.json file will be re-downloaded
    private var updateDownloads: Boolean? =
        null // If this is true, all the downloaded images will be downloaded again
    private var downloadAPKUrl: String? = null

    /**
     * If true, app won't be launched when finished updating.
     */
    private var quietUpdate: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_updating)

        if (intent == null || intent.extras == null) {
            Timber.e("Intent doesn't have data")
            onBackPressed()
            return
        }

        updateArea = intent.getExtra(UPDATE_AREA)
        updateZone = intent.getExtra(UPDATE_ZONE)
        updateSector = intent.getExtra(UPDATE_SECTOR)
        downloadAPKUrl = intent.getExtra(DOWNLOAD_APK)
        updateCache = intent.getExtra(UPDATE_CACHE)
        updateDownloads = intent.getExtra(UPDATE_IMAGES)
        quietUpdate = intent.getExtra(QUIET_UPDATE) ?: false

        if (updateDownloads == true)
            updateCache = true // If downloads should be updated, updateCache also should be ran
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        val hasInternet = state.hasInternet
        val wifiConnected = state.wifiConnected

        if (!hasInternet) {
            progressBar.isIndeterminate = true
            progress_textView.setText(R.string.update_progress_internet_waiting)
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
        val hasInternet = networkState.hasInternet

        if (updateCache == true) { // Cache should be re-downloaded
            Timber.v("Has to update cache.json")

            val cacheFile = IntroActivity.cacheFile(this) // First delete the cache file
            progress_textView.setText(R.string.update_progress_deleting_old_cache)
            Timber.v("Deleting the cache file")
            cacheFile.deleteIfExists()

            Timber.v("Downloading newest data...")
            progress_textView.setText(R.string.update_progress_downloading_new_cache)
            progressBar.isIndeterminate = true
            DownloadAreasIntroFragment.downloadAreasCache(this, null, null, {
                // On finished
                Timber.v("  Device data updated!")

                if (updateDownloads == true) {
                    // TODO: Update downloads
                }
            }, { message -> // On error
                vibrate(this, 500)
                visibility(progressBar, false, setGone = false)
                updating_textView.setText(R.string.update_progress_error)
                progress_textView.setText(message)
            })
        }

        if (updateArea != null || updateZone != null || updateSector != null) { // Cache should be re-downloaded
            Timber.v("Has to update - updateArea:$updateArea updateZone:$updateZone updateSector:$updateSector")

            val cacheFile = IntroActivity.cacheFile(this)
            progress_textView.setText(R.string.update_progress_deleting_old_cache)
            Timber.v("Deleting the cache file")
            cacheFile.deleteIfExists()

            Timber.v("Downloading newest data...")
            progress_textView.setText(R.string.update_progress_downloading_new_cache)
            progressBar.isIndeterminate = true
            DownloadAreasIntroFragment.downloadAreasCache(this, null, null, {
                // Download complete
                val updatesToDo = 0 +
                        (if (updateArea != null) 1 else 0) +
                        (if (updateZone != null) 1 else 0) +
                        (if (updateSector != null) 1 else 0)
                var updatesCounter = 0

                fun iterateUpdate(dataClass: DataClass<*, *>) {
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
                        progressBar.isIndeterminate = false
                        progressBar.max = max
                        progressBar.progress = progress
                    }, {
                        // Error
                        vibrate(this@UpdatingActivity, 500)
                        visibility(progressBar, false, setGone = false)
                        updating_textView.setText(R.string.update_progress_error)
                    })
                }

                GlobalScope.launch {
                    if (updateArea != null) // Update area
                        iterateUpdate(Area.fromId(updateArea!!))
                    if (updateZone != null) // Update zone
                        iterateUpdate(Zone.fromId(updateZone!!))
                    if (updateSector != null) // Update sector
                        iterateUpdate(Sector.fromId(updateSector!!))
                }
            }, { message -> // On error
                vibrate(this, 500)
                visibility(progressBar, false, setGone = false)
                updating_textView.setText(R.string.update_progress_error)
                progress_textView.setText(message)
            })
            return
        }

        when {
            downloadAPKUrl != null -> {
                progressBar.isIndeterminate = true
                doAsync {
                    if (hasInternet) {
                        val storage = Firebase.storage

                        val apkFile = File(cacheDir, "update.apk")
                        if (apkFile.exists())
                            apkFile.delete()

                        val gsRef = storage.getReferenceFromUrl(downloadAPKUrl!!)
                        gsRef.getFile(apkFile).addOnSuccessListener {
                            if (apkFile.exists()) {
                                val apkURI = if (Build.VERSION.SDK_INT >= 24)
                                    FileProvider.getUriForFile(
                                        this@UpdatingActivity,
                                        applicationContext.packageName.toString() + ".provider",
                                        apkFile
                                    )
                                else
                                    Uri.fromFile(apkFile)

                                val install = Intent(Intent.ACTION_VIEW, apkURI)
                                install.setDataAndType(
                                    apkURI,
                                    "application/vnd.android.package-archive"
                                )

                                install.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                                install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)

                                Timber.v("File ready, launching installation")
                                startActivity(install)
                                finishAffinity()
                            } else {
                                Timber.e("APK file doesn't exist.")
                                toast(R.string.toast_error_internal)
                                finishAffinity()
                            }
                        }.addOnFailureListener {
                            Timber.e("Could not download: $it")
                            toast(R.string.toast_error_internal)
                            finishAffinity()
                        }.addOnProgressListener { taskSnapshot ->
                            val max = taskSnapshot.totalByteCount
                            val progress = taskSnapshot.bytesTransferred
                            progressBar.isIndeterminate = false
                            progressBar.max = max.toInt()
                            progressBar.progress = progress.toInt()
                        }
                    } else {
                        Timber.e("Internet connection not available")
                        toast(R.string.toast_error_no_internet)
                        finishAffinity()
                    }
                }
            }
            else -> onBackPressed()
        }
    }

}