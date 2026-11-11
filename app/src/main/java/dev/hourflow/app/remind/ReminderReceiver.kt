package dev.hourflow.app.remind

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.hourflow.app.MainActivity
import dev.hourflow.app.R
import dev.hourflow.app.data.Category
import dev.hourflow.app.util.formatRange

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!ReminderScheduler.notificationsAllowed(context)) return

        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Your next block"
        val category = runCatching {
            Category.valueOf(intent.getStringExtra(ReminderScheduler.EXTRA_CATEGORY) ?: "")
        }.getOrDefault(Category.PERSONAL)
        val start = intent.getIntExtra(ReminderScheduler.EXTRA_START, -1)
        val end = intent.getIntExtra(ReminderScheduler.EXTRA_END, -1)
        val blockId = intent.getStringExtra(ReminderScheduler.EXTRA_BLOCK_ID) ?: title
        val notificationId = (blockId.hashCode() * 31 + start)

        ensureChannel(context)
        val text = if (start >= 0 && end >= 0) {
            "${formatRange(start, end)} · ${category.label} — starts in 5 minutes."
        } else {
            "Your routine block starts in 5 minutes."
        }

        val notification = buildNotification(
            context = context,
            title = "Coming up: $title",
            text = text,
            notificationId = notificationId,
            startMinute = start.takeIf { it >= 0 },
            endMinute = end.takeIf { it >= 0 }
        )

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    companion object {

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(ReminderScheduler.CHANNEL_ID) != null) return

            val channel = NotificationChannel(
                ReminderScheduler.CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.reminder_channel_description)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        fun buildNotification(
            context: Context,
            title: String,
            text: String,
            notificationId: Int,
            startMinute: Int?,
            endMinute: Int?
        ) = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(ReminderScheduler.smallIcon())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    notificationId,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        fun reminderText(startMinute: Int?, endMinute: Int?): String {
            if (startMinute == null || endMinute == null) return "Starts in 5 minutes."
            return "${formatRange(startMinute, endMinute)} — starts in 5 minutes."
        }
    }
}
