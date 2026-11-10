package de.rezeptbuch.app.service.timer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground-Service, der laufende Timer im Hintergrund überwacht.
 * Ein Partial-WakeLock verhindert, dass die CPU schläft, solange ein Timer läuft.
 * Bei Ablauf wird eine Benachrichtigung mit Alarmton gezeigt.
 */
@AndroidEntryPoint
class CookingTimerService : LifecycleService() {

    @Inject lateinit var timerManager: CookingTimerManager

    private var loop: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastNotifiedKey: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val running = timerManager.timers.value.values.filterNot { it.finished }
        ServiceCompat.startForeground(
            this,
            TimerNotifications.ONGOING_ID,
            TimerNotifications.ongoing(this, running),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        acquireWakeLock(running)
        if (loop?.isActive != true) loop = lifecycleScope.launch { runLoop() }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission") // Ohne Berechtigung wird die Benachrichtigung still verworfen
    private suspend fun runLoop() {
        while (lifecycleScope.isActive) {
            val now = SystemClock.elapsedRealtime()
            timerManager.timers.value.values
                .filter { !it.finished && it.endAtElapsed <= now }
                .forEach { timer ->
                    timerManager.markFinished(timer.id)
                    TimerNotifications.notifyFinished(this, timer)
                }

            val running = timerManager.timers.value.values.filterNot { it.finished }
            if (running.isEmpty()) {
                stopSelfCleanly()
                return
            }
            // Benachrichtigung nur aktualisieren, wenn sich die Menge der Timer geändert hat
            val key = running.sortedBy { it.endAtElapsed }.joinToString { "${it.id}@${it.endAtElapsed}" }
            if (key != lastNotifiedKey) {
                lastNotifiedKey = key
                NotificationManagerCompat.from(this)
                    .notify(TimerNotifications.ONGOING_ID, TimerNotifications.ongoing(this, running))
                acquireWakeLock(running)
            }
            delay(TICK_MS)
        }
    }

    private fun acquireWakeLock(running: List<CookingTimer>) {
        val longest = running.maxOfOrNull { it.remainingMs() } ?: return
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Rezeptbuch:CookingTimer")
            .apply {
                setReferenceCounted(false)
                acquire(longest + WAKELOCK_MARGIN_MS)
            }
    }

    private fun stopSelfCleanly() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    companion object {
        private const val TICK_MS = 500L
        private const val WAKELOCK_MARGIN_MS = 60_000L

        fun startIntent(context: Context) = Intent(context, CookingTimerService::class.java)
    }
}
