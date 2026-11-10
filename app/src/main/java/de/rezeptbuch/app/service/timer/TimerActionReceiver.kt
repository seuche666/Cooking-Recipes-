package de.rezeptbuch.app.service.timer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Verarbeitet „Abbrechen“/„Stopp“ aus den Timer-Benachrichtigungen. */
@AndroidEntryPoint
class TimerActionReceiver : BroadcastReceiver() {

    @Inject lateinit var timerManager: CookingTimerManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CANCEL) {
            intent.getStringExtra(EXTRA_TIMER_ID)?.let(timerManager::cancel)
        }
    }

    companion object {
        private const val ACTION_CANCEL = "de.rezeptbuch.app.action.CANCEL_TIMER"
        private const val EXTRA_TIMER_ID = "timer_id"

        fun cancelIntent(context: Context, timerId: String): PendingIntent = PendingIntent.getBroadcast(
            context,
            timerId.hashCode(),
            Intent(context, TimerActionReceiver::class.java)
                .setAction(ACTION_CANCEL)
                .putExtra(EXTRA_TIMER_ID, timerId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
