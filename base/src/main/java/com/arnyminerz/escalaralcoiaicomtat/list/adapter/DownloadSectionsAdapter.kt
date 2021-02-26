package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.AREAS
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Zone
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.find
import com.arnyminerz.escalaralcoiaicomtat.data.climb.download.DownloadedSection
import com.arnyminerz.escalaralcoiaicomtat.data.climb.enum.DownloadStatus
import com.arnyminerz.escalaralcoiaicomtat.exception.AlreadyLoadingException
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.DownloadDialog
import com.arnyminerz.escalaralcoiaicomtat.generic.humanReadableByteCountBin
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.list.holder.DownloadSectionViewHolder
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import timber.log.Timber

const val TOGGLED_CARD_HEIGHT = 96f

@ExperimentalUnsignedTypes
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

            val sectionDownloadStatus = section.isDownloaded(mainActivity)
            val sectionHasDownloadedChildren = section.hasAnyDownloadedChildren(mainActivity)
            downloadButton.setOnClickListener {
                if (sectionDownloadStatus == DownloadStatus.NOT_DOWNLOADED)
                    try {
                        section.download(mainActivity, true, {
                            mainActivity.toast(R.string.toast_downloading)
                            downloadProgressBar.isIndeterminate = true
                            visibility(downloadProgressBar, true)
                        }, {
                            visibility(downloadProgressBar, false)
                            Timber.v("Finished downloading. Updating Downloads Recycler View...")
                            mainActivity.downloadsFragment.reloadSizeTextView()
                            notifyDataSetChanged()
                        }, { progress, max ->
                            downloadProgressBar.isIndeterminate = false
                            downloadProgressBar.max = max
                            downloadProgressBar.progress = progress
                        }, {
                            mainActivity.toast(R.string.toast_error_internal)
                            visibility(downloadProgressBar, false)
                        })
                    } catch (ex: FileAlreadyExistsException) {
                        // If the data is already downloaded
                        // This will never be caught, since sectionDownloadStatus is NOT_DOWNLOADED, but who knows
                        mainActivity.toast(R.string.toast_error_already_downloaded)
                    } catch (ex: AlreadyLoadingException) {
                        // If the download has already been started
                        // This will never be caught, since sectionDownloadStatus is NOT_DOWNLOADED, but who knows
                        mainActivity.toast(R.string.message_already_downloading)
                    }
                else if (section.isDownloaded(mainActivity) == DownloadStatus.DOWNLOADING)
                    mainActivity.toast(R.string.message_already_downloading)
            }
            visibility(downloadProgressBar, sectionDownloadStatus == DownloadStatus.DOWNLOADING)
            visibility(downloadButton, sectionDownloadStatus != DownloadStatus.DOWNLOADED)
            visibility(deleteButton, sectionDownloadStatus == DownloadStatus.DOWNLOADED)
            visibility(viewButton, sectionDownloadStatus == DownloadStatus.DOWNLOADED)

            Timber.v("Section is Zone? ${section is Zone} Area? ${section is Area}. Type: ${section.javaClass.simpleName}")

            visibility(
                toggleButton,
                (section is Zone || section is Area) && sectionHasDownloadedChildren
            )
            toggleButton.setOnClickListener {
                downloadedSection.toggle(cardView, it as ImageButton, recyclerView, mainActivity)
            }
            downloadedSection.updateView(cardView, toggleButton, recyclerView, mainActivity)

            viewButton.setOnClickListener {
                toast(mainActivity, R.string.toast_loading)
                val scan = AREAS.find(section)
                if (!scan.launchActivity(mainActivity)) {
                    Timber.w("Could not launch activity.")
                    toast(mainActivity, R.string.toast_error_internal)
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
                }
                visibility(progressBar, false)
            } else
                Timber.e("Section is not valid!")
        }
    }
}