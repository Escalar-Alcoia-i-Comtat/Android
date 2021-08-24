package com.arnyminerz.escalaralcoiaicomtat.core.worker

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import java.util.concurrent.TimeUnit

class BlockStatusWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return Result.success()
    }

    companion object {
        const val WORKER_TAG = "BlockStatusWorker"

        @WorkerThread
        suspend fun isScheduled(context: Context): Boolean {
            val workInfos = WorkManager
                .getInstance(context)
                .getWorkInfosByTag(WORKER_TAG)
                .await()
            return workInfos.isNotEmpty()
        }

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

            val workRequest = PeriodicWorkRequestBuilder<BlockStatusWorker>(
                6, TimeUnit.HOURS, // Repeat interval
                15, TimeUnit.MINUTES, // Flex interval
            )
                .addTag(WORKER_TAG)
                .setConstraints(workConstraints)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}