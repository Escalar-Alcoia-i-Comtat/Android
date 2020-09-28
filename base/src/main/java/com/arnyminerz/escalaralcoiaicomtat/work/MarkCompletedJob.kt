package com.arnyminerz.escalaralcoiaicomtat.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.async.EXTENDED_API_URL
import com.arnyminerz.escalaralcoiaicomtat.exception.JSONResultException
import com.arnyminerz.escalaralcoiaicomtat.generic.jsonFromUrl
import com.arnyminerz.escalaralcoiaicomtat.notification.TASK_COMPLETED_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.notification.updateNotification
import kotlinx.coroutines.runBlocking
import timber.log.Timber

const val MARK_COMPLETED_JOB_PATH = "path"
const val MARK_COMPLETED_JOB_PATH_DISPLAY_NAME = "path_name"
const val MARK_COMPLETED_JOB_COMPLETED_TYPE = "completed_type"
const val MARK_COMPLETED_JOB_ATTEMPTS = "attempts"
const val MARK_COMPLETED_JOB_HANGS = "hangs"
const val MARK_COMPLETED_JOB_USER_UID = "user_uid"

@ExperimentalUnsignedTypes
class MarkCompletedJob(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val id = inputData.getInt(MARK_COMPLETED_JOB_PATH, -1)
        val displayName = inputData.getString(MARK_COMPLETED_JOB_PATH_DISPLAY_NAME)
        val completedType = inputData.getInt(MARK_COMPLETED_JOB_COMPLETED_TYPE, -1)
        val attempts = inputData.getInt(MARK_COMPLETED_JOB_ATTEMPTS, -1)
        val hangs = inputData.getInt(MARK_COMPLETED_JOB_HANGS, -1)
        val uid = inputData.getString(MARK_COMPLETED_JOB_USER_UID)

        var error = false
        if (id < 0) {
            Timber.e("No ID was set")
            error = true
        }
        if (displayName == null) {
            Timber.e("No Display Name was set")
            error = true
        }
        if (completedType < 0) {
            Timber.e("No Completion Type was set")
            error = true
        }
        if (attempts < 0) {
            Timber.e("No Attempts was set")
            error = true
        }
        if (hangs < 0) {
            Timber.e("No Hangs was set")
            error = true
        }
        if (uid == null) {
            Timber.e("No UID was set")
            error = true
        }
        if (error)
            return Result.failure()

        val json = runBlocking {
            jsonFromUrl("$EXTENDED_API_URL/user/$uid/mark_completed/$id/?type=$completedType&attempts=$attempts&hangs=$hangs")
        }

        if (json.has("error")) {
            Timber.e(JSONResultException(json, "Could not mark as completed."))
            return Result.retry()
        }

        updateNotification(
            applicationContext,
            TASK_COMPLETED_CHANNEL_ID,
            R.drawable.ic_notifications,
            R.string.notification_task_completed_title,
            applicationContext.getString(
                R.string.notification_task_completed_mark_completed_message,
                displayName
            )
        )

        Timber.v("Successfully marked as completed")
        return Result.success()
    }
}