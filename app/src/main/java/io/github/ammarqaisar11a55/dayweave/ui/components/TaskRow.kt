package io.github.ammarqaisar11a55.dayweave.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.ammarqaisar11a55.dayweave.core.Completed
import io.github.ammarqaisar11a55.dayweave.core.Dayweave
import io.github.ammarqaisar11a55.dayweave.core.Task
import io.github.ammarqaisar11a55.dayweave.core.params
import io.github.ammarqaisar11a55.dayweave.ui.theme.Accent
import io.github.ammarqaisar11a55.dayweave.ui.theme.AccentInk
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw
import io.github.ammarqaisar11a55.dayweave.ui.theme.named
import io.github.ammarqaisar11a55.dayweave.util.formatShortDate
import kotlinx.serialization.json.decodeFromJsonElement

/** A round completion control, used for anything that can be finished. */
@Composable
fun DoneCheck(checked: Boolean, label: String, onToggle: () -> Unit, ring: Color = Dw.colors.ink3, size: Int = 24) {
    val fill = animateColorAsState(if (checked) Accent else Color.Transparent, label = "check").value
    Box(
        Modifier
            .size(44.dp)
            .clickable(role = Role.Checkbox, onClickLabel = label, onClick = onToggle)
            .semantics {
                contentDescription = label
                stateDescription = if (checked) "Done" else "Not done"
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(fill)
                .then(if (checked) Modifier else Modifier.border(1.5.dp, ring, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(Icons.Filled.Check, contentDescription = null, tint = AccentInk, modifier = Modifier.size((size - 8).dp))
        }
    }
}

fun toggleComplete(actions: Actions, task: Task) {
    if (task.completed) {
        actions.run("Task reopened") { Dayweave.mutate("tasks.reopen", params("id" to task.id)) }
        return
    }
    actions.report(undo = { Dayweave.mutate("tasks.reopen", params("id" to task.id)) }) {
        val result: Completed = Dayweave.json.decodeFromJsonElement(Dayweave.mutate("tasks.complete", params("id" to task.id)))
        result.next?.dueDate?.let { "Task completed. Next on ${formatShortDate(it)}" } ?: "Task completed"
    }
}

@Composable
fun TaskRow(task: Task, clock24: Boolean, onOpen: () -> Unit, showProject: Boolean = true) {
    val actions = LocalActions.current
    val colors = Dw.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open ${task.title}", onClick = onOpen)
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(if (task.overdue) colors.danger else Color.Transparent))
        DoneCheck(
            checked = task.completed,
            label = if (task.completed) "Reopen ${task.title}" else "Complete ${task.title}",
            onToggle = { toggleComplete(actions, task) },
            ring = when (task.priority) {
                "urgent" -> colors.danger
                "high" -> colors.warning
                else -> colors.ink3
            },
        )
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (task.completed) colors.ink2 else colors.ink,
                textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val details = buildList {
                if (showProject && task.projectName != null) add("project")
                if (task.subtaskTotal > 0) add("subtasks")
                if (task.recurrenceId != null) add("repeat")
                if (task.tags.isNotEmpty()) add("tags")
            }
            if (details.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if ("project" in details) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ColorDot(colors.named(task.projectColor))
                            Spacer(Modifier.width(5.dp))
                            Meta(task.projectName ?: "")
                        }
                    }
                    if ("subtasks" in details) MetaIcon(Icons.Filled.Checklist, "${task.subtaskDone}/${task.subtaskTotal}", "Subtasks")
                    if ("repeat" in details) MetaIcon(Icons.Filled.Repeat, "Repeats", null)
                    if ("tags" in details) Meta(task.tags.joinToString(" ") { "#${it.name}" })
                }
            }
        }
        Column(Modifier.padding(start = 8.dp, end = 12.dp), horizontalAlignment = Alignment.End) {
            DeadlineBadge(task.status, task.dueDate, task.dueTime, clock24)
            if (task.priority != "medium") {
                Spacer(Modifier.height(2.dp))
                PriorityMark(task.priority)
            }
        }
    }
}

@Composable
private fun Meta(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun MetaIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, description: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = description, tint = Dw.colors.ink2, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(3.dp))
        Meta(text)
    }
}
