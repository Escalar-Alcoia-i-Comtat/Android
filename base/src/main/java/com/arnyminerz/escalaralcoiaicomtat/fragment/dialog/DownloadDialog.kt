package com.arnyminerz.escalaralcoiaicomtat.fragment.dialog

import android.app.Activity
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.generic.doAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.humanReadableByteCountBin
import com.arnyminerz.escalaralcoiaicomtat.generic.uiContext
import com.arnyminerz.escalaralcoiaicomtat.shared.App
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class DownloadDialog<T : DataClass<*, *>>(
    private val activity: Activity,
    private val data: T,
    private val firestore: FirebaseFirestore,
    private val partialDownload: Boolean,
    private val downloader: (dataClass: T) -> Unit,
) {
    fun show(deleteCallback: (() -> Unit)? = null) {
        var builder =
            MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle(data.displayName)
        if (partialDownload) {
            builder = builder
                .setMessage(
                    activity.getString(
                        R.string.dialog_downloaded_partially_msg,
                        data.displayName
                    )
                )
                .setPositiveButton(R.string.action_download) { dialog, _ ->
                    downloader(data)
                    dialog.dismiss()
                }
        } else {
            val date = data.downloadDate(activity)

            val message = activity.getString(
                R.string.dialog_downloaded_msg,
                date?.let {
                    android.text.format.DateFormat.format(
                        activity.getString(R.string.date_format),
                        date
                    )
                } ?: "N/A"
            ) + '\n' + activity.getString(
                R.string.dialog_uses_storage_msg,
                runBlocking { humanReadableByteCountBin(data.size(activity)) }
            )

            builder = builder
                .setMessage(message)
                .setNeutralButton(R.string.action_delete) { dialog, _ ->
                    dialog.dismiss()
                    MaterialAlertDialogBuilder(
                        activity,
                        R.style.ThemeOverlay_App_MaterialAlertDialog
                    )
                        .setTitle(R.string.downloads_delete_dialog_title)
                        .setMessage(
                            activity.getString(
                                R.string.downloads_delete_dialog_msg,
                                data.displayName
                            )
                        )
                        .setPositiveButton(R.string.action_delete) { d, _ ->
                            d.dismiss()
                            when (data) {
                                is Area, is Zone, is Sector ->
                                    doAsync {
                                        val children = arrayListOf<DataClassImpl>()
                                        data
                                            .getChildren(activity.application as App, firestore)
                                            .toCollection(children)
                                        for (child in children)
                                            if (child is DataClass<*, *>)
                                                child.delete(activity)

                                        data.delete(activity)
                                        uiContext {
                                            deleteCallback?.invoke()
                                        }
                                    }
                                else -> Timber.e("Data is not valid.")
                            }
                        }
                        .setNegativeButton(R.string.action_cancel) { d, _ ->
                            d.dismiss()
                        }
                        .show()
                }
                .setNegativeButton(R.string.action_close, null)
        }
        builder.show()
    }
}
