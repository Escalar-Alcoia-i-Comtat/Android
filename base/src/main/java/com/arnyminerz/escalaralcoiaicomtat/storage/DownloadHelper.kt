package com.arnyminerz.escalaralcoiaicomtat.storage

import android.app.DownloadManager
import android.app.DownloadManager.EXTRA_DOWNLOAD_ID
import android.app.DownloadManager.Request.VISIBILITY_VISIBLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException
import android.net.Uri
import android.os.Environment
import androidx.annotation.StringRes
import com.arnyminerz.escalaralcoiaicomtat.R
import timber.log.Timber
import java.io.*
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.*

data class DownloadProgress(
    val downloadId: Long,
    val progress: Long,
    val max: Long,
    private val lastModifiedLong: Long,
    private val localUriString: String?,
    val status: Int,
    val reason: Int
) : Serializable {
    val lastModified: Date = Date(lastModifiedLong)
    val localUri: URI? = localUriString?.let { URI.create(it) }

    fun isDownloading(): Boolean = status == DownloadManager.STATUS_RUNNING
    fun completed(): Boolean = status == DownloadManager.STATUS_SUCCESSFUL
    fun hasFailed(): Boolean = status == DownloadManager.STATUS_FAILED
}

@Suppress("unused")
class DownloadHelper(private val context: Context) {
    private val downloadManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
    private var downloads = arrayListOf<Long>()

    private var mobileDownloadAllowed = false
    private var roamingDownloadAllowed = false

    private var targetFile: File? = null

    private var notificationTitle = R.string.notification_download_progress_title
    private var notificationMessage = R.string.notification_download_progress_msg

    private var onFinishedListener: ((context: Context?, file: File?, intent: Intent?) -> Unit)? =
        null
    private var onNotificationClickListener: ((context: Context?, intent: Intent?) -> Unit)? = null

    private val onComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val downloads = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = if (targetFile != null && downloads != null) File(
                downloads,
                targetFile!!.name
            ) else null

            if (intent != null && intent.extras != null) {
                var extras = "\n"
                for (key in intent.extras!!.keySet()) {
                    extras += "  $key -> ${intent.extras!![key]}\n"
                }
                Timber.w("Download Completed! Intent Extras:$extras")

                val data = progressDataOf(intent.extras!!.getLong(EXTRA_DOWNLOAD_ID))
                if (data != null)
                    if (data.hasFailed())
                        Timber.e("Download has failed! Reason: ${data.reason}")
                    else {
                        Timber.v("Download Complete. Status: ${data.status}. Progress: ${data.progress}/${data.max}")
                        onFinishedListener?.invoke(context, file, intent)
                    }
            } else
                onFinishedListener?.invoke(context, file, intent)

            context?.unregisterReceiver(this)
            context?.unregisterReceiver(onNotificationClick)
        }
    }

    private val onNotificationClick = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.w("Clicked notification!")
            onNotificationClickListener?.invoke(context, intent)
        }
    }

    fun allowMobile(allowed: Boolean = true) {
        mobileDownloadAllowed = allowed
    }

    fun allowRoaming(allowed: Boolean = true) {
        roamingDownloadAllowed = allowed
    }

    fun notificationTitle(@StringRes title: Int) {
        notificationTitle = title
    }

    fun notificationMessage(@StringRes message: Int) {
        notificationMessage = message
    }

    fun onFinished(listener: (context: Context?, file: File?, intent: Intent?) -> Unit) {
        onFinishedListener = listener
    }

    fun onNotificationClick(listener: (context: Context?, intent: Intent?) -> Unit) {
        onNotificationClickListener = listener
    }

    fun download(url: String, targetFile: File, override: Boolean = true) =
        download(Uri.parse(url), targetFile, override)

    fun download(uri: Uri, targetFile: File, override: Boolean = true) {
        this.targetFile = targetFile
        Timber.d("Preparing to download $uri...")
        targetFile.parentFile?.mkdirs()

        if (targetFile.exists() && override) {
            targetFile.delete()
            Timber.w("Overriding old file...")
        }

        Timber.d("Registering receivers...")
        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        context.registerReceiver(
            onNotificationClick,
            IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED)
        )

        Timber.d("Enqueueing download!")
        downloads.add(
            downloadManager.enqueue(
                DownloadManager.Request(uri)
                    .apply {
                        setAllowedNetworkTypes(
                            if (mobileDownloadAllowed)
                                DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                            else DownloadManager.Request.NETWORK_WIFI
                        )
                        setAllowedOverRoaming(roamingDownloadAllowed)
                        setDestinationInExternalFilesDir(
                            context,
                            Environment.DIRECTORY_PICTURES,
                            targetFile.name
                        )
                        setTitle(context.getString(notificationTitle))
                        setDescription(context.getString(notificationMessage))
                        setNotificationVisibility(VISIBILITY_VISIBLE)
                    }
            )
        )

        queryProgress()
    }

    fun progressDataOf(id: Long): DownloadProgress? {
        val c: Cursor =
            downloadManager.query(DownloadManager.Query().setFilterById(id)) ?: return null

        try {
            c.moveToFirst()

            val colId = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_ID))
            val progress =
                c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val max = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val lastModified =
                c.getLong(c.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP))
            val uri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
            val status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))
            val reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON))

            c.close()

            return DownloadProgress(colId, progress, max, lastModified, uri, status, reason)
        } catch (ex: CursorIndexOutOfBoundsException) {
            Timber.e(ex, "Index out of bounds!")
            return null
        } catch (ex: OutOfMemoryError) {
            Timber.e(
                ex,
                "Is this android.database.CursorWindowAllocationException? Don't know, and don't want to know :) Just skipping the exception."
            )
            return null
        }
    }

    fun queryProgress(index: Int = 0): DownloadProgress? = progressDataOf(downloads[index])
}

/**
 * Download a file
 *
 * @param url The url to download
 * @param file The path of the file for writing to
 * @throws FileNotFoundException 404 Handling
 * @throws IOException Other IO issues
 */
@Deprecated("Please, use DownloadHelper Class")
@ExperimentalUnsignedTypes
fun downloadFile(
    url: String,
    file: File,
    progressUpdate: ((progress: UInt, max: UInt) -> Unit)? = null
) {
    Timber.v("Downloading $url...")

    Timber.d("Preparing to download $url...")
    file.parentFile?.mkdirs()

    try {
        val u = URL(url)
        val c = u.openConnection() as HttpURLConnection
        c.setRequestProperty("User-agent", "Mozilla/5.0")
        c.setRequestProperty("Accept-Charset", "UTF-8")
        c.setRequestProperty("Content-Type", "text/plain; charset=utf-8")
        c.requestMethod = "GET"
        when (val responseCode = c.responseCode) {
            200 -> {
                val fileContainer = file.parentFile!!
                fileContainer.mkdirs()
                if (!fileContainer.exists()) {
                    Timber.e("Could not create parent directory")
                    throw IOException("Could not create parent directory")
                }

                val fos = FileOutputStream(file)
                val inputStream = c.inputStream

                val buffer = ByteArray(4096)
                val size = inputStream.available()
                var i = 0

                while (true) {
                    val read = inputStream.read(buffer)
                    if (read == -1) break
                    i++
                    fos.write(buffer, 0, read)
                    progressUpdate?.let { it(i.toUInt(), size.toUInt()) }
                }

                fos.close()
                inputStream.close()
            }
            else -> throw IOException("Could not fetch url. Error code: $responseCode. Message: ${c.responseMessage}")
        }
    } catch (ex: IOException) {
        throw ex
    }
}
