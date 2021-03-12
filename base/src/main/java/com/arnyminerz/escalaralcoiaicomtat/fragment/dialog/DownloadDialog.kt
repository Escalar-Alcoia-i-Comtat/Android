package com.arnyminerz.escalaralcoiaicomtat.fragment.dialog

import android.content.Context
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Zone
import com.arnyminerz.escalaralcoiaicomtat.generic.humanReadableByteCountBin
import com.arnyminerz.escalaralcoiaicomtat.generic.runAsync
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import timber.log.Timber

@ExperimentalUnsignedTypes
class DownloadDialog(private val context: Context, private val data: DataClass<*, *>) {
    fun show(deleteCallback: (() -> Unit)? = null) {
        val date = data.downloadDate(context)
        MaterialAlertDialogBuilder(context)
            .setTitle(data.displayName)
            .setMessage(
                context.getString(
                    R.string.dialog_downloaded_msg,
                    date?.let {
                        android.text.format.DateFormat.format(
                            context.getString(R.string.date_format),
                            date
                        )
                    } ?: "N/A"
                ) + '\n' + context.getString(
                    R.string.dialog_uses_storage_msg,
                    humanReadableByteCountBin(data.size(context))
                )
            )
            .setNeutralButton(R.string.action_delete) { dialog, _ ->
                dialog.dismiss()
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.downloads_delete_dialog_title)
                    .setMessage(
                        context.getString(
                            R.string.downloads_delete_dialog_msg,
                            data.displayName
                        )
                    )
                    .setPositiveButton(R.string.action_delete) { d, _ ->
                        d.dismiss()
                        when (data) {
                            is Area, is Zone, is Sector -> runAsync {
                                for (child in data.children)
                                    if (child is DataClass<*, *>)
                                        child.delete(context)

                                data.delete(context)
                                deleteCallback?.invoke()
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
            .show()
    }
}
