package com.arnyminerz.escalaralcoiaicomtat.core.worker

import android.content.Context
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.request.MarkCompletedData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.request.MarkingDataInt
import com.arnyminerz.escalaralcoiaicomtat.core.notification.ALERT_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toWorkData
import com.arnyminerz.escalaralcoiaicomtat.core.notification.Notification
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
 * Stores the key for passing the worker parameter for if the completion the user made on the path
 * is a project.
 * @author Arnau Mora
 * @since 20210502
 */
private const val MARK_COMPLETED_IS_PROJECT = "is_project"

/**
 * Stores the key for passing the worker parameter of the notification id that tells the user there's
 * an scheduled mark as completed.
 * @author Arnau Mora
 * @since 20210502
 */
private const val MARK_COMPLETED_NOTIFICATION_ID = "notification_id"

/**
 * Notifies that the worker was called with missing data
 */
private const val MARK_COMPLETED_ERROR_MISSING_DATA = "missing_data"

/**
 * Notifies that the worker had an error while uploading data to the server
 */
private const val MARK_COMPLETED_ERROR_SERVER_UPLOAD = "server_upload"

/**
 * Stores the data required to mark a path as completed
 */
class MarkCompletedWorkerData(
    val path: Path,
    val data: MarkingDataInt
)

class MarkCompletedWorker private constructor(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    private var notificationId: Int = -1

    private var pathDisplayName: String? = null

    private fun showError(error: String): Result {
        val notification = Notification.get(notificationId)
        notification?.build()?.destroy()

        Notification.Builder(applicationContext)
            .withChannelId(ALERT_CHANNEL_ID)
            .withIcon(R.drawable.ic_notifications)
            .withTitle(R.string.notification_mark_completed_error_title)
            .withText(
                applicationContext.getString(
                    R.string.notification_mark_completed_error_message,
                    pathDisplayName,
                    error
                )
            )
            .buildAndShow()
        return failure(error)
    }

    override fun doWork(): Result {
        val pathDocument = inputData.getString(MARK_COMPLETED_PATH_DOCUMENT)
        val userUid = inputData.getString(MARK_COMPLETED_USER_UID)
        val comment = inputData.getString(MARK_COMPLETED_COMMENT)
        val notes = inputData.getString(MARK_COMPLETED_NOTES)
        notificationId = inputData.getInt(MARK_COMPLETED_NOTIFICATION_ID, -1)
        val attempts = inputData.getInt(MARK_COMPLETED_ATTEMPTS, -1)
        val falls = inputData.getInt(MARK_COMPLETED_FALLS, -1)
        val grade = inputData.getString(MARK_COMPLETED_GRADE)
        val type = inputData.getString(MARK_COMPLETED_TYPE)
        val isProject = inputData.getBoolean(MARK_COMPLETED_IS_PROJECT, false)

        pathDisplayName = pathDocument

        val notiBuilder = Notification.Builder(applicationContext, notificationId)
            .withChannelId(ALERT_CHANNEL_ID)
            .withIcon(R.drawable.ic_notifications)
            .setPersistent()

        return if (userUid == null || pathDocument == null || notificationId < 0) {
            NotificationManagerCompat.from(applicationContext)
                .cancel(notificationId)
            showError(MARK_COMPLETED_ERROR_MISSING_DATA)
        } else {
            val firestore = Firebase.firestore
            runBlocking {
                suspendCoroutine { cont ->
                    if (isProject) {
                        Timber.v("Marking \"$pathDocument\" as project...")
                        notiBuilder
                            .withTitle(R.string.notification_mark_completed_marking_project_title)
                            .withText(R.string.notification_mark_completed_marking_project_message)
                            .buildAndShow()
                        firestore
                            .document(pathDocument)
                            .collection("Completions")
                            .add(
                                hashMapOf(
                                    "timestamp" to FieldValue.serverTimestamp(),
                                    "user" to userUid,
                                    "comment" to comment,
                                    "notes" to notes,
                                    "project" to true,
                                )
                            )
                            .addOnSuccessListener {
                                Timber.i("Marked \"$pathDocument\" as project!")
                                cont.resume(Result.success())
                            }
                            .addOnFailureListener { e ->
                                Timber.e(e, "Could not mark \"$pathDocument\" as project!")
                                cont.resume(showError(MARK_COMPLETED_ERROR_SERVER_UPLOAD))
                            }
                    } else {
                        Timber.v("Marking \"$pathDocument\" as completed...")
                        notiBuilder
                            .withTitle(R.string.notification_mark_completed_marking_project_title)
                            .withText(R.string.notification_mark_completed_marking_project_message)
                            .buildAndShow()
                        firestore
                            .document(pathDocument)
                            .collection("Completions")
                            .add(
                                hashMapOf(
                                    "timestamp" to FieldValue.serverTimestamp(),
                                    "user" to userUid,
                                    "attempts" to attempts,
                                    "falls" to falls,
                                    "grade" to grade,
                                    "type" to type,
                                    "comment" to comment,
                                    "notes" to notes,
                                    "project" to false
                                )
                            )
                            .addOnSuccessListener {
                                Timber.i("Marked \"$pathDocument\" as completed!")
                                cont.resume(Result.success())
                            }
                            .addOnFailureListener { e ->
                                Timber.e(e, "Could not mark \"$pathDocument\" as completed!")
                                cont.resume(showError(MARK_COMPLETED_ERROR_SERVER_UPLOAD))
                            }
                    }
                        // This will get called in both cases.
                        .addOnCompleteListener {
                            NotificationManagerCompat.from(applicationContext)
                                .cancel(notificationId)
                        }
                }
            }
        }
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
                .setPersistent(true)
            val notificationId = notificationBuilder.id
            notificationBuilder.buildAndShow()

            val completionData = data.data

            Timber.v("Processing request parameters...")
            val inputDataPairs: ArrayMap<String, Any?> = arrayMapOf(
                MARK_COMPLETED_PATH_DOCUMENT to data.path.documentPath,

                MARK_COMPLETED_USER_UID to completionData.user.uid,
                MARK_COMPLETED_COMMENT to completionData.comment,
                MARK_COMPLETED_NOTES to completionData.notes,

                MARK_COMPLETED_NOTIFICATION_ID to notificationId
            )
            if (completionData is MarkCompletedData)
                inputDataPairs.apply {
                    put(MARK_COMPLETED_ATTEMPTS, completionData.attempts.toString())
                    put(MARK_COMPLETED_FALLS, completionData.falls.toString())
                    put(MARK_COMPLETED_GRADE, completionData.grade)
                    put(MARK_COMPLETED_TYPE, completionData.type.id)
                    put(MARK_COMPLETED_IS_PROJECT, false)
                }
            else
                inputDataPairs.apply {
                    put(MARK_COMPLETED_IS_PROJECT, true)
                }
            val workData = inputDataPairs.toWorkData()

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
