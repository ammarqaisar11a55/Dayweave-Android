package io.github.ammarqaisar11a55.dayweave.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ammarqaisar11a55.dayweave.core.Dayweave
import io.github.ammarqaisar11a55.dayweave.core.FocusSession
import io.github.ammarqaisar11a55.dayweave.core.FocusStats
import io.github.ammarqaisar11a55.dayweave.core.Habit
import io.github.ammarqaisar11a55.dayweave.core.Task
import io.github.ammarqaisar11a55.dayweave.core.params
import io.github.ammarqaisar11a55.dayweave.ui.LocalSettings
import io.github.ammarqaisar11a55.dayweave.ui.components.ChoiceField
import io.github.ammarqaisar11a55.dayweave.ui.components.FieldRow
import io.github.ammarqaisar11a55.dayweave.ui.components.GroupedList
import io.github.ammarqaisar11a55.dayweave.ui.components.LocalActions
import io.github.ammarqaisar11a55.dayweave.ui.components.RowDivider
import io.github.ammarqaisar11a55.dayweave.ui.components.SectionHeader
import io.github.ammarqaisar11a55.dayweave.ui.components.rememberRpc
import io.github.ammarqaisar11a55.dayweave.ui.theme.Accent
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw
import io.github.ammarqaisar11a55.dayweave.ui.theme.Tabular
import io.github.ammarqaisar11a55.dayweave.util.formatClock
import io.github.ammarqaisar11a55.dayweave.util.formatDuration
import io.github.ammarqaisar11a55.dayweave.util.formatInstant
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import java.time.Instant

private val KindLabels = mapOf("focus" to "Focus", "short_break" to "Short break", "long_break" to "Long break")

@Composable
private fun Dial(fraction: Float, paused: Boolean, content: @Composable () -> Unit) {
    val sweep by animateFloatAsState(fraction, tween(500), label = "dial")
    val track = Dw.colors.threadGhost
    val color = if (paused) Dw.colors.ink3 else Accent
    Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(280.dp)) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2
            drawArc(track, 0f, 360f, false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke))
            drawArc(color, -90f, 360f * sweep, false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

/** Seconds left, from the core's end time rather than counted ticks, so it stays right in the background. */
@Composable
private fun remaining(session: FocusSession): Long {
    var now by androidx.compose.runtime.remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session.endsAt, session.status) {
        while (session.status == "running") {
            now = System.currentTimeMillis()
            if (session.endsAt != null && now >= Instant.parse(session.endsAt).toEpochMilli() + 1500) Dayweave.refresh()
            delay(500)
        }
    }
    val end = session.endsAt
    return if (session.status == "running" && end != null) ((Instant.parse(end).toEpochMilli() - now + 999) / 1000).coerceAtLeast(0) else session.remainingSeconds
}

@Composable
fun FocusScreen(initialTask: Long?) {
    val current = rememberRpc<JsonElement>("focus.current")
    val session = current?.takeIf { it !is JsonNull }?.let { Dayweave.json.decodeFromJsonElement(FocusSession.serializer(), it) }
    val stats = rememberRpc<FocusStats>("focus.stats")
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (current == null) return@Column
        if (session != null) Running(session) else Setup(initialTask, stats)
        if (session == null) History(stats)
    }
}

@Composable
private fun Running(session: FocusSession) {
    val actions = LocalActions.current
    val left = remaining(session)
    val paused = session.status == "paused"
    Spacer(Modifier.height(32.dp))
    Dial(left.toFloat() / session.plannedSeconds.coerceAtLeast(1), paused) {
        Text(if (paused) "Paused" else KindLabels[session.kind] ?: "", style = MaterialTheme.typography.labelLarge, color = Dw.colors.ink2)
        Text(
            formatClock(left),
            style = MaterialTheme.typography.displayMedium.merge(Tabular).copy(fontSize = 56.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.semantics { contentDescription = "${formatDuration(left)} left" },
        )
        (session.taskTitle ?: session.habitName)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Dw.colors.ink2, maxLines = 1) }
    }
    Spacer(Modifier.height(32.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (paused) {
            Button(onClick = { actions.run { Dayweave.mutate("focus.resume") } }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Resume")
            }
        } else {
            Button(onClick = { actions.run { Dayweave.mutate("focus.pause") } }) {
                Icon(Icons.Filled.Pause, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Pause")
            }
        }
        FilledTonalButton(onClick = { actions.run("Session completed") { Dayweave.mutate("focus.complete") } }) {
            Icon(Icons.Filled.Check, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Finish")
        }
        TextButton(onClick = { actions.run("Session stopped. Time so far is saved") { Dayweave.mutate("focus.stop") } }) {
            Icon(Icons.Filled.Stop, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Stop")
        }
    }
}

@Composable
private fun Setup(initialTask: Long?, stats: FocusStats?) {
    val settings = LocalSettings.current
    val actions = LocalActions.current
    var kind by rememberSaveable { mutableStateOf("focus") }
    var length by rememberSaveable { mutableStateOf("default") }
    var taskId by rememberSaveable { mutableStateOf(initialTask) }
    var habitId by rememberSaveable { mutableStateOf<Long?>(null) }
    val tasks = rememberRpc<List<Task>>("tasks.list", params("view" to "all", "statuses" to listOf("todo", "in_progress"))).orEmpty()
    val habits = rememberRpc<List<Habit>>("habits.list").orEmpty()
    val longBreakDue = (stats?.sessionsSinceLongBreak ?: 0) >= settings.sessionsBeforeLongBreak
    val minutes = when (kind) {
        "short_break" -> settings.shortBreakMinutes
        "long_break" -> settings.longBreakMinutes
        else -> if (length == "50") 50 else settings.focusMinutes
    }

    Spacer(Modifier.height(12.dp))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        KindLabels.entries.forEachIndexed { index, (key, label) ->
            SegmentedButton(selected = kind == key, onClick = { kind = key }, shape = SegmentedButtonDefaults.itemShape(index, KindLabels.size)) { Text(label) }
        }
    }
    Spacer(Modifier.height(24.dp))
    Dial(1f, paused = false) {
        Text(KindLabels.getValue(kind), style = MaterialTheme.typography.labelLarge, color = Dw.colors.ink2)
        Text(formatClock(minutes * 60L), style = MaterialTheme.typography.displayMedium.merge(Tabular).copy(fontSize = 56.sp, fontWeight = FontWeight.SemiBold))
        if (kind == "short_break" && longBreakDue) Text("A long break is due", style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink2)
    }
    Spacer(Modifier.height(20.dp))
    if (kind == "focus") {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("default" to "${settings.focusMinutes} min", "50" to "50 min").forEachIndexed { index, (key, label) ->
                SegmentedButton(selected = length == key, onClick = { length = key }, shape = SegmentedButtonDefaults.itemShape(index, 2)) { Text(label) }
            }
        }
        Spacer(Modifier.height(12.dp))
        GroupedList {
            FieldRow("Working on") {
                ChoiceField("Task", taskId, listOf<Pair<Long?, String>>(null to "Nothing in particular") + tasks.map { it.id to it.title }, onChange = { taskId = it })
            }
            RowDivider()
            FieldRow("Counts towards") {
                ChoiceField("Habit", habitId, listOf<Pair<Long?, String>>(null to "No habit") + habits.map { it.id to it.name }, onChange = { habitId = it })
            }
        }
    }
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = {
            actions.run {
                Dayweave.mutate(
                    "focus.start",
                    params("kind" to kind, "minutes" to minutes, "taskId" to taskId.takeIf { kind == "focus" }, "habitId" to habitId.takeIf { kind == "focus" }),
                )
            }
        },
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(if (kind == "focus") "Start focusing" else "Start break", style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun History(stats: FocusStats?) {
    val sessions = rememberRpc<List<FocusSession>>("focus.history", params("limit" to 12)).orEmpty()
    SectionHeader("Your focus")
    GroupedList {
        Row(Modifier.padding(16.dp)) {
            listOf(
                "Today" to formatDuration(stats?.todaySeconds ?: 0),
                "Last 7 days" to formatDuration(stats?.weekSeconds ?: 0),
                "Sessions" to "${stats?.totalSessions ?: 0}",
            ).forEach { (label, value) ->
                Column(Modifier.weight(1f)) {
                    Text(value, style = MaterialTheme.typography.titleMedium.merge(Tabular))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink2)
                }
            }
        }
    }
    if (sessions.isNotEmpty()) {
        SectionHeader("Session history")
        GroupedList {
            sessions.forEachIndexed { index, session ->
                if (index > 0) RowDivider()
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(session.taskTitle ?: session.habitName ?: KindLabels[session.kind] ?: "", maxLines = 1)
                        Text(
                            formatInstant(session.startedAt) + if (session.status == "stopped") ". Stopped early" else "",
                            style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink2,
                        )
                    }
                    Text(formatDuration(session.elapsedSeconds), style = MaterialTheme.typography.labelMedium.merge(Tabular), color = Dw.colors.ink2)
                }
            }
        }
    }
    Spacer(Modifier.height(24.dp))
}
