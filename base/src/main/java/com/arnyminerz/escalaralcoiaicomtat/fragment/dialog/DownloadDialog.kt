package com.arnyminerz.escalaralcoiaicomtat.fragment.dialog

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.UpdatingActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Zone
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.generic.humanReadableByteCountBin
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.runOnUiThread
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
                            is Area, is Zone, is Sector -> GlobalScope.launch {
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
            .setPositiveButton(
                R.string.action_check_update
            ) { dialog, _ ->
                dialog as AlertDialog
                dialog.show()
                Timber.v("Checking for updates...")

                val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                button.isEnabled = false
                button.text = context.getString(R.string.status_checking_updates)

                GlobalScope.launch {
                    try {
                        val updateAvailable = data.updateAvailable(context)
                        context.runOnUiThread {
                            dialog.dismiss()
                            if (updateAvailable) {
                                Timber.v("  New version available!")
                                context.runOnUiThread {
                                    MaterialAlertDialogBuilder(context)
                                        .setTitle(R.string.dialog_update_title)
                                        .setMessage(R.string.dialog_update_message)
                                        .setPositiveButton(R.string.action_update) { _, _ ->
                                            Timber.v("  Going to update!")
                                            startActivity(
                                                Intent(
                                                    context,
                                                    UpdatingActivity::class.java
                                                ).putExtra("update_${data.namespace}", data.id)
                                            )
                                        }.show()
                                }
                            } else {
                                Timber.v("  No new version available!")
                                context.runOnUiThread {
                                    MaterialAlertDialogBuilder(context)
                                        .setTitle(R.string.dialog_no_update_title)
                                        .setMessage(R.string.dialog_no_update_message)
                                        .setNegativeButton(R.string.action_close, null)
                                        .show()
                                }
                            }
                        }
                    } catch (error: NoInternetAccessException) {
                        Timber.e(error, "No internet access was detected")
                        toast(context, R.string.toast_error_no_internet)
                    } catch (error: Exception) {
                        Timber.e(error, "Could not check for updates.")
                        toast(context, R.string.toast_error_internal)
                    }
                }
            }
            .setNegativeButton(R.string.action_close, null)
            .show()
    }
}