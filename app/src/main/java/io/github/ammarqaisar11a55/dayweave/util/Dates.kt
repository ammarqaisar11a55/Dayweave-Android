package io.github.ammarqaisar11a55.dayweave.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

// Calendar dates travel as 'YYYY-MM-DD' text and times as 'HH:MM', matching the shared core.

fun parseDate(text: String): LocalDate = LocalDate.parse(text)

fun today(): LocalDate = LocalDate.now()

fun LocalDate.text(): String = toString()

enum class DeadlineTone { Overdue, Today, Soon, Later, Done, None }

data class DeadlineLabel(val label: String, val detail: String, val tone: DeadlineTone)

private fun plural(count: Long, unit: String) = "$count $unit${if (count == 1L) "" else "s"}"

/**
 * Describes a deadline. A date-only deadline has no hour to count towards, so it is only ever described
 * in days. The same rules as the desktop app's src/lib/deadline.ts, with the same tests.
 */
fun describeDeadline(
    status: String,
    dueDate: String?,
    dueTime: String?,
    now: LocalDateTime,
    clock24: Boolean = true,
): DeadlineLabel {
    if (status == "completed") return DeadlineLabel("Completed", "Completed", DeadlineTone.Done)
    if (dueDate == null) return DeadlineLabel("", "No deadline", DeadlineTone.None)
    val due = parseDate(dueDate)
    val days = ChronoUnit.DAYS.between(now.toLocalDate(), due)
    val time = dueTime?.let { formatTime(it, clock24) }
    val at = time?.let { ", $it" } ?: ""

    fun overdue(duration: String) = DeadlineLabel("Overdue by $duration", "Overdue by $duration", DeadlineTone.Overdue)

    if (dueTime != null) {
        val minutes = Duration.between(now, due.atTime(LocalTime.parse(dueTime))).toMinutes()
        if (minutes < 0) {
            val late = -minutes
            return when {
                late < 60 -> overdue(plural(late, "minute"))
                days == 0L -> overdue(plural(late / 60, "hour"))
                else -> overdue(plural(-days, "day"))
            }
        }
        if (days == 0L) {
            return when {
                minutes == 0L -> DeadlineLabel("Now", "Due now", DeadlineTone.Today)
                minutes < 60 -> DeadlineLabel("In $minutes min", "Due in ${plural(minutes, "minute")}", DeadlineTone.Today)
                else -> DeadlineLabel("Today$at", "Due in ${plural(minutes / 60, "hour")}", DeadlineTone.Today)
            }
        }
    } else if (days < 0) {
        return overdue(plural(-days, "day"))
    } else if (days == 0L) {
        return DeadlineLabel("Today", "Due today", DeadlineTone.Today)
    }

    if (days == 1L) return DeadlineLabel("Tomorrow$at", "Due tomorrow$at", DeadlineTone.Soon)
    if (days < 7) {
        return DeadlineLabel(
            "In $days days", "Due in $days days$at", if (days <= 3) DeadlineTone.Soon else DeadlineTone.Later,
        )
    }
    val date = formatShortDate(due, now.toLocalDate())
    return DeadlineLabel(date, "Due $date$at", DeadlineTone.Later)
}

fun formatTime(time: String, clock24: Boolean): String {
    if (clock24) return time
    val parsed = LocalTime.parse(time)
    val hour = parsed.hour % 12
    return "${if (hour == 0) 12 else hour}:${"%02d".format(parsed.minute)} ${if (parsed.hour < 12) "AM" else "PM"}"
}

private val shortDate = DateTimeFormatter.ofPattern("d MMM")
private val yearDate = DateTimeFormatter.ofPattern("d MMM yyyy")
private val longDate = DateTimeFormatter.ofPattern("EEEE, d MMMM")
private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy")

fun formatShortDate(date: LocalDate, reference: LocalDate = today()): String =
    (if (date.year == reference.year) shortDate else yearDate).format(date)

fun formatShortDate(text: String): String = formatShortDate(parseDate(text))

fun formatLongDate(date: LocalDate): String = longDate.format(date)

fun formatMonth(date: LocalDate): String = monthYear.format(date)

fun formatInstant(text: String): String = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    .format(Instant.parse(text).atZone(ZoneId.systemDefault()))

fun greeting(hour: Int): String = when {
    hour < 5 -> "Good night"
    hour < 12 -> "Good morning"
    hour < 18 -> "Good afternoon"
    else -> "Good evening"
}

fun formatDuration(seconds: Long): String {
    val minutes = (seconds + 30) / 60
    if (minutes < 60) return "$minutes min"
    val rest = minutes % 60
    return if (rest == 0L) "${minutes / 60} h" else "${minutes / 60} h $rest min"
}

fun formatClock(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val rest = "%02d".format(safe % 60)
    return if (hours > 0) "$hours:${"%02d".format(minutes)}:$rest" else "${"%02d".format(minutes)}:$rest"
}

fun percent(ratio: Double): String = "${Math.round(ratio * 100)}%"

/** The first day of the week holding [date], given the week-start setting. */
fun startOfWeek(date: LocalDate, weekStart: String): LocalDate {
    val first = when (weekStart) {
        "sunday" -> java.time.DayOfWeek.SUNDAY
        "saturday" -> java.time.DayOfWeek.SATURDAY
        else -> java.time.DayOfWeek.MONDAY
    }
    val offset = (date.dayOfWeek.value - first.value + 7) % 7
    return date.minusDays(offset.toLong())
}

/** The full weeks covering a month, as rows of dates. */
fun monthGrid(date: LocalDate, weekStart: String): List<List<LocalDate>> {
    val first = date.withDayOfMonth(1)
    val last = date.withDayOfMonth(date.lengthOfMonth())
    val rows = mutableListOf<List<LocalDate>>()
    var cursor = startOfWeek(first, weekStart)
    while (!cursor.isAfter(last)) {
        rows += (0L..6L).map { cursor.plusDays(it) }
        cursor = cursor.plusDays(7)
    }
    return rows
}
