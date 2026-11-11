package dev.hourflow.app.remind

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.hourflow.app.R
import dev.hourflow.app.data.RoutineBlock
import dev.hourflow.app.util.formatRange
import java.time.ZoneId
import java.time.ZonedDateTime

object ReminderScheduler {

    const val EXTRA_TITLE = "block_title"
    const val EXTRA_CATEGORY = "block_category"
    const val EXTRA_START = "block_start"
    const val EXTRA_END = "block_end"
    const val EXTRA_BLOCK_ID = "block_id"
    const val CHANNEL_ID = "hourflow_reminders"
    const val TEST_NOTIFICATION_ID = 99001

    private const val PREFS = "hourflow_reminders"
    private const val KEY_CODES = "scheduled_codes"
    private const val LEAD_MINUTES = 5L

    fun reschedule(context: Context, blocks: List<RoutineBlock>, enabled: Boolean) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // Cancel all previously scheduled reminders first.
        val previous = prefs.getStringSet(KEY_CODES, emptySet())?.toSet().orEmpty()
        previous.forEach { raw ->
            val code = raw.toIntOrNull() ?: return@forEach
            val intent = Intent(context, ReminderReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                code,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
        }

        if (!enabled) {
            prefs.edit().putStringSet(KEY_CODES, emptySet()).apply()
            return
        }

        val newCodes = mutableSetOf<String>()
        val now = ZonedDateTime.now()
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        blocks.filter { it.reminder && it.durationMinutes > 0 }.forEach { block ->
            for (offset in 0..7) {
                val date = now.toLocalDate().plusDays(offset.toLong())
                if (!block.occursOn(date)) continue

                val start = date
                    .atTime(block.startMinute / 60, block.startMinute % 60)
                    .atZone(ZoneId.systemDefault())
                val trigger = start.minusMinutes(LEAD_MINUTES)
                if (!trigger.isAfter(now.plusSeconds(30))) continue

                val code = (block.id + date.toString()).hashCode()
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra(EXTRA_BLOCK_ID, block.id)
                    putExtra(EXTRA_TITLE, block.title)
                    putExtra(EXTRA_CATEGORY, block.category.name)
                    putExtra(EXTRA_START, block.startMinute)
                    putExtra(EXTRA_END, block.endMinute)
                }
                val pending = PendingIntent.getBroadcast(
                    context,
                    code,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (canExact) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        trigger.toInstant().toEpochMilli(),
                        pending
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        trigger.toInstant().toEpochMilli(),
                        pending
                    )
                }
                newCodes += code.toString()
            }
        }

        prefs.edit().putStringSet(KEY_CODES, newCodes).apply()
    }

    fun sendTest(context: Context): Boolean {
        ReminderReceiver.ensureChannel(context)
        if (!notificationsAllowed(context)) return false
        val notification = ReminderReceiver.buildNotification(
            context = context,
            title = "Reminder test",
            text = "HourFlow reminders are working. You'll get a quiet nudge 5 minutes before each block.",
            notificationId = TEST_NOTIFICATION_ID,
            startMinute = null,
            endMinute = null
        )
        return runCatching {
            NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
            true
        }.getOrDefault(false)
    }

    fun notificationsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun reminderDescription(block: RoutineBlock): String =
        "${formatRange(block.startMinute, block.endMinute)} · ${block.category.label}"

    fun smallIcon(): Int = R.drawable.ic_notification
}
