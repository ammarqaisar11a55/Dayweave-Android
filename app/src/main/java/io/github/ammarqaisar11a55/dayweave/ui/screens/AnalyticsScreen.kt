package io.github.ammarqaisar11a55.dayweave.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.ammarqaisar11a55.dayweave.core.Analytics
import io.github.ammarqaisar11a55.dayweave.core.HabitSummary
import io.github.ammarqaisar11a55.dayweave.core.params
import io.github.ammarqaisar11a55.dayweave.ui.components.BarChart
import io.github.ammarqaisar11a55.dayweave.ui.components.EmptyState
import io.github.ammarqaisar11a55.dayweave.ui.components.GroupedList
import io.github.ammarqaisar11a55.dayweave.ui.components.RowDivider
import io.github.ammarqaisar11a55.dayweave.ui.components.SectionHeader
import io.github.ammarqaisar11a55.dayweave.ui.components.iconFor
import io.github.ammarqaisar11a55.dayweave.ui.components.rememberRpc
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw
import io.github.ammarqaisar11a55.dayweave.ui.theme.Tabular
import io.github.ammarqaisar11a55.dayweave.ui.theme.named
import io.github.ammarqaisar11a55.dayweave.util.formatDuration
import io.github.ammarqaisar11a55.dayweave.util.formatShortDate
import io.github.ammarqaisar11a55.dayweave.util.percent

@Composable
fun AnalyticsScreen() {
    var days by rememberSaveable { mutableIntStateOf(90) }
    val data = rememberRpc<Analytics>("analytics.get", params("days" to days))
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(30 to "30 days", 90 to "90 days", 365 to "Year").forEachIndexed { index, (value, label) ->
                SegmentedButton(selected = days == value, onClick = { days = value }, shape = SegmentedButtonDefaults.itemShape(index, 3)) { Text(label) }
            }
        }
        if (data == null) return@Column
        if (data.tasksCompleted + data.tasksPending == 0 && data.habits.isEmpty() && data.focusSeconds == 0L) {
            Spacer(Modifier.height(16.dp))
            GroupedList { EmptyState(Icons.Filled.BarChart, "Nothing to show yet", "Complete tasks, log habits and run focus sessions, and your trends appear here.") }
            return@Column
        }
        Spacer(Modifier.height(12.dp))
        val thisWeek = data.weekly.lastOrNull()?.tasksCompleted ?: 0
        val change = thisWeek - (data.weekly.getOrNull(data.weekly.size - 2)?.tasksCompleted ?: 0)
        val from = data.weekly.firstOrNull()?.let { "Week of ${formatShortDate(it.week)}" }
        GroupedList {
            SummaryRow("Tasks finished", percent(data.taskCompletionRate), "${data.tasksCompleted} done, ${data.tasksOverdue} overdue")
            RowDivider()
            SummaryRow("Habits kept", percent(data.habitCompletionRate), "Of the days they were scheduled")
            RowDivider()
            SummaryRow("Time focused", formatDuration(data.focusSeconds), "${data.focusSessions} sessions")
            RowDivider()
            SummaryRow(
                "This week", "$thisWeek tasks",
                if (change == 0) "Same as last week" else "${kotlin.math.abs(change)} ${if (change > 0) "more" else "fewer"} than last week",
            )
        }

        ChartCard("Tasks finished each week") {
            BarChart(
                data.weekly.map { it.tasksCompleted.toFloat() }, Dw.colors.info,
                "Tasks finished each week: " + data.weekly.joinToString { "${it.tasksCompleted}" },
                start = from, peak = "Most in a week: ${data.weekly.maxOfOrNull { it.tasksCompleted } ?: 0}",
            )
        }
        ChartCard("Habits kept each week") {
            BarChart(
                data.weekly.map { ((it.habitRate ?: 0.0) * 100).toFloat() }, Dw.colors.success,
                "Habits kept by week: " + data.weekly.joinToString { percent(it.habitRate ?: 0.0) },
                start = from, peak = "This week: ${percent(data.weekly.lastOrNull()?.habitRate ?: 0.0)}",
            )
        }
        ChartCard("Time focused each week") {
            BarChart(
                data.weekly.map { it.focusSeconds / 60f }, Dw.colors.violet,
                "Minutes focused each week: " + data.weekly.joinToString { "${it.focusSeconds / 60}" },
                start = from, peak = "This week: ${formatDuration(data.weekly.lastOrNull()?.focusSeconds ?: 0)}",
            )
        }
        if (data.mostConsistent.isNotEmpty()) HabitList("Most consistent", data.mostConsistent)
        if (data.needsImprovement.isNotEmpty()) HabitList("Needs attention", data.needsImprovement)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryRow(label: String, value: String, note: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(note, style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink2)
        }
        Text(value, style = MaterialTheme.typography.titleMedium.merge(Tabular))
    }
}

@Composable
private fun ChartCard(title: String, chart: @Composable () -> Unit) {
    SectionHeader(title)
    GroupedList { Column(Modifier.padding(16.dp)) { chart() } }
}

@Composable
private fun HabitList(title: String, habits: List<HabitSummary>) {
    SectionHeader(title)
    GroupedList {
        habits.forEachIndexed { index, habit ->
            if (index > 0) RowDivider()
            val tint = Dw.colors.named(habit.color)
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                    Icon(iconFor(habit.icon), contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(habit.name, modifier = Modifier.weight(1f))
                Text(percent(habit.monthlyConsistency), style = MaterialTheme.typography.labelMedium.merge(Tabular), color = Dw.colors.ink2)
            }
        }
    }
}
