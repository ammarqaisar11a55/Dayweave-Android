package io.github.ammarqaisar11a55.dayweave.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.ammarqaisar11a55.dayweave.BuildConfig
import io.github.ammarqaisar11a55.dayweave.ui.components.LocalActions
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Where an update stands. One instance is shared by the automatic check and Settings. */
sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data class Available(val release: Release) : UpdateStatus
    data class Downloading(val release: Release, val progress: Float) : UpdateStatus
    data class Ready(val release: Release, val file: File) : UpdateStatus
}

object Updates {
    private val state = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = state.asStateFlow()

    /** Checks GitHub. Returns a message for the person when they asked, and stays quiet otherwise. */
    suspend fun check(context: Context, manual: Boolean): String? {
        if (state.value !is UpdateStatus.Idle) return null
        state.value = UpdateStatus.Checking
        return try {
            val release = Updater.check()
            UpdatePreferences(context).lastCheck = System.currentTimeMillis()
            state.value = release?.let { UpdateStatus.Available(it) } ?: UpdateStatus.Idle
            if (release == null && manual) "Dayweave ${BuildConfig.VERSION_NAME.removeSuffix("-debug")} is the latest version" else null
        } catch (error: UpdateException) {
            state.value = UpdateStatus.Idle
            if (manual) error.message else null
        }
    }

    fun download(context: Context, scope: CoroutineScope, release: Release, report: (String) -> Unit) {
        state.value = UpdateStatus.Downloading(release, 0f)
        scope.launch {
            try {
                val file = Updater.download(context, release) { progress ->
                    state.value = UpdateStatus.Downloading(release, progress.coerceIn(0f, 1f))
                }
                state.value = UpdateStatus.Ready(release, file)
            } catch (error: UpdateException) {
                state.value = UpdateStatus.Idle
                report(error.message ?: "The update couldn't be downloaded")
            }
        }
    }

    fun dismiss() {
        if (state.value !is UpdateStatus.Downloading) state.value = UpdateStatus.Idle
    }
}

/** Checks for an update at most once a day, and shows the update dialog whenever one is waiting. */
@Composable
fun UpdateCheck() {
    val context = LocalContext.current
    val actions = LocalActions.current
    val scope = rememberCoroutineScope()
    val status by Updates.status.collectAsState()

    LaunchedEffect(Unit) {
        if (UpdatePreferences(context).dueForCheck()) Updates.check(context, manual = false)
    }
    // Installing needs a permission granted in system settings, so it is read again on every return.
    var resumed by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { resumed++ } }

    when (val current = status) {
        is UpdateStatus.Available -> UpdateDialog(
            current.release,
            body = { Notes(current.release) },
            confirm = "Download",
            onConfirm = { Updates.download(context, scope, current.release, actions::say) },
            dismiss = "Later",
        )
        is UpdateStatus.Downloading -> UpdateDialog(
            current.release,
            body = {
                Text("Downloading… ${Math.round(current.progress * 100)}%", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { current.progress }, modifier = Modifier.fillMaxWidth())
            },
            confirm = null,
            onConfirm = {},
            dismiss = null,
        )
        is UpdateStatus.Ready -> {
            val allowed = remember(resumed) { Updater.canInstall(context) }
            UpdateDialog(
                current.release,
                body = {
                    Text(
                        if (allowed) {
                            "The download is verified. Android will ask you to confirm the update, and your data stays as it is."
                        } else {
                            "To install updates from inside Dayweave, allow it to install apps, then come back and press Install."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirm = if (allowed) "Install" else "Allow",
                onConfirm = {
                    if (allowed) {
                        runCatching { Updater.install(context, current.file) }
                            .onFailure { actions.say("Android couldn't start the update") }
                    } else {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")),
                        )
                    }
                },
                dismiss = "Later",
            )
        }
        else -> Unit
    }
}

@Composable
private fun Notes(release: Release) {
    Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
        Text("You have ${BuildConfig.VERSION_NAME.removeSuffix("-debug")}.", style = MaterialTheme.typography.bodyMedium, color = Dw.colors.ink2)
        if (release.notes.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(release.notes.trim(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun UpdateDialog(
    release: Release,
    body: @Composable () -> Unit,
    confirm: String?,
    onConfirm: () -> Unit,
    dismiss: String?,
) {
    AlertDialog(
        onDismissRequest = { if (dismiss != null) Updates.dismiss() },
        title = { Text("Dayweave ${release.version} is available") },
        text = { Column { body() } },
        confirmButton = {
            if (confirm != null) TextButton(onClick = onConfirm) { Text(confirm) }
        },
        dismissButton = { if (dismiss != null) TextButton(onClick = Updates::dismiss) { Text(dismiss) } },
    )
}
