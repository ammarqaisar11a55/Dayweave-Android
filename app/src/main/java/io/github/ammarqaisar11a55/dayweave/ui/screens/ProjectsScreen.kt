package io.github.ammarqaisar11a55.dayweave.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.ammarqaisar11a55.dayweave.core.Dayweave
import io.github.ammarqaisar11a55.dayweave.core.Project
import io.github.ammarqaisar11a55.dayweave.core.params
import io.github.ammarqaisar11a55.dayweave.ui.LocalSettings
import io.github.ammarqaisar11a55.dayweave.ui.components.DateField
import io.github.ammarqaisar11a55.dayweave.ui.components.EmptyState
import io.github.ammarqaisar11a55.dayweave.ui.components.GroupedList
import io.github.ammarqaisar11a55.dayweave.ui.components.LocalActions
import io.github.ammarqaisar11a55.dayweave.ui.components.iconFor
import io.github.ammarqaisar11a55.dayweave.ui.components.rememberRpc
import io.github.ammarqaisar11a55.dayweave.ui.theme.Accent
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw
import io.github.ammarqaisar11a55.dayweave.ui.theme.Tabular
import io.github.ammarqaisar11a55.dayweave.ui.theme.named
import io.github.ammarqaisar11a55.dayweave.util.formatShortDate
import io.github.ammarqaisar11a55.dayweave.util.percent

@Composable
fun ProjectsScreen(navigation: NavHostController) {
    val projects = rememberRpc<List<Project>>("projects.list")
    var editing by remember { mutableStateOf<Project?>(null) }
    var creating by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (projects?.isEmpty() == true) {
                item {
                    GroupedList {
                        EmptyState(Icons.Filled.Folder, "No projects yet", "A project gathers the tasks behind something bigger, with its own deadline.") {
                            Button(onClick = { creating = true }) { Text("Add a project") }
                        }
                    }
                }
            }
            items(projects.orEmpty(), key = { it.id }) { project ->
                ProjectCard(project, onOpen = { navigation.navigate("tasks?project=${project.id}") }, onEdit = { editing = project })
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
        ExtendedFloatingActionButton(
            onClick = { creating = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("New project") },
            containerColor = Accent,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
    if (creating || editing != null) ProjectDialog(editing, onClose = { creating = false; editing = null })
}

@Composable
private fun ProjectCard(project: Project, onOpen: () -> Unit, onEdit: () -> Unit) {
    val actions = LocalActions.current
    val tint = Dw.colors.named(project.color)
    var menu by remember { mutableStateOf(false) }
    GroupedList(Modifier.clickable(onClickLabel = "Open ${project.name}", onClick = onOpen)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                    Icon(iconFor(project.icon), contentDescription = null, tint = tint)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(project.name, style = MaterialTheme.typography.titleSmall)
                    Text(project.description.ifBlank { "${project.taskTotal} tasks" }, style = MaterialTheme.typography.bodyMedium, color = Dw.colors.ink2, maxLines = 1)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Actions for ${project.name}") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit() })
                        DropdownMenuItem(text = { Text("Archive") }, onClick = {
                            menu = false
                            actions.run("Project archived", undo = { Dayweave.mutate("projects.restore", params("id" to project.id)) }) {
                                Dayweave.mutate("projects.archive", params("id" to project.id))
                            }
                        })
                        DropdownMenuItem(text = { Text("Delete", color = Dw.colors.danger) }, onClick = {
                            menu = false
                            actions.run("Project deleted. Its tasks are kept") { Dayweave.mutate("projects.delete", params("id" to project.id)) }
                        })
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row {
                Text("${project.taskDone} of ${project.taskTotal} tasks done", style = MaterialTheme.typography.labelSmall.merge(Tabular), color = Dw.colors.ink2, modifier = Modifier.weight(1f))
                Text(percent(project.completion), style = MaterialTheme.typography.labelMedium.merge(Tabular))
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { project.completion.toFloat() }, color = tint, trackColor = Dw.colors.sunken,
                gapSize = 0.dp, drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            )
            if (project.deadline != null || project.taskOverdue > 0) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    project.deadline?.let { Text("Deadline ${formatShortDate(it)}", style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink2) }
                    if (project.taskOverdue > 0) Text("${project.taskOverdue} overdue", style = MaterialTheme.typography.labelSmall, color = Dw.colors.danger)
                }
            }
        }
    }
}

@Composable
private fun ProjectDialog(project: Project?, onClose: () -> Unit) {
    val actions = LocalActions.current
    var name by remember { mutableStateOf(project?.name ?: "") }
    var description by remember { mutableStateOf(project?.description ?: "") }
    var color by remember { mutableStateOf(project?.color ?: "orange") }
    var deadline by remember { mutableStateOf(project?.deadline) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (project == null) "New project" else "Edit project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                DateField("No deadline", deadline, onChange = { deadline = it })
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Dw.colors.palette.forEach { (key, value) ->
                        Box(
                            Modifier.size(26.dp).clip(CircleShape).background(value)
                                .then(if (color == key) Modifier.border(2.dp, Dw.colors.ink, CircleShape) else Modifier)
                                .clickable { color = key }.semantics { contentDescription = key },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                actions.run(if (project == null) "Project added" else "Project saved") {
                    Dayweave.mutate(
                        if (project == null) "projects.create" else "projects.update",
                        params("id" to project?.id, "name" to name, "description" to description, "color" to color, "icon" to (project?.icon ?: "folder"), "deadline" to deadline, "startDate" to project?.startDate),
                    )
                }
                onClose()
            }) { Text(if (project == null) "Add project" else "Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}
