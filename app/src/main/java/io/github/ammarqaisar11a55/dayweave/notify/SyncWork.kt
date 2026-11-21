package io.github.ammarqaisar11a55.dayweave.notify

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.ammarqaisar11a55.dayweave.core.Dayweave
import io.github.ammarqaisar11a55.dayweave.core.DayweaveException
import io.github.ammarqaisar11a55.dayweave.core.SyncReport
import io.github.ammarqaisar11a55.dayweave.core.SyncStatus
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.concurrent.TimeUnit

/**
 * Sync runs as WorkManager jobs that wait for a network connection, so the app works offline and
 * changes go out as soon as the phone is back online. A change queues a sync a few seconds later,
 * and a periodic job fetches other devices' changes every 15 minutes or so.
 */
object SyncWork {
    private const val PERIODIC = "dayweave-sync"
    private const val SOON = "dayweave-sync-soon"

    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun signedIn(): Boolean = runCatching {
        Dayweave.json.decodeFromJsonElement<SyncStatus>(Dayweave.callBlocking("sync.status")).signedIn
    }.getOrDefault(false)

    fun ensurePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(online)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Queues a sync shortly. Several changes in a row lead to one sync, a few seconds after the last. */
    fun requestSoon(context: Context, delaySeconds: Long = 5) {
        if (!signedIn()) return
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(online)
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(SOON, ExistingWorkPolicy.REPLACE, request)
    }
}

class SyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        Dayweave.init(applicationContext)
        if (!SyncWork.signedIn()) return Result.success()
        return try {
            val report = Dayweave.json.decodeFromJsonElement<SyncReport>(Dayweave.callBlocking("sync.run"))
            if (report.received > 0) {
                // Other devices' changes may bring new deadlines, reminders or a finished focus session.
                Scheduler.reschedule(applicationContext)
            }
            Result.success()
        } catch (error: DayweaveException) {
            // Offline or the server is down: try again later. Anything else, such as being signed out,
            // won't fix itself by retrying.
            if (error.code == "offline") Result.retry() else Result.success()
        } finally {
            Dayweave.refresh()
        }
    }
}
