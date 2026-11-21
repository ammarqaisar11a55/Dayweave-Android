package io.github.ammarqaisar11a55.dayweave.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.ammarqaisar11a55.dayweave.core.Dayweave
import io.github.ammarqaisar11a55.dayweave.core.SyncStatus
import io.github.ammarqaisar11a55.dayweave.core.params
import io.github.ammarqaisar11a55.dayweave.ui.components.GroupedList
import io.github.ammarqaisar11a55.dayweave.ui.components.LocalActions
import io.github.ammarqaisar11a55.dayweave.ui.components.RowDivider
import io.github.ammarqaisar11a55.dayweave.ui.components.SectionHeader
import io.github.ammarqaisar11a55.dayweave.ui.components.rememberRpc
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw
import io.github.ammarqaisar11a55.dayweave.util.formatInstant

/** The public Dayweave sync server. Anyone running their own can enter its address instead. */
const val DefaultSyncServer = "https://dayweave-server.vercel.app"

private fun summary(status: SyncStatus): String = when {
    status.pending > 0 -> "${status.pending} ${if (status.pending == 1) "change" else "changes"} waiting to sync"
    status.lastSyncAt != null -> "Up to date. Last synced ${formatInstant(status.lastSyncAt)}"
    else -> "Not synced yet"
}

@Composable
fun SyncSection() {
    val status = rememberRpc<SyncStatus>("sync.status") ?: return
    SectionHeader("Sync")
    GroupedList {
        if (status.signedIn) SignedIn(status) else SignInForm(status)
    }
}

@Composable
private fun SignInForm(status: SyncStatus) {
    val actions = LocalActions.current
    var server by rememberSaveable { mutableStateOf(status.server ?: DefaultSyncServer) }
    var customServer by rememberSaveable { mutableStateOf(server != DefaultSyncServer) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val ready = server.isNotBlank() && email.isNotBlank() && password.isNotEmpty() && !busy

    fun submit(create: Boolean) {
        busy = true
        actions.report {
            try {
                Dayweave.mutate(
                    if (create) "sync.signUp" else "sync.signIn",
                    params("server" to server, "email" to email, "password" to password, "device" to Build.MODEL),
                )
                if (create) "Account created. Syncing is on" else "Signed in. Syncing is on"
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Dayweave works fully on this phone. Sign in to keep your tasks and habits in step with your computer and other devices. Changes made offline sync when you're back online.",
            style = MaterialTheme.typography.bodyMedium, color = Dw.colors.ink2,
        )
        if (customServer) {
            OutlinedTextField(
                value = server, onValueChange = { server = it }, label = { Text("Server address") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!customServer) {
                TextButton(onClick = { customServer = true }) { Text("Use a different server") }
            }
            Spacer(Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { submit(create = true) }, enabled = ready, modifier = Modifier.weight(1f)) {
                Text("Create account")
            }
            Button(onClick = { submit(create = false) }, enabled = ready, modifier = Modifier.weight(1f)) {
                Text("Sign in")
            }
        }
    }
}

@Composable
private fun SignedIn(status: SyncStatus) {
    val actions = LocalActions.current
    var syncing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Syncing as ${status.email ?: "you"}", style = MaterialTheme.typography.bodyLarge)
        Text(summary(status), style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink2)
        Text(status.server.orEmpty(), style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink3)
    }
    status.lastError?.let { error ->
        Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.CloudOff, contentDescription = null, tint = Dw.colors.warning, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(error, style = MaterialTheme.typography.labelSmall, color = Dw.colors.warning)
        }
    }
    Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
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
            enabled = !syncing,
            modifier = Modifier.weight(1f),
        ) { Text(if (syncing) "Syncing…" else "Sync now") }
        FilledTonalButton(
            onClick = { actions.run("Signed out. Syncing is off") { Dayweave.mutate("sync.signOut") } },
            modifier = Modifier.weight(1f),
        ) { Text("Sign out") }
    }
    RowDivider()
    TextButton(onClick = { deleting = true }, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text("Delete account from the server", color = Dw.colors.danger)
    }
    if (deleting) DeleteAccountDialog(onClose = { deleting = false })
}

@Composable
private fun DeleteAccountDialog(onClose: () -> Unit) {
    val actions = LocalActions.current
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Delete your sync account?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Everything stored on the server for this account is deleted, and every device stops syncing. The data on this phone stays as it is.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = password.isNotEmpty(), onClick = {
                actions.run("Account deleted from the server. Your data here is unchanged") {
                    Dayweave.mutate("sync.deleteAccount", params("password" to password))
                }
                onClose()
            }) { Text("Delete account", color = Dw.colors.danger) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}
