package de.rezeptbuch.app.service.timer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import de.rezeptbuch.app.MainActivity
import de.rezeptbuch.app.R
import de.rezeptbuch.app.domain.portion.formatTimer

object TimerNotifications {
    const val CHANNEL_RUNNING = "timer_running"
    const val CHANNEL_ALARM = "timer_alarm"
    const val ONGOING_ID = 1001

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val running = NotificationChannel(
            CHANNEL_RUNNING,
            context.getString(R.string.channel_timer_running),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) }

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarm = NotificationChannel(
            CHANNEL_ALARM,
            context.getString(R.string.channel_timer_alarm),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            setSound(
                alarmSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)
        }
        manager.createNotificationChannels(listOf(running, alarm))
    }

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun ongoing(context: Context, running: List<CookingTimer>): Notification {
        val next = running.minByOrNull { it.endAtElapsed }
        val builder = NotificationCompat.Builder(context, CHANNEL_RUNNING)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentIntent(openAppIntent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (next == null) {
            return builder.setContentTitle(context.getString(R.string.timer_notification_idle)).build()
        }
        val remaining = next.remainingMs()
        builder
            .setContentTitle(next.label)
            .setContentText(
                if (running.size > 1) context.getString(R.string.timer_notification_more, running.size - 1)
                else context.getString(R.string.timer_notification_running),
            )
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(System.currentTimeMillis() + remaining)
            .setShowWhen(true)
            .addAction(
                0,
                context.getString(R.string.timer_cancel),
                TimerActionReceiver.cancelIntent(context, next.id),
            )
        return builder.build()
    }

    @SuppressLint("MissingPermission") // Berechtigung wird oben geprüft
    fun notifyFinished(context: Context, timer: CookingTimer) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= 33
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(context.getString(R.string.timer_finished_title))
            .setContentText("${timer.label} (${formatTimer(timer.durationMs / 1000)})")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .setDeleteIntent(TimerActionReceiver.cancelIntent(context, timer.id))
            .addAction(0, context.getString(R.string.timer_stop), TimerActionReceiver.cancelIntent(context, timer.id))
            .build()
            .apply { flags = flags or Notification.FLAG_INSISTENT } // Ton wiederholen, bis quittiert
        NotificationManagerCompat.from(context).notify(timer.notificationId, notification)
    }
}
