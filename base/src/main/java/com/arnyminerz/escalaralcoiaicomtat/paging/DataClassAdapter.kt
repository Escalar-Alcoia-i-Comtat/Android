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
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DownloadStatus
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
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

            @UiThread
            fun updateDownloadStatus(status: DownloadStatus) {
                binding.progressIndicator.visibility(status.downloading)
                binding.downloadImageButton.setImageResource(status.getIcon())

                if (status.downloading) {
                    binding.progressIndicator.show()
                    if (!binding.progressIndicator.isIndeterminate) {
                        binding.progressIndicator.hide()
                        binding.progressIndicator.isIndeterminate = true
                        binding.progressIndicator.show()
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

            binding.imageView.layoutParams.height =
                activity.resources.getDimension(itemHeight).toPx.toInt()

            val oddColumns = columns % 2 == 0
            if (oddColumns)
                binding.imageView.setImageResource(R.drawable.ic_tall_placeholder)
            else
                binding.imageView.setImageResource(R.drawable.ic_wide_placeholder)

            if (dataClass == null) {
                Timber.v("DataClass is null, showing placeholder...")
                binding.titleTextView.hide()
                binding.actionButtons.hide()
            } else {
                Timber.v("DataClass is not null, displaying data...")
                val downloadable = dataClass.displayOptions.downloadable
                binding.titleTextView.show()
                binding.actionButtons.visibility(downloadable)

                if (!oddColumns)
                    binding.titleTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                binding.titleTextView.text = dataClass.displayName
                binding.imageView.setOnClickListener {
                    clickListener?.invoke(binding, position, dataClass)
                }

                if (downloadable) {
                    binding.downloadImageButton.setOnClickListener {
                        binding.progressIndicator.show()
                        doAsync {
                            val downloadStatus =
                                dataClass.downloadStatus(context, app.searchSession, storage)
                            uiContext {
                                binding.progressIndicator.hide()

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
                            dataClass.downloadStatus(context, app.searchSession, storage)
                            { binding.progressIndicator.progress = it.percentage() }
                        uiContext {
                            updateDownloadStatus(downloadStatus)
                        }
                    }
                }

                // Load the image asyncronously
                doAsync {
                    val image = dataClass.image(
                        context,
                        storage,
                        ImageLoadParameters().withResultImageScale(.5f)
                    ) { }
                    uiContext {
                        if (image != null)
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
