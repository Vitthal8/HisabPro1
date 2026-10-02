package com.hisabpro.app.data.sync.outbox

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Utility object to schedule SyncWorker operations using Android WorkManager.
 */
object SyncWorkerScheduler {

    private const val UNIQUE_ONE_TIME_WORK_NAME = "HisabPro_OutboxSync_OneTime"
    private const val UNIQUE_PERIODIC_WORK_NAME = "HisabPro_OutboxSync_Periodic"

    /**
     * Triggers an immediate one-time background sync job with exponential backoff constraints.
     */
    fun scheduleImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncWorkRequest
        )
    }

    /**
     * Schedules periodic background sync every 15 minutes with exponential backoff.
     */
    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
    }
}
