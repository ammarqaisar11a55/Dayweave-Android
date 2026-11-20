package io.github.ammarqaisar11a55.dayweave.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.ammarqaisar11a55.dayweave.ui.components.RowDivider
import io.github.ammarqaisar11a55.dayweave.ui.components.FieldRow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.ammarqaisar11a55.dayweave.core.Dayweave
import io.github.ammarqaisar11a55.dayweave.core.Project
import io.github.ammarqaisar11a55.dayweave.core.Recurrence
import io.github.ammarqaisar11a55.dayweave.core.Reminder
import io.github.ammarqaisar11a55.dayweave.core.Task
import io.github.ammarqaisar11a55.dayweave.core.TaskInput
import io.github.ammarqaisar11a55.dayweave.core.params
import io.github.ammarqaisar11a55.dayweave.core.toInput
import io.github.ammarqaisar11a55.dayweave.core.toJson
import io.github.ammarqaisar11a55.dayweave.core.toParams
import io.github.ammarqaisar11a55.dayweave.ui.LocalSettings
import io.github.ammarqaisar11a55.dayweave.ui.components.ChoiceField
import io.github.ammarqaisar11a55.dayweave.ui.components.DateField
import io.github.ammarqaisar11a55.dayweave.ui.components.DeadlineBadge
import io.github.ammarqaisar11a55.dayweave.ui.components.GroupedList
import io.github.ammarqaisar11a55.dayweave.ui.components.LocalActions
import io.github.ammarqaisar11a55.dayweave.ui.components.Priorities
import io.github.ammarqaisar11a55.dayweave.ui.components.SectionHeader
import io.github.ammarqaisar11a55.dayweave.ui.components.TimeField
import io.github.ammarqaisar11a55.dayweave.ui.components.rememberRpc
import io.github.ammarqaisar11a55.dayweave.ui.components.toggleComplete
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw
import io.github.ammarqaisar11a55.dayweave.util.formatInstant
import kotlinx.serialization.json.JsonObject

val ReminderPresets = listOf(0L to "At deadline", 10L to "10 min before", 30L to "30 min before", 60L to "1 hour before", 1440L to "1 day before")

val RepeatChoices = listOf<Pair<String?, String>>(null to "Does not repeat", "daily" to "Every day", "weekly" to "Every week", "monthly" to "Every month", "yearly" to "Every year")

private val Statuses = listOf("todo" to "To do", "in_progress" to "In progress", "completed" to "Completed", "archived" to "Archived")

@Composable
fun projectChoices(): List<Pair<Long?, String>> {
    val projects = rememberRpc<List<Project>>("projects.list").orEmpty()
    return listOf<Pair<Long?, String>>(null to "No project") + projects.map { it.id to it.name }
}

private fun withId(id: Long, input: TaskInput): JsonObject = JsonObject(input.toParams() + ("id" to id.toJson()))

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderChips(reminders: List<Reminder>, hasDeadline: Boolean, onChange: (List<Reminder>) -> Unit) {
    if (!hasDeadline) {
        Text("Choose a deadline to add reminders.", style = MaterialTheme.typography.bodyMedium, color = Dw.colors.ink2)
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReminderPresets.forEach { (minutes, label) ->
            val selected = reminders.any { it.kind == "before" && it.offsetMinutes == minutes }
            FilterChip(
                selected = selected,
                onClick = {
                    onChange(
                        if (selected) reminders.filterNot { it.kind == "before" && it.offsetMinutes == minutes }
                        else reminders + Reminder("before", minutes),
                    )
                },
                label = { Text(label) },
                leadingIcon = if (selected) ({ Icon(Icons.Filled.Check, contentDescription = null) }) else null,
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Dw.colors.accentSoft, selectedLabelColor = Dw.colors.accentText),
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = Dw.colors.ink2, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
}

@Composable
fun TaskDetailScreen(navigation: NavHostController, id: Long) {
    val task = rememberRpc<Task>("tasks.get", params("id" to id)) ?: return
    val settings = LocalSettings.current
    val clock24 = settings.timeFormat == "24h"
    val actions = LocalActions.current
    val projects = projectChoices()
    var confirmDelete by remember { mutableStateOf(false) }
    var title by remember(task.updatedAt) { mutableStateOf(task.title) }
    var description by remember(task.updatedAt) { mutableStateOf(task.description) }
    var tags by remember(task.updatedAt) { mutableStateOf(task.tags.joinToString(", ") { it.name }) }
    var newSubtask by remember { mutableStateOf("") }

    fun save(change: TaskInput.() -> TaskInput) {
        actions.run { Dayweave.mutate("tasks.update", withId(task.id, task.toInput().change())) }
    }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).imePadding()) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            textStyle = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused && title.isNotBlank() && title != task.title) save { copy(title = title) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        Row(Modifier.padding(top = 8.dp)) { DeadlineBadge(task.status, task.dueDate, task.dueTime, clock24) }

        Spacer(Modifier.height(16.dp))
        GroupedList {
            FieldRow("Status") {
                ChoiceField("Status", task.status, Statuses, onChange = { status ->
                    when {
                        status == task.status -> Unit
                        task.archived -> actions.run("Task restored") { Dayweave.mutate("tasks.restore", params("id" to task.id)) }
                        status == "archived" -> actions.run("Task archived", undo = { Dayweave.mutate("tasks.restore", params("id" to task.id)) }) {
                            Dayweave.mutate("tasks.archive", params("id" to task.id))
                        }
                        status == "completed" || task.completed -> toggleComplete(actions, task)
                        else -> save { copy(status = status) }
                    }
                })
            }
            RowDivider()
            FieldRow("Priority") {
                ChoiceField("Priority", task.priority, Priorities.map { it.key to it.value.label }, onChange = { save { copy(priority = it) } })
            }
            RowDivider()
            FieldRow("Project") {
                ChoiceField("Project", task.projectId, projects, onChange = { save { copy(projectId = it) } })
            }
        }

        Spacer(Modifier.height(12.dp))
        GroupedList {
            FieldRow("Deadline") {
                DateField("No date", task.dueDate, onChange = { date ->
                    save {
                        copy(
                            dueDate = date,
                            dueTime = if (date == null) null else dueTime,
                            recurrence = if (date == null) null else recurrence,
                            reminders = if (date == null) reminders.filter { it.kind == "at" } else reminders,
                        )
                    }
                })
            }
            if (task.dueDate != null) {
                RowDivider()
                FieldRow("Time") {
                    TimeField("All day", task.dueTime, clock24, onChange = { save { copy(dueTime = it) } })
                }
            }
            RowDivider()
            FieldRow("Starts") {
                DateField("Not set", task.startDate, onChange = { save { copy(startDate = it) } })
            }
            RowDivider()
            FieldRow("Repeat") {
                ChoiceField("Repeat", task.recurrence?.frequency, RepeatChoices, onChange = { frequency ->
                    if (task.dueDate == null && frequency != null) actions.say("Choose a deadline before repeating a task")
                    else save { copy(recurrence = frequency?.let { Recurrence(it) }) }
                })
            }
        }

        Label("Reminders")
        ReminderChips(task.reminders, task.dueDate != null, onChange = { save { copy(reminders = it) } })

        Label("Tags")
        OutlinedTextField(
            value = tags,
            onValueChange = { tags = it },
            placeholder = { Text("Separate tags with commas") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().onFocusChanged {
                val list = tags.split(',').map(String::trim).filter(String::isNotEmpty)
                if (!it.isFocused && list != task.tags.map { tag -> tag.name }) save { copy(tags = list) }
            },
        )

        SectionHeader("Subtasks")
        if (task.subtaskTotal > 0) {
            LinearProgressIndicator(
                progress = { task.subtaskDone.toFloat() / task.subtaskTotal },
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                trackColor = Dw.colors.sunken,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
        GroupedList {
            task.subtasks.forEach { subtask ->
                val depth = generateSequence(subtask.parentId) { parent -> task.subtasks.firstOrNull { it.id == parent }?.parentId }.count()
                Row(
                    Modifier.fillMaxWidth().clickable { actions.run { Dayweave.mutate("subtasks.toggle", params("id" to subtask.id)) } }
                        .padding(start = (8 + depth * 24).dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = subtask.completed, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                    Text(
                        subtask.title, modifier = Modifier.weight(1f),
                        color = if (subtask.completed) Dw.colors.ink2 else Dw.colors.ink,
                        textDecoration = if (subtask.completed) TextDecoration.LineThrough else null,
                    )
                    IconButton(onClick = { actions.run { Dayweave.mutate("subtasks.delete", params("id" to subtask.id)) } }) {
                        Icon(Icons.Filled.Close, contentDescription = "Delete ${subtask.title}", tint = Dw.colors.ink2)
                    }
                }
            }
            OutlinedTextField(
                value = newSubtask,
                onValueChange = { newSubtask = it },
                placeholder = { Text("Add a subtask") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val text = newSubtask.trim()
                    if (text.isNotEmpty()) actions.run { Dayweave.mutate("subtasks.add", params("taskId" to task.id, "title" to text)) }
                    newSubtask = ""
                }),
                modifier = Modifier.fillMaxWidth().padding(8.dp),
            )
        }

        Label("Description")
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Add a description") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused && description != task.description) save { copy(description = description) } },
        )

        Text(
            "Created ${formatInstant(task.createdAt)}" + (task.completedAt?.let { ". Completed ${formatInstant(it)}" } ?: ""),
            style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink2, modifier = Modifier.padding(vertical = 16.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (task.archived) {
                Button(onClick = { actions.run("Task restored") { Dayweave.mutate("tasks.restore", params("id" to task.id)) } }) {
                    Icon(Icons.Filled.Unarchive, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Restore")
                }
            } else {
                Button(onClick = { toggleComplete(actions, task) }) {
                    Icon(if (task.completed) Icons.Filled.RestartAlt else Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (task.completed) "Reopen" else "Complete")
                }
            }
            Spacer(Modifier.weight(1f))
            if (!task.completed && !task.archived) {
                IconButton(onClick = { navigation.navigate("focus?task=${task.id}") }) { Icon(Icons.Filled.TrackChanges, contentDescription = "Focus on this task") }
            }
            IconButton(onClick = {
                actions.run("Task duplicated") { Dayweave.mutate("tasks.duplicate", params("id" to task.id)) }
            }) { Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate") }
            if (!task.archived) {
                IconButton(onClick = {
                    actions.run("Task archived", undo = { Dayweave.mutate("tasks.restore", params("id" to task.id)) }) {
                        Dayweave.mutate("tasks.archive", params("id" to task.id))
                    }
                    navigation.popBackStack()
                }) { Icon(Icons.Filled.Archive, contentDescription = "Archive") }
            }
            IconButton(onClick = {
                if (settings.confirmDelete) confirmDelete = true
                else {
                    actions.run("Task deleted") { Dayweave.mutate("tasks.delete", params("id" to task.id)) }
                    navigation.popBackStack()
                }
            }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
        }
        Spacer(Modifier.height(32.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this task?") },
            text = { Text("\"${task.title}\" and its subtasks will be deleted. This can't be undone. Archive it instead to keep it out of the way.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.run("Task deleted") { Dayweave.mutate("tasks.delete", params("id" to task.id)) }
                    navigation.popBackStack()
                }) { Text("Delete task", color = Dw.colors.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTaskSheet(defaults: TaskInput, onClose: () -> Unit) {
    val settings = LocalSettings.current
    val clock24 = settings.timeFormat == "24h"
    val actions = LocalActions.current
    val projects = projectChoices()
    var input by remember { mutableStateOf(defaults.copy(priority = if (defaults.title.isEmpty()) settings.defaultPriority else defaults.priority)) }
    var reminders by remember { mutableStateOf<List<Reminder>?>(null) }
    val focus = remember { FocusRequester() }
    // A deadline gets the default reminder until the person chooses their own.
    val defaultReminders = if (input.dueDate != null && settings.defaultReminderMinutes != null) {
        listOf(Reminder("before", settings.defaultReminderMinutes))
    } else {
        emptyList()
    }
    val effective = reminders ?: defaultReminders

    fun save(keepOpen: Boolean) {
        val final = input.copy(reminders = if (input.dueDate == null) emptyList() else effective, dueTime = if (input.dueDate == null) null else input.dueTime)
        actions.run("Task added") { Dayweave.mutate("tasks.create", final.toParams()) }
        if (keepOpen) input = input.copy(title = "", description = "") else onClose()
    }

    LaunchedEffect(Unit) { focus.requestFocus() }
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Dw.colors.elevated) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().imePadding().verticalScroll(rememberScrollState())) {
            Text("New task", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = input.title,
                onValueChange = { input = input.copy(title = it) },
                placeholder = { Text("What needs doing?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (input.title.isNotBlank()) save(false) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = input.description,
                onValueChange = { input = input.copy(description = it) },
                placeholder = { Text("Add a description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Label("Deadline")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("No date", input.dueDate, onChange = { input = input.copy(dueDate = it) }, modifier = Modifier.weight(1f))
                TimeField("No time", input.dueTime, clock24, onChange = { input = input.copy(dueTime = it) }, modifier = Modifier.weight(1f), enabled = input.dueDate != null)
            }
            Label("Priority")
            ChoiceField("Priority", input.priority, Priorities.map { it.key to it.value.label }, onChange = { input = input.copy(priority = it) })
            Label("Project")
            ChoiceField("Project", input.projectId, projects, onChange = { input = input.copy(projectId = it) })
            Label("Reminders")
            ReminderChips(effective, input.dueDate != null, onChange = { reminders = it })
            Label("Repeat")
            ChoiceField("Repeat", input.recurrence?.frequency, RepeatChoices, onChange = { frequency ->
                input = input.copy(recurrence = frequency?.let { Recurrence(it) })
            })
            if (input.recurrence != null && input.dueDate == null) {
                Text("Choose a deadline for a repeating task.", style = MaterialTheme.typography.labelSmall, color = Dw.colors.warning)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                OutlinedButton(onClick = { save(true) }, enabled = input.title.isNotBlank()) { Text("Add another") }
                Button(onClick = { save(false) }, enabled = input.title.isNotBlank() && (input.recurrence == null || input.dueDate != null)) { Text("Add task") }
            }
        }
    }
}
