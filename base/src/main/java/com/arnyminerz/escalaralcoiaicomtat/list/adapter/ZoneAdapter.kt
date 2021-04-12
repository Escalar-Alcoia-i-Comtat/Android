package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MapsActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.DataClassListActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DownloadStatus
import com.arnyminerz.escalaralcoiaicomtat.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.DownloadDialog
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.list.holder.ZonesViewHolder
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_KML_ADDRESS
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_KMZ_FILE
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_ZONE_NAME
import com.arnyminerz.escalaralcoiaicomtat.shared.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.storage.filesDir
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import timber.log.Timber
import java.io.File

class ZoneAdapter(
    private val zones: List<Zone>,
    private val dataClassListActivity: DataClassListActivity<*>,
    listener: ((zone: Zone, viewHolder: ZonesViewHolder, index: Int) -> Unit)? = null
) : RecyclerView.Adapter<ZonesViewHolder>() {
    private var onItemSelected: ((zone: Zone, viewHolder: ZonesViewHolder, index: Int) -> Unit)? =
        listener

    init {
        Timber.d("Created ZoneAdapter!")
    }

    override fun getItemCount(): Int = zones.size

    override fun getItemViewType(position: Int): Int {
        return position % 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ZonesViewHolder(
            LayoutInflater.from(dataClassListActivity).inflate(
                R.layout.list_item_zone, parent, false
            )
        )

    override fun onBindViewHolder(holder: ZonesViewHolder, position: Int) {
        val zone = zones[position]
        Timber.d("Setting view for zone \"%s\"", zone.displayName)

        holder.titleTextView.text = zone.displayName
        ViewCompat.setTransitionName(holder.titleTextView, zone.transitionName)

        holder.imageView.setOnClickListener {
            Timber.v("Loading area \"%s\"", zone.displayName)

            onItemSelected?.let { it(zone, holder, position) }
        }
        zone.asyncLoadImage(dataClassListActivity, holder.imageView)

        if (zone.downloadStatus(dataClassListActivity) == DownloadStatus.DOWNLOADED ||
            zone.kmlAddress != null
        )
            holder.mapImageButton.setOnClickListener {
                showMap(zone)
            }
        else visibility(holder.mapImageButton, false)

        holder.downloadImageButton.setOnClickListener {
            if (!appNetworkState.hasInternet)
                dataClassListActivity.toast(R.string.toast_error_no_internet)
            else
                when (zone.downloadStatus(dataClassListActivity)) {
                    DownloadStatus.NOT_DOWNLOADED -> {
                        val result = zone.download(
                            dataClassListActivity,
                            dataClassListActivity.mapStyle?.uri
                        )
                        result.observe(dataClassListActivity) { workInfo ->
                            val state = workInfo.state
                            val data = workInfo.outputData
                            when (state) {
                                WorkInfo.State.FAILED -> {
                                    toast(dataClassListActivity, R.string.toast_error_internal)
                                    visibility(holder.progressBar, false)
                                    Timber.w("Download failed! Error: ${data.getString("error")}")
                                }
                                WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> {
                                    Timber.v("Download running...")
                                    visibility(holder.progressBar, true)
                                    updateImageRes(holder, zone)
                                }
                                WorkInfo.State.SUCCEEDED -> {
                                    Timber.v("Download complete!")
                                    visibility(holder.progressBar, false)
                                    updateImageRes(holder, zone)
                                }
                                else -> Timber.v("Current download status: ${workInfo.state}")
                            }
                        }
                    }
                    DownloadStatus.DOWNLOADING -> dataClassListActivity.toast(R.string.message_already_downloading)
                    else -> DownloadDialog(
                        dataClassListActivity,
                        zone,
                        dataClassListActivity.firestore
                    ).show {
                        updateImageRes(holder, zone)
                    }
                }
        }
        updateImageRes(holder, zone)
    }

    private fun updateImageRes(holder: ZonesViewHolder, zone: Zone) {
        holder.downloadImageButton.setImageResource(
            when (zone.downloadStatus(dataClassListActivity)) {
                DownloadStatus.DOWNLOADED -> R.drawable.cloud_check
                DownloadStatus.DOWNLOADING -> R.drawable.download_outline
                else -> R.drawable.download
            }
        )
    }

    private fun showMap(zone: Zone) {
        when {
            zone.downloadStatus(dataClassListActivity) == DownloadStatus.DOWNLOADED ->
                dataClassListActivity.startActivity(
                    Intent(dataClassListActivity, MapsActivity::class.java)
                        .putExtra(
                            EXTRA_KMZ_FILE,
                            File(
                                filesDir(dataClassListActivity),
                                "data/zone_${zone.objectId}.kmz"
                            ).path
                        )
                        .putExtra(EXTRA_ZONE_NAME, zone.displayName)
                )
            zone.kmlAddress != null ->
                dataClassListActivity.startActivity(
                    Intent(dataClassListActivity, MapsActivity::class.java)
                        .putExtra(
                            EXTRA_KML_ADDRESS,
                            zone.kmlAddress
                        )
                )
        }
    }
}
