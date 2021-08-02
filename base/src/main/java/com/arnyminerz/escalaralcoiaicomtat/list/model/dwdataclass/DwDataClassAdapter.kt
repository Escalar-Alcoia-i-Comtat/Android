package com.arnyminerz.escalaralcoiaicomtat.list.model.dwdataclass

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import androidx.collection.arrayMapOf
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MapsActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.DataClassListActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DownloadStatus
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DATACLASS_WAIT_CHILDREN_DELAY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_KMZ_FILE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.core.shared.exception_handler.handleStorageException
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.show
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.DownloadDialog
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.delay
import timber.log.Timber

class DwDataClassAdapter<T : DataClass<*, *>, P : DataClass<*, *>>(
    private val activity: DataClassListActivity<P>,
    val items: List<T>,
    private val itemsPerRow: Int = 1,
    private val itemHeightPx: Int? = null,
    private val onItemSelected: ((data: T, holder: DwDataClassViewHolder, position: Int) -> Unit)?
) : RecyclerView.Adapter<DwDataClassViewHolder>() {
    private val storage = Firebase.storage

    /**
     * Stores the step that is currently updating the UI, this is, the one that is using [downloadStatuses].
     * @author Arnau Mora
     * @since 20210516
     */
    private var updatingUiStep = 1

    /**
     * Stores the amount of update UI steps.
     * @author Arnau Mora
     * @since 20210516
     */
    private var updatingUiStepCount = 0
    private val downloadStatuses = arrayMapOf<String, DownloadStatus>()

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = position % itemsPerRow

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DwDataClassViewHolder =
        DwDataClassViewHolder(
            LayoutInflater.from(activity).inflate(R.layout.list_item_dw_dataclass, parent, false)
        )

    override fun onBindViewHolder(holder: DwDataClassViewHolder, position: Int) {
        val data = items[position]
        Timber.d("Setting view for ${data.namespace} \"%s\"", data.displayName)

        holder.titleTextView.text = data.displayName
        ViewCompat.setTransitionName(holder.titleTextView, data.pin)

        holder.imageView.setOnClickListener {
            Timber.v("Showing \"%s\"", data.displayName)

            onItemSelected?.let { it(data, holder, position) }
        }

        holder.mapImageButton.visibility(data.kmzReferenceUrl != null)
        if (data.kmzReferenceUrl != null)
            holder.mapImageButton.setOnClickListener {
                try {
                    Timber.v("Showing map for $data.")
                    showMap(data)
                } catch (e: IllegalStateException) {
                    Firebase.crashlytics.recordException(e)
                    Timber.w("The DataClass ($data) does not contain a KMZ address")
                    toast(activity, R.string.toast_error_no_kmz)
                } catch (e: StorageException) {
                    Firebase.crashlytics.recordException(e)
                    val handler = handleStorageException(e)
                    if (handler != null) {
                        Timber.e(e, handler.second)
                        toast(activity, handler.first)
                    }
                }
            }

        holder.downloadImageButton.setOnClickListener {
            if (!appNetworkState.hasInternet)
                activity.toast(R.string.toast_error_no_internet)
            else {
                updateUi(holder, data, false)
                when (val status = downloadStatuses[data.objectId]!!) {
                    DownloadStatus.NOT_DOWNLOADED -> requestDownload(data, holder)
                    DownloadStatus.DOWNLOADING -> activity.toast(R.string.message_already_downloading)
                    else -> DownloadDialog(
                        activity,
                        data,
                        activity.storage,
                        status == DownloadStatus.PARTIALLY
                    ) { requestDownload(it, holder) }.show {
                        updateUi(holder, data, true)
                    }
                }
            }
        }

        val itemHeightDp = itemHeightPx?.let {
            itemHeightPx / activity.resources.displayMetrics.density
        }?.toInt()
        if (itemHeightDp != null)
            holder.imageView.layoutParams = holder.imageView.layoutParams.apply {
                height = itemHeightDp
            }
        data.loadImage(activity, activity.storage, holder.imageView, null)
        updateUi(holder, data, true)
    }

    /**
     * Updates the [holder] with the data gotten from [data].
     * @author Arnau Mora
     * @since 20210417
     * @param holder The holder to update.
     * @param data The [DataClass] to fetch from.
     * @param updateDownloadStatus If the download status should be fetched again.
     */
    @UiThread
    private fun updateUi(
        holder: DwDataClassViewHolder,
        data: T,
        updateDownloadStatus: Boolean
    ) {
        val step = ++updatingUiStepCount
        holder.progressIndicator.visibility(true)
        holder.downloadImageButton.visibility(false, setGone = false)

        doAsync {
            Timber.v("Waiting for other threads to free up downloadStatuses...")
            while (updatingUiStep > step) {
                delay(DATACLASS_WAIT_CHILDREN_DELAY)
            }
            Timber.v("updatingUiStep reached $step. Updating UI...")

            // Get the last download status
            val lastDownloadStatus =
                synchronized(downloadStatuses) { downloadStatuses[data.objectId] }
            val status = if (
            // Check if there is no download status
                lastDownloadStatus == null ||
                // Or it has been requested to update the download status
                updateDownloadStatus
            ) {
                // Fetch the new downloa status
                val newStatus = data.downloadStatus(activity, activity.storage)
                // Store it in cache
                synchronized(downloadStatuses) {
                    downloadStatuses[data.objectId] = newStatus
                }
                Timber.v("$data download status: $newStatus")
                // Return the new status
                newStatus
            } else
                lastDownloadStatus

            val shouldAddObserver =
                lastDownloadStatus != status && status == DownloadStatus.DOWNLOADING

            val workInfo = data.downloadWorkInfo(activity)
            if (status == DownloadStatus.DOWNLOADING && workInfo != null && shouldAddObserver)
                uiContext {
                    Timber.v("$data is being downloaded, observing...")
                    val workManager = WorkManager.getInstance(activity)

                    val liveData = workManager.getWorkInfoByIdLiveData(workInfo.id)
                    liveData.observe(activity) {
                        val state = workInfo.state
                        val outputData = workInfo.outputData
                        when (state) {
                            WorkInfo.State.FAILED -> {
                                toast(activity, R.string.toast_error_internal)
                                Timber.w("Download failed! Error: ${outputData.getString("error")}")
                            }
                            WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED ->
                                Timber.v("Download running...")
                            WorkInfo.State.SUCCEEDED ->
                                Timber.v("Download complete!")
                            else -> Timber.v("Current download status: ${workInfo.state}")
                        }
                        updateUi(holder, data, true)
                    }
                    Timber.v("Observing download progress for \"$data\"")
                }

            uiContext {
                holder.downloadImageButton.setImageResource(
                    when (status) {
                        DownloadStatus.DOWNLOADED -> R.drawable.cloud_check
                        DownloadStatus.DOWNLOADING -> R.drawable.cloud_sync
                        DownloadStatus.PARTIALLY -> R.drawable.cloud_braces
                        DownloadStatus.NOT_DOWNLOADED -> R.drawable.download
                    }
                )
                holder.downloadImageButton.show()
                holder.progressIndicator.visibility(status == DownloadStatus.DOWNLOADING)
            }

            updatingUiStep++
        }
    }

    /**
     * Requests to show the map of a [Zone]
     * @author Arnau Mora
     * @since 20210413
     * @param data The [T] to show.
     * @throws IllegalStateException When there's no KMZ file in the [data], so the map cannot be shown.
     * @throws StorageException When there has been an error while loading the KMZ file.
     * @see DataClass.kmzFile
     */
    @MainThread
    @Throws(IllegalStateException::class, StorageException::class)
    private fun showMap(data: T) = doAsync {
        val kmzFile = data.kmzFile(activity, storage, false)
        uiContext {
            activity.launch(MapsActivity::class.java) {
                putExtra(EXTRA_KMZ_FILE, kmzFile.path)
            }
        }
    }

    /**
     * Requests a [T] to be downloaded, and observes its status.
     * @author Arnau Mora
     * @since 20210417
     * @param data The [T] to download.
     * @param holder The [DwDataClassViewHolder] for the [data].
     */
    @UiThread
    fun requestDownload(data: T, holder: DwDataClassViewHolder) {
        val result = data.download(activity)
        updateUi(holder, data, true)
        result.observe(activity) { workInfo ->
            val state = workInfo.state
            val outputData = workInfo.outputData
            when (state) {
                WorkInfo.State.FAILED -> {
                    toast(activity, R.string.toast_error_internal)
                    Timber.w("Download failed! Error: ${outputData.getString("error")}")
                }
                WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED ->
                    Timber.v("Download running...")
                WorkInfo.State.SUCCEEDED ->
                    Timber.v("Download complete!")
                else -> Timber.v("Current download status: ${workInfo.state}")
            }
            updateUi(holder, data, true)
        }
    }
}
