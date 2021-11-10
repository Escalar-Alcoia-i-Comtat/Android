package com.arnyminerz.escalaralcoiaicomtat.paging

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DimenRes
import androidx.annotation.UiThread
import androidx.lifecycle.LifecycleOwner
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DownloadStatus
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DATACLASS_PREVIEW_SCALE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.mapsIntent
import com.arnyminerz.escalaralcoiaicomtat.core.utils.then
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toPx
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.ImageLoadParameters
import com.arnyminerz.escalaralcoiaicomtat.core.view.hide
import com.arnyminerz.escalaralcoiaicomtat.core.view.show
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.databinding.ListItemDwDataclassBinding
import com.arnyminerz.escalaralcoiaicomtat.databinding.ListItemPathBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.DownloadDialog
import com.arnyminerz.escalaralcoiaicomtat.worker.DownloadWorker
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import timber.log.Timber

class AreaAdapter(
    clickListener: ((binding: ListItemDwDataclassBinding, position: Int, item: DataClassImpl) -> Unit)?
) : DataClassAdapter(1, false, R.dimen.area_item_height, clickListener, DataClassComparator())

open class DataClassAdapter(
    private val columns: Int,
    private val isPath: Boolean,
    @DimenRes private val itemHeight: Int,
    private val clickListener: ((binding: ListItemDwDataclassBinding, position: Int, item: DataClassImpl) -> Unit)?,
    diffCallback: DiffUtil.ItemCallback<DataClassImpl>
) : PagingDataAdapter<DataClassImpl, DataClassAdapter.DataClassViewHolder>(diffCallback) {
    class DataClassViewHolder(
        val dataClassBinding: ListItemDwDataclassBinding?,
        val pathBinding: ListItemPathBinding?,
    ) : RecyclerView.ViewHolder((dataClassBinding ?: pathBinding!!).root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataClassViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val dataClassBinding = (!isPath).then {
            Timber.v("DataClassBinding won't be null.")
            ListItemDwDataclassBinding.inflate(layoutInflater, parent, false)
        }
        val pathBinding = isPath.then {
            Timber.v("PathBinding won't be null.")
            ListItemPathBinding.inflate(layoutInflater, parent, false)
        }
        return DataClassViewHolder(dataClassBinding, pathBinding)
    }

    override fun onBindViewHolder(holder: DataClassViewHolder, position: Int) {
        if (holder.dataClassBinding != null) {
            // This is a DataClass
            val dataClass = getItem(position) as DataClass<*, *>?
            Timber.v("Displaying contents for DataClass \"$dataClass\"")
            val binding = holder.dataClassBinding
            val context = binding.root.context
            val activity = context as Activity
            val app = activity.app
            val storage = Firebase.storage
            val oddColumns = columns % 2 == 0

            @UiThread
            fun updateDownloadStatus(status: DownloadStatus) {
                binding.wideProgressBar.visibility(status.downloading)
                binding.wideDownloadButton.setIconResource(status.getIcon())
                binding.wideDownloadButton.isEnabled = !status.downloading

                if (status.downloading) {
                    binding.wideProgressBar.show()
                    if (!binding.wideProgressBar.isIndeterminate) {
                        binding.wideProgressBar.hide()
                        binding.wideProgressBar.isIndeterminate = true
                        binding.wideProgressBar.show()
                    }
                    val liveData = dataClass?.downloadWorkInfoLiveData(context) ?: return
                    liveData.observe(activity as LifecycleOwner) { workInfos ->
                        if (workInfos.isEmpty())
                            return@observe
                        val workInfo = workInfos[0]
                        val finished = workInfo.state.isFinished
                        if (finished)
                            updateDownloadStatus(DownloadStatus.DOWNLOADED)
                    }
                }
            }

            val itemHeight = activity.resources.getDimension(itemHeight).toPx
            binding.cardView.layoutParams.height = itemHeight.toInt()

            binding.wideImageView.layoutParams.width = (itemHeight * (3f / 5f)).toInt()

            binding.imageView.setImageResource(R.drawable.ic_tall_placeholder)
            binding.wideImageView.setImageResource(R.drawable.ic_tall_placeholder)

            binding.wideProgressBar.hide()
            binding.wideLayout.visibility(oddColumns)
            binding.thinLayout.visibility(!oddColumns)

            if (dataClass == null) {
                Timber.v("DataClass is null, showing placeholder...")
                binding.titleTextView.hide()
                binding.wideTitleTextView.hide()
                binding.wideDownloadButton.hide()
                binding.wideMapButton.hide()
            } else {
                Timber.v("DataClass is not null, displaying data...")
                val downloadable = dataClass.displayOptions.downloadable
                val showLocation = dataClass.displayOptions.showLocation
                binding.titleTextView.show()
                binding.wideTitleTextView.show()
                binding.wideDownloadButton.visibility(downloadable)
                binding.wideMapButton.visibility(showLocation)

                binding.wideTitleTextView.text = dataClass.displayName
                binding.wideInfoTextView.setText(R.string.status_loading)
                binding.titleTextView.text = dataClass.displayName
                binding.infoTextView.setText(R.string.status_loading)
                doAsync {
                    val size = dataClass.getSize(app.searchSession)
                    uiContext {
                        val info = when (dataClass) {
                            is Area -> context.resources.getString(
                                R.string.dataclass_area_children_count,
                                size
                            )
                            is Zone -> context.resources.getString(
                                R.string.dataclass_zone_children_count,
                                size
                            )
                            is Sector -> context.resources.getString(
                                R.string.dataclass_sector_children_count,
                                size
                            )
                            else -> ""
                        }
                        binding.infoTextView.text = info
                        binding.wideInfoTextView.text = info
                    }
                }
                binding.imageView.setOnClickListener {
                    clickListener?.invoke(binding, position, dataClass)
                }
                binding.enterButton.setOnClickListener {
                    clickListener?.invoke(binding, position, dataClass)
                }

                // dataClass.location check is a bit redundant, but just in case
                val dataClassLocation = dataClass.location
                if (showLocation && dataClassLocation != null) {
                    binding.wideMapButton.setOnClickListener {
                        context.startActivity(
                            dataClassLocation.mapsIntent(true, dataClass.displayName)
                        )
                    }
                }

                if (downloadable) {
                    binding.wideDownloadButton.setOnClickListener {
                        binding.wideProgressBar.show()
                        doAsync {
                            val downloadStatus =
                                dataClass.downloadStatus(context, app.searchSession)
                            uiContext {
                                binding.wideProgressBar.hide()

                                if (!downloadStatus.downloaded && !downloadStatus.downloading)
                                    dataClass.download<DownloadWorker>(context)
                                        .observe(activity as LifecycleOwner) {
                                            val finished = it.state.isFinished

                                            updateDownloadStatus(
                                                if (finished)
                                                    DownloadStatus.DOWNLOADED
                                                else
                                                    DownloadStatus.DOWNLOADING
                                            )
                                        }
                                else if (downloadStatus.downloading)
                                    toast(context, R.string.message_already_downloading)
                                else {
                                    Timber.v("Showing download dialog")
                                    DownloadDialog(activity, dataClass, storage)
                                        .show {
                                            Timber.v("Deleted DataClass at $position.")
                                            notifyItemChanged(position)
                                        }
                                }
                            }
                        }
                    }

                    // Check download status
                    doAsync {
                        val downloadStatus =
                            dataClass.downloadStatus(context, app.searchSession)
                            { binding.wideProgressBar.progress = it.percentage }
                        uiContext {
                            updateDownloadStatus(downloadStatus)
                        }
                    }
                }

                // Load the image asynchronously
                doAsync {
                    // TODO: Add error handlers
                    val image = dataClass.image(
                        context,
                        storage,
                        ImageLoadParameters().withResultImageScale(DATACLASS_PREVIEW_SCALE)
                    ) { }
                    uiContext {
                        if (image != null)
                            if (oddColumns)
                                binding.wideImageView.setImageBitmap(image)
                            else
                                binding.imageView.setImageBitmap(image)
                        else {
                            Timber.e("Could not load image")
                            context.toast(R.string.toast_error_load_image)
                        }
                    }
                }
            }
        } else {
            // This is a Path
            val path = getItem(position) as Path?
            Timber.v("Displaying contents for Path \"$path\"")
            val binding = holder.pathBinding!!
        }
    }
}
