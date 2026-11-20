package io.github.ammarqaisar11a55.dayweave.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.ammarqaisar11a55.dayweave.BuildConfig
import io.github.ammarqaisar11a55.dayweave.core.AppInfo
import io.github.ammarqaisar11a55.dayweave.core.BackupCounts
import io.github.ammarqaisar11a55.dayweave.core.BackupPreview
import io.github.ammarqaisar11a55.dayweave.core.Dayweave
import io.github.ammarqaisar11a55.dayweave.core.DayweaveException
import io.github.ammarqaisar11a55.dayweave.core.params
import io.github.ammarqaisar11a55.dayweave.notify.Notifications
import io.github.ammarqaisar11a55.dayweave.notify.Scheduler
import io.github.ammarqaisar11a55.dayweave.ui.LocalSettings
import io.github.ammarqaisar11a55.dayweave.ui.components.ChoiceField
import io.github.ammarqaisar11a55.dayweave.ui.components.FieldRow
import io.github.ammarqaisar11a55.dayweave.ui.components.GroupedList
import io.github.ammarqaisar11a55.dayweave.ui.components.LocalActions
import io.github.ammarqaisar11a55.dayweave.ui.components.RowDivider
import io.github.ammarqaisar11a55.dayweave.ui.components.SectionHeader
import io.github.ammarqaisar11a55.dayweave.ui.components.TimeField
import io.github.ammarqaisar11a55.dayweave.ui.components.rememberRpc
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw
import io.github.ammarqaisar11a55.dayweave.ui.theme.Tabular
import io.github.ammarqaisar11a55.dayweave.update.UpdatePreferences
import io.github.ammarqaisar11a55.dayweave.update.UpdateStatus
import io.github.ammarqaisar11a55.dayweave.update.Updates
import io.github.ammarqaisar11a55.dayweave.util.formatInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

private val Themes = listOf("system" to "Match the system", "light" to "Light", "dark" to "Dark")
private val WeekStarts = listOf("monday" to "Monday", "sunday" to "Sunday", "saturday" to "Saturday")
private val TimeFormats = listOf("24h" to "24-hour", "12h" to "12-hour")
private val PriorityChoices = listOf("low" to "Low", "medium" to "Medium", "high" to "High", "urgent" to "Urgent")
private val DefaultReminders = listOf<Pair<Long?, String>>(null to "No reminder") + ReminderPresets

private enum class Export(val label: String, val method: String, val mime: String, val extension: String) {
    Backup("Full backup", "backup.exportJson", "application/json", "json"),
    Database("Database copy", "backup.exportDatabase", "application/vnd.sqlite3", "db"),
    Tasks("Tasks as CSV", "backup.exportTasksCsv", "text/csv", "csv"),
    Habits("Habits as CSV", "backup.exportHabitsCsv", "text/csv", "csv"),
}

@Composable
fun SettingsScreen() {
    val settings = LocalSettings.current
    val actions = LocalActions.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun set(key: String, value: Any?) = actions.run { Dayweave.mutate("settings.set", params("key" to key, "value" to value)) }

    // Permission states can change in system settings, so they are read again whenever the app returns.
    var resumed by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { resumed++ } }
    val notificationsAllowed = remember(resumed) { Notifications.allowed(context) }
    val exactAlarms = remember(resumed) { Scheduler.canScheduleExact(context) }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { resumed++ }

    var pending by remember { mutableStateOf<Export?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val export = pending ?: return@rememberLauncherForActivityResult
        pending = null
        if (uri != null) actions.run("${export.label} saved") { exportTo(context, export, uri) }
    }

    var restoreFile by remember { mutableStateOf<File?>(null) }
    var preview by remember { mutableStateOf<BackupPreview?>(null) }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val staged = withContext(Dispatchers.IO) { stage(context, uri) }
                preview = Dayweave.get<BackupPreview>("backup.preview", params("path" to staged.path))
                restoreFile = staged
            } catch (error: DayweaveException) {
                actions.say(error.message ?: "This file can't be restored")
            } catch (error: java.io.IOException) {
                actions.say("The file couldn't be read")
            }
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        SectionHeader("Appearance")
        GroupedList {
            ChoiceRow("Theme", settings.theme, Themes) { set("theme", it) }
            RowDivider()
            ChoiceRow("Week starts on", settings.weekStart, WeekStarts) { set("weekStart", it) }
            RowDivider()
            ChoiceRow("Time format", settings.timeFormat, TimeFormats) { set("timeFormat", it) }
        }

        SectionHeader("Tasks")
        GroupedList {
            ChoiceRow("Default priority", settings.defaultPriority, PriorityChoices) { set("defaultPriority", it) }
            RowDivider()
            ChoiceRow("Default reminder", settings.defaultReminderMinutes, DefaultReminders) { set("defaultReminderMinutes", it) }
            RowDivider()
            SwitchRow("Confirm before deleting", null, settings.confirmDelete) { set("confirmDelete", it) }
        }

        SectionHeader("Notifications")
        GroupedList {
            if (!notificationsAllowed) {
                ActionRow(
                    "Notifications are off for Dayweave",
                    "Reminders can't reach you until Android allows them.",
                    "Allow",
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName))
                    }
                }
                RowDivider()
            }
            if (!exactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActionRow(
                    "Reminders may arrive a few minutes late",
                    "Allow alarms and reminders so they arrive on the minute.",
                    "Allow",
                ) {
                    context.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                }
                RowDivider()
            }
            SwitchRow("Notifications", "Turn every reminder on or off", settings.notificationsEnabled) { set("notificationsEnabled", it) }
            RowDivider()
            SwitchRow("Task reminders", null, settings.taskNotifications, enabled = settings.notificationsEnabled) { set("taskNotifications", it) }
            RowDivider()
            SwitchRow("Habit reminders", null, settings.habitNotifications, enabled = settings.notificationsEnabled) { set("habitNotifications", it) }
            RowDivider()
            SwitchRow("Focus sessions", "When a session or break ends", settings.focusNotifications, enabled = settings.notificationsEnabled) { set("focusNotifications", it) }
            RowDivider()
            FieldRow("Remind all-day tasks at") {
                TimeField("Time", settings.dateOnlyReminderTime, settings.timeFormat == "24h", { it?.let { time -> set("dateOnlyReminderTime", time) } }, clearable = false)
            }
        }

        SectionHeader("Focus")
        GroupedList {
            NumberRow("Focus length", settings.focusMinutes, 5, 240, 5, "min") { set("focusMinutes", it) }
            RowDivider()
            NumberRow("Short break", settings.shortBreakMinutes, 1, 60, 1, "min") { set("shortBreakMinutes", it) }
            RowDivider()
            NumberRow("Long break", settings.longBreakMinutes, 5, 120, 5, "min") { set("longBreakMinutes", it) }
            RowDivider()
            NumberRow("Sessions before a long break", settings.sessionsBeforeLongBreak, 1, 12, 1, null) { set("sessionsBeforeLongBreak", it) }
        }

        SectionHeader("Backup")
        GroupedList {
            Text(
                "Everything stays on this phone. Save a backup to keep a copy elsewhere, or to move your data to Dayweave on another phone or your Ubuntu desktop.",
                style = MaterialTheme.typography.bodyMedium, color = Dw.colors.ink2,
                modifier = Modifier.padding(16.dp),
            )
            Export.entries.forEach { export ->
                RowDivider()
                ActionRow(export.label, null, "Save") {
                    pending = export
                    save.launch("dayweave-${export.name.lowercase()}-${LocalDate.now()}.${export.extension}")
                }
            }
            RowDivider()
            ActionRow("Restore from a backup", "Replaces everything here. You'll see what's in the file first.", "Choose") {
                open.launch(arrayOf("application/json", "application/octet-stream", "application/vnd.sqlite3", "application/x-sqlite3", "*/*"))
            }
        }

        UpdatesSection()

        SectionHeader("About")
        val info = rememberRpc<AppInfo>("app.info")
        GroupedList {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Dayweave ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleSmall)
                info?.let { Text("Core ${it.version}, data format ${it.schemaVersion}", style = MaterialTheme.typography.labelSmall.merge(Tabular), color = Dw.colors.ink2) }
                Spacer(Modifier.height(4.dp))
                Text(
                    "No account, no tracking and no ads. Your tasks and habits never leave this phone unless you save a backup. The only network use is checking GitHub for updates.",
                    style = MaterialTheme.typography.bodyMedium, color = Dw.colors.ink2,
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    val currentPreview = preview
    val file = restoreFile
    if (currentPreview != null && file != null) {
        RestoreDialog(
            currentPreview,
            onCancel = { preview = null; restoreFile = null; file.delete() },
            onConfirm = {
                preview = null
                restoreFile = null
                actions.run("Backup restored. A copy of what was here is kept in case you need it") {
                    try {
                        Dayweave.mutate("backup.restore", params("path" to file.path))
                    } finally {
                        file.delete()
                    }
                }
            },
        )
    }
}

@Composable
private fun UpdatesSection() {
    val context = LocalContext.current
    val actions = LocalActions.current
    val scope = rememberCoroutineScope()
    val preferences = remember { UpdatePreferences(context) }
    var autoCheck by remember { mutableStateOf(preferences.autoCheck) }
    val status by Updates.status.collectAsState()
    SectionHeader("Updates")
    GroupedList {
        SwitchRow("Check for updates automatically", "Once a day, from Dayweave's GitHub releases", autoCheck) {
            preferences.autoCheck = it
            autoCheck = it
        }
        RowDivider()
        val lastCheck = preferences.lastCheck
        ActionRow(
            "Check now",
            if (lastCheck > 0) "Last checked ${formatInstant(java.time.Instant.ofEpochMilli(lastCheck).toString())}" else "Not checked yet",
            if (status is UpdateStatus.Checking) "Checking…" else "Check",
            enabled = status is UpdateStatus.Idle,
        ) {
            scope.launch { Updates.check(context, manual = true)?.let(actions::say) }
        }
    }
}

@Composable
private fun RestoreDialog(preview: BackupPreview, onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Replace everything with this backup?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Saved ${formatInstant(preview.exportedAt)} by Dayweave ${preview.appVersion}, as a ${if (preview.kind == "database") "database copy" else "full backup"}.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                CountTable(preview.incoming, preview.current)
                Text(
                    "Everything here now is replaced. A copy of it is kept on this phone first.",
                    style = MaterialTheme.typography.bodyMedium, color = Dw.colors.ink2,
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Restore", color = Dw.colors.danger) } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}

@Composable
private fun CountTable(incoming: BackupCounts, current: BackupCounts) {
    val rows = listOf(
        "Tasks" to (incoming.tasks to current.tasks),
        "Projects" to (incoming.projects to current.projects),
        "Habits" to (incoming.habits to current.habits),
        "Habit check-ins" to (incoming.habitEntries to current.habitEntries),
        "Focus sessions" to (incoming.focusSessions to current.focusSessions),
    )
    Column {
        Row {
            Spacer(Modifier.weight(1f))
            Text("Backup", style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink2, modifier = Modifier.width(64.dp))
            Text("Now", style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink2, modifier = Modifier.width(48.dp))
        }
        rows.forEach { (label, counts) ->
            Row {
                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("${counts.first}", style = MaterialTheme.typography.bodyMedium.merge(Tabular), modifier = Modifier.width(64.dp))
                Text("${counts.second}", style = MaterialTheme.typography.bodyMedium.merge(Tabular), color = Dw.colors.ink2, modifier = Modifier.width(48.dp))
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, summary: String?, control: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            summary?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink2) }
        }
        control()
    }
}

@Composable
private fun <T> ChoiceRow(title: String, value: T, options: List<Pair<T, String>>, onChange: (T) -> Unit) =
    FieldRow(title) { ChoiceField(title, value, options, onChange) }

@Composable
private fun SwitchRow(title: String, summary: String?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) Dw.colors.ink else Dw.colors.ink3)
            summary?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink2) }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun ActionRow(title: String, summary: String?, action: String, enabled: Boolean = true, onClick: () -> Unit) =
    SettingRow(title, summary) { FilledTonalButton(onClick = onClick, enabled = enabled) { Text(action) } }

@Composable
private fun NumberRow(title: String, value: Int, min: Int, max: Int, step: Int, unit: String?, onChange: (Int) -> Unit) {
    SettingRow(title, null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange((value - step).coerceAtLeast(min)) }, enabled = value > min) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease $title")
            }
            Text(
                if (unit != null) "$value $unit" else "$value",
                style = MaterialTheme.typography.bodyLarge.merge(Tabular),
                modifier = Modifier.widthIn(min = 56.dp),
            )
            IconButton(onClick = { onChange((value + step).coerceAtMost(max)) }, enabled = value < max) {
                Icon(Icons.Filled.Add, contentDescription = "Increase $title")
            }
        }
    }
}

/** The core writes to a private file, which is then copied to wherever the person chose. */
private suspend fun exportTo(context: Context, export: Export, uri: Uri) {
    val directory = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(directory, "export.${export.extension}")
    try {
        file.delete()
        Dayweave.call(export.method, params("path" to file.path))
        withContext(Dispatchers.IO) {
            val output = context.contentResolver.openOutputStream(uri, "wt") ?: throw java.io.IOException("No output")
            output.use { stream -> file.inputStream().use { it.copyTo(stream) } }
        }
    } catch (error: java.io.IOException) {
        throw DayweaveException("io", "The file couldn't be saved there")
    } finally {
        file.delete()
    }
}

/** Copies a chosen file somewhere the core can read it. */
private fun stage(context: Context, uri: Uri): File {
    val directory = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
    val file = File(directory, "backup")
    val input = context.contentResolver.openInputStream(uri) ?: throw java.io.IOException("No input")
    input.use { stream -> file.outputStream().use { stream.copyTo(it) } }
    return file
}
