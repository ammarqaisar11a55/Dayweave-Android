package io.github.ammarqaisar11a55.dayweave.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.ammarqaisar11a55.dayweave.MainActivity
import io.github.ammarqaisar11a55.dayweave.R
import io.github.ammarqaisar11a55.dayweave.core.Dayweave
import io.github.ammarqaisar11a55.dayweave.core.FocusSession
import io.github.ammarqaisar11a55.dayweave.core.Notice
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import java.time.Instant

object Notifications {
    const val REMINDERS = "reminders"
    const val FOCUS = "focus"
    const val UPDATES = "updates"

    const val ACTION_COMPLETE = "io.github.ammarqaisar11a55.dayweave.COMPLETE"
    const val ACTION_SNOOZE = "io.github.ammarqaisar11a55.dayweave.SNOOZE"
    const val EXTRA_TASK = "taskId"
    const val EXTRA_HABIT = "habitId"
    const val EXTRA_KIND = "kind"
    const val EXTRA_NOTIFICATION = "notificationId"
    const val FOCUS_NOTIFICATION = 1

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(REMINDERS, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Task deadlines and habit reminders"
                },
                NotificationChannel(FOCUS, "Focus timer", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "The running focus session and when it ends"
                },
                NotificationChannel(UPDATES, "Updates", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "New versions of Dayweave"
                },
            ),
        )
    }

    /** Whether a notification would be shown: the permission on Android 13 and later, and not switched off. */
    fun allowed(context: Context): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun openApp(context: Context, notice: Notice? = null, request: Int = 0): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            notice?.taskId?.let { putExtra(EXTRA_TASK, it) }
            notice?.habitId?.let { putExtra(EXTRA_HABIT, it) }
            notice?.kind?.let { putExtra(EXTRA_KIND, it) }
        }
        return PendingIntent.getActivity(context, request, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun action(context: Context, action: String, taskId: Long, notification: Int): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_TASK, taskId)
            putExtra(EXTRA_NOTIFICATION, notification)
        }
        return PendingIntent.getBroadcast(
            context, (action + notification).hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    fun show(context: Context, notice: Notice) {
        if (!allowed(context)) return
        val id = notice.key.hashCode()
        val builder = NotificationCompat.Builder(context, if (notice.kind == "focus") FOCUS else REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notice.title)
            .setContentText(notice.body)
            .setContentIntent(openApp(context, notice, id))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        val task = notice.taskId
        if (notice.kind == "task" && task != null) {
            builder.addAction(0, "Complete", action(context, ACTION_COMPLETE, task, id))
            builder.addAction(0, "Snooze 10 min", action(context, ACTION_SNOOZE, task, id))
        }
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // Permission was withdrawn between the check and the post.
        }
    }
}

/** Keeps an ongoing countdown in the notification shade while a focus session runs. */
object FocusNotification {
    fun sync(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        val session = runCatching {
            val reply = Dayweave.callBlocking("focus.current")
            if (reply is JsonNull) null else Dayweave.json.decodeFromJsonElement<FocusSession>(reply)
        }.getOrNull()
        val end = session?.endsAt
        if (session == null || session.status != "running" || end == null || !Notifications.allowed(context)) {
            manager.cancel(Notifications.FOCUS_NOTIFICATION)
            return
        }
        val title = when (session.kind) {
            "focus" -> session.taskTitle ?: session.habitName ?: "Focusing"
            "long_break" -> "Long break"
            else -> "Short break"
        }
        val notification = NotificationCompat.Builder(context, Notifications.FOCUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("Time left")
            .setWhen(Instant.parse(end).toEpochMilli())
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(Notifications.openApp(context, Notice("focus", "focus", "")))
            .build()
        try {
            manager.notify(Notifications.FOCUS_NOTIFICATION, notification)
        } catch (_: SecurityException) {
        }
    }
}
