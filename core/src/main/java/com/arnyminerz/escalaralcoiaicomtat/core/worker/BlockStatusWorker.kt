package com.arnyminerz.escalaralcoiaicomtat.core.worker

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.database.BlockingDatabase
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.database.DataClassDatabase
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.repository.BlockingRepository
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.repository.DataClassRepository
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.BlockingData
import com.arnyminerz.escalaralcoiaicomtat.core.notification.Notification
import com.arnyminerz.escalaralcoiaicomtat.core.notification.TASK_FAILED_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.notification.TASK_IN_PROGRESS_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.utils.clipboard
import com.arnyminerz.escalaralcoiaicomtat.core.utils.copy
import timber.log.Timber
import java.util.concurrent.TimeUnit

class BlockStatusWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Timber.v("Getting search session...")
        val database = DataClassDatabase.getInstance(applicationContext)
        val repository = DataClassRepository(
            database.areasDao(),
            database.zonesDao(),
            database.sectorsDao(),
            database.pathsDao(),
        )

        val blockingDatabase = BlockingDatabase.getInstance(applicationContext)
        val blockingRepository = BlockingRepository(blockingDatabase.blockingDao())

        Timber.v("Getting paths from search session...")
        val paths = repository.getPaths().map { it.data() }
        val pathsCount = paths.size

        return if (paths.isEmpty()) {
            Timber.e("Paths is empty.")
            Result.failure()
        } else {// First, create the info notification...
            var notification = Notification.Builder(applicationContext)
                .withChannelId(TASK_IN_PROGRESS_CHANNEL_ID)
                .withIcon(R.drawable.ic_notifications)
                .withTitle(R.string.notification_block_status_title)
                .withText(R.string.notification_block_status_message)
                .setPersistent(true)
                .setAlertOnce(true)
                .withProgress(0, pathsCount)
                .buildAndShow()

            Timber.v("Extracting block statuses...")
            val blockingStatuses = arrayListOf<BlockingData>()
            val failedBlockStatus = arrayListOf<String>()
            for ((i, path) in paths.withIndex()) {
                try {
                    notification = notification.edit()
                        .withText(
                            applicationContext.getString(
                                R.string.notification_block_status_progress,
                                path.displayName
                            )
                        )
                        .withInfoText("${i + 1}/$pathsCount")
                        .withProgress(i, pathsCount)
                        .buildAndShow()
                    path.fetchBlockStatus(applicationContext)
                        ?.let { blockingStatuses.add(it) }
                } catch (e: RuntimeException) {
                    Timber.e(e, "Could not fetch block status of ${path.displayName}")
                    failedBlockStatus.add(path.displayName)
                }
            }

            if (failedBlockStatus.isNotEmpty()) {
                notification.destroy()
                Timber.v("Showing failed block status fetch notification")
                Notification.Builder(applicationContext)
                    .withChannelId(TASK_FAILED_CHANNEL_ID)
                    .withIcon(R.drawable.ic_notifications)
                    .withTitle(R.string.notification_block_status_error_title)
                    .withText(
                        applicationContext.getString(
                            R.string.notification_block_status_error_count,
                            failedBlockStatus.size
                        )
                    )
                    .withLongText(
                        applicationContext.getString(
                            R.string.notification_block_status_error_count,
                            failedBlockStatus.size
                        )
                    )
                    .addAction(
                        R.drawable.content_copy,
                        R.string.action_copy
                    ) {
                        // Generate the items list
                        val sb = StringBuilder()
                        for (pathName in failedBlockStatus)
                            sb.appendLine("- $pathName")

                        // Copy to clipboard
                        val clipboard = applicationContext.clipboard
                        val text = context.getString(
                            R.string.notification_block_status_error_log,
                            failedBlockStatus.size,
                            sb.toString()
                        )
                        text.copy(clipboard)

                        // Hide the notification
                        Notification.dismiss(applicationContext, id)
                    }
                    .buildAndShow()
            }

            Timber.v("Building storing notification...")
            notification = notification.edit()
                .withText(R.string.notification_block_status_storing)
                .withProgress(-1, 0)
                .buildAndShow()

            try {
                Timber.v("Putting elements...")
                blockingRepository.addAll(blockingStatuses)

                Timber.v("Closing databases...")
                database.close()
                blockingDatabase.close()

                Timber.v("Dismissing progress notification...")
                notification.destroy()

                Timber.v("Finished fetching block statuses...")
                Result.success()
            } catch (e: Exception) {
                // Show the error notification
                notification.destroy()
                Notification.Builder(applicationContext)
                    .withChannelId(TASK_FAILED_CHANNEL_ID)
                    .withIcon(R.drawable.ic_notifications)
                    .withTitle(R.string.notification_block_status_error_title)
                    .withText(R.string.notification_block_status_error_store_short)
                    .buildAndShow()
                return Result.failure()
            } finally {
                // Destroy the progress notification
                Timber.v("Destroying progress notification.")
                notification.destroy()
            }
        }
    }

    companion object {
        const val WORKER_TAG = "BlockStatusWorker"

        /**
         * The amount of time that should be between status updates.
         * The first element is the amount, and the second one the time unit.
         * @author Arnau Mora
         * @since 20210825
         */
        val WORKER_SCHEDULE: Pair<Long, TimeUnit> = 12L to TimeUnit.HOURS

        /**
         * Serves as the error work result for showing how much successes there were.
         * @author Arnau Mora
         * @since 20210824
         */
        const val WORK_RESULT_STORE_SUCCESSES = "StoreSuccesses"

        /**
         * Serves as the error work result for showing how much failures there were.
         * @author Arnau Mora
         * @since 20210824
         */
        const val WORK_RESULT_STORE_FAILURES = "StoreFailures"

        /**
         * Generates a tag from [WORKER_SCHEDULE] so it can be detected when the schedule should
         * be updated.
         * @author Arnau Mora
         * @since 20210825
         */
        val WORKER_SCHEDULE_TAG: String
            get() = "BSW" + WORKER_SCHEDULE.second.toMinutes(WORKER_SCHEDULE.first)

        /**
         * Checks if the worker is scheduled.
         * @author Arnau Mora
         * @since 20210826
         * @param context The [Context] that wants to make the check.
         */
        @WorkerThread
        suspend fun isScheduled(context: Context): Boolean {
            val workInfos = WorkManager
                .getInstance(context)
                .getWorkInfosByTag(WORKER_TAG)
                .await()
            return workInfos.isNotEmpty()
        }

        /**
         * Fetches the work info for the [BlockStatusWorker].
         * @author Arnau Mora
         * @since 20210825
         * @param context The [Context] that wants to make the check.
         * @return The [WorkInfo] of the job, or null if no job is scheduled.
         */
        @WorkerThread
        suspend fun info(context: Context): WorkInfo? =
            WorkManager
                .getInstance(context)
                .getWorkInfosByTag(WORKER_TAG) // Gets the work info
                .await() // Awaits the result
                .ifEmpty { null } // If not results were found, update to null
                ?.get(0) // Get the first element if not null

        /**
         * Checks if the worker is scheduled correctly with the latest update.
         * @author Arnau Mora
         * @since 20210825
         * @param context The [Context] that wants to make the check.
         * @return true if the worker is scheduled badly, and should be updated.
         */
        @WorkerThread
        suspend fun shouldUpdateSchedule(context: Context): Boolean {
            val workManager = WorkManager.getInstance(context)
            val workInfo = workManager
                .getWorkInfosByTag(WORKER_TAG) // Gets the work info
                .await() // Awaits the result
                .ifEmpty { null } // If not results were found, update to null
                ?.get(0) // Get the first element if not null
            val tags = workInfo?.tags ?: emptySet() // Get the work's tags
            return !tags.contains(WORKER_SCHEDULE_TAG)
        }

        /**
         * Cancels all ongoing BlockStatusWorkers.
         * @author Arnau Mora
         * @since 20210824
         * @param context The [Context] that wants to cancel the worker.
         */
        @WorkerThread
        suspend fun cancel(context: Context) =
            WorkManager.getInstance(context)
                .cancelAllWorkByTag(WORKER_TAG)
                .await()

        /**
         * Schedules the worker to run. Note that the worker should not be scheduled more than once,
         * for checking whether or not to schedule the job, you can use [isScheduled]. [schedule]
         * should only be called if [isScheduled] is false, otherwise the user's device may be
         * overworking.
         * @author Arnau Mora
         * @since 20210824
         * @param context The [Context] that is scheduling the worker.
         */
        fun schedule(context: Context) {
            val workConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val builder = PeriodicWorkRequestBuilder<BlockStatusWorker>(
                WORKER_SCHEDULE.first, WORKER_SCHEDULE.second, // Repeat interval
            )
            val workRequest = builder
                .addTag(WORKER_TAG)
                .addTag(WORKER_SCHEDULE_TAG)
                .setConstraints(workConstraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORKER_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
