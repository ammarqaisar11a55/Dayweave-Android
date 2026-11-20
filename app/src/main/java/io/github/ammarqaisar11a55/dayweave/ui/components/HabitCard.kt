package io.github.ammarqaisar11a55.dayweave.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.ammarqaisar11a55.dayweave.core.Dayweave
import io.github.ammarqaisar11a55.dayweave.core.Habit
import io.github.ammarqaisar11a55.dayweave.core.params
import io.github.ammarqaisar11a55.dayweave.ui.theme.Accent
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw
import io.github.ammarqaisar11a55.dayweave.ui.theme.Tabular
import io.github.ammarqaisar11a55.dayweave.ui.theme.named

fun streakLabel(habit: Habit): String {
    val count = habit.stats.currentStreak
    val unit = habit.stats.streakUnit
    return "$count ${if (count == 1) unit.removeSuffix("s") else unit}"
}

fun frequencyLabel(habit: Habit): String = when (habit.frequency) {
    "daily" -> "Every day"
    "weekly_target" -> if (habit.weeklyTarget == 1) "Once a week" else "${habit.weeklyTarget} times a week"
    else -> {
        val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        if (habit.weekdays == listOf(0, 1, 2, 3, 4)) "Weekdays" else habit.weekdays.joinToString(", ") { names[it] }
    }
}

private fun formatAmount(value: Double) = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

@Composable
fun HabitControl(habit: Habit) {
    val actions = LocalActions.current
    val log: (Map<String, Any?>) -> Unit = { extra ->
        actions.run { Dayweave.mutate("habits.log", params("id" to habit.id, *extra.toList().toTypedArray())) }
    }
    val undo = { actions.run { Dayweave.mutate("habits.undo", params("id" to habit.id)) } }
    when (habit.kind) {
        "quantity" -> {
            val target = habit.quantityTarget ?: 1.0
            val value = habit.today.value ?: 0.0
            val step = if (target >= 10) 1.0 else if (target >= 3) 0.5 else 0.25
            var editing by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { log(mapOf("value" to (value - step).coerceAtLeast(0.0))) }, enabled = value > 0) {
                    Icon(Icons.Filled.Remove, contentDescription = "Reduce ${habit.name}")
                }
                Text(
                    "${formatAmount(value)}/${formatAmount(target)}",
                    style = MaterialTheme.typography.labelLarge.merge(Tabular),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = "Enter an amount") { editing = true }.padding(6.dp),
                )
                IconButton(onClick = { log(mapOf("value" to value + step)) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add to ${habit.name}")
                }
            }
            if (editing) {
                var text by remember { mutableStateOf(if (value > 0) formatAmount(value) else "") }
                AlertDialog(
                    onDismissRequest = { editing = false },
                    title = { Text(habit.name) },
                    text = {
                        OutlinedTextField(
                            value = text, onValueChange = { text = it }, singleLine = true,
                            label = { Text(if (habit.unit.isBlank()) "Amount" else "Amount in ${habit.unit}") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            text.toDoubleOrNull()?.let { log(mapOf("value" to it)) }
                            editing = false
                        }) { Text("Save") }
                    },
                    dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } },
                )
            }
        }
        "checklist" -> {
            var open by remember { mutableStateOf(false) }
            val checked = habit.today.checkedItems
            val done = habit.checklist.count { it.id in checked }
            FilledTonalButton(onClick = { open = true }) {
                Icon(if (done == habit.checklist.size) Icons.Filled.Check else Icons.Filled.Checklist, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("$done/${habit.checklist.size}", style = Tabular)
            }
            if (open) {
                AlertDialog(
                    onDismissRequest = { open = false },
                    title = { Text(habit.name) },
                    text = {
                        Column {
                            habit.checklist.forEach { item ->
                                val isChecked = item.id in checked
                                Row(
                                    Modifier.fillMaxWidth().clickable {
                                        val next = if (isChecked) checked - item.id else checked + item.id
                                        log(mapOf("checkedItems" to next))
                                    },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(checked = isChecked, onCheckedChange = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(item.title)
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { open = false }) { Text("Done") } },
                )
            }
        }
        "quit" -> if (habit.today.slipped) {
            TextButton(onClick = undo) { Text("Undo slip") }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Dw.colors.success, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text("On track", style = MaterialTheme.typography.labelSmall, color = Dw.colors.success)
                IconButton(onClick = { log(mapOf("slip" to true)) }) {
                    Icon(Icons.Filled.Block, contentDescription = "Log a slip for ${habit.name}", tint = Dw.colors.ink2)
                }
            }
        }
        else -> {
            val done = habit.today.state == "done"
            DoneCheck(
                checked = done,
                label = if (done) "Undo ${habit.name}" else "Complete ${habit.name}",
                onToggle = { if (done) undo() else log(emptyMap()) },
                ring = Dw.colors.named(habit.color),
                size = 28,
            )
        }
    }
}

private val StripLabels = mapOf(
    "done" to "Done", "partial" to "Partly done", "missed" to "Missed", "pending" to "Due today",
    "not_scheduled" to "Not scheduled", "inactive" to "Not started",
)

@Composable
/**
 * A habit with today's control. On its own it is a card with the last seven days; [inList] makes it a
 * plain row for a grouped list, where the list already draws the surface.
 */
fun HabitCard(habit: Habit, onOpen: () -> Unit, detailed: Boolean = false, inList: Boolean = false) {
    val colors = Dw.colors
    val tint = colors.named(habit.color)
    val highlight = habit.met && habit.today.due && !inList
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (inList) {
                    Modifier
                } else {
                    Modifier.clip(MaterialTheme.shapes.large)
                        .background(if (highlight) Accent.copy(alpha = if (colors.dark) 0.10f else 0.07f).compositeOn(colors.card) else colors.card)
                },
            )
            .clickable(onClickLabel = "Open ${habit.name}", onClick = onOpen)
            .padding(horizontal = if (inList) 16.dp else 12.dp, vertical = if (inList) 10.dp else 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(if (inList) 36.dp else 40.dp).clip(RoundedCornerShape(if (inList) 10.dp else 12.dp)).background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(iconFor(habit.icon), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocalFireDepartment, contentDescription = "Current streak",
                        tint = if (habit.stats.currentStreak > 0) colors.accentText else colors.ink3, modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(streakLabel(habit), style = MaterialTheme.typography.labelSmall.merge(Tabular), color = colors.ink2)
                    if (detailed) {
                        Spacer(Modifier.width(12.dp))
                        Text(
                            frequencyLabel(habit), style = MaterialTheme.typography.labelSmall, color = colors.ink3,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            HabitControl(habit)
        }
        if (detailed && habit.recent.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            val summary = "Last seven days: " + habit.recent.joinToString(", ") { StripLabels[it.state] ?: it.state }
            Row(
                Modifier.semantics(mergeDescendants = true) { contentDescription = summary },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                habit.recent.forEach { day ->
                    val color = when (day.state) {
                        "done" -> Accent
                        "partial" -> colors.heat[2]
                        "missed" -> colors.danger.copy(alpha = 0.3f)
                        else -> colors.sunken
                    }
                    Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
                }
            }
        }
    }
}

/** Blends a translucent colour over an opaque one, so a tinted card stays opaque. */
fun Color.compositeOn(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue = blue * a + background.blue * (1 - a),
    )
}
