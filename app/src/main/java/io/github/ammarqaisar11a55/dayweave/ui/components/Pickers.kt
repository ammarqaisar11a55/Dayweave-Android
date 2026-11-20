package io.github.ammarqaisar11a55.dayweave.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.ammarqaisar11a55.dayweave.util.formatShortDate
import io.github.ammarqaisar11a55.dayweave.util.formatTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** A field that shows a date and opens the calendar picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, value: String?, onChange: (String?) -> Unit, modifier: Modifier = Modifier, clearable: Boolean = true) {
    var open by remember { mutableStateOf(false) }
    PickerButton(
        icon = Icons.Filled.CalendarMonth,
        text = value?.let(::formatShortDate) ?: label,
        description = if (value != null) "$label: ${formatShortDate(value)}" else label,
        onClick = { open = true },
        onClear = if (clearable && value != null) ({ onChange(null) }) else null,
        modifier = modifier,
    )
    if (open) {
        // The picker works in UTC midnights, so dates convert through UTC to stay on the same day.
        val initial = value?.let { LocalDate.parse(it).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())
                    }
                    open = false
                }) { Text("Choose") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(
    label: String,
    value: String?,
    clock24: Boolean,
    onChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    clearable: Boolean = true,
) {
    var open by remember { mutableStateOf(false) }
    PickerButton(
        icon = Icons.Filled.Schedule,
        text = value?.let { formatTime(it, clock24) } ?: label,
        description = if (value != null) "$label: ${formatTime(value, clock24)}" else label,
        onClick = { open = true },
        onClear = if (clearable && value != null) ({ onChange(null) }) else null,
        modifier = modifier,
        enabled = enabled,
    )
    if (open) {
        val initial = value?.let(LocalTime::parse) ?: LocalTime.of(9, 0)
        val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = clock24)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onChange("%02d:%02d".format(state.hour, state.minute))
                    open = false
                }) { Text("Choose") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        )
    }
}

/**
 * How fields draw. Standalone fields are tonal buttons. Inside a [FieldRow] the row's label already says
 * what the field is, so the value sits quietly at the end of the row instead.
 */
val LocalInlineFields = staticCompositionLocalOf { false }

/** A labelled row of a grouped list holding a field, like a settings row. */
@Composable
fun FieldRow(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(end = 12.dp))
        CompositionLocalProvider(LocalInlineFields provides true) { content() }
    }
}

@Composable
private fun InlineValue(text: String, set: Boolean, enabled: Boolean, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                !enabled || !set -> Dw.colors.ink3
                else -> Dw.colors.ink
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 10.dp),
        )
        trailing()
    }
}

@Composable
private fun PickerButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    description: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)?,
    modifier: Modifier,
    enabled: Boolean = true,
) {
    if (LocalInlineFields.current) {
        InlineValue(text, set = onClear != null || description != text, enabled = enabled, onClick = onClick) {
            if (onClear != null) {
                IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear $description", tint = Dw.colors.ink3, modifier = Modifier.size(18.dp))
                }
            }
        }
        return
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        FilledTonalButton(onClick = onClick, enabled = enabled, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
        }
        if (onClear != null) {
            IconButton(onClick = onClear) { Icon(Icons.Filled.Close, contentDescription = "Clear $description") }
        }
    }
}

/** A labelled choice from a short list, shown as a button with a menu. */
@Composable
fun <T> ChoiceField(label: String, value: T, options: List<Pair<T, String>>, onChange: (T) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == value }?.second ?: label
    val inline = LocalInlineFields.current
    Box(if (inline) Modifier else modifier) {
        if (inline) {
            InlineValue(current, set = true, enabled = true, onClick = { open = true }) {
                Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = Dw.colors.ink3, modifier = Modifier.padding(end = 8.dp).size(18.dp))
            }
        } else {
            FilledTonalButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(current, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (option, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = {
                    onChange(option)
                    open = false
                })
            }
        }
    }
}
