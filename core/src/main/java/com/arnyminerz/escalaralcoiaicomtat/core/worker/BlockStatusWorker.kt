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
import com.android.volley.TimeoutError
import com.android.volley.VolleyError
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.database.BlockingDatabase
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.repository.BlockingRepository
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.BlockingData
import com.arnyminerz.escalaralcoiaicomtat.core.notification.Notification
import com.arnyminerz.escalaralcoiaicomtat.core.notification.TASK_FAILED_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.notification.TASK_IN_PROGRESS_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REST_API_BLOCKING_ENDPOINT
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getDate
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getJson
import com.arnyminerz.escalaralcoiaicomtat.core.utils.hasValid
import com.arnyminerz.escalaralcoiaicomtat.core.utils.then
import org.json.JSONObject
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit

class BlockStatusWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = blockStatusFetchRoutine(applicationContext)

    companion object {
        private const val WORKER_TAG = "BlockStatusWorker"

        /**
         * The amount of time that should be between status updates.
         * The first element is the amount, and the second one the time unit.
         * @author Arnau Mora
         * @since 20210825
         */
        private val WORKER_SCHEDULE: Pair<Long, TimeUnit> = 12L to TimeUnit.HOURS

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
        private val WORKER_SCHEDULE_TAG: String
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

            Timber.v("Enqueueing BlockStatusWorker periodic work...")
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORKER_TAG,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }

        /**
         * Runs [Context.getJson] for fetching all the blockages on the server endpoint.
         * @author Arnau Mora
         * @since 20220330
         * @param context The context to fetch from.
         */
        private suspend fun fetchAllBlockagesFromServer(context: Context): JSONObject =
            context.getJson("$REST_API_BLOCKING_ENDPOINT/*")

        @WorkerThread
        suspend fun blockStatusFetchRoutine(applicationContext: Context): Result {
            Timber.d("Opening blockages database...")
            val blockingDatabase = BlockingDatabase.getInstance(applicationContext)
            val blockingRepository = BlockingRepository(blockingDatabase.blockingDao())

            var notification = Notification.Builder(applicationContext)
                .withChannelId(TASK_IN_PROGRESS_CHANNEL_ID)
                .withIcon(R.drawable.ic_notifications)
                .withTitle(R.string.notification_block_status_title)
                .withText(R.string.notification_block_status_message)
                .setPersistent(true)
                .setAlertOnce(true)
                .build()

            return try {
                // First, create the info notification...
                notification.show()

                Timber.v("Getting block statuses from server...")
                val blockStatusJson = fetchAllBlockagesFromServer(applicationContext)

                Timber.v("Parsing block statuses response...")
                if (!blockStatusJson.has("result"))
                    throw IllegalArgumentException("Blocking result list doesn't have a result. JSON: $blockStatusJson")
                val blockStatusResult = blockStatusJson.getJSONObject("result")
                val pathIds = blockStatusResult.keys()

                Timber.v("Extracting block statuses...")
                val blockingStatuses = arrayListOf<BlockingData>()

                for (objectId in pathIds) {
                    // Get the JSON object at current iteration position
                    val blockStatus = blockStatusResult.getJSONObject(objectId)

                    // If blocked is false, continue
                    if (!blockStatus.getBoolean("blocked")) continue

                    // Otherwise, add item to blockingStatuses
                    val blockingData = try {
                        BlockingData(
                            UUID.randomUUID().toString(),
                            objectId,
                            blockStatus.getString("type"),
                            blockStatus.hasValid("endDate").then { blockStatus.getDate("endDate") },
                        )
                    } catch (e: Exception) {
                        continue
                    }
                    blockingStatuses.add(blockingData)
                }

                Timber.v("Building storing notification...")
                notification = notification.edit()
                    .withText(R.string.notification_block_status_storing)
                    .withProgress(-1, 0)
                    .buildAndShow()

                try {
                    Timber.v("Clearing old blocking statuses...")
                    blockingRepository.clear()

                    Timber.v("Putting elements...")
                    blockingRepository.addAll(blockingStatuses)

                    // Timber.v("Closing database...")
                    // blockingDatabase.close()

                    Timber.v("Dismissing progress notification...")
                    notification.destroy()

                    Timber.v("Finished fetching block statuses...")
                    Result.success()
                } catch (e: Exception) {
                    Timber.e(e, "Could not store the blocking status list into the database.")

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
            } catch (e: TimeoutError) {
                Result.failure()
            } catch (e: VolleyError) {
                Result.failure()
            } finally {
                if (!notification.destroyed) {
                    // Destroy the progress notification
                    Timber.v("Destroying progress notification.")
                    notification.destroy()
                }
            }
        }
    }
}
