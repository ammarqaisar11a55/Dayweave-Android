package io.github.ammarqaisar11a55.dayweave.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import io.github.ammarqaisar11a55.dayweave.core.Dayweave
import io.github.ammarqaisar11a55.dayweave.core.SyncStatus
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw
import io.github.ammarqaisar11a55.dayweave.util.formatInstant

/**
 * Syncs now, from the top bar. Shown only once signed in. A number counts changes waiting to be sent,
 * and a warning icon replaces the usual one when the last sync couldn't finish.
 */
@Composable
fun SyncAction() {
    val status = rememberRpc<SyncStatus>("sync.status") ?: return
    if (!status.signedIn) return
    val actions = LocalActions.current
    var syncing by remember { mutableStateOf(false) }
    val spin by rememberInfiniteTransition(label = "sync").animateFloat(
        initialValue = 0f,
        targetValue = if (syncing) -360f else 0f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "sync rotation",
    )
    val description = when {
        syncing -> "Syncing"
        status.lastError != null -> "Sync now. Last sync didn't finish: ${status.lastError}"
        status.pending > 0 -> "Sync now. ${status.pending} changes waiting"
        status.lastSyncAt != null -> "Sync now. Last synced ${formatInstant(status.lastSyncAt)}"
        else -> "Sync now"
    }

    IconButton(
        enabled = !syncing,
        onClick = {
            syncing = true
            actions.report {
                try {
                    Dayweave.mutate("sync.run")
                    null
                } finally {
                    syncing = false
                }
            }
        },
    ) {
        BadgedBox(badge = {
            if (status.pending > 0 && !syncing && status.lastError == null) {
                Badge { Text(if (status.pending > 99) "99+" else "${status.pending}") }
            }
        }) {
            if (status.lastError != null && !syncing) {
                Icon(Icons.Filled.SyncProblem, contentDescription = description, tint = Dw.colors.warning)
            } else {
                Icon(Icons.Filled.Sync, contentDescription = description, modifier = Modifier.rotate(spin))
            }
        }
    }
}
