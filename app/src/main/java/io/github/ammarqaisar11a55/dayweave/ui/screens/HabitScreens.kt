package io.github.ammarqaisar11a55.dayweave.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.ammarqaisar11a55.dayweave.core.ChecklistItem
import io.github.ammarqaisar11a55.dayweave.core.Dayweave
import io.github.ammarqaisar11a55.dayweave.core.Habit
import io.github.ammarqaisar11a55.dayweave.core.HabitInput
import io.github.ammarqaisar11a55.dayweave.core.HeatmapDay
import io.github.ammarqaisar11a55.dayweave.core.HistoryDay
import io.github.ammarqaisar11a55.dayweave.core.params
import io.github.ammarqaisar11a55.dayweave.core.toInput
import io.github.ammarqaisar11a55.dayweave.core.toJson
import io.github.ammarqaisar11a55.dayweave.core.toParams
import io.github.ammarqaisar11a55.dayweave.ui.LocalSettings
import io.github.ammarqaisar11a55.dayweave.ui.components.DateField
import io.github.ammarqaisar11a55.dayweave.ui.components.EmptyState
import io.github.ammarqaisar11a55.dayweave.ui.components.GroupedList
import io.github.ammarqaisar11a55.dayweave.ui.components.HabitCard
import io.github.ammarqaisar11a55.dayweave.ui.components.HabitIcons
import io.github.ammarqaisar11a55.dayweave.ui.components.Heatmap
import io.github.ammarqaisar11a55.dayweave.ui.components.LocalActions
import io.github.ammarqaisar11a55.dayweave.ui.components.SectionHeader
import io.github.ammarqaisar11a55.dayweave.ui.components.TimeField
import io.github.ammarqaisar11a55.dayweave.ui.components.frequencyLabel
import io.github.ammarqaisar11a55.dayweave.ui.components.iconFor
import io.github.ammarqaisar11a55.dayweave.ui.components.rememberRpc
import io.github.ammarqaisar11a55.dayweave.ui.theme.Accent
import io.github.ammarqaisar11a55.dayweave.ui.theme.AccentInk
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw
import io.github.ammarqaisar11a55.dayweave.ui.theme.Tabular
import io.github.ammarqaisar11a55.dayweave.ui.theme.named
import io.github.ammarqaisar11a55.dayweave.util.formatLongDate
import io.github.ammarqaisar11a55.dayweave.util.formatMonth
import io.github.ammarqaisar11a55.dayweave.util.monthGrid
import io.github.ammarqaisar11a55.dayweave.util.parseDate
import io.github.ammarqaisar11a55.dayweave.util.percent
import io.github.ammarqaisar11a55.dayweave.util.startOfWeek
import io.github.ammarqaisar11a55.dayweave.util.today
import kotlinx.serialization.json.JsonObject

@Composable
fun HabitsScreen(navigation: NavHostController) {
    var archived by rememberSaveable { mutableStateOf(false) }
    val habits = rememberRpc<List<Habit>>("habits.list", params("includeArchived" to archived))
    val weekStart = LocalSettings.current.weekStart
    val heat = rememberRpc<List<HeatmapDay>>(
        "habits.heatmap",
        params("from" to startOfWeek(today().minusDays(364), weekStart).toString(), "to" to today().toString()),
    )
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        habits?.let { list -> "${list.count { it.today.due && it.archivedAt == null }} due today" } ?: "",
                        style = MaterialTheme.typography.bodyMedium, color = Dw.colors.ink2, modifier = Modifier.weight(1f),
                    )
                    Text("Show archived", style = MaterialTheme.typography.labelMedium, color = Dw.colors.ink2)
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = archived, onCheckedChange = { archived = it })
                }
            }
            if (habits?.isEmpty() == true) {
                item {
                    GroupedList {
                        EmptyState(
                            Icons.Filled.Repeat, "No habits yet",
                            "Start small. A habit can be something to do, to avoid, to measure, or a short routine.",
                        ) { Button(onClick = { navigation.navigate("habitEdit") }) { Text("Add a habit") } }
                    }
                }
            }
            items(habits.orEmpty(), key = { it.id }) { habit ->
                HabitCard(habit, onOpen = { navigation.navigate("habit/${habit.id}") }, detailed = true)
            }
            if (!habits.isNullOrEmpty() && heat != null) {
                item {
                    SectionHeader("Activity")
                    GroupedList { Box(Modifier.padding(16.dp)) { Heatmap(heat) } }
                }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
        ExtendedFloatingActionButton(
            onClick = { navigation.navigate("habitEdit") },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("New habit") },
            containerColor = Accent,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

/** "1 day", "3 weeks": the core names the unit in the plural. */
private fun count(value: Int, unit: String) = if (value == 1) "1 ${unit.removeSuffix("s")}" else "$value $unit"

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge.merge(Tabular))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink2)
    }
}

private val DayStates = mapOf(
    "done" to "done", "partial" to "partly done", "missed" to "missed", "pending" to "due today",
    "not_scheduled" to "not scheduled", "inactive" to "not active",
)

@Composable
fun HabitDetailScreen(navigation: NavHostController, id: Long) {
    val habit = rememberRpc<Habit>("habits.get", params("id" to id)) ?: return
    val actions = LocalActions.current
    val settings = LocalSettings.current
    val tint = Dw.colors.named(habit.color)
    var month by rememberSaveable { mutableStateOf(today().toString()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val rows = monthGrid(parseDate(month), settings.weekStart)
    val history = rememberRpc<List<HistoryDay>>(
        "habits.history", params("id" to habit.id, "from" to rows.first().first().toString(), "to" to rows.last().last().toString()),
    ).orEmpty().associateBy { it.date }
    val heat = rememberRpc<List<HeatmapDay>>(
        "habits.heatmap",
        params("id" to habit.id, "from" to startOfWeek(today().minusDays(364), settings.weekStart).toString(), "to" to today().toString()),
    )

    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(iconFor(habit.icon), contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleLarge)
                Text(habit.description.ifBlank { frequencyLabel(habit) }, style = MaterialTheme.typography.bodyMedium, color = Dw.colors.ink2)
            }
            IconButton(onClick = { navigation.navigate("habitEdit?id=${habit.id}") }) { Icon(Icons.Filled.Edit, contentDescription = "Edit habit") }
        }

        Spacer(Modifier.height(16.dp))
        GroupedList {
            val unit = habit.stats.streakUnit
            Row(Modifier.padding(16.dp)) {
                Stat("Current streak", count(habit.stats.currentStreak, unit), Modifier.weight(1f))
                Stat("Best streak", count(habit.stats.bestStreak, unit), Modifier.weight(1f))
                Stat("Completion", percent(habit.stats.completionRate), Modifier.weight(1f))
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Stat("Last 7 days", percent(habit.stats.weeklyConsistency), Modifier.weight(1f))
                Stat("Last 30 days", percent(habit.stats.monthlyConsistency), Modifier.weight(1f))
                Stat("Times done", "${habit.stats.totalDone}", Modifier.weight(1f))
            }
        }

        if (habit.frequency != "weekly_target") {
            SectionHeader("Weekly schedule")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEachIndexed { index, day ->
                    val on = index in habit.weekdays
                    Box(
                        Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (on) Dw.colors.accentSoft else Dw.colors.sunken)
                            .semantics { contentDescription = "$day: ${if (on) "scheduled" else "not scheduled"}" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(day, style = MaterialTheme.typography.labelSmall, color = if (on) Dw.colors.accentText else Dw.colors.ink3)
                    }
                }
            }
        }

        SectionHeader("History") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = parseDate(month).minusMonths(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                Text(formatMonth(parseDate(month)), style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = { month = parseDate(month).plusMonths(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                }
            }
        }
        GroupedList {
            Column(Modifier.padding(12.dp)) {
                rows.forEach { week ->
                    Row {
                        week.forEach { date ->
                            val day = history[date.toString()]
                            val state = day?.state ?: "inactive"
                            val outside = date.month != parseDate(month).month
                            val (background, foreground) = when (state) {
                                "done" -> Accent to AccentInk
                                "partial" -> Dw.colors.accentSoft to Dw.colors.accentText
                                "missed" -> Dw.colors.danger.copy(alpha = 0.12f) to Dw.colors.danger
                                else -> androidx.compose.ui.graphics.Color.Transparent to (if (outside) Dw.colors.ink3 else Dw.colors.ink2)
                            }
                            Box(
                                Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(CircleShape).background(background)
                                    .then(if (state == "pending") Modifier.border(1.dp, Dw.colors.ink3, CircleShape) else Modifier)
                                    .then(if (selected == date.toString()) Modifier.border(2.dp, Accent, CircleShape) else Modifier)
                                    .clickable(enabled = state != "inactive") { selected = date.toString() }
                                    .semantics { contentDescription = "${formatLongDate(date)}: ${DayStates[state]}" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${date.dayOfMonth}", style = MaterialTheme.typography.labelMedium.merge(Tabular), color = foreground)
                            }
                        }
                    }
                }
            }
        }
        selected?.let { date -> history[date]?.let { DayEditor(habit, it) } }

        if (heat != null) {
            SectionHeader("Activity")
            GroupedList { Box(Modifier.padding(16.dp)) { Heatmap(heat) } }
        }

        Row(Modifier.padding(vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (habit.archivedAt == null) {
                TextButton(onClick = {
                    actions.run("Habit archived") { Dayweave.mutate("habits.archive", params("id" to habit.id)) }
                    navigation.popBackStack()
                }) {
                    Icon(Icons.Filled.Archive, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Archive")
                }
            } else {
                TextButton(onClick = { actions.run("Habit restored") { Dayweave.mutate("habits.restore", params("id" to habit.id)) } }) {
                    Icon(Icons.Filled.Unarchive, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Restore")
                }
            }
            TextButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = Dw.colors.danger)
                Spacer(Modifier.width(6.dp))
                Text("Delete", color = Dw.colors.danger)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this habit?") },
            text = { Text("\"${habit.name}\" and its whole history will be deleted. This can't be undone. Archive it instead to keep the history.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.run("Habit deleted") { Dayweave.mutate("habits.delete", params("id" to habit.id)) }
                    navigation.popBackStack()
                }) { Text("Delete habit", color = Dw.colors.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DayEditor(habit: Habit, day: HistoryDay) {
    val actions = LocalActions.current
    var note by remember(day.date, day.note) { mutableStateOf(day.note) }
    var value by remember(day.date, day.value) { mutableStateOf(day.value?.toString() ?: "") }
    val logged = day.state == "done" || day.state == "partial" || (habit.kind == "quit" && day.state == "missed")
    fun log(vararg extra: Pair<String, Any?>) = actions.run { Dayweave.mutate("habits.log", params("id" to habit.id, "date" to day.date, *extra)) }
    GroupedList(Modifier.padding(top = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(formatLongDate(parseDate(day.date)), style = MaterialTheme.typography.titleSmall)
            Text(DayStates[day.state]?.replaceFirstChar { it.uppercase() } ?: "", color = Dw.colors.ink2)
            if (habit.kind == "quantity") {
                OutlinedTextField(
                    value = value, onValueChange = { value = it }, singleLine = true,
                    label = { Text(if (habit.unit.isBlank()) "Amount" else "Amount in ${habit.unit}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    when (habit.kind) {
                        "quantity" -> log("value" to value.toDoubleOrNull(), "note" to note)
                        "quit" -> log("note" to note)
                        else -> log("note" to note)
                    }
                }) { Text(if (habit.kind == "build" && !logged) "Mark done" else "Save") }
                if (habit.kind == "quit" && !logged) TextButton(onClick = { log("slip" to true, "note" to note) }) { Text("Log a slip") }
                if (logged) TextButton(onClick = { actions.run { Dayweave.mutate("habits.undo", params("id" to habit.id, "date" to day.date)) } }) {
                    Text(if (habit.kind == "quit") "Remove slip" else "Undo")
                }
            }
        }
    }
}

private val Kinds = listOf(
    "build" to ("Build" to "Something to do, like exercising"),
    "quit" to ("Quit" to "Something to avoid. Every day counts until you log a slip"),
    "quantity" to ("Amount" to "An amount to reach, like 20 pages"),
    "checklist" to ("Checklist" to "Several steps that all need doing"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitEditorScreen(navigation: NavHostController, id: Long?) {
    val existing = id?.let { rememberRpc<Habit>("habits.get", params("id" to it)) }
    if (id != null && existing == null) return
    val actions = LocalActions.current
    val clock24 = LocalSettings.current.timeFormat == "24h"
    var input by remember(existing) { mutableStateOf(existing?.toInput() ?: HabitInput(name = "", startDate = today().toString())) }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).imePadding()) {
        OutlinedTextField(
            value = input.name, onValueChange = { input = input.copy(name = it) },
            label = { Text("Name") }, placeholder = { Text("Read 20 pages") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        SectionHeader("Type")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Kinds.forEachIndexed { index, (kind, text) ->
                SegmentedButton(
                    selected = input.kind == kind,
                    onClick = {
                        input = input.copy(kind = kind, frequency = if (kind == "quit" && input.frequency == "weekly_target") "daily" else input.frequency)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, Kinds.size),
                ) { Text(text.first) }
            }
        }
        Text(Kinds.first { it.first == input.kind }.second.second, style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink2, modifier = Modifier.padding(top = 6.dp))

        if (input.kind == "quantity") {
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input.quantityTarget?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "",
                    onValueChange = { input = input.copy(quantityTarget = it.toDoubleOrNull()) },
                    label = { Text("Daily target") }, singleLine = true, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = input.unit, onValueChange = { input = input.copy(unit = it) },
                    label = { Text("Unit") }, placeholder = { Text("pages") }, singleLine = true, modifier = Modifier.weight(1f),
                )
            }
        }
        if (input.kind == "checklist") {
            SectionHeader("Steps")
            input.checklist.forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = item.title, singleLine = true, modifier = Modifier.weight(1f).padding(bottom = 6.dp),
                        onValueChange = { text -> input = input.copy(checklist = input.checklist.mapIndexed { i, it -> if (i == index) it.copy(title = text) else it }) },
                    )
                    IconButton(onClick = { input = input.copy(checklist = input.checklist.filterIndexed { i, _ -> i != index }) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove step ${index + 1}")
                    }
                }
            }
            TextButton(onClick = { input = input.copy(checklist = input.checklist + ChecklistItem(title = "")) }) { Text("Add step") }
        }

        SectionHeader("Schedule")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val options = listOf("daily" to "Every day", "weekdays" to "Set days") +
                (if (input.kind == "quit") emptyList() else listOf("weekly_target" to "Times a week"))
            options.forEachIndexed { index, (frequency, label) ->
                SegmentedButton(
                    selected = input.frequency == frequency, onClick = { input = input.copy(frequency = frequency) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(label) }
            }
        }
        if (input.frequency == "weekdays") {
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { index, day ->
                    val on = index in input.weekdays
                    FilterChip(
                        selected = on, label = { Text(day) }, modifier = Modifier.weight(1f),
                        onClick = { input = input.copy(weekdays = if (on) input.weekdays - index else (input.weekdays + index).sorted()) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent, selectedLabelColor = AccentInk),
                    )
                }
            }
        }
        if (input.frequency == "weekly_target") {
            OutlinedTextField(
                value = input.weeklyTarget?.toString() ?: "", onValueChange = { input = input.copy(weeklyTarget = it.toIntOrNull()) },
                label = { Text("Times a week, on any days") }, singleLine = true, modifier = Modifier.padding(top = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }

        SectionHeader("Start date and reminder")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField("Start date", input.startDate, onChange = { input = input.copy(startDate = it) }, modifier = Modifier.weight(1f), clearable = false)
            TimeField("No reminder", input.reminderTime, clock24, onChange = { input = input.copy(reminderTime = it) }, modifier = Modifier.weight(1f))
        }

        SectionHeader("Colour")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Dw.colors.palette.forEach { (name, color) ->
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(color)
                        .then(if (input.color == name) Modifier.border(2.dp, Dw.colors.ink, CircleShape) else Modifier)
                        .clickable { input = input.copy(color = name) }
                        .semantics { contentDescription = "$name${if (input.color == name) ", selected" else ""}" },
                )
            }
        }
        SectionHeader("Icon")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val tint = Dw.colors.named(input.color)
            HabitIcons.forEach { (name, icon) ->
                val on = input.icon == name
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(if (on) tint.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { input = input.copy(icon = name) },
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, contentDescription = name.replace('-', ' '), tint = if (on) tint else Dw.colors.ink2) }
            }
        }
        OutlinedTextField(
            value = input.description, onValueChange = { input = input.copy(description = it) },
            label = { Text("Description") }, placeholder = { Text("Why this matters to you") }, minLines = 2,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.End) {
            Button(
                enabled = input.name.isNotBlank(),
                onClick = {
                    val body = input.copy(checklist = input.checklist.filter { it.title.isNotBlank() }).toParams()
                    actions.run(if (id == null) "Habit added" else "Habit saved") {
                        Dayweave.mutate(
                            if (id == null) "habits.create" else "habits.update",
                            if (id == null) body else JsonObject(body + ("id" to id.toJson())),
                        )
                        navigation.popBackStack()
                    }
                },
            ) { Text(if (id == null) "Add habit" else "Save habit", fontWeight = FontWeight.SemiBold) }
        }
    }
}
