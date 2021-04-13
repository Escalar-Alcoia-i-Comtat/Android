package com.arnyminerz.escalaralcoiaicomtat.fragment.dialog

import android.content.Context
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.generic.doAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.humanReadableByteCountBin
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class DownloadDialog(
    private val context: Context,
    private val data: DataClass<*, *>,
    private val firestore: FirebaseFirestore
) {
    fun show(deleteCallback: (() -> Unit)? = null) {
        val date = data.downloadDate(context)
        MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_App_MaterialAlertDialog)
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
                    runBlocking { humanReadableByteCountBin(data.size(context)) }
                )
            )
            .setNeutralButton(R.string.action_delete) { dialog, _ ->
                dialog.dismiss()
                MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_App_MaterialAlertDialog)
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
                            is Area, is Zone, is Sector ->
                                doAsync {
                                    val children = arrayListOf<DataClassImpl>()
                                    data.getChildren(firestore).toCollection(children)
                                    for (child in children)
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
