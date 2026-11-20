package io.github.ammarqaisar11a55.dayweave.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.ammarqaisar11a55.dayweave.core.Dayweave
import io.github.ammarqaisar11a55.dayweave.core.DayweaveException
import io.github.ammarqaisar11a55.dayweave.core.NoParams
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw
import io.github.ammarqaisar11a55.dayweave.util.DeadlineTone
import io.github.ammarqaisar11a55.dayweave.util.describeDeadline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import java.time.LocalDateTime

/** Loads data from the core and reloads it after any write. Keeps the last result while reloading. */
@Composable
inline fun <reified T> rememberRpc(method: String, params: JsonObject = NoParams): T? {
    val version by Dayweave.versions.collectAsState()
    var value by remember(method, params) { mutableStateOf<T?>(null) }
    LaunchedEffect(method, params, version) {
        value = runCatching { Dayweave.get<T>(method, params) }.getOrElse { value }
    }
    return value
}

/** Runs writes from the interface, and reports failures and undo offers in a snackbar. */
class Actions(private val scope: CoroutineScope, val snackbar: SnackbarHostState) {
    fun run(success: String? = null, undo: (suspend () -> Unit)? = null, block: suspend () -> Unit) =
        report(undo) {
            block()
            success
        }

    /** Runs [block], then shows the message it returns, if any. */
    fun report(undo: (suspend () -> Unit)? = null, block: suspend () -> String?) {
        scope.launch {
            val success = try {
                block()
            } catch (error: DayweaveException) {
                snackbar.showSnackbar(error.message ?: "Something went wrong", duration = SnackbarDuration.Long)
                return@launch
            }
            if (success != null) {
                val result = snackbar.showSnackbar(success, actionLabel = if (undo != null) "Undo" else null, duration = SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed && undo != null) {
                    runCatching { undo() }
                }
            }
        }
    }

    fun say(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }
}

val LocalActions = staticCompositionLocalOf<Actions> { error("Actions are provided by the app") }

/** One grouped surface whose rows share it and separate only by a hairline. */
@Composable
fun GroupedList(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Dw.colors.card),
        content = content,
    )
}

@Composable
fun RowDivider() = HorizontalDivider(color = Dw.colors.line, thickness = 1.dp)

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp)
            .height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        action?.invoke()
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String? = null, action: @Composable (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Dw.colors.sunken), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Dw.colors.ink2)
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (body != null) {
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Dw.colors.ink2)
        }
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

@Composable
fun DeadlineBadge(status: String, dueDate: String?, dueTime: String?, clock24: Boolean) {
    val (label, detail, tone) = describeDeadline(status, dueDate, dueTime, LocalDateTime.now(), clock24)
    if (label.isEmpty()) return
    val colors = Dw.colors
    val (foreground, background, icon) = when (tone) {
        DeadlineTone.Overdue -> Triple(colors.danger, colors.danger.copy(alpha = 0.12f), Icons.Filled.ErrorOutline)
        DeadlineTone.Today -> Triple(colors.accentText, colors.accentSoft, Icons.Filled.Schedule)
        DeadlineTone.Soon -> Triple(colors.warning, Color.Transparent, null)
        DeadlineTone.Later -> Triple(colors.ink2, Color.Transparent, null)
        DeadlineTone.Done -> Triple(colors.success, Color.Transparent, Icons.Filled.Check)
        DeadlineTone.None -> return
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .semantics { contentDescription = detail },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(13.dp))
            Spacer(Modifier.size(3.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

data class PriorityStyle(val label: String, val icon: ImageVector, val color: @Composable () -> Color)

val Priorities = linkedMapOf(
    "urgent" to PriorityStyle("Urgent", Icons.Filled.KeyboardDoubleArrowUp) { Dw.colors.danger },
    "high" to PriorityStyle("High", Icons.Filled.KeyboardArrowUp) { Dw.colors.warning },
    "medium" to PriorityStyle("Medium", Icons.Filled.Schedule) { Dw.colors.info },
    "low" to PriorityStyle("Low", Icons.Filled.KeyboardArrowDown) { Dw.colors.ink2 },
)

@Composable
fun PriorityMark(priority: String) {
    val style = Priorities[priority] ?: return
    if (priority == "medium") return
    Icon(style.icon, contentDescription = "${style.label} priority", tint = style.color(), modifier = Modifier.size(18.dp))
}

@Composable
fun ColorDot(color: Color, size: Int = 8) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(color))
}
