package com.arnyminerz.escalaralcoiaicomtat.worker

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.completion.request.MarkCompletedData
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.completion.request.MarkingDataInt
import com.arnyminerz.escalaralcoiaicomtat.notification.ALERT_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.notification.Notification
import timber.log.Timber

/**
 * Stores the key for passing the worker parameter of the [Path]'s Firestore document path.
 * @author Arnau Mora
 * @since 20210502
 */
private const val MARK_COMPLETED_PATH_DOCUMENT = "path_document"

/**
 * Stores the key for passing the worker parameter of the user UID that is marking the path.
 * @author Arnau Mora
 * @since 20210502
 */
private const val MARK_COMPLETED_USER_UID = "user_uid"

/**
 * Stores the key for passing the worker parameter of the comment that the user made on the path.
 * @author Arnau Mora
 * @since 20210502
 */
private const val MARK_COMPLETED_COMMENT = "comment"

/**
 * Stores the key for passing the worker parameter of the notes the user took on the path.
 * @author Arnau Mora
 * @since 20210502
 */
private const val MARK_COMPLETED_NOTES = "notes"

/**
 * Stores the key for passing the worker parameter of the attempts the user made on the path.
 * @author Arnau Mora
 * @since 20210502
 */
private const val MARK_COMPLETED_ATTEMPTS = "attempts"

/**
 * Stores the key for passing the worker parameter of the falls the user made on the path.
 * @author Arnau Mora
 * @since 20210502
 */
private const val MARK_COMPLETED_FALLS = "falls"

/**
 * Stores the key for passing the worker parameter of the grade the user estimates on the path.
 * @author Arnau Mora
 * @since 20210502
 */
private const val MARK_COMPLETED_GRADE = "grade"

/**
 * Stores the key for passing the worker parameter of the type of completion the user made on the path.
 * @author Arnau Mora
 * @since 20210502
 */
private const val MARK_COMPLETED_TYPE = "type"

/**
 * Stores the key for passing the worker parameter of the notification id that tells the user there's
 * an scheduled mark as completed.
 * @author Arnau Mora
 * @since 20210502
 */
private const val MARK_COMPLETED_NOTIFICATION_ID = "notification_id"

/**
 * Stores the data required to mark a path as completed
 */
class MarkCompletedWorkerData(
    val path: Path,
    val data: MarkingDataInt
)

class MarkCompletedWorker private constructor(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        TODO("Not yet implemented")
    }

    companion object {
        /**
         * Schedules a new mark completed job as a new Worker.
         * @author Arnau Mora
         * @since 20210313
         * @param context The context to run from
         * @param tag The tag of the worker
         * @param data The data of the marking
         * @return The scheduled operation
         */
        fun schedule(
            context: Context,
            tag: String,
            data: MarkCompletedWorkerData
        ): LiveData<WorkInfo> {
            Timber.v("Scheduling new mark completed work...")
            Timber.v("Building mark completed constraints...")
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)

            Timber.v("Showing waiting notification...")
            val notificationBuilder = Notification.Builder(context)
                .withChannelId(ALERT_CHANNEL_ID)
                .withIcon(R.drawable.ic_notifications)
                .withTitle(R.string.notification_mark_completed_waiting_title)
                .withText(R.string.notification_mark_completed_waiting_message)
            val notificationId = notificationBuilder.id
            notificationBuilder.buildAndShow()

            val completionData = data.data

            Timber.v("Processing request parameters...")
            val inputDataPairs = arrayListOf(
                MARK_COMPLETED_PATH_DOCUMENT to data.path.documentPath,

                MARK_COMPLETED_USER_UID to completionData.user.uid,
                MARK_COMPLETED_COMMENT to completionData.comment,
                MARK_COMPLETED_NOTES to completionData.notes,

                MARK_COMPLETED_NOTIFICATION_ID to notificationId
            )
            if (completionData is MarkCompletedData)
                inputDataPairs.apply {
                    add(MARK_COMPLETED_ATTEMPTS to completionData.attempts.toString())
                    add(MARK_COMPLETED_FALLS to completionData.falls.toString())
                    add(MARK_COMPLETED_GRADE to completionData.grade)
                    add(MARK_COMPLETED_TYPE to completionData.type.id)
                }
            val workData = workDataOf(*inputDataPairs.toTypedArray())

            Timber.v("Building MarkCompletedWorker request...")
            val request = OneTimeWorkRequestBuilder<MarkCompletedWorker>()
                .setConstraints(constraints.build())
                .addTag(tag)
                .setInputData(workData)
                .build()

            Timber.v("Getting WorkManager instance, and enqueueing job.")
            val workManager = WorkManager
                .getInstance(context)
            workManager.enqueue(request)

            return workManager.getWorkInfoByIdLiveData(request.id)
        }
    }
}
