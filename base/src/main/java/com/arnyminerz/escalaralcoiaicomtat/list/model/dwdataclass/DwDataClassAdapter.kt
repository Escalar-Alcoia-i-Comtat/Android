package com.arnyminerz.escalaralcoiaicomtat.list.model.dwdataclass

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import androidx.collection.arrayMapOf
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MapsActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.DataClassListActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DownloadStatus
import com.arnyminerz.escalaralcoiaicomtat.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.DownloadDialog
import com.arnyminerz.escalaralcoiaicomtat.generic.doAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.generic.uiContext
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_KMZ_FILE
import com.arnyminerz.escalaralcoiaicomtat.shared.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.view.show
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import timber.log.Timber

class DwDataClassAdapter<T : DataClass<*, *>, P : DataClass<*, *>>(
    private val activity: DataClassListActivity<P>,
    val items: List<T>,
    private val onItemSelected: ((data: T, holder: DwDataClassViewHolder, position: Int) -> Unit)?
) : RecyclerView.Adapter<DwDataClassViewHolder>() {
    private val storage = Firebase.storage

    private val downloadStatuses = arrayMapOf<String, DownloadStatus>()

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DwDataClassViewHolder =
        DwDataClassViewHolder(
            LayoutInflater.from(activity).inflate(R.layout.list_item_dw_dataclass, parent, false)
        )

    override fun onBindViewHolder(holder: DwDataClassViewHolder, position: Int) {
        val data = items[position]
        Timber.d("Setting view for ${data.namespace} \"%s\"", data.displayName)

        holder.titleTextView.text = data.displayName
        ViewCompat.setTransitionName(holder.titleTextView, data.transitionName)

        holder.imageView.setOnClickListener {
            Timber.v("Showing \"%s\"", data.displayName)

            onItemSelected?.let { it(data, holder, position) }
        }
        holder.mapImageButton.setOnClickListener {
            showMap(data)
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
                        activity.firestore,
                        status == DownloadStatus.PARTIALLY,
                        { requestDownload(it, holder) }
                    ).show {
                        updateUi(holder, data, true)
                    }
                }
            }
        }

        doAsync {
            data.loadImage(activity, activity.storage, holder.imageView)
        }
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
        holder.progressIndicator.visibility(true)
        holder.downloadImageButton.visibility(false, setGone = false)
        doAsync {
            if (!downloadStatuses.containsKey(data.objectId) || updateDownloadStatus)
                downloadStatuses[data.objectId] = data.downloadStatus(activity, activity.firestore)
            val status = downloadStatuses[data.objectId]!!

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
        }
    }

    /**
     * Requests to show the map of a [Zone]
     * @author Arnau Mora
     * @since 20210413
     * @param data The [T] to show.
     */
    @MainThread
    private fun showMap(data: T) = doAsync {
        val kmzFile = data.getKmzFile(activity, storage)
        uiContext {
            activity.startActivity(
                Intent(activity, MapsActivity::class.java)
                    .putExtra(
                        EXTRA_KMZ_FILE,
                        kmzFile.path
                    )
            )
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
        val result = data.download(
            activity,
            activity.mapStyle?.uri
        )
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
