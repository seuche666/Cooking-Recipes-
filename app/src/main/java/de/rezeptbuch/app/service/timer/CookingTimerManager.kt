package de.rezeptbuch.app.service.timer

import android.content.Context
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class CookingTimer(
    val id: String,
    val recipeId: Long,
    val recipeTitle: String,
    val stepIndex: Int,
    val durationMs: Long,
    /** Ende auf Basis von [SystemClock.elapsedRealtime] – unabhängig von Uhrzeit-Änderungen. */
    val endAtElapsed: Long,
    val finished: Boolean = false,
) {
    fun remainingMs(nowElapsed: Long = SystemClock.elapsedRealtime()): Long =
        (endAtElapsed - nowElapsed).coerceAtLeast(0)

    val label: String get() = "Schritt ${stepIndex + 1} · $recipeTitle"

    /** Stabile Notification-ID pro Timer. */
    val notificationId: Int get() = 2000 + (id.hashCode() and 0x0FFFFFFF) % 100_000

    companion object {
        fun idFor(recipeId: Long, stepIndex: Int) = "$recipeId-$stepIndex"
    }
}

/**
 * Hält den Zustand aller laufenden Koch-Timer prozessweit. Der [CookingTimerService]
 * sorgt dafür, dass der Prozess im Hintergrund weiterläuft und bei Ablauf alarmiert.
 */
@Singleton
class CookingTimerManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _timers = MutableStateFlow<Map<String, CookingTimer>>(emptyMap())
    val timers: StateFlow<Map<String, CookingTimer>> = _timers.asStateFlow()

    fun start(recipeId: Long, recipeTitle: String, stepIndex: Int, durationSeconds: Int) {
        val id = CookingTimer.idFor(recipeId, stepIndex)
        val durationMs = durationSeconds * 1000L
        val timer = CookingTimer(
            id = id,
            recipeId = recipeId,
            recipeTitle = recipeTitle,
            stepIndex = stepIndex,
            durationMs = durationMs,
            endAtElapsed = SystemClock.elapsedRealtime() + durationMs,
        )
        NotificationManagerCompat.from(context).cancel(timer.notificationId)
        _timers.update { it + (id to timer) }
        ContextCompat.startForegroundService(context, CookingTimerService.startIntent(context))
    }

    /** Laufenden Timer abbrechen oder abgelaufenen Timer quittieren. */
    fun cancel(id: String) {
        val timer = _timers.value[id] ?: return
        _timers.update { it - id }
        NotificationManagerCompat.from(context).cancel(timer.notificationId)
    }

    internal fun markFinished(id: String) {
        _timers.update { map -> map[id]?.let { map + (id to it.copy(finished = true)) } ?: map }
    }
}
