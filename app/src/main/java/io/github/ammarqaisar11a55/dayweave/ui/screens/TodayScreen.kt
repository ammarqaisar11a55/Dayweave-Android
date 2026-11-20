package io.github.ammarqaisar11a55.dayweave.ui.screens

import io.github.ammarqaisar11a55.dayweave.notify.Scheduler
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import android.provider.Settings
import android.net.Uri
import android.content.Intent
import android.content.Context
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import io.github.ammarqaisar11a55.dayweave.core.Dayweave
import io.github.ammarqaisar11a55.dayweave.core.Overview
import io.github.ammarqaisar11a55.dayweave.core.TaskInput
import io.github.ammarqaisar11a55.dayweave.core.params
import io.github.ammarqaisar11a55.dayweave.notify.Notifications
import io.github.ammarqaisar11a55.dayweave.ui.LocalNewTask
import io.github.ammarqaisar11a55.dayweave.ui.LocalSettings
import io.github.ammarqaisar11a55.dayweave.ui.components.EmptyState
import io.github.ammarqaisar11a55.dayweave.ui.components.GroupedList
import io.github.ammarqaisar11a55.dayweave.ui.components.HabitCard
import io.github.ammarqaisar11a55.dayweave.ui.components.LocalActions
import io.github.ammarqaisar11a55.dayweave.ui.components.RowDivider
import io.github.ammarqaisar11a55.dayweave.ui.components.SectionHeader
import io.github.ammarqaisar11a55.dayweave.ui.components.TaskRow
import io.github.ammarqaisar11a55.dayweave.ui.components.Thread
import io.github.ammarqaisar11a55.dayweave.ui.components.Weave
import io.github.ammarqaisar11a55.dayweave.ui.components.rememberRpc
import io.github.ammarqaisar11a55.dayweave.ui.theme.Accent
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw
import io.github.ammarqaisar11a55.dayweave.ui.theme.Tabular
import io.github.ammarqaisar11a55.dayweave.util.formatDuration
import io.github.ammarqaisar11a55.dayweave.util.formatLongDate
import io.github.ammarqaisar11a55.dayweave.util.greeting
import io.github.ammarqaisar11a55.dayweave.util.parseDate
import io.github.ammarqaisar11a55.dayweave.util.today
import java.time.LocalTime

private fun summary(data: Overview): String {
    val tasksLeft = data.tasksTotal - data.tasksDone
    val habitsLeft = data.habitsTotal - data.habitsDone
    if (data.tasksTotal + data.habitsTotal == 0) return "Let's make today count."
    if (tasksLeft + habitsLeft == 0) return "Everything for today is done."
    val parts = listOfNotNull(
        tasksLeft.takeIf { it > 0 }?.let { "$it task${if (it == 1) "" else "s"}" },
        habitsLeft.takeIf { it > 0 }?.let { "$it habit${if (it == 1) "" else "s"}" },
    )
    return parts.joinToString(" and ") + " left today."
}

@Composable
fun TodayScreen(navigation: NavHostController) {
    val data = rememberRpc<Overview>("overview.get") ?: return
    val settings = LocalSettings.current
    val clock24 = settings.timeFormat == "24h"
    val newTask = LocalNewTask.current
    val actions = LocalActions.current
    var showAll by remember { mutableStateOf(false) }
    val empty = data.tasksTotal + data.habitsTotal == 0 && data.overdue.isEmpty() && data.upcoming.isEmpty()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item { Hero(data) }
            item { NotificationPrompt() }
            if (empty) {
                item {
                    GroupedList {
                        EmptyState(
                            Icons.Filled.EventAvailable, "Your day is open",
                            "Add a task with a deadline, or a habit to build. They'll show up here each day.",
                        ) {
                            Button(onClick = { newTask(TaskInput(title = "", dueDate = today().toString())) }) { Text("Add a task for today") }
                        }
                    }
                }
            }
            if (data.overdue.isNotEmpty()) {
                item {
                    SectionHeader("Overdue (${data.overdue.size})") {
                        TextButton(onClick = {
                            actions.run("Moved to today") {
                                data.overdue.forEach {
                                    Dayweave.mutate("tasks.reschedule", params("id" to it.id, "dueDate" to today().toString(), "dueTime" to it.dueTime))
                                }
                            }
                        }) { Text("Move all to today") }
                    }
                    GroupedList {
                        data.overdue.take(5).forEachIndexed { index, task ->
                            if (index > 0) RowDivider()
                            TaskRow(task, clock24, onOpen = { navigation.navigate("task/${task.id}") })
                        }
                    }
                }
            }
            if (!empty) {
                item {
                    SectionHeader("Today's tasks") {
                        TextButton(onClick = { newTask(TaskInput(title = "", dueDate = today().toString())) }) { Text("Add") }
                    }
                    GroupedList {
                        if (data.tasks.isEmpty()) EmptyState(Icons.Filled.CheckCircle, "No tasks today", "Add one, or plan ahead in Calendar.")
                        val visible = if (showAll) data.tasks else data.tasks.take(8)
                        visible.forEachIndexed { index, task ->
                            if (index > 0) RowDivider()
                            TaskRow(task, clock24, onOpen = { navigation.navigate("task/${task.id}") })
                        }
                    }
                    if (!showAll && data.tasks.size > 8) TextButton(onClick = { showAll = true }) { Text("Show ${data.tasks.size - 8} more") }
                }
                item {
                    SectionHeader("Today's habits") { TextButton(onClick = { navigation.navigate("habits") }) { Text("All habits") } }
                    if (data.habits.isEmpty()) {
                        GroupedList { EmptyState(Icons.Filled.Repeat, "No habits due today") }
                    }
                    if (data.habits.isNotEmpty()) {
                        GroupedList {
                            data.habits.forEachIndexed { index, habit ->
                                if (index > 0) RowDivider()
                                HabitCard(habit, onOpen = { navigation.navigate("habit/${habit.id}") }, inList = true)
                            }
                        }
                    }
                }
                if (data.upcoming.isNotEmpty()) {
                    item {
                        SectionHeader("Upcoming deadlines")
                        GroupedList {
                            data.upcoming.forEachIndexed { index, task ->
                                if (index > 0) RowDivider()
                                TaskRow(task, clock24, onOpen = { navigation.navigate("task/${task.id}") })
                            }
                        }
                    }
                }
                item {
                    SectionHeader("This week")
                    Week(data)
                }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
        ExtendedFloatingActionButton(
            onClick = { newTask(null) },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("New task") },
            containerColor = Accent,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

@Composable
private fun Hero(data: Overview) {
    val total = data.tasksTotal + data.habitsTotal
    val done = data.tasksDone + data.habitsDone
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
        Text(formatLongDate(parseDate(data.date)), style = MaterialTheme.typography.bodyMedium, color = Dw.colors.ink2)
        Text(greeting(LocalTime.now().hour), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 2.dp))
        Text(summary(data), style = MaterialTheme.typography.bodyLarge, color = Dw.colors.ink2, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(20.dp))
        // The day's cloth is the screen's one bold element: habits run down as the warp, tasks across as
        // the weft, and each thread fills in as it is done.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Weave(
                tasks = data.tasks.map { Thread(it.title, it.completed) },
                habits = data.habits.map { Thread(it.name, it.met) },
                background = Dw.colors.window,
                label = "Today's weave: $done of $total done",
                modifier = Modifier.height(168.dp),
            )
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Legend(Accent, "${data.tasksDone} of ${data.tasksTotal}", "tasks")
                Legend(Dw.colors.threadWarp, "${data.habitsDone} of ${data.habitsTotal}", "habits")
                if (data.focusSeconds >= 60) Legend(null, formatDuration(data.focusSeconds), "focused")
            }
        }
    }
}

/** Names a thread colour, so the weave reads as a chart rather than a pattern. */
@Composable
private fun Legend(color: Color?, value: String, what: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium.merge(Tabular))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (color != null) {
                Box(Modifier.size(width = 12.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(color))
                Spacer(Modifier.width(6.dp))
            }
            Text(what, style = MaterialTheme.typography.labelMedium, color = Dw.colors.ink2)
        }
    }
}

@Composable
private fun Week(data: Overview) {
    val best = (data.week.maxOfOrNull { it.tasksDone + it.habitsDone } ?: 0).coerceAtLeast(1)
    GroupedList {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            data.week.forEach { day ->
                val count = day.tasksDone + day.habitsDone
                val isToday = day.date == data.date
                val future = day.date > data.date
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(if (future) "" else "$count", style = MaterialTheme.typography.labelSmall.merge(Tabular), color = Dw.colors.ink2)
                    Box(Modifier.height(56.dp).width(16.dp), contentAlignment = Alignment.BottomCenter) {
                        if (!future) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight((count.toFloat() / best).coerceAtLeast(if (count > 0) 0.08f else 0.03f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (isToday) Accent else Dw.colors.threadWarp),
                            )
                        }
                    }
                    Text(
                        parseDate(day.date).dayOfWeek.name.take(2).lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) Dw.colors.accentText else Dw.colors.ink2,
                        fontWeight = if (isToday) FontWeight.SemiBold else null,
                    )
                }
            }
        }
    }
}

/** Android 13 and later need permission before any reminder can appear. */
@Composable
private fun NotificationPrompt() {
    val context = LocalContext.current
    // Both permissions can change in system settings, so they are read again whenever the app returns.
    var resumed by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { resumed++ } }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { resumed++ }
    val notifications = remember(resumed) { Notifications.allowed(context) }
    val exact = remember(resumed) { Scheduler.canScheduleExact(context) }
    val preferences = remember { context.getSharedPreferences("prompts", Context.MODE_PRIVATE) }
    var exactDismissed by remember { mutableStateOf(preferences.getBoolean("exactAlarmDismissed", false)) }

    when {
        !notifications -> Prompt(
            "Turn on reminders",
            "Dayweave needs permission to remind you about deadlines and habits.",
            "Allow notifications",
        ) {
            if (Build.VERSION.SDK_INT >= 33) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }
        }
        !exact && !exactDismissed && Build.VERSION.SDK_INT >= 31 -> Prompt(
            "Get reminders on the minute",
            "Without the alarms permission, Android may deliver reminders several minutes late.",
            "Allow alarms",
            onDismiss = {
                preferences.edit().putBoolean("exactAlarmDismissed", true).apply()
                exactDismissed = true
            },
        ) {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
        }
    }
}

@Composable
private fun Prompt(title: String, body: String, action: String, onDismiss: (() -> Unit)? = null, onClick: () -> Unit) {
    GroupedList(Modifier.padding(top = 8.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Dw.colors.accentText)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = Dw.colors.ink2)
            }
        }
        Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onDismiss != null) TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Not now") }
            OutlinedButton(onClick = onClick, modifier = Modifier.weight(1f)) { Text(action) }
        }
    }
}
