package io.github.ammarqaisar11a55.dayweave.core

import kotlinx.serialization.Serializable

// Mirrors the JSON returned by the shared core. Unknown fields are ignored, so newer cores still work.

@Serializable data class Tag(val id: Long, val name: String)

@Serializable data class Subtask(val id: Long, val parentId: Long? = null, val title: String, val completed: Boolean)

@Serializable data class Reminder(val kind: String, val offsetMinutes: Long? = null, val remindAt: String? = null)

@Serializable
data class Recurrence(
    val frequency: String,
    val interval: Int = 1,
    val weekdays: List<Int> = emptyList(),
    val endDate: String? = null,
    val maxCount: Int? = null,
)

@Serializable
data class Task(
    val id: Long,
    val title: String,
    val description: String = "",
    val status: String,
    val priority: String,
    val category: String? = null,
    val projectId: Long? = null,
    val projectName: String? = null,
    val projectColor: String? = null,
    val habitId: Long? = null,
    val habitName: String? = null,
    val recurrenceId: Long? = null,
    val startDate: String? = null,
    val dueDate: String? = null,
    val dueTime: String? = null,
    val estimatedMinutes: Long? = null,
    val createdAt: String,
    val updatedAt: String,
    val completedAt: String? = null,
    val completedOn: String? = null,
    val archivedAt: String? = null,
    val overdue: Boolean = false,
    val subtaskTotal: Int = 0,
    val subtaskDone: Int = 0,
    val reminderCount: Int = 0,
    val tags: List<Tag> = emptyList(),
    val subtasks: List<Subtask> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val recurrence: Recurrence? = null,
) {
    val completed get() = status == "completed"
    val archived get() = status == "archived"
}

@Serializable
data class TaskInput(
    val title: String,
    val description: String = "",
    val status: String? = null,
    val priority: String = "medium",
    val category: String? = null,
    val projectId: Long? = null,
    val habitId: Long? = null,
    val startDate: String? = null,
    val dueDate: String? = null,
    val dueTime: String? = null,
    val estimatedMinutes: Long? = null,
    val tags: List<String> = emptyList(),
    val subtasks: List<String> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val recurrence: Recurrence? = null,
)

fun Task.toInput() = TaskInput(
    title = title, description = description, status = status, priority = priority, category = category,
    projectId = projectId, habitId = habitId, startDate = startDate, dueDate = dueDate, dueTime = dueTime,
    estimatedMinutes = estimatedMinutes, tags = tags.map { it.name }, reminders = reminders,
    recurrence = recurrence,
)

@Serializable
data class TaskCounts(
    val all: Int = 0, val today: Int = 0, val upcoming: Int = 0, val overdue: Int = 0,
    val completed: Int = 0, val archived: Int = 0,
)

@Serializable data class Completed(val task: Task, val next: Task? = null)

@Serializable
data class Project(
    val id: Long,
    val name: String,
    val description: String = "",
    val color: String = "orange",
    val icon: String = "folder",
    val startDate: String? = null,
    val deadline: String? = null,
    val archivedAt: String? = null,
    val taskTotal: Int = 0,
    val taskDone: Int = 0,
    val taskOpen: Int = 0,
    val taskOverdue: Int = 0,
    val completion: Double = 0.0,
)

@Serializable
data class HabitStats(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val streakUnit: String = "days",
    val completionRate: Double = 0.0,
    val weeklyConsistency: Double = 0.0,
    val monthlyConsistency: Double = 0.0,
    val totalDone: Int = 0,
)

@Serializable data class ChecklistItem(val id: Long = 0, val title: String)

@Serializable
data class TodayEntry(
    val state: String,
    val due: Boolean,
    val value: Double? = null,
    val checkedItems: List<Long> = emptyList(),
    val note: String = "",
    val slipped: Boolean = false,
)

@Serializable data class RecentDay(val date: String, val state: String)

@Serializable
data class Habit(
    val id: Long,
    val name: String,
    val description: String = "",
    val icon: String = "sparkles",
    val color: String = "orange",
    val kind: String = "build",
    val frequency: String = "daily",
    val weekdays: List<Int> = emptyList(),
    val weeklyTarget: Int? = null,
    val startDate: String,
    val reminderTime: String? = null,
    val quantityTarget: Double? = null,
    val unit: String = "",
    val archivedAt: String? = null,
    val checklist: List<ChecklistItem> = emptyList(),
    val today: TodayEntry,
    val stats: HabitStats = HabitStats(),
    val recent: List<RecentDay> = emptyList(),
) {
    /** A quit habit stays on track until a slip is logged. */
    val met get() = if (kind == "quit") !today.slipped else today.state == "done"
}

@Serializable
data class HabitInput(
    val name: String,
    val description: String = "",
    val icon: String = "sparkles",
    val color: String = "orange",
    val kind: String = "build",
    val frequency: String = "daily",
    val weekdays: List<Int> = listOf(0, 1, 2, 3, 4),
    val weeklyTarget: Int? = 3,
    val startDate: String? = null,
    val reminderTime: String? = null,
    val quantityTarget: Double? = null,
    val unit: String = "",
    val checklist: List<ChecklistItem> = emptyList(),
)

fun Habit.toInput() = HabitInput(
    name = name, description = description, icon = icon, color = color, kind = kind, frequency = frequency,
    weekdays = weekdays.ifEmpty { listOf(0, 1, 2, 3, 4) }, weeklyTarget = weeklyTarget ?: 3,
    startDate = startDate, reminderTime = reminderTime, quantityTarget = quantityTarget, unit = unit,
    checklist = checklist,
)

@Serializable data class HeatmapDay(val date: String, val level: Int, val done: Int, val due: Int, val missed: Boolean = false)

@Serializable data class HistoryDay(val date: String, val state: String, val value: Double? = null, val note: String = "")

@Serializable
data class WeekDay(val date: String, val tasksDone: Int, val habitsDone: Int, val habitsDue: Int, val focusSeconds: Long)

@Serializable
data class Overview(
    val date: String,
    val tasks: List<Task> = emptyList(),
    val overdue: List<Task> = emptyList(),
    val upcoming: List<Task> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val tasksDone: Int = 0,
    val tasksTotal: Int = 0,
    val habitsDone: Int = 0,
    val habitsTotal: Int = 0,
    val progress: Double = 0.0,
    val focusSeconds: Long = 0,
    val focusSessions: Int = 0,
    val week: List<WeekDay> = emptyList(),
)

@Serializable
data class FocusSession(
    val id: Long,
    val kind: String,
    val status: String,
    val plannedSeconds: Long,
    val elapsedSeconds: Long,
    val remainingSeconds: Long,
    val taskId: Long? = null,
    val taskTitle: String? = null,
    val habitId: Long? = null,
    val habitName: String? = null,
    val startedAt: String,
    val endsAt: String? = null,
)

@Serializable
data class FocusStats(
    val todaySeconds: Long = 0,
    val weekSeconds: Long = 0,
    val totalSeconds: Long = 0,
    val todaySessions: Int = 0,
    val totalSessions: Int = 0,
    val sessionsSinceLongBreak: Int = 0,
)

@Serializable
data class CalendarItem(
    val kind: String,
    val id: Long,
    val title: String,
    val time: String? = null,
    val priority: String? = null,
    val completed: Boolean = false,
    val overdue: Boolean = false,
    val seconds: Long? = null,
    val color: String? = null,
)

@Serializable
data class CalendarDay(
    val date: String,
    val items: List<CalendarItem> = emptyList(),
    val habitsDue: Int = 0,
    val habitsDone: Int = 0,
    val focusSeconds: Long = 0,
)

@Serializable
data class DayPoint(
    val date: String,
    val tasksCompleted: Int = 0,
    val habitRate: Double? = null,
    val habitsDone: Int = 0,
    val focusSeconds: Long = 0,
)

@Serializable data class WeekPoint(val week: String, val tasksCompleted: Int = 0, val habitRate: Double? = null, val focusSeconds: Long = 0)

@Serializable
data class HabitSummary(
    val id: Long,
    val name: String,
    val color: String = "orange",
    val icon: String = "sparkles",
    val currentStreak: Int = 0,
    val streakUnit: String = "days",
    val monthlyConsistency: Double = 0.0,
)

@Serializable
data class Analytics(
    val days: Int,
    val tasksCompleted: Int = 0,
    val tasksOverdue: Int = 0,
    val tasksPending: Int = 0,
    val taskCompletionRate: Double = 0.0,
    val habitCompletionRate: Double = 0.0,
    val focusSeconds: Long = 0,
    val focusSessions: Int = 0,
    val daily: List<DayPoint> = emptyList(),
    val weekly: List<WeekPoint> = emptyList(),
    val habits: List<HabitSummary> = emptyList(),
    val mostConsistent: List<HabitSummary> = emptyList(),
    val needsImprovement: List<HabitSummary> = emptyList(),
)

@Serializable
data class Settings(
    val theme: String = "system",
    val weekStart: String = "monday",
    val timeFormat: String = "24h",
    val defaultPriority: String = "medium",
    val defaultReminderMinutes: Long? = 30,
    val confirmDelete: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val taskNotifications: Boolean = true,
    val habitNotifications: Boolean = true,
    val focusNotifications: Boolean = true,
    val dateOnlyReminderTime: String = "09:00",
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
)

@Serializable
data class Notice(
    val key: String,
    val kind: String,
    val title: String,
    val body: String = "",
    val taskId: Long? = null,
    val habitId: Long? = null,
)

@Serializable data class NextWake(val at: String? = null)

/** Whether this phone syncs with a Dayweave server. Everything works without it. */
@Serializable
data class SyncStatus(
    val signedIn: Boolean = false,
    val server: String? = null,
    val email: String? = null,
    val lastSyncAt: String? = null,
    val lastError: String? = null,
    /** Changes on this phone not yet sent. */
    val pending: Int = 0,
)

@Serializable data class SyncReport(val sent: Int = 0, val received: Int = 0)

@Serializable
data class BackupCounts(val tasks: Int = 0, val habits: Int = 0, val habitEntries: Int = 0, val projects: Int = 0, val focusSessions: Int = 0)

@Serializable
data class BackupPreview(
    val kind: String,
    val exportedAt: String,
    val appVersion: String,
    val incoming: BackupCounts,
    val current: BackupCounts,
)

@Serializable data class AppInfo(val version: String, val schemaVersion: Int)
