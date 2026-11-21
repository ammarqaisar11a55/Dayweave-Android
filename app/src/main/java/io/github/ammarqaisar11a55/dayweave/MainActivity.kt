package io.github.ammarqaisar11a55.dayweave

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import kotlin.concurrent.thread
import io.github.ammarqaisar11a55.dayweave.notify.Notifications
import io.github.ammarqaisar11a55.dayweave.notify.SyncWork
import io.github.ammarqaisar11a55.dayweave.ui.App
import io.github.ammarqaisar11a55.dayweave.ui.Destination

class MainActivity : ComponentActivity() {
    /** Where a tapped notification or launcher shortcut wants the app to open. */
    private val pending = mutableStateOf<Destination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pending.value = destinationOf(intent)
        setContent { App(pending.value) { pending.value = null } }
    }

    /** Every time Dayweave comes to the front, it fetches other devices' changes if it can. */
    override fun onStart() {
        super.onStart()
        val context = applicationContext
        thread(name = "dayweave-sync-on-open") { SyncWork.requestSoon(context, delaySeconds = 0) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pending.value = destinationOf(intent)
    }

    private fun destinationOf(intent: Intent?): Destination? {
        intent ?: return null
        val task = intent.getLongExtra(Notifications.EXTRA_TASK, -1)
        val habit = intent.getLongExtra(Notifications.EXTRA_HABIT, -1)
        return when {
            intent.getStringExtra(Notifications.EXTRA_KIND) == "focus" -> Destination.Focus
            task >= 0 -> Destination.Task(task)
            habit >= 0 -> Destination.Habit(habit)
            intent.action == ACTION_NEW_TASK -> {
                // Lets the launcher rank the shortcut by how often it is used.
                getSystemService(android.content.pm.ShortcutManager::class.java)?.reportShortcutUsed("new_task")
                Destination.NewTask
            }
            else -> null
        }
    }

    companion object {
        const val ACTION_NEW_TASK = "io.github.ammarqaisar11a55.dayweave.NEW_TASK"
    }
}
