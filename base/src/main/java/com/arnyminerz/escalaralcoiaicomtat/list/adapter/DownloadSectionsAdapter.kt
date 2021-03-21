package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.area.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.dataclass.DataClass.Companion.getIntent
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.dataclass.DownloadStatus
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.data.climb.download.DownloadedSection
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.DownloadDialog
import com.arnyminerz.escalaralcoiaicomtat.generic.humanReadableByteCountBin
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.list.holder.DownloadSectionViewHolder
import com.arnyminerz.escalaralcoiaicomtat.shared.appNetworkState
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import timber.log.Timber

const val TOGGLED_CARD_HEIGHT = 96f

class DownloadSectionsAdapter(
    private val downloadedSections: ArrayList<DownloadedSection>,
    private val mainActivity: MainActivity
) : RecyclerView.Adapter<DownloadSectionViewHolder>() {
    override fun getItemCount(): Int = downloadedSections.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadSectionViewHolder =
        DownloadSectionViewHolder(
            LayoutInflater.from(mainActivity).inflate(
                R.layout.download_section_item, parent, false
            )
        )

    override fun onBindViewHolder(holder: DownloadSectionViewHolder, position: Int) {
        val downloadedSection = downloadedSections[position]
        val section = downloadedSection.section
        with(holder) {
            titleTextView.text = section.displayName

            visibility(progressBar, true)

            val sectionDownloadStatus = section.downloadStatus(mainActivity)
            val sectionHasDownloadedChildren = section.hasAnyDownloadedChildren(mainActivity)
            downloadButton.setOnClickListener {
                if (!appNetworkState.hasInternet)
                    mainActivity.toast(R.string.toast_error_no_internet)
                else if (sectionDownloadStatus == DownloadStatus.NOT_DOWNLOADED) {
                        val result = section.download(mainActivity)
                        result.observe(mainActivity) { workInfo ->
                            val state = workInfo.state
                            val data = workInfo.outputData
                            Timber.v("Current download status: ${workInfo.state}")
                            when (state) {
                                WorkInfo.State.FAILED -> {
                                    mainActivity.toast(R.string.toast_error_internal)
                                    visibility(downloadProgressBar, false)
                                    Timber.w("Download failed! Error: ${data.getString("error")}")
                                }
                                WorkInfo.State.SUCCEEDED -> {
                                    visibility(downloadProgressBar, false)
                                    Timber.v("Finished downloading. Updating Downloads Recycler View...")
                                    mainActivity.downloadsFragment.reloadSizeTextView()
                                    notifyDataSetChanged()
                                }
                                else -> downloadProgressBar.isIndeterminate = true
                            }
                        }
                    } else if (section.downloadStatus(mainActivity) == DownloadStatus.DOWNLOADING)
                    mainActivity.toast(R.string.message_already_downloading)
            }
            visibility(downloadProgressBar, sectionDownloadStatus == DownloadStatus.DOWNLOADING)
            visibility(downloadButton, sectionDownloadStatus != DownloadStatus.DOWNLOADED)
            visibility(deleteButton, sectionDownloadStatus == DownloadStatus.DOWNLOADED)
            visibility(viewButton, sectionDownloadStatus == DownloadStatus.DOWNLOADED)

            Timber.v(
                "Section is Zone? ${section is Zone} Area? ${section is Area}. Type: ${section.javaClass.simpleName}"
            )

            visibility(
                toggleButton,
                (section is Zone || section is Area) && sectionHasDownloadedChildren
            )
            toggleButton.setOnClickListener {
                downloadedSection.toggle(cardView, it as ImageButton, recyclerView, mainActivity)
            }
            downloadedSection.updateView(cardView, toggleButton, recyclerView, mainActivity)

            viewButton.setOnClickListener {
                val intent = getIntent(mainActivity, section.displayName)
                if (intent == null) {
                    Timber.w("Could not launch activity.")
                    toast(mainActivity, R.string.toast_error_internal)
                } else {
                    Timber.v("Loading intent...")
                    mainActivity.startActivity(intent)
                }
            }

            recyclerView.layoutManager = LinearLayoutManager(mainActivity)
            if (section is Area || section is Zone || section is Sector) {
                if (sectionDownloadStatus != DownloadStatus.DOWNLOADED) {
                    visibility(progressBar, false)
                    sizeChip.text = mainActivity.getString(R.string.status_not_downloaded)
                } else {
                    val size = section.size(mainActivity)

                    sizeChip.text = humanReadableByteCountBin(size)

                    sizeChip.setOnClickListener {
                        Timber.v("Showing download dialog for ZONE")
                        DownloadDialog(mainActivity, section).show()
                    }
                }
                if (sectionHasDownloadedChildren) {
                    Timber.v("Loading section list for \"${section.displayName}\"...")
                    val sectionList = section.downloadedSectionList()
                    Timber.v("  Section List has ${sectionList.count()} sections")
                    Timber.v("Loading data for \"${section.displayName}\"...")
                    recyclerView.adapter = DownloadSectionsAdapter(sectionList, mainActivity)
                }

                deleteButton.setOnClickListener {
                    MaterialAlertDialogBuilder(mainActivity)
                        .setTitle(R.string.downloads_delete_dialog_title)
                        .setMessage(
                            mainActivity.getString(
                                R.string.downloads_delete_dialog_msg,
                                section.displayName
                            )
                        )
                        .setPositiveButton(R.string.action_delete) { _, _ ->
                            section.delete(mainActivity)
                            mainActivity.downloadsFragment.reloadSizeTextView()
                            notifyDataSetChanged()
                            recyclerView.adapter?.notifyDataSetChanged()
                        }
                        .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.dismiss() }
                        .show()
                }

                visibility(progressBar, false)
            } else
                Timber.e("Section is not valid!")
        }
    }
}
