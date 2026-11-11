package dev.hourflow.app.remind

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.hourflow.app.data.AppRepository

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        runCatching {
            val repository = AppRepository(context)
            val state = repository.state.value
            ReminderScheduler.reschedule(
                context = context,
                blocks = state.blocks,
                enabled = state.settings.remindersEnabled && state.settings.onboarded
            )
        }
    }
}
