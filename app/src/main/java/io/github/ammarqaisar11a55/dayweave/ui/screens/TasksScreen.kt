package io.github.ammarqaisar11a55.dayweave.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.ammarqaisar11a55.dayweave.core.Dayweave
import io.github.ammarqaisar11a55.dayweave.core.Project
import io.github.ammarqaisar11a55.dayweave.core.Task
import io.github.ammarqaisar11a55.dayweave.core.TaskCounts
import io.github.ammarqaisar11a55.dayweave.core.TaskInput
import io.github.ammarqaisar11a55.dayweave.core.params
import io.github.ammarqaisar11a55.dayweave.ui.LocalNewTask
import io.github.ammarqaisar11a55.dayweave.ui.LocalSettings
import io.github.ammarqaisar11a55.dayweave.ui.components.EmptyState
import io.github.ammarqaisar11a55.dayweave.ui.components.LocalActions
import io.github.ammarqaisar11a55.dayweave.ui.components.RowDivider
import io.github.ammarqaisar11a55.dayweave.ui.components.TaskRow
import io.github.ammarqaisar11a55.dayweave.ui.components.rememberRpc
import io.github.ammarqaisar11a55.dayweave.ui.theme.Accent
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw
import io.github.ammarqaisar11a55.dayweave.util.today

private val Views = listOf(
    "all" to "All", "today" to "Today", "upcoming" to "Upcoming", "overdue" to "Overdue",
    "completed" to "Completed", "archived" to "Archived",
)

private val Sorts = listOf(
    "deadline" to "Deadline", "priority" to "Priority", "created" to "Date created", "status" to "Status", "title" to "Name",
)

private val EmptyText = mapOf(
    "all" to ("No tasks yet" to "Add your first task to start planning."),
    "today" to ("Nothing due today" to "Tasks due or starting today appear here."),
    "upcoming" to ("Nothing coming up" to "Tasks with a future deadline appear here."),
    "overdue" to ("Nothing overdue" to "You're on top of your deadlines."),
    "completed" to ("Nothing completed yet" to "Finished tasks stay here as a record."),
    "archived" to ("Nothing archived" to "Archive tasks to keep them without seeing them."),
)

@Composable
fun TasksScreen(navigation: NavHostController, initialView: String, projectId: Long?) {
    val settings = LocalSettings.current
    val clock24 = settings.timeFormat == "24h"
    val newTask = LocalNewTask.current
    val actions = LocalActions.current
    var view by rememberSaveable { mutableStateOf(initialView) }
    var search by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf("deadline") }
    var sortOpen by remember { mutableStateOf(false) }
    val counts = rememberRpc<TaskCounts>("tasks.counts")
    val project = projectId?.let { id -> rememberRpc<List<Project>>("projects.list", params("includeArchived" to true))?.firstOrNull { it.id == id } }
    val tasks = rememberRpc<List<Task>>(
        "tasks.list",
        params("view" to view, "search" to search.ifBlank { null }, "sort" to sort, "projectId" to projectId),
    )

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            if (project != null) {
                item {
                    Text(project.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Views.forEach { (key, label) ->
                        val count = when (key) {
                            "all" -> counts?.all
                            "today" -> counts?.today
                            "upcoming" -> counts?.upcoming
                            "overdue" -> counts?.overdue
                            else -> null
                        }
                        FilterChip(
                            selected = view == key,
                            onClick = { view = key },
                            label = { Text(if (count != null && count > 0) "$label  $count" else label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Dw.colors.accentSoft,
                                selectedLabelColor = Dw.colors.accentText,
                            ),
                        )
                    }
                }
            }
            item {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search these tasks") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Dw.colors.card, focusedContainerColor = Dw.colors.card),
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        IconButton(onClick = { sortOpen = true }) { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort by ${Sorts.first { it.first == sort }.second}") }
                        DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                            Sorts.forEach { (key, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = { sort = key; sortOpen = false })
                            }
                        }
                    }
                }
            }
            if (view == "overdue" && !tasks.isNullOrEmpty()) {
                item {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            actions.run("Moved to today") {
                                tasks.forEach { Dayweave.mutate("tasks.reschedule", params("id" to it.id, "dueDate" to today().toString(), "dueTime" to it.dueTime)) }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) { Text("Move all to today") }
                }
            }
            if (tasks != null) {
                if (tasks.isEmpty()) {
                    item {
                        val (title, body) = if (search.isNotBlank()) "No matching tasks" to "Try a different search." else EmptyText.getValue(view)
                        Box(Modifier.padding(16.dp).clip(MaterialTheme.shapes.large).background(Dw.colors.card)) {
                            EmptyState(if (search.isNotBlank()) Icons.Filled.Search else Icons.Filled.CheckCircle, title, body)
                        }
                    }
                }
                itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
                    val shape = when {
                        tasks.size == 1 -> MaterialTheme.shapes.large
                        index == 0 -> MaterialTheme.shapes.large.copy(bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp), bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp))
                        index == tasks.lastIndex -> MaterialTheme.shapes.large.copy(topStart = androidx.compose.foundation.shape.CornerSize(0.dp), topEnd = androidx.compose.foundation.shape.CornerSize(0.dp))
                        else -> androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
                    }
                    Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(shape).background(Dw.colors.card)) {
                        androidx.compose.foundation.layout.Column {
                            if (index > 0) RowDivider()
                            TaskRow(task, clock24, onOpen = { navigation.navigate("task/${task.id}") }, showProject = projectId == null)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
        if (view != "archived" && view != "completed") {
            ExtendedFloatingActionButton(
                onClick = {
                    newTask(TaskInput(title = "", dueDate = if (view == "today") today().toString() else null, projectId = projectId))
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New task") },
                containerColor = Accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }
}
