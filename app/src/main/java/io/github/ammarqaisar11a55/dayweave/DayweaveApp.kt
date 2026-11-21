package io.github.ammarqaisar11a55.dayweave

import android.app.Application
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import io.github.ammarqaisar11a55.dayweave.core.Dayweave
import io.github.ammarqaisar11a55.dayweave.notify.Notifications
import io.github.ammarqaisar11a55.dayweave.notify.Scheduler
import io.github.ammarqaisar11a55.dayweave.notify.SyncWork
import kotlin.concurrent.thread

class DayweaveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Dayweave.init(this)
        Notifications.createChannels(this)
        Scheduler.ensurePeriodic(this)
        SyncWork.ensurePeriodic(this)
        addShortcuts()
        // Syncing on open is MainActivity's job, so a reminder waking the app doesn't sync needlessly.
        thread(name = "dayweave-start") { Scheduler.runTick(this) }
    }

    /** Long-pressing the launcher icon offers "New task". Added in code so debug builds point at themselves. */
    private fun addShortcuts() {
        val intent = Intent(this, MainActivity::class.java).setAction(MainActivity.ACTION_NEW_TASK)
        val shortcut = ShortcutInfo.Builder(this, "new_task")
            .setShortLabel(getString(R.string.shortcut_new_task_short))
            .setLongLabel(getString(R.string.shortcut_new_task_long))
            .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_add))
            .setIntent(intent)
            .build()
        runCatching { getSystemService(ShortcutManager::class.java)?.dynamicShortcuts = listOf(shortcut) }
    }
}
