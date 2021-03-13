package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.KML_ADDRESS_BUNDLE_EXTRA
import com.arnyminerz.escalaralcoiaicomtat.activity.KMZ_FILE_BUNDLE_EXTRA
import com.arnyminerz.escalaralcoiaicomtat.activity.MapsActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.ZONE_NAME_BUNDLE_EXTRA
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.DataClassListActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Zone
import com.arnyminerz.escalaralcoiaicomtat.data.climb.types.DownloadStatus
import com.arnyminerz.escalaralcoiaicomtat.exception.AlreadyLoadingException
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.DownloadDialog
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.runAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.list.holder.ZonesViewHolder
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
            Timber.v("Loading area \"%s\" with %s sectors", zone.displayName, zone.count())

            onItemSelected?.let { it(zone, holder, position) }
        }
        runAsync {
            zone.asyncLoadImage(dataClassListActivity, holder.imageView)
        }

        if (zone.isDownloaded(
                dataClassListActivity
            ) == DownloadStatus.DOWNLOADED || zone.kmlAddress != null
        )
            holder.mapImageButton.setOnClickListener {
                showMap(zone)
            }
        else visibility(holder.mapImageButton, false)

        holder.downloadImageButton.setOnClickListener {
            if (!dataClassListActivity.networkState.hasInternet)
                dataClassListActivity.toast(R.string.toast_error_no_internet)
            else
                when (zone.isDownloaded(dataClassListActivity)) {
                    DownloadStatus.NOT_DOWNLOADED ->
                        try {
                            zone.download(dataClassListActivity, true, {
                                dataClassListActivity.runOnUiThread {
                                    holder.downloadImageButton.setImageResource(R.drawable.download_outline)
                                    holder.progressBar.isIndeterminate = true
                                    visibility(holder.progressBar, true)
                                }
                            }, {
                                dataClassListActivity.runOnUiThread {
                                    visibility(holder.progressBar, false)
                                    updateImageRes(holder, zone)
                                }
                            }, { progress, max ->
                                holder.progressBar.apply {
                                    isIndeterminate = false
                                    this.max = max
                                    this.progress = progress
                                }
                            }, {
                                toast(dataClassListActivity, R.string.toast_error_internal)
                                visibility(holder.progressBar, false)
                            })
                        } catch (error: FileAlreadyExistsException) {
                            Timber.w(error, "Zone already downloaded!")
                            toast(dataClassListActivity, R.string.toast_error_already_downloaded)

                            visibility(holder.progressBar, false)
                            holder.downloadImageButton.setImageResource(R.drawable.download)
                        } catch (error: NoInternetAccessException) {
                            dataClassListActivity.toast(R.string.toast_error_no_internet)
                        } catch (error: AlreadyLoadingException) {
                            Timber.w("Already downloading!")
                            toast(dataClassListActivity, R.string.toast_error_already_downloading)
                        }
                    DownloadStatus.DOWNLOADING -> dataClassListActivity.toast(R.string.message_already_downloading)
                    else -> DownloadDialog(
                        dataClassListActivity,
                        zone
                    ).show {
                        updateImageRes(holder, zone)
                    }
                }
        }
        updateImageRes(holder, zone)
    }

    private fun updateImageRes(holder: ZonesViewHolder, zone: Zone) {
        holder.downloadImageButton.setImageResource(
            when (zone.isDownloaded(dataClassListActivity)) {
                DownloadStatus.DOWNLOADED -> R.drawable.cloud_check
                DownloadStatus.DOWNLOADING -> R.drawable.download_outline
                else -> R.drawable.download
            }
        )
    }

    private fun showMap(zone: Zone) {
        when {
            zone.isDownloaded(dataClassListActivity) == DownloadStatus.DOWNLOADED ->
                dataClassListActivity.startActivity(
                    Intent(dataClassListActivity, MapsActivity::class.java)
                        .putExtra(
                            KMZ_FILE_BUNDLE_EXTRA,
                            File(filesDir(dataClassListActivity), "data/zone_${zone.objectId}.kmz").path
                        )
                        .putExtra(ZONE_NAME_BUNDLE_EXTRA, zone.displayName)
                )
            zone.kmlAddress != null ->
                dataClassListActivity.startActivity(
                    Intent(dataClassListActivity, MapsActivity::class.java)
                        .putExtra(
                            KML_ADDRESS_BUNDLE_EXTRA,
                            zone.kmlAddress
                        )
                )
        }
    }
}
