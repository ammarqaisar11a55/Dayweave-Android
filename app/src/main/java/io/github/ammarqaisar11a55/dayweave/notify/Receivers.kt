package io.github.ammarqaisar11a55.dayweave.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import io.github.ammarqaisar11a55.dayweave.core.Dayweave
import io.github.ammarqaisar11a55.dayweave.core.params
import kotlin.concurrent.thread

/** Runs [work] off the main thread while keeping the broadcast alive until it finishes. */
private fun BroadcastReceiver.inBackground(work: () -> Unit) {
    val pending = goAsync()
    thread(name = "dayweave-broadcast") {
        try {
            work()
        } finally {
            pending.finish()
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = inBackground { Scheduler.runTick(context) }
}

/** After a restart, an update, or a change of clock or timezone, every alarm has to be set again. */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in Actions) return
        inBackground {
            Scheduler.ensurePeriodic(context)
            Scheduler.runTick(context)
        }
    }

    private companion object {
        val Actions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            // AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED, which only exists from Android 12.
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}

/** The Complete and Snooze buttons on a task reminder. They work without opening the app. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = inBackground {
        val taskId = intent.getLongExtra(Notifications.EXTRA_TASK, -1)
        val notification = intent.getIntExtra(Notifications.EXTRA_NOTIFICATION, 0)
        if (taskId < 0) return@inBackground
        Dayweave.init(context)
        runCatching {
            when (intent.action) {
                Notifications.ACTION_COMPLETE -> Dayweave.callBlocking("tasks.complete", params("id" to taskId))
                Notifications.ACTION_SNOOZE -> Dayweave.callBlocking("tasks.snooze", params("id" to taskId, "minutes" to 10))
            }
        }
        NotificationManagerCompat.from(context).cancel(notification)
        Dayweave.refresh()
        Scheduler.reschedule(context)
    }
}
