package com.arnyminerz.escalaralcoiaicomtat.fragment.dialog

import android.app.Activity
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.humanReadableByteCountBin
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * Shows the downloaded info of a downloaded DataClass with type [T].
 * @author Arnau Mora
 * @since 20210820
 * @param T The type of the DataClass to display.
 * @param activity The [Activity] where the dialog will be shown.
 * @param data The [DataClass] to show.
 * @param storage The [FirebaseStorage] reference.
 * @param partialDownload Whether or not the [data] is completely downloaded including children.
 * @param downloader When [partialDownload] is true, this will get called when the download button
 * is clicked.
 */
class DownloadDialog<T : DataClass<*, *>>(
    private val activity: Activity,
    private val data: T,
    private val storage: FirebaseStorage,
    private val partialDownload: Boolean,
    private val downloader: (dataClass: T) -> Unit,
) {
    /**
     * Shows the dialog to the user.
     * @author Arnau Mora
     * @since 20210820
     * @param deleteCallback This will get called when the user selects the option to delete the
     * [DataClass].
     */
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
                runBlocking {
                    humanReadableByteCountBin(
                        data.size(
                            activity.app,
                            activity.app.searchSession
                        )
                    )
                }
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
                                        val app = activity.app
                                        val searchSession = app.searchSession
                                        val children = data.getChildren(searchSession)
                                        for (child in children)
                                            if (child is DataClass<*, *>)
                                                child.delete(app, searchSession)

                                        data.delete(app, searchSession)
                                        uiContext { deleteCallback?.invoke() }
                                    }
                                else -> Timber.e("Data is not valid.")
                            }
                        }
                        .setNegativeButton(R.string.action_cancel) { d, _ -> d.dismiss() }
                        .show()
                }
                .setNegativeButton(R.string.action_close, null)
        }
        builder.show()
    }
}
