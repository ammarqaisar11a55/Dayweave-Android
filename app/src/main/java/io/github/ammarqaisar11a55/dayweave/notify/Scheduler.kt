package io.github.ammarqaisar11a55.dayweave.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.ammarqaisar11a55.dayweave.core.Dayweave
import io.github.ammarqaisar11a55.dayweave.core.NextWake
import kotlinx.serialization.json.decodeFromJsonElement
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Android stops apps polling, so reminders run on an exact alarm set for the moment the shared core says
 * something next falls due. A WorkManager job every 15 minutes is the safety net, and it also catches
 * anything missed while the phone was off. The core's notification log makes a second delivery impossible.
 */
object Scheduler {
    private const val ALARM_REQUEST = 1
    private const val PERIODIC_WORK = "dayweave-reminders"

    private fun alarmIntent(context: Context) = PendingIntent.getBroadcast(
        context, ALARM_REQUEST, Intent(context, AlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun reschedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = alarmIntent(context)
        val next = runCatching {
            Dayweave.json.decodeFromJsonElement<NextWake>(Dayweave.callBlocking("scheduler.nextWake")).at
        }.getOrNull()
        if (next == null) {
            alarms.cancel(intent)
            return
        }
        val at = Instant.parse(next).toEpochMilli().coerceAtLeast(System.currentTimeMillis() + 1_000)
        if (canScheduleExact(context)) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } else {
            // Without the exact-alarm permission Android may deliver this a few minutes late.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    fun ensurePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<TickWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** One scheduler pass: deliver what is due, refresh the screens, and set the next alarm. */
    fun runTick(context: Context) {
        Dayweave.init(context)
        val notices = runCatching { Dayweave.tickBlocking() }.getOrDefault(emptyList())
        notices.forEach { Notifications.show(context, it) }
        FocusNotification.sync(context)
        Dayweave.refresh()
        reschedule(context)
    }
}

class TickWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        Scheduler.runTick(applicationContext)
        return Result.success()
    }
}
