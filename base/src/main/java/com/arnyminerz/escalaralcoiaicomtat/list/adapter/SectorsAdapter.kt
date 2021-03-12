package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.AREAS
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.DataClassListActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.types.DownloadStatus
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.exception.AlreadyLoadingException
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.DownloadDialog
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_PREVIEW_SCALE_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.list.holder.SectorsViewHolder
import com.arnyminerz.escalaralcoiaicomtat.view.ImageLoadParameters
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestOptions
import timber.log.Timber

private const val IMAGE_LOAD_TRANSITION_TIME = 50
private const val IMAGE_THUMBNAIL_SIZE = 0.1f

@Suppress("unused")
@ExperimentalUnsignedTypes
class SectorsAdapter(
    private val dataClassListActivity: DataClassListActivity<*>,
    areaId: String,
    zoneId: String,
    listener: ((viewHolder: SectorsViewHolder, index: Int) -> Unit)? = null
) : RecyclerView.Adapter<SectorsViewHolder>() {
    private var onItemSelected: ((viewHolder: SectorsViewHolder, index: Int) -> Unit)? =
        listener

    private val sectors = AREAS[areaId]!![zoneId].children

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectorsViewHolder =
        SectorsViewHolder(
            LayoutInflater.from(dataClassListActivity).inflate(
                R.layout.list_item_sector_image, parent, false
            )
        )

    override fun getItemCount(): Int = sectors.size

    override fun onBindViewHolder(holder: SectorsViewHolder, position: Int) {
        if (position >= sectors.size) {
            Timber.e("Retrieving position $position on sectors with size ${sectors.size}")
            holder.cardView.hide()
            return
        }

        val sector = sectors[position]
        with(holder) {
            val width = imageView.width
            val height = imageView.height

            sector.asyncLoadImage(
                dataClassListActivity,
                imageView,
                downloadProgressBar,
                ImageLoadParameters()
                    .withRequestOptions(
                        with(RequestOptions()) {
                            centerCrop()
                            override(width, height)
                            format(DecodeFormat.PREFER_RGB_565)
                            this
                        }
                    )
                    .withTransitionOptions(
                        BitmapTransitionOptions.withCrossFade(IMAGE_LOAD_TRANSITION_TIME)
                    )
                    .withThumbnailSize(IMAGE_THUMBNAIL_SIZE)
                    .withResultImageScale(
                        with(SETTINGS_PREVIEW_SCALE_PREF.get(dataClassListActivity.sharedPreferences)) {
                            Timber.v("Preview scale: $this")
                            this
                        }
                    )
            )
            titleTextView.text = sector.displayName
            visibility(downloadProgressBar, false)

            imageView.setOnClickListener {
                onItemSelected?.let { it(holder, position) }
            }

            refreshDownloadImage(sector, downloadImageButton, downloadProgressBar)

            downloadImageButton.setOnClickListener {
                if (!dataClassListActivity.networkState.hasInternet)
                    dataClassListActivity.toast(R.string.toast_error_no_internet)
                else
                    when (sector.isDownloaded(dataClassListActivity)) {
                        DownloadStatus.NOT_DOWNLOADED ->
                            try {
                                sector.download(dataClassListActivity, false, {
                                    // start
                                    Timber.v("Started downloading \"${sector.displayName}\"")
                                    dataClassListActivity.runOnUiThread {
                                        visibility(downloadProgressBar, true)
                                        downloadProgressBar.isIndeterminate = true
                                        downloadImageButton.setImageResource(R.drawable.download_outline)
                                    }
                                }, {
                                    visibility(downloadProgressBar, false)
                                    refreshDownloadImage(
                                        sector,
                                        downloadImageButton,
                                        downloadProgressBar
                                    )
                                }, { progress, max ->
                                    downloadProgressBar.isIndeterminate = false
                                    downloadProgressBar.max = max
                                    downloadProgressBar.progress = progress
                                }, {
                                    toast(dataClassListActivity, R.string.toast_error_internal)
                                    visibility(downloadProgressBar, false)
                                })
                            } catch (error: FileAlreadyExistsException) {
                                Timber.e(error, "Could not download!")
                                toast(
                                    dataClassListActivity,
                                    R.string.toast_error_already_downloaded
                                )

                                visibility(holder.downloadProgressBar, false)
                                downloadImageButton.setImageResource(R.drawable.download)
                            } catch (error: AlreadyLoadingException) {
                                Timber.w("Already downloading!")
                                toast(
                                    dataClassListActivity,
                                    R.string.toast_error_already_downloading
                                )
                            }
                        DownloadStatus.DOWNLOADING -> dataClassListActivity.toast(R.string.toast_downloading)
                        DownloadStatus.DOWNLOADED -> DownloadDialog(
                            dataClassListActivity,
                            sector
                        ).show {
                            refreshDownloadImage(sector, downloadImageButton, downloadProgressBar)
                        }
                    }
            }
        }
    }

    private fun refreshDownloadImage(
        sector: Sector,
        downloadImagebutton: ImageButton,
        downloadProgressbar: ProgressBar
    ) {
        when (sector.isDownloaded(dataClassListActivity)) {
            DownloadStatus.NOT_DOWNLOADED -> downloadImagebutton.setImageResource(R.drawable.download)
            DownloadStatus.DOWNLOADING -> {
                downloadImagebutton.setImageResource(R.drawable.download_outline)
                visibility(downloadProgressbar, true)
                downloadProgressbar.isIndeterminate = true
            }
            DownloadStatus.DOWNLOADED -> downloadImagebutton.setImageResource(R.drawable.cloud_check)
        }
    }
}
