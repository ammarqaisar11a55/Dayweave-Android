package io.github.ammarqaisar11a55.dayweave.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.ammarqaisar11a55.dayweave.core.CalendarDay
import io.github.ammarqaisar11a55.dayweave.core.CalendarItem
import io.github.ammarqaisar11a55.dayweave.core.TaskInput
import io.github.ammarqaisar11a55.dayweave.core.params
import io.github.ammarqaisar11a55.dayweave.ui.LocalNewTask
import io.github.ammarqaisar11a55.dayweave.ui.LocalSettings
import io.github.ammarqaisar11a55.dayweave.ui.components.EmptyState
import io.github.ammarqaisar11a55.dayweave.ui.components.GroupedList
import io.github.ammarqaisar11a55.dayweave.ui.components.RowDivider
import io.github.ammarqaisar11a55.dayweave.ui.components.rememberRpc
import io.github.ammarqaisar11a55.dayweave.ui.theme.Accent
import io.github.ammarqaisar11a55.dayweave.ui.theme.AccentInk
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw
import io.github.ammarqaisar11a55.dayweave.ui.theme.Tabular
import io.github.ammarqaisar11a55.dayweave.util.formatDuration
import io.github.ammarqaisar11a55.dayweave.util.formatLongDate
import io.github.ammarqaisar11a55.dayweave.util.formatMonth
import io.github.ammarqaisar11a55.dayweave.util.formatTime
import io.github.ammarqaisar11a55.dayweave.util.monthGrid
import io.github.ammarqaisar11a55.dayweave.util.parseDate
import io.github.ammarqaisar11a55.dayweave.util.today

private data class Kind(val label: String, val icon: ImageVector, val color: @Composable () -> Color)

private val Kinds = linkedMapOf(
    "deadline" to Kind("Deadlines", Icons.Filled.Flag) { Dw.colors.accentText },
    "task" to Kind("Tasks starting", Icons.Filled.Circle) { Dw.colors.info },
    "habit" to Kind("Habits", Icons.Filled.Repeat) { Dw.colors.success },
    "focus" to Kind("Focus sessions", Icons.Filled.Timer) { Dw.colors.violet },
    "reminder" to Kind("Reminders", Icons.Filled.Notifications) { Dw.colors.ink2 },
)

@Composable
fun CalendarScreen(navigation: NavHostController) {
    val settings = LocalSettings.current
    val clock24 = settings.timeFormat == "24h"
    val newTask = LocalNewTask.current
    var month by rememberSaveable { mutableStateOf(today().toString()) }
    var selected by rememberSaveable { mutableStateOf(today().toString()) }
    val rows = monthGrid(parseDate(month), settings.weekStart)
    val days = rememberRpc<List<CalendarDay>>(
        "calendar.range", params("from" to rows.first().first().toString(), "to" to rows.last().last().toString()),
    ).orEmpty().associateBy { it.date }
    val current = parseDate(month).month

    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatMonth(parseDate(month)), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { month = today().toString(); selected = today().toString() }) { Text("Today") }
            IconButton(onClick = { month = parseDate(month).minusMonths(1).toString() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            IconButton(onClick = { month = parseDate(month).plusMonths(1).toString() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
            }
        }
        GroupedList {
            Column(Modifier.padding(8.dp)) {
                Row {
                    rows.first().forEach { date ->
                        Text(
                            date.dayOfWeek.name.take(1), style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink2,
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                rows.forEach { week ->
                    Row {
                        week.forEach { date ->
                            val text = date.toString()
                            val day = days[text]
                            val isToday = date == today()
                            val isSelected = text == selected
                            val kinds = day?.items?.map { it.kind }?.filter { it != "reminder" }?.distinct().orEmpty()
                            Column(
                                Modifier.weight(1f).aspectRatio(0.85f).clip(MaterialTheme.shapes.small)
                                    .background(if (isSelected) Dw.colors.accentSoft else Color.Transparent)
                                    .clickable { selected = text }
                                    .semantics { contentDescription = "${formatLongDate(date)}, ${day?.items?.size ?: 0} items" },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Box(
                                    Modifier.size(28.dp).clip(CircleShape).background(if (isToday) Accent else Color.Transparent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "${date.dayOfMonth}", style = MaterialTheme.typography.labelLarge.merge(Tabular),
                                        color = when {
                                            isToday -> AccentInk
                                            date.month != current -> Dw.colors.ink3
                                            else -> Dw.colors.ink
                                        },
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(8.dp).padding(top = 2.dp)) {
                                    kinds.take(4).forEach { kind ->
                                        Box(Modifier.size(5.dp).clip(CircleShape).background(Kinds.getValue(kind).color()))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Kinds.values.take(4).forEach { kind ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(kind.color()))
                    Spacer(Modifier.width(4.dp))
                    Text(kind.label, style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink2)
                }
            }
        }

        val day = days[selected]
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
            Text(formatLongDate(parseDate(selected)), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { newTask(TaskInput(title = "", dueDate = selected)) }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Task")
            }
        }
        GroupedList {
            val groups = Kinds.keys.mapNotNull { kind -> day?.items?.filter { it.kind == kind }?.takeIf { it.isNotEmpty() }?.let { kind to it } }
            if (groups.isEmpty()) EmptyState(Icons.Filled.Circle, "Nothing planned", "Add a task to this date.")
            groups.forEachIndexed { index, (kind, items) ->
                if (index > 0) RowDivider()
                val style = Kinds.getValue(kind)
                Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(style.icon, contentDescription = null, tint = style.color(), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(style.label, style = MaterialTheme.typography.labelMedium, color = Dw.colors.ink2)
                }
                items.forEach { item -> AgendaRow(item, clock24) { open(navigation, item) } }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun open(navigation: NavHostController, item: CalendarItem) = when (item.kind) {
    "habit" -> navigation.navigate("habit/${item.id}")
    "focus" -> navigation.navigate("focus")
    else -> navigation.navigate("task/${item.id}")
}

@Composable
private fun AgendaRow(item: CalendarItem, clock24: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            item.title,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = when {
                item.completed && item.kind != "focus" -> Dw.colors.ink2
                item.overdue -> Dw.colors.danger
                else -> Dw.colors.ink
            },
            textDecoration = if (item.completed && item.kind != "focus" && item.kind != "habit") TextDecoration.LineThrough else null,
        )
        when {
            item.kind == "habit" && item.completed -> Text("Done", style = MaterialTheme.typography.labelSmall, color = Dw.colors.success)
            item.kind == "habit" && item.overdue -> Text("Missed", style = MaterialTheme.typography.labelSmall, color = Dw.colors.danger)
            item.overdue -> Text("Overdue", style = MaterialTheme.typography.labelSmall, color = Dw.colors.danger)
        }
        item.seconds?.let { Text(formatDuration(it), style = MaterialTheme.typography.labelSmall.merge(Tabular), color = Dw.colors.ink2) }
        item.time?.let {
            Spacer(Modifier.width(8.dp))
            Text(formatTime(it, clock24), style = MaterialTheme.typography.labelSmall.merge(Tabular), color = Dw.colors.ink2)
        }
    }
}
